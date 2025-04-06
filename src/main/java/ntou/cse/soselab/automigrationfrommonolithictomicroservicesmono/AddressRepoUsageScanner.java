package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AddressRepoUsageScanner {

    private static final String TARGET_CLASS = "UserRepo";
    private static final Set<String> usedMethods = new HashSet<>();

    public static void main(String[] args) throws IOException {
        String baseDir = "/home/popocorn/output/AdminService";
        File root = new File(baseDir);
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(root, javaFiles);

        for (File file : javaFiles) {
            scanFile(file);
        }

        System.out.println("=== Methods used from " + TARGET_CLASS + " ===");
        usedMethods.forEach(System.out::println);
    }

    private static void collectJavaFiles(File dir, List<File> result) {
        if (dir.isFile() && dir.getName().endsWith(".java")) {
            result.add(dir);
        } else if (dir.isDirectory()) {
            for (File sub : Objects.requireNonNull(dir.listFiles())) {
                collectJavaFiles(sub, result);
            }
        }
    }

    private static void scanFile(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            Set<String> addressRepoNames = new HashSet<>();

            // === 收集所有 AddressRepo 的變數名稱 ===
            cu.findAll(FieldDeclaration.class).forEach(field -> {
                for (VariableDeclarator var : field.getVariables()) {
                    if (var.getTypeAsString().equals(TARGET_CLASS)) {
                        addressRepoNames.add(var.getNameAsString());
                    }
                }
            });

            // === 收集所有變數對應型別（欄位 + 方法內部 + 方法參數） ===
            Map<String, String> nameToType = new HashMap<>();

            // 欄位變數
            cu.findAll(FieldDeclaration.class).forEach(field -> {
                for (VariableDeclarator var : field.getVariables()) {
                    nameToType.put(var.getNameAsString(), var.getTypeAsString());
                }
            });

            // 方法內變數 + 方法參數
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                // 方法內變數
                method.getBody().ifPresent(body -> {
                    body.findAll(VariableDeclarator.class).forEach(var -> {
                        nameToType.put(var.getNameAsString(), var.getTypeAsString());
                    });
                });
                // 方法參數
                method.getParameters().forEach(param -> {
//                    nameToType.put(param.getNameAsString(), param.getTypeAsString().asString());
                    nameToType.put(param.getNameAsString(), param.getTypeAsString());
                });
            });

            // === 分析 AddressRepo 的方法呼叫 ===
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                methodCall.getScope().ifPresent(scope -> {
                    if (scope instanceof NameExpr) {
                        NameExpr nameExpr = (NameExpr) scope;
                        String caller = nameExpr.getNameAsString();
                        if (addressRepoNames.contains(caller)) {
                            String methodName = methodCall.getNameAsString();

                            // 處理參數
                            List<String> argsWithTypes = new ArrayList<>();
                            for (Expression arg : methodCall.getArguments()) {
                                if (arg instanceof NameExpr) {
                                    String argName = ((NameExpr) arg).getNameAsString();
                                    String type = nameToType.getOrDefault(argName, "Unknown");
                                    argsWithTypes.add(type + " " + argName);
                                } else {
                                    argsWithTypes.add(arg.toString());
                                }
                            }

                            String methodUsage = caller + "." + methodName + "(" +
                                    String.join(", ", argsWithTypes) + ")";
                            usedMethods.add(methodUsage);
                        }
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("Failed to parse " + file.getPath() + ": " + e.getMessage());
        }
    }
}
