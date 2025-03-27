package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NoDuplicateRepositoryCleaner {
    private final String basePath;
    private final Map<String, Set<String>> serviceRepoMap;

    public NoDuplicateRepositoryCleaner(Map<String, Set<String>> serviceRepoMap, String basePath) {
        this.serviceRepoMap = serviceRepoMap;
        this.basePath = basePath;
    }

    public void cleanUnusedRepositories() throws IOException {
        for (Map.Entry<String, Set<String>> entry : serviceRepoMap.entrySet()) {
            String baseDir = basePath + entry.getKey(); // e.g., "CustomerService"
            Set<String> repositoriesToKeep = entry.getValue();

            Path rootPath = Paths.get(baseDir);
            if (!Files.exists(rootPath)) {
                System.out.println("⚠️ Path not found: " + baseDir);
                continue;
            }

            Files.walk(rootPath)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            if (!containsRepositoryAnnotation(path)) {
                                return; // skip non-@Repository files
                            }

                            String classFQN = getFullyQualifiedClassName(path.toFile(), baseDir);
                            if (!repositoriesToKeep.contains(classFQN)) {
                                Files.delete(path);
                                System.out.println("🗑 Deleted unused repository: " + classFQN);
                            }
                        } catch (IOException e) {
                            System.err.println("❌ Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    private boolean containsRepositoryAnnotation(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().startsWith("@Repository")) {
                return true;
            }
        }
        return false;
    }

    private String getFullyQualifiedClassName(File file, String baseDir) {
        String absPath = file.getAbsolutePath().replace(File.separatorChar, '/');
        String basePath = new File(baseDir).getAbsolutePath().replace(File.separatorChar, '/');

        String relativePath = absPath.replace(basePath + "/", "");
        String className = relativePath.replace(".java", "").replace("/", ".");

        // 預設都是 com.app.repositories 下的 repository
        return "com.app.repositories." + className.substring(className.lastIndexOf('.') + 1);
    }
}
