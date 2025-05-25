package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RestApiMethodInjector {

    private final String controllerPath;
    private final String controllerName;
    private final Map<String, Map<String, String>> repositoryMethodsMap;
    private final Map<String, String> importMap;

    public RestApiMethodInjector(String controllerPath,
                                 String controllerName,
                                 Map<String, Map<String, String>> repositoryMethodsMap,
                                 Map<String, String> importMap) {
        this.controllerPath = controllerPath;
        this.controllerName = controllerName;
        this.repositoryMethodsMap = repositoryMethodsMap;
        this.importMap = importMap;
    }

    public void inject() throws IOException {
        String controllerFilePath = Paths.get(controllerPath, controllerName + ".java").toString();
        File controllerFile = new File(controllerFilePath);
        if (!controllerFile.exists()) {
            System.out.println("Controller not found: " + controllerFilePath);
            return;
        }

        List<String> originalLines = Files.readAllLines(controllerFile.toPath());
        List<String> modifiedLines = new ArrayList<>();

        // 自動推論的 Spring 註解 imports
        Map<String, String> springAnnotationImports = Map.of(
                "RestController", "import org.springframework.web.bind.annotation.RestController;",
                "PostMapping", "import org.springframework.web.bind.annotation.PostMapping;",
                "GetMapping", "import org.springframework.web.bind.annotation.GetMapping;",
                "DeleteMapping", "import org.springframework.web.bind.annotation.DeleteMapping;",
                "RequestBody", "import org.springframework.web.bind.annotation.RequestBody;",
                "RequestParam", "import org.springframework.web.bind.annotation.RequestParam;"
        );

        // Java Standard Imports
        Map<String, String> standardJavaImports = Map.of(
                "Optional", "import java.util.Optional;"
        );

        Set<String> neededImports = new LinkedHashSet<>();
        String repositoryType = controllerName.replace("Controller", "");
        neededImports.add(repositoryType); // 需要加 repository 的 import

        for (Map.Entry<String, Map<String, String>> entry : repositoryMethodsMap.entrySet()) {
            String methodName = entry.getKey();
            Map<String, String> paramMap = entry.getValue();

            if (paramMap.size() != 1) continue;

            String fullParamType = paramMap.keySet().iterator().next();
            neededImports.addAll(extractTypesFromGeneric(fullParamType)); // 支援泛型（例如 Optional<Customer>）

            // 根據推論 HTTP method，加入對應註解 import
            String httpMethod = guessHttpMethod(methodName);
            switch (httpMethod) {
                case "GET":
                    neededImports.add("GetMapping");
                    neededImports.add("RequestParam");
                    break;
                case "POST":
                    neededImports.add("PostMapping");
                    neededImports.add("RequestBody");
                    break;
                case "DELETE":
                    neededImports.add("DeleteMapping");
                    neededImports.add("RequestBody");
                    break;
            }
        }

        boolean packageInserted = false;
        boolean constructorInserted = false;
        boolean methodsInserted = false;

        for (int i = 0; i < originalLines.size(); i++) {
            String line = originalLines.get(i);

            // 插入 import 區塊
            if (!packageInserted && line.startsWith("package ")) {
                modifiedLines.add(line);
                modifiedLines.add("");
                for (String key : neededImports) {
                    if (importMap.containsKey(key)) {
                        modifiedLines.add(importMap.get(key));
                    } else if (springAnnotationImports.containsKey(key)) {
                        modifiedLines.add(springAnnotationImports.get(key));
                    } else if (standardJavaImports.containsKey(key)) {
                        modifiedLines.add(standardJavaImports.get(key));
                    }
                }
                packageInserted = true;
                continue;
            }

            // 插入 constructor
            if (!constructorInserted && line.contains("public class")) {
                modifiedLines.add(line);
                modifiedLines.add("");
                modifiedLines.add("    private final " + repositoryType + " repository;");
                modifiedLines.add("");
                modifiedLines.add("    public " + controllerName + "(" + repositoryType + " repository) {");
                modifiedLines.add("        this.repository = repository;");
                modifiedLines.add("    }");
                constructorInserted = true;
                continue;
            }

            // 插入 REST API 方法
            if (!methodsInserted && line.trim().equals("}")) {
                for (Map.Entry<String, Map<String, String>> methodEntry : repositoryMethodsMap.entrySet()) {
                    String methodName = methodEntry.getKey();
                    Map<String, String> paramMap = methodEntry.getValue();

                    if (paramMap.size() != 1) continue;

                    String paramType = paramMap.keySet().iterator().next();
                    String paramName = paramMap.get(paramType);

                    String httpMethod = guessHttpMethod(methodName);
                    String annotation = buildAnnotation(httpMethod, methodName);

                    modifiedLines.add("");
                    modifiedLines.add("    " + annotation);
                    modifiedLines.add("    public void " + methodName + "(" + buildParamAnnotation(httpMethod, paramType, paramName) + ") {");
                    modifiedLines.add("        repository." + methodName + "(" + paramName + ");");
                    modifiedLines.add("    }");
                }
                methodsInserted = true;
                modifiedLines.add(line); // 最後加上 class 的 }
            } else {
                modifiedLines.add(line);
            }
        }

        Files.write(Paths.get(controllerFilePath), modifiedLines);
        System.out.println("Injected REST methods + imports into: " + controllerFilePath);
    }

    private String guessHttpMethod(String methodName) {
        if (methodName.startsWith("find")) return "GET";
        if (methodName.startsWith("save")) return "POST";
        if (methodName.startsWith("delete")) return "DELETE";
        return "POST";
    }

    private String buildAnnotation(String httpMethod, String methodName) {
        switch (httpMethod) {
            case "GET": return "@GetMapping(\"/" + methodName + "\")";
            case "POST": return "@PostMapping(\"/" + methodName + "\")";
            case "DELETE": return "@DeleteMapping(\"/" + methodName + "\")";
            default: return "@PostMapping(\"/" + methodName + "\")";
        }
    }

    private String buildParamAnnotation(String httpMethod, String paramType, String paramName) {
        if (httpMethod.equals("GET")) {
            return "@RequestParam " + paramType + " " + paramName;
        } else {
            return "@RequestBody " + paramType + " " + paramName;
        }
    }

    // ✅ 萃取泛型中的主類型，例如 Optional<Customer> → Optional, Customer
    private Set<String> extractTypesFromGeneric(String type) {
        Set<String> types = new LinkedHashSet<>();
        if (type.contains("<") && type.contains(">")) {
            String mainType = type.substring(0, type.indexOf("<")).trim();
            String innerType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">")).trim();
            types.add(mainType);
            types.add(innerType);
        } else {
            types.add(type.trim());
        }
        return types;
    }
}
