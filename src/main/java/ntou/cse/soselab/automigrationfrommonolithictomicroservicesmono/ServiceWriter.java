package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServiceWriter {
    public static void writeToFile(String content, String filePath) {
        try {
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Successfully wrote to " + filePath);
        } catch (Exception e) {
            System.err.println("Failed to write to " + filePath);
            e.printStackTrace();
        }
    }
}
