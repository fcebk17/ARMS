package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
