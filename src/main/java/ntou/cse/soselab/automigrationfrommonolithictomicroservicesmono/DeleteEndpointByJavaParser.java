package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
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

    public void classifyEndpoints(List<Map<String, Object>> endpointGroupMappingList, List<String> groupNames) {
        Map<String, List<String>> groupEndpoints = new LinkedHashMap<>();

        // initialize groupEndpoints with empty lists
        for (String groupName : groupNames) {
            groupEndpoints.put(groupName, new ArrayList<>());
        }

        // traverse endpointGroupMappingList and classify endpoints base on `service` value
        for (Map<String, Object> endpoint : endpointGroupMappingList) {
            int serviceIndex = (int) endpoint.get("service") - 1;
            String endpointName = (String) endpoint.get("endpoint");

            String groupName = groupNames.get(serviceIndex);
            groupEndpoints.get(groupName).add(endpointName);
        }

        System.out.println(groupEndpoints);
    }

}
