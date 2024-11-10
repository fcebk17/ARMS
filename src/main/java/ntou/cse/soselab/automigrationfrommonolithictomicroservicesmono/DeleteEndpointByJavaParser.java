package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;

public class DeleteEndpointByJavaParser {



    public List<Map<String, Object>> getEndpointGroupMapping(String projectName, String concept) {
        List<Map<String, Object>> endpointGroupMappingList = new ArrayList<>();

        try (var mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("demoData");
            MongoCollection<Document> clusters = database.getCollection("clusters");

            // Query `endpointGroupMapping` array based on projectName and concept
            Document query = new Document("projectName", projectName).append("concept", concept);
            Document cluster = clusters.find(query).first();

            if (cluster != null) {

                if (cluster.containsKey("endpointGroupMapping")) {
                    // 使用 Document 類型來取得 endpointGroupMapping
                    List<Document> endpointGroupDocuments = cluster.getList("endpointGroupMapping", Document.class);

                    // 將 List<Document> 轉換為 List<Map<String, Object>>
                    for (Document doc : endpointGroupDocuments) {
                        Map<String, Object> map = new HashMap<>(doc);
                        endpointGroupMappingList.add(map);
                    }
                } else {
                    System.out.println("EndpointGroupMapping field does not exist in the document.");
                }
            } else {
                System.out.println("No document found for the given query.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endpointGroupMappingList;
    }

    public Map<String, List<String>> classifyEndpoints(List<Map<String, Object>> endpointGroupMappingList, List<String> groupNames) {
        Map<String, List<String>> groupEndpointsByKey = new LinkedHashMap<>();

        // initialize groupEndpointsByKey with empty lists
        for (String groupName : groupNames) {
            groupEndpointsByKey.put(groupName, new ArrayList<>());
        }

        // traverse endpointGroupMappingList and classify endpoints base on `service` value
        for (Map<String, Object> endpoint : endpointGroupMappingList) {
            int serviceIndex = (int) endpoint.get("service") - 1;
            String endpointName = (String) endpoint.get("endpoint");

            String groupName = groupNames.get(serviceIndex);
            groupEndpointsByKey.get(groupName).add(endpointName);
        }

        return groupEndpointsByKey;
    }

    public void RestfulMethodRemoval(Map<String, List<String>> groupEndpointsByKey) {
        for (String key : groupEndpointsByKey.keySet()) {
            File projectDir = new File(key);
            removeMethodInControllerFolder(projectDir, groupEndpointsByKey.get(key));
        }
    }

    private void removeMethodInControllerFolder(File folder, List<String> keepMethods) {
        for (File file : folder.listFiles()) {
            // recusively folder till find the controller file
            if (file.isDirectory()) {
                removeMethodInControllerFolder(file, keepMethods);
            } else if (file.getName().endsWith("Controller.java")) {
                try {
                    deleteEndpoint(file, keepMethods);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteEndpoint(File file, List<String> keepMethods) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(file);

        // Delete RESTful methods that are not in the `keepMethods` list
        cu.accept(new ModifierVisitor<Void>() {
            @Override
            public MethodDeclaration visit(MethodDeclaration method, Void arg) {
                if (!keepMethods.contains(method.getNameAsString()) &&
                        method.getAnnotations().stream().anyMatch(this::isRestfulAnnotation)) {
                    method.remove();
                    return null;
                }
                return (MethodDeclaration) super.visit(method, arg);
            }

            private boolean isRestfulAnnotation(AnnotationExpr annotation) {
                String name = annotation.getNameAsString();
                return name.equals("GetMapping") || name.equals("PostMapping") ||
                        name.equals("PutMapping") || name.equals("DeleteMapping") || name.equals("RequestMapping");
            }
        }, null);

        // check if the controller file has any methods left
        boolean hasMethods = cu.findAll(MethodDeclaration.class).stream()
                .anyMatch(method -> method.getAnnotations().stream().anyMatch(this::isRestfulAnnotation) ||
                        keepMethods.contains(method.getNameAsString()));

        if (!hasMethods) {
            // if no methods left, delete the file
            if (file.delete()) {
                System.out.println("Deleted file: " + file.getAbsolutePath());
            } else {
                System.out.println("Failed to delete file: " + file.getAbsolutePath());
            }
        } else {
            // if methods left, write the modified CompilationUnit back to the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(cu.toString());
            }
        }
    }

    private boolean isRestfulAnnotation(AnnotationExpr annotation) {
        // check if the annotation is a RESTful annotation
        String name = annotation.getNameAsString();
        return name.equals("GetMapping") || name.equals("PostMapping") ||
                name.equals("PutMapping") || name.equals("DeleteMapping") || name.equals("RequestMapping");
    }
}
