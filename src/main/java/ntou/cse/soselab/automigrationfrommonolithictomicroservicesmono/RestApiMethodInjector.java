package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RestApiMethodInjector {

    private final String controllerPath;
    private final String controllerName;
    private final Map<String, Map<String, String>> repositoryMethodsMap;

    public RestApiMethodInjector(String controllerPath, String controllerName, Map<String, Map<String, String>> repositoryMethodsMap) {
        this.controllerPath = controllerPath;
        this.controllerName = controllerName;
        this.repositoryMethodsMap = repositoryMethodsMap;
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
        boolean insertedConstructor = false;
        boolean insertedMethods = false;

        for (String line : originalLines) {
            modifiedLines.add(line);

            if (!insertedConstructor && line.contains("public class")) {
                String repositoryType = controllerName.replace("Controller", "");
                modifiedLines.add("");
                modifiedLines.add("    private final " + repositoryType + " repository;");
                modifiedLines.add("");
                modifiedLines.add("    public " + controllerName + "(" + repositoryType + " repository) {");
                modifiedLines.add("        this.repository = repository;");
                modifiedLines.add("    }");
                insertedConstructor = true;
            }

            if (!insertedMethods && line.trim().equals("}")) {
                for (Map.Entry<String, Map<String, String>> methodEntry : repositoryMethodsMap.entrySet()) {
                    String methodName = methodEntry.getKey();
                    Map<String, String> paramMap = methodEntry.getValue();

                    if (paramMap.size() != 1) continue; // 暫時只支援單參數

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
                insertedMethods = true;
            }
        }

        Files.write(Paths.get(controllerFilePath), modifiedLines);
        System.out.println("✅ Injected REST methods into: " + controllerFilePath);
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
}
