package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAccessing {
    public int getNumberOfGroups(String projectName, String concept) {
        try (var mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("demoData");
            MongoCollection<Document> clusters = database.getCollection("clusters");

            // Query `numberOfGroups` value
            Document query = new Document("projectName", projectName).append("concept", concept);

            // Query document
            Document cluster = clusters.find(query).first();

            if (cluster != null && cluster.containsKey("numberOfGroups")) {
                return cluster.getInteger("numberOfGroups");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<String> getServiceName(String projectName, String concept) {
        List<String> groupNames = new ArrayList<>();
        try (var mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("demoData");
            MongoCollection<Document> clusters = database.getCollection("clusters");

            // Query `groupNames` array based on projectName and concept
            Document query = new Document("projectName", projectName).append("concept", concept);
            Document cluster = clusters.find(query).first();

            if (cluster != null) {

                if (cluster.containsKey("groupNames")) {
                    // 取得 groupNames 列表，並提取 serviceName
                    List<Document> groupDocs = (List<Document>) cluster.get("groupNames");

                    // 提取每個 Document 中的 serviceName
                    for (Document doc : groupDocs) {
                        if (doc.containsKey("serviceName")) {
                            groupNames.add(doc.getString("serviceName"));  // 將每個 serviceName 加入到 groupNames 列表
                        }
                    }
                } else {
                    System.out.println("groupNames field does not exist in the document.");
                }
            } else {
                System.out.println("No document found for the given query.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groupNames;
    }
}
