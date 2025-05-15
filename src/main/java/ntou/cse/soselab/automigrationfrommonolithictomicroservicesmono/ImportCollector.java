package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImportCollector {

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

    // 測試用 main（可選）
    public static void main(String[] args) {
        Map<String, String> imports = getImports();
        imports.forEach((key, value) -> System.out.println(key + " -> " + value));
    }
}

