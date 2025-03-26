package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ServiceAutowiredRepositoryFinder {

    private final String rootDir;
    private final String serviceFullName;
    private final Map<String, String> classToFilePath = new HashMap<>();
    private final Map<String, Boolean> repositoryCache = new HashMap<>();
    private final Map<String, List<String>> autowiredRepositories = new LinkedHashMap<>();

    public ServiceAutowiredRepositoryFinder(String rootDir, String serviceFullName) {
        this.rootDir = rootDir;
        this.serviceFullName = serviceFullName;
    }

    public Map<String, List<String>> getAutowiredRepositories() {
        return autowiredRepositories;
    }

    public void scan() throws Exception {
        indexJavaFiles(new File(rootDir));
        String serviceFile = classToFilePath.get(serviceFullName);
        if (serviceFile == null) {
            System.err.println("Service class not found: " + serviceFullName);
            return;
        }

        // System.out.println("Scanning Service: " + serviceFullName);
        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(serviceFile));

        // Build import map
        Map<String, String> importMap = new HashMap<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String full = imp.getNameAsString();
            String simple = full.substring(full.lastIndexOf('.') + 1);
            importMap.put(simple, full);
        }

        List<String> repoInfo = new ArrayList<>();

        cu.findAll(FieldDeclaration.class).forEach(field -> {
            if (field.isAnnotationPresent("Autowired")) {
                String fieldType = field.getVariable(0).getType().asString();
                String fullType = importMap.getOrDefault(fieldType, fieldType);
                String repoFile = classToFilePath.get(fullType);

                if (repoFile != null && isUsedRepositoryClass(repoFile)) {
                    repoInfo.add(fullType);
                }
            }
        });

        autowiredRepositories.put(serviceFullName, repoInfo);
    }

    private void indexJavaFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                indexJavaFiles(f);
            } else if (f.getName().endsWith(".java")) {
                try (FileInputStream in = new FileInputStream(f)) {
                    CompilationUnit cu = StaticJavaParser.parse(in);
                    String packageName = cu.getPackageDeclaration()
                            .map(pkg -> pkg.getNameAsString() + ".")
                            .orElse("");

                    cu.getTypes().forEach(t -> {
                        String fullClassName = packageName + t.getNameAsString();
                        classToFilePath.put(fullClassName, f.getAbsolutePath());
                    });
                } catch (Exception e) {
                    System.err.println("Parse error: " + f.getAbsolutePath());
                }
            }
        }
    }

    private boolean isRepositoryClass(String filePath) {
        if (repositoryCache.containsKey(filePath)) {
            return repositoryCache.get(filePath);
        }

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            boolean hasRepoAnnotation = cu.getTypes().stream()
                    .flatMap(t -> t.getAnnotations().stream())
                    .anyMatch(ann -> ann.getNameAsString().equals("Repository"));

            repositoryCache.put(filePath, hasRepoAnnotation);
            return hasRepoAnnotation;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isUsedRepositoryClass(String filePath) {
        if (repositoryCache.containsKey(filePath)) {
            return repositoryCache.get(filePath);
        }

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = StaticJavaParser.parse(in);

            // 1. 檢查是否有 @Repository 註解
            boolean hasRepoAnnotation = cu.getTypes().stream()
                    .flatMap(t -> t.getAnnotations().stream())
                    .anyMatch(ann -> ann.getNameAsString().equals("Repository"));

            if (!hasRepoAnnotation) {
                repositoryCache.put(filePath, false);
                return false;
            }

            // 2. 找出 class 名稱 → 再根據它找出變數名稱
            String className = cu.getTypes().get(0).getNameAsString();

            // 3. 找出 service 檔案
            String serviceFilePath = classToFilePath.get(serviceFullName);
            if (serviceFilePath == null) {
                repositoryCache.put(filePath, false);
                return false;
            }

            // 4. 找到所有 @Autowired 且類型為 className 的變數名稱
            try (FileInputStream serviceIn = new FileInputStream(serviceFilePath)) {
                CompilationUnit serviceCu = StaticJavaParser.parse(serviceIn);

                List<String> matchingFields = serviceCu.findAll(FieldDeclaration.class).stream()
                        .filter(f -> f.isAnnotationPresent("Autowired"))
                        .filter(f -> f.getElementType().asString().equals(className))
                        .flatMap(f -> f.getVariables().stream())
                        .map(v -> v.getNameAsString())
                        .toList();

                // 5. 檢查這些變數名稱是否出現在程式中
                Set<String> usedNames = new HashSet<>();
                serviceCu.findAll(com.github.javaparser.ast.expr.NameExpr.class).forEach(expr -> {
                    usedNames.add(expr.getNameAsString());
                });

                boolean isUsed = matchingFields.stream().anyMatch(usedNames::contains);

                repositoryCache.put(filePath, isUsed);
                return isUsed;
            }

        } catch (Exception e) {
            repositoryCache.put(filePath, false);
            return false;
        }
    }
}
