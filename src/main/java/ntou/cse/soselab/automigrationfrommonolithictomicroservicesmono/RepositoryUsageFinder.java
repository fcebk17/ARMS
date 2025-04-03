package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class RepositoryUsageFinder {

    // ✅ 設定 basePath 為根目錄
    static String basePath = "/home/popocorn/output/";

    // ✅ 你的輸入資料結構
    static Map<String, Map<String, List<String>>> microserviceToServiceImplToRepositoryMap = Map.of(
            "AdminService", Map.of(
                    "com.app.services.UserServiceImpl", List.of(
                            "com.app.repositories.UserRepo",
                            "com.app.repositories.RoleRepo",
                            "com.app.repositories.AddressRepo"
                    ),
                    "com.app.services.AddressServiceImpl", List.of(
                            "com.app.repositories.AddressRepo",
                            "com.app.repositories.UserRepo"
                    )
            ),
            "CustomerService", Map.of(
                    "com.app.services.CartServiceImpl", List.of(
                            "com.app.repositories.CartRepo",
                            "com.app.repositories.ProductRepo",
                            "com.app.repositories.CartItemRepo"
                    ),
                    "com.app.services.CategoryServiceImpl", List.of("com.app.repositories.CategoryRepo"),
                    "com.app.services.OrderServiceImpl", List.of(
                            "com.app.repositories.CartRepo",
                            "com.app.repositories.OrderRepo",
                            "com.app.repositories.PaymentRepo",
                            "com.app.repositories.OrderItemRepo"
                    ),
                    "com.app.services.ProductServiceImpl", List.of(
                            "com.app.repositories.ProductRepo",
                            "com.app.repositories.CategoryRepo",
                            "com.app.repositories.CartRepo"
                    )
            )
    );

    public static void main(String[] args) throws IOException {
        Map<String, Map<String, Map<String, List<String>>>> analysisResult = new LinkedHashMap<>();

        for (var microserviceEntry : microserviceToServiceImplToRepositoryMap.entrySet()) {
            String microserviceName = microserviceEntry.getKey();
            String microservicePath = basePath + microserviceName;
            Map<String, List<String>> serviceToRepoMap = microserviceEntry.getValue();

            Map<String, Map<String, List<String>>> serviceImplResult = new LinkedHashMap<>();

            for (var serviceEntry : serviceToRepoMap.entrySet()) {
                String serviceClass = serviceEntry.getKey();
                List<String> repos = serviceEntry.getValue();

                String relativePath = serviceClass.replace(".", "/") + ".java";
                File serviceFile = findFileRecursively(microservicePath, relativePath);

                if (serviceFile == null) {
                    System.out.println("Not found: " + relativePath + " under " + microserviceName);
                    continue;
                }

                CompilationUnit cu = StaticJavaParser.parse(serviceFile);
                Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.getClassByName(getSimpleName(serviceClass));
                if (clazzOpt.isEmpty()) continue;

                ClassOrInterfaceDeclaration clazz = clazzOpt.get();
                Map<String, List<String>> repoUsageMap = new LinkedHashMap<>();

                for (String repoFQCN : repos) {
                    String repoSimple = getSimpleName(repoFQCN);
                    List<String> usages = new ArrayList<>();

                    // 欄位使用
                    clazz.findAll(FieldDeclaration.class).forEach(field -> {
                        if (field.getVariables().stream().anyMatch(v -> v.getType().asString().equals(repoSimple))) {
                            usages.add("Field: " + field);
                        }
                    });

                    // 方法調用
                    clazz.findAll(MethodDeclaration.class).forEach(method -> {
                        method.findAll(MethodCallExpr.class).forEach(call -> {
                            call.getScope().ifPresent(scope -> {
                                if (scope instanceof NameExpr nameExpr &&
                                        nameExpr.getNameAsString().toLowerCase().contains(repoSimple.toLowerCase())) {
                                    usages.add("Method: " + method.getName() + " -> " + call);
                                }
                            });
                        });
                    });

                    repoUsageMap.put(repoFQCN, usages);
                }

                serviceImplResult.put(serviceClass, repoUsageMap);
            }

            analysisResult.put(microserviceName, serviceImplResult);
        }

        // ✅ 印出結果
        for (var microservice : analysisResult.entrySet()) {
            System.out.println("Microservice: " + microservice.getKey());
            for (var service : microservice.getValue().entrySet()) {
                System.out.println("  ServiceImpl: " + service.getKey());
                for (var repo : service.getValue().entrySet()) {
                    System.out.println("    Repository: " + repo.getKey());
                    if (repo.getValue().isEmpty()) {
                        System.out.println("      (No usage found)");
                    }
                    for (String usage : repo.getValue()) {
                        System.out.println("      " + usage);
                    }
                }
            }
        }
    }

    // 🔍 遞迴尋找相對路徑檔案
    private static File findFileRecursively(String baseDir, String relativePath) throws IOException {
        Path start = Paths.get(baseDir);
        final String targetPath = relativePath.replace(File.separatorChar, '/');
        try (Stream<Path> stream = Files.walk(start)) {
            Optional<Path> result = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().replace(File.separatorChar, '/').endsWith(targetPath))
                    .findFirst();
            return result.map(Path::toFile).orElse(null);
        }
    }

    private static String getSimpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }
}

