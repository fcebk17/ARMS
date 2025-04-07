package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class RepositoryUsageFinder {

    private final String basePath;
    private final Map<String, Map<String, List<String>>> microserviceMap;

    private final Map<String, Map<String, Set<String>>> microserviceToRepositoryMethodMap = new LinkedHashMap<>();

    public RepositoryUsageFinder(String basePath, Map<String, Map<String, List<String>>> microserviceMap) {
        this.basePath = basePath;
        this.microserviceMap = microserviceMap;
    }

    public Map<String, Map<String, Set<String>>> getMicroserviceToRepositoryMethodMap() {
        return microserviceToRepositoryMethodMap;
    }

    public void analyze() throws IOException {
        Map<String, Map<String, Map<String, List<String>>>> analysisResult = new LinkedHashMap<>();

        for (var microserviceEntry : microserviceMap.entrySet()) {
            String microserviceName = microserviceEntry.getKey();
            String microservicePath = basePath + "/" + microserviceName;
            Map<String, List<String>> serviceToRepoMap = microserviceEntry.getValue();

            Map<String, Map<String, List<String>>> serviceImplResult = new LinkedHashMap<>();
            Map<String, Set<String>> repoToMethods = new LinkedHashMap<>();

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
                    Set<String> calledMethods = repoToMethods.computeIfAbsent(repoFQCN, k -> new LinkedHashSet<>());

                    clazz.findAll(FieldDeclaration.class).forEach(field -> {
                        if (field.getVariables().stream().anyMatch(v -> v.getType().asString().equals(repoSimple))) {
                            usages.add("Field: " + field);
                        }
                    });

                    clazz.findAll(MethodDeclaration.class).forEach(method -> {
                        // 1. 建立變數名稱到型別的映射表
                        Map<String, String> varTypeMap = new HashMap<>();

                        // method parameters
                        method.getParameters().forEach(p -> varTypeMap.put(p.getNameAsString(), p.getTypeAsString()));

                        // method local variables
                        method.findAll(com.github.javaparser.ast.body.VariableDeclarator.class).forEach(v -> {
                            varTypeMap.put(v.getNameAsString(), v.getType().asString());
                        });

                        // class fields
                        clazz.findAll(FieldDeclaration.class).forEach(field -> {
                            field.getVariables().forEach(v -> {
                                varTypeMap.put(v.getNameAsString(), field.getElementType().asString());
                            });
                        });

                        // 2. 掃 method call 並輸出
                        method.findAll(MethodCallExpr.class).forEach(call -> {
                            call.getScope().ifPresent(scope -> {
                                if (scope instanceof NameExpr nameExpr &&
                                        nameExpr.getNameAsString().toLowerCase().contains(repoSimple.toLowerCase())) {

                                    // 推論參數型別
                                    List<String> paramWithTypes = new ArrayList<>();
                                    for (int i = 0; i < call.getArguments().size(); i++) {
                                        String argName = call.getArgument(i).toString();
                                        String type = varTypeMap.getOrDefault(argName, "UnknownType");
                                        paramWithTypes.add(type + " " + argName);
                                    }

                                    String methodCallWithArgs = call.getNameAsString() + "(" + String.join(", ", paramWithTypes) + ")";
                                    String usageLine = "Method: " + method.getName() + " -> " + nameExpr.getNameAsString() + "." + methodCallWithArgs;

                                    usages.add(usageLine);
                                    calledMethods.add(methodCallWithArgs);
                                }
                            });
                        });
                    });


                    repoUsageMap.put(repoFQCN, usages);
                }

                serviceImplResult.put(serviceClass, repoUsageMap);
            }

            analysisResult.put(microserviceName, serviceImplResult);
            microserviceToRepositoryMethodMap.put(microserviceName, repoToMethods); // ✅ 填入結果 map
        }

        // 原有 console 輸出保留
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

    // 👉 Demo 用 main()：你可以在單元測試或其他地方自行建立實例使用
    public static void main(String[] args) throws IOException {
        String base = "/home/popocorn/output";

        Map<String, Map<String, List<String>>> input = Map.of(
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

        RepositoryUsageFinder analyzer = new RepositoryUsageFinder(base, input);
        analyzer.analyze();
        System.out.println(analyzer.getMicroserviceToRepositoryMethodMap());
    }
}


