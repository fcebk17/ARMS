package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ImportCollector {

    private final String folderPath;

    public ImportCollector(String folderPath) {
        this.folderPath = folderPath;
    }

    public Map<String, String> getAllImports() throws IOException {
        Map<String, String> mergedImports = new LinkedHashMap<>();

        mergedImports.putAll(getImports());

        Map<String, String> classImports = getClassImportsFromPath(this.folderPath);
        mergedImports.putAll(classImports);

        return mergedImports;
    }

    public static Map<String, String> getImports() {
        Map<String, String> importMap = new LinkedHashMap<>();

        // 1. 核心註解類別
        importMap.put("RestController", "import org.springframework.web.bind.annotation.RestController;");
        importMap.put("RequestMapping", "import org.springframework.web.bind.annotation.RequestMapping;");
        importMap.put("GetMapping", "import org.springframework.web.bind.annotation.GetMapping;");
        importMap.put("PostMapping", "import org.springframework.web.bind.annotation.PostMapping;");
        importMap.put("PutMapping", "import org.springframework.web.bind.annotation.PutMapping;");
        importMap.put("DeleteMapping", "import org.springframework.web.bind.annotation.DeleteMapping;");
        importMap.put("PatchMapping", "import org.springframework.web.bind.annotation.PatchMapping;");

        // 2. 請求參數與路徑參數
        importMap.put("RequestParam", "import org.springframework.web.bind.annotation.RequestParam;");
        importMap.put("PathVariable", "import org.springframework.web.bind.annotation.PathVariable;");
        importMap.put("RequestBody", "import org.springframework.web.bind.annotation.RequestBody;");
        importMap.put("RequestHeader", "import org.springframework.web.bind.annotation.RequestHeader;");
        importMap.put("RequestPart", "import org.springframework.web.bind.annotation.RequestPart;");

        // 3. HTTP 響應處理
        importMap.put("ResponseEntity", "import org.springframework.http.ResponseEntity;");
        importMap.put("HttpStatus", "import org.springframework.http.HttpStatus;");

        // 4. 跨域處理
        importMap.put("CrossOrigin", "import org.springframework.web.bind.annotation.CrossOrigin;");

        // 5. 驗證與資料綁定
        importMap.put("Valid", "import javax.validation.Valid;");
        importMap.put("BindingResult", "import org.springframework.validation.BindingResult;");

        // 6. 例外處理
        importMap.put("ExceptionHandler", "import org.springframework.web.bind.annotation.ExceptionHandler;");
        importMap.put("ControllerAdvice", "import org.springframework.web.bind.annotation.ControllerAdvice;");

        // 7. 檔案上傳
        importMap.put("MultipartFile", "import org.springframework.web.multipart.MultipartFile;");

        // 8. Security
        importMap.put("PreAuthorize", "import org.springframework.security.access.prepost.PreAuthorize;");
        importMap.put("AuthenticationPrincipal", "import org.springframework.security.core.annotation.AuthenticationPrincipal;");

        // 9. JSON 處理
        importMap.put("JsonProperty", "import com.fasterxml.jackson.annotation.JsonProperty;");
        importMap.put("ObjectMapper", "import com.fasterxml.jackson.databind.ObjectMapper;");

        // 10. 日誌工具
        importMap.put("Logger", "import org.slf4j.Logger;");
        importMap.put("LoggerFactory", "import org.slf4j.LoggerFactory;");

        // 11. Servlet / Http 類
        importMap.put("HttpServletRequest", "import javax.servlet.http.HttpServletRequest;");
        importMap.put("HttpServletResponse", "import javax.servlet.http.HttpServletResponse;");

        return importMap;
    }

    public static Map<String, String> getClassImportsFromPath(String folderPath) throws IOException {
        Map<String, String> classImportMap = new LinkedHashMap<>();
        Path rootPath = Paths.get(folderPath);

        if (!Files.exists(rootPath)) {
            throw new IllegalArgumentException("路徑不存在: " + folderPath);
        }

        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(file -> {
                        try {
                            List<String> lines = Files.readAllLines(file);
                            String packageName = null;
                            String className = null;

                            for (String line : lines) {
                                line = line.trim();
                                if (line.startsWith("package ")) {
                                    packageName = line.replace("package", "")
                                            .replace(";", "")
                                            .trim();
                                }
                                if (line.startsWith("public class ") || line.startsWith("class ")) {
                                    String[] parts = line.split("\\s+");
                                    for (int i = 0; i < parts.length; i++) {
                                        if (parts[i].equals("class") && i + 1 < parts.length) {
                                            className = parts[i + 1];
                                            break;
                                        }
                                    }
                                    break; // 偵測到 class 就可以離開
                                }
                            }

                            if (packageName != null && className != null) {
                                classImportMap.put(className, "import " + packageName + "." + className + ";");
                            }

                        } catch (IOException e) {
                            System.err.println("Failed to read: " + file + " -> " + e.getMessage());
                        }
                    });
        }

        return classImportMap;
    }

    // 測試用 main（可選）
    public static void main(String[] args) throws IOException {

        ImportCollector importCollector = new ImportCollector("/home/popocorn/test-project/Online-Shopping-App-SpringBoot-/OnlineShopingApp/src/main/java/com/project");

        Map<String, String> mergedImports = importCollector.getAllImports();
        mergedImports.forEach((k, v) -> System.out.println(k + " -> " + v));
        System.out.println("map length: " + mergedImports.size());
    }
}

