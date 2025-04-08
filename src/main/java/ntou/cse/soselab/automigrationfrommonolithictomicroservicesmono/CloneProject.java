package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.IOException;
import java.nio.file.*;

public class CloneProject {
    public void copyDirectory(String source, String Dir1) {
        Path sourceDir = Paths.get(source);
        Path targetDir1 = Paths.get(Dir1);

        try {
            Files.walk(sourceDir).forEach(sourcePath -> {
                try {
                    Path targetPath = targetDir1.resolve(sourceDir.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            System.out.println("Directory copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}