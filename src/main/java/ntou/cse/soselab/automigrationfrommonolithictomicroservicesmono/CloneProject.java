package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.*;

public class CloneProject {
    public void copyDirectory(String source, String Dir1) {
        Path sourceDir = Paths.get(source);
        Path targetDir1 = Paths.get(Dir1);

        try {
            copyDirectoryLogic(sourceDir,targetDir1);
            System.out.println("Directory copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void copyDirectoryLogic(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                }
                else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

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
}