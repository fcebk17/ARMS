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

    // 🔧 Base 路徑（不要寫死）
    static String basePath = "/home/popocorn/output/AdminService";

    // ✅ Service-to-Repo 的對應關係
    static Map<String, List<String>> serviceToRepos = Map.of(
            "com.app.services.UserServiceImpl", List.of(
                    "com.app.repositories.UserRepo",
                    "com.app.repositories.RoleRepo",
                    "com.app.repositories.AddressRepo"
            )
    );

    public static void main(String[] args) throws IOException {
        Map<String, Map<String, List<String>>> result = new HashMap<>();

        for (var entry : serviceToRepos.entrySet()) {
            String serviceClassName = entry.getKey();
            List<String> repoClassNames = entry.getValue();

            String relativePath = serviceClassName.replace(".", "/") + ".java";

            // ✅ 使用遞迴方式尋找檔案
            File javaFile = findFileRecursively(basePath, relativePath);
            if (javaFile == null) {
                System.out.println("Not found: " + relativePath);
                continue;
            }

            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            Optional<ClassOrInterfaceDeclaration> clazz = cu.getClassByName(getSimpleName(serviceClassName));
            if (clazz.isEmpty()) continue;

            Map<String, List<String>> repoUsages = new HashMap<>();
            for (String repoClass : repoClassNames) {
                String repoSimpleName = getSimpleName(repoClass);
                repoUsages.put(repoClass, new ArrayList<>());

                // 掃欄位宣告
                clazz.get().findAll(FieldDeclaration.class).forEach(field -> {
                    if (field.getVariables().stream().anyMatch(v -> v.getType().asString().equals(repoSimpleName))) {
                        repoUsages.get(repoClass).add("Field: " + field);
                    }
                });

                // 掃方法中調用
                clazz.get().findAll(MethodDeclaration.class).forEach(method -> {
                    method.findAll(MethodCallExpr.class).forEach(call -> {
                        call.getScope().ifPresent(scope -> {
                            if (scope instanceof NameExpr nameExpr &&
                                    nameExpr.getNameAsString().toLowerCase().contains(repoSimpleName.toLowerCase())) {
                                repoUsages.get(repoClass).add("Method: " + method.getName() + " -> " + call.toString());
                            }
                        });
                    });
                });
            }

            result.put(serviceClassName, repoUsages);
        }

        // 印出分析結果
        for (var service : result.entrySet()) {
            System.out.println("Service: " + service.getKey());
            for (var repo : service.getValue().entrySet()) {
                System.out.println("  Repo: " + repo.getKey());
                for (String usage : repo.getValue()) {
                    System.out.println("    " + usage);
                }
            }
        }
    }

    // 📂 遞迴從 base 路徑中尋找相對路徑對應的 Java 檔
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

    // 工具方法：從 FQCN 取得簡單類名
    private static String getSimpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }
}
