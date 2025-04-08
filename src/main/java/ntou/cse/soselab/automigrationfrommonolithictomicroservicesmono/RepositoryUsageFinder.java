package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class RepositoryUsageFinder {

    private final String baseDir;
    private final Map<String, Set<String>> serviceRepos;
    private static final Map<String, Set<String>> repositoryMethodUsage = new HashMap<>();

    private static final Map<String, Map<String, Map<String, String>>> repositoryMethodParameters = new HashMap<>();

    public RepositoryUsageFinder(String baseDir, Map<String, Set<String>> serviceRepos) {
        this.baseDir = baseDir;
        this.serviceRepos = serviceRepos;
    }

    public Map<String, Map<String, Map<String, String>>> getRepositoryMethodParameters() {
        return repositoryMethodParameters;
    }

    public void scan() throws IOException {
        // 掃描每個服務中的 repository 使用情況
        for (String serviceName : serviceRepos.keySet()) {
            String serviceDir = baseDir + "/" + serviceName;
            Set<String> repositories = serviceRepos.get(serviceName);

            for (String repository : repositories) {
                String repoName = repository.substring(repository.lastIndexOf('.') + 1);
                scanServiceForRepositoryUsage(serviceDir, repoName);
            }
        }
    }

    private static void scanServiceForRepositoryUsage(String serviceDir, String repositoryName) {
        // 初始化此 repository 的方法使用集合
        if (!repositoryMethodUsage.containsKey(repositoryName)) {
            repositoryMethodUsage.put(repositoryName, new HashSet<>());
        }

        // 取得所有 Java 檔案
        List<File> javaFiles = new ArrayList<>();
        File serviceRoot = new File(serviceDir);
        if (!serviceRoot.exists()) {
            System.err.println("服務目錄不存在: " + serviceDir);
            return;
        }

        collectJavaFiles(serviceRoot, javaFiles);
        // System.out.println("在 " + serviceDir + " 找到 " + javaFiles.size() + " 個 Java 檔案");

        // 掃描每個 Java 檔案
        for (File file : javaFiles) {
            scanFile(file, repositoryName);
        }
    }

    private static void collectJavaFiles(File dir, List<File> result) {
        if (dir.isFile() && dir.getName().endsWith(".java")) {
            result.add(dir);
        } else if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File sub : files) {
                    collectJavaFiles(sub, result);
                }
            }
        }
    }

    private static void scanFile(File file, String targetClass) {
        try (FileInputStream in = new FileInputStream(file)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            Set<String> repoVarNames = new HashSet<>();

            // === 收集所有 Repository 變數名稱 ===
            cu.findAll(FieldDeclaration.class).forEach(field -> {
                for (VariableDeclarator var : field.getVariables()) {
                    if (var.getTypeAsString().equals(targetClass)) {
                        repoVarNames.add(var.getNameAsString());
                    }
                }
            });

            // 如果此檔案沒有使用目標 Repository，則跳過後續分析
            if (repoVarNames.isEmpty()) {
                return;
            }

            // System.out.println("在檔案 " + file.getName() + " 中找到 " + targetClass + " 的使用");

            // === 收集所有變數對應型別（欄位 + 方法內部 + 方法參數） ===
            Map<String, String> nameToType = new HashMap<>();
            Map<String, String> genericCollectionTypes = new HashMap<>();

            // 欄位變數
            cu.findAll(FieldDeclaration.class).forEach(field -> {
                for (VariableDeclarator var : field.getVariables()) {
                    nameToType.put(var.getNameAsString(), var.getTypeAsString());
                    // 檢查是否為泛型集合
                    extractGenericType(var, genericCollectionTypes);
                }
            });

            // 方法內變數 + 方法參數
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                // 方法內變數
                method.getBody().ifPresent(body -> {
                    body.findAll(VariableDeclarator.class).forEach(var -> {
                        nameToType.put(var.getNameAsString(), var.getTypeAsString());
                        // 檢查是否為泛型集合
                        extractGenericType(var, genericCollectionTypes);
                    });
                });
                // 方法參數
                method.getParameters().forEach(param -> {
                    nameToType.put(param.getNameAsString(), param.getTypeAsString());
                });
            });

            // 處理 forEach 迴圈中的變數
            trackVariablesFromForEach(cu, nameToType, genericCollectionTypes);

            // 處理 Lambda 表達式中的變數
            trackVariablesFromLambda(cu, nameToType, genericCollectionTypes);

            // === 分析 Repository 的方法呼叫 ===
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                methodCall.getScope().ifPresent(scope -> {
                    if (scope instanceof NameExpr) {
                        NameExpr nameExpr = (NameExpr) scope;
                        String caller = nameExpr.getNameAsString();
                        if (repoVarNames.contains(caller)) {
                            String methodName = methodCall.getNameAsString();

                            // 處理參數
                            List<String> argsWithTypes = new ArrayList<>();
                            for (Expression arg : methodCall.getArguments()) {
                                if (arg instanceof NameExpr) {
                                    String argName = ((NameExpr) arg).getNameAsString();
                                    String type = resolveVariableType(argName, nameToType, genericCollectionTypes);
                                    argsWithTypes.add(type + " " + argName);

                                    // 新增到 repositoryMethodParameters
                                    repositoryMethodParameters
                                            .computeIfAbsent(targetClass, k -> new HashMap<>())
                                            .computeIfAbsent(methodName, k -> new HashMap<>())
                                            .put(type, argName);

                                } else {
                                    argsWithTypes.add(arg.toString());
                                }
                            }

                            String methodUsage = caller + "." + methodName + "(" +
                                    String.join(", ", argsWithTypes) + ")";
                            repositoryMethodUsage.get(targetClass).add(methodUsage);
                        }
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("解析檔案失敗 " + file.getPath() + ": " + e.getMessage());
        }
    }

    // 新增一個方法印出 repositoryMethodParameters
    public static void printRepositoryMethodParameters() {
        System.out.println("=== Repository 方法參數 ===");
        for (String repo : repositoryMethodParameters.keySet()) {
            System.out.println("\n" + repo + ":");
            Map<String, Map<String, String>> methods = repositoryMethodParameters.get(repo);
            for (String methodName : methods.keySet()) {
                System.out.println("  方法: " + methodName);
                Map<String, String> parameters = methods.get(methodName);
                for (String paramType : parameters.keySet()) {
                    System.out.println("    參數型態: " + paramType + ", 參數名稱: " + parameters.get(paramType));
                }
            }
        }
    }

    // 從變數宣告中提取泛型類型
    private static void extractGenericType(VariableDeclarator var, Map<String, String> genericCollectionTypes) {
        Type type = var.getType();
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType classType = type.asClassOrInterfaceType();
            // 檢查是否有泛型參數（例如 List<User>）
            if (classType.getTypeArguments().isPresent() &&
                    isCollectionType(classType.getNameAsString())) {
                String genericType = classType.getTypeArguments().get().get(0).toString();
                genericCollectionTypes.put(var.getNameAsString(), genericType);
            }
        }
    }

    // 判斷是否為集合類型
    private static boolean isCollectionType(String typeName) {
        return typeName.equals("List") || typeName.equals("ArrayList") ||
                typeName.equals("Set") || typeName.equals("HashSet") ||
                typeName.equals("Collection") || typeName.equals("HashMap") ||
                typeName.contains("Collection") || typeName.contains("LinkedList") ||
                typeName.contains("TreeSet") || typeName.contains("Map") ||
                typeName.contains("TreeMap") || typeName.contains("LinkedHashMap") ||
                typeName.contains("Queue") || typeName.contains("Deque") ||
                typeName.contains("PriorityQueue");
    }

    // 處理 forEach 迴圈中的變數類型
    private static void trackVariablesFromForEach(CompilationUnit cu,
                                                  Map<String, String> nameToType,
                                                  Map<String, String> genericCollectionTypes) {
        cu.findAll(ForEachStmt.class).forEach(forEach -> {
            // 獲取迴圈中宣告的變數名稱
            String variableName = forEach.getVariable().getVariables().get(0).getNameAsString();

            // 獲取被遍歷的集合名稱
            if (forEach.getIterable() instanceof NameExpr) {
                String collectionName = ((NameExpr) forEach.getIterable()).getNameAsString();
                // 如果我們知道這個集合的泛型類型，則將迴圈變數標記為該類型
                if (genericCollectionTypes.containsKey(collectionName)) {
                    String elementType = genericCollectionTypes.get(collectionName);
                    nameToType.put(variableName, elementType);
                }
            }
        });
    }

    // 處理 Lambda 表達式中的變數類型
    private static void trackVariablesFromLambda(CompilationUnit cu,
                                                 Map<String, String> nameToType,
                                                 Map<String, String> genericCollectionTypes) {
        // 找出所有的方法呼叫表達式
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            // 檢查是否是類似 collection.forEach() 的方法調用
            if (methodCall.getNameAsString().equals("forEach") ||
                    methodCall.getNameAsString().equals("map") ||
                    methodCall.getNameAsString().equals("filter") ||
                    methodCall.getNameAsString().equals("stream")) {

                methodCall.getScope().ifPresent(scope -> {
                    if (scope instanceof NameExpr) {
                        String collectionName = ((NameExpr) scope).getNameAsString();

                        // 檢查是否為已知的泛型集合
                        if (genericCollectionTypes.containsKey(collectionName)) {
                            String elementType = genericCollectionTypes.get(collectionName);

                            // 處理 forEach 的 Lambda 參數
                            if (!methodCall.getArguments().isEmpty() &&
                                    methodCall.getArgument(0) instanceof LambdaExpr) {

                                LambdaExpr lambdaExpr = (LambdaExpr) methodCall.getArgument(0);

                                // 獲取 Lambda 的參數名稱
                                if (!lambdaExpr.getParameters().isEmpty()) {
                                    String paramName = lambdaExpr.getParameter(0).getNameAsString();

                                    // 設定 Lambda 參數的類型
                                    nameToType.put(paramName, elementType);

                                    // 進一步處理 Lambda 內部的程式碼
                                    analyzeMethodCallsInLambda(lambdaExpr, nameToType);
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    // 分析 Lambda 表達式內部的方法調用
    private static void analyzeMethodCallsInLambda(LambdaExpr lambdaExpr, Map<String, String> nameToType) {
        if (lambdaExpr.getBody().isBlockStmt()) {
            BlockStmt block = lambdaExpr.getBody().asBlockStmt();

            // 收集 Lambda 內部宣告的本地變數
            block.findAll(VariableDeclarator.class).forEach(var -> {
                nameToType.put(var.getNameAsString(), var.getTypeAsString());
            });

            // 特別處理可能存在的賦值操作
            block.findAll(AssignExpr.class).forEach(assign -> {
                if (assign.getTarget() instanceof NameExpr) {
                    String targetName = ((NameExpr) assign.getTarget()).getNameAsString();
                    // 這裡可以進一步分析賦值的來源，例如方法調用、其他變數等
                }
            });
        }
    }

    // 解析變數類型時考慮泛型集合中的元素類型
    private static String resolveVariableType(String varName,
                                              Map<String, String> nameToType,
                                              Map<String, String> genericCollectionTypes) {
        // 直接從已知類型查詢
        if (nameToType.containsKey(varName)) {
            return nameToType.get(varName);
        }

        // 檢查是否是集合中的元素（來自泛型）
        for (Map.Entry<String, String> entry : genericCollectionTypes.entrySet()) {
            // 這裡可以實現更複雜的變數命名模式匹配
            // 例如，如果有一個叫 "users" 的集合，我們可以猜測 "user" 可能是它的元素
            if (varName.equals(getSingularForm(entry.getKey()))) {
                return entry.getValue();
            }
        }

        return "Unknown";
    }

    // 嘗試獲取集合名稱的單數形式（簡單實現）
    private static String getSingularForm(String collectionName) {
        if (collectionName.endsWith("s") && collectionName.length() > 1) {
            return collectionName.substring(0, collectionName.length() - 1);
        }
        return collectionName;
    }

    // 輸出 Repository 使用的方法
    public static void printRepositoryMethodUsage() {
        System.out.println("=== Repository 方法使用情況 ===");
        for (String repo : repositoryMethodUsage.keySet()) {
            Set<String> methods = repositoryMethodUsage.get(repo);
            if (!methods.isEmpty()) {
                System.out.println("\n" + repo + ":");
                methods.forEach(method -> System.out.println("  - " + method));
            } else {
                System.out.println("\n" + repo + ": 未發現方法使用");
            }
        }
    }
}