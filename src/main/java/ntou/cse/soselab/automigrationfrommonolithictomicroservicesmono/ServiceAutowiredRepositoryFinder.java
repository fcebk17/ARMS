package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ServiceAutowiredRepositoryFinder {

    private final String rootDir;
    private final String serviceName;
    private final Map<String, String> classToFilePath = new HashMap<>();
    private final Map<String, Boolean> repositoryCache = new HashMap<>();
    private final Map<String, List<String>> autowiredRepositories = new LinkedHashMap<>();

    public ServiceAutowiredRepositoryFinder(String rootDir, String serviceName) {
        this.rootDir = rootDir;
        this.serviceName = serviceName;
    }

    public Map<String, List<String>> getAutowiredRepositories() {
        return autowiredRepositories;
    }

    public void scan() throws Exception {
        indexJavaFiles(new File(rootDir));
        String serviceFile = classToFilePath.get(serviceName);
        if (serviceFile == null) {
            System.err.println("Service class not found: " + serviceName);
            return;
        }

        System.out.println("Scanning Service: " + serviceName);
        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(serviceFile));

        List<String> repoInfo = new ArrayList<>();

        cu.findAll(FieldDeclaration.class).forEach(field -> {
            if (field.isAnnotationPresent("Autowired")) {
                String fieldType = field.getVariable(0).getType().asString();
                String fieldName = field.getVariable(0).getNameAsString();
                String repoFile = classToFilePath.get(fieldType);

                if (repoFile != null && isRepositoryClass(repoFile)) {
                    System.out.println("Autowired Repository Found: " + fieldName + " : " + fieldType);
                    repoInfo.add(fieldName);
                }
            }
        });

        autowiredRepositories.put(serviceName, repoInfo);
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
                    cu.getTypes().forEach(t -> {
                        String className = t.getNameAsString();
                        classToFilePath.put(className, f.getAbsolutePath());
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

    public static void main(String[] args) throws Exception {
        ServiceAutowiredRepositoryFinder scanner = new ServiceAutowiredRepositoryFinder(
                "/home/popocorn/output/AdminService", "AddressServiceImpl"
        );
        scanner.scan();
        System.out.println(scanner.getAutowiredRepositories());
    }
}
