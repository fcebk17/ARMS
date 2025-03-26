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

                if (repoFile != null && isRepositoryClass(repoFile)) {
                    String fieldName = field.getVariable(0).getNameAsString();
                    // System.out.println("Autowired Repository Found: " + fieldName + " : " + fullType);
                    repoInfo.add(fieldName);
                }
            }
        });

        String simpleServiceName = serviceFullName.substring(serviceFullName.lastIndexOf('.') + 1);
        autowiredRepositories.put(simpleServiceName, repoInfo);
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

//    public static void main(String[] args) throws Exception {
//        ServiceAutowiredRepositoryFinder scanner = new ServiceAutowiredRepositoryFinder(
//                "/home/popocorn/output/AdminService",
//                "com.app.services.AddressServiceImpl"
//        );
//        scanner.scan();
//        System.out.println(scanner.getAutowiredRepositories());
//    }
}
