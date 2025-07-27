package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.DatabaseSegmentationApplicationZ;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.DatabaseSegmentationCopyApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/process")
    public String processConfiguration(
            @RequestParam("basePath") String basePath,
            @RequestParam("sourceProjectPath") String sourceProjectPath,
            @RequestParam("microservices") List<String> microservices,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            // 驗證輸入
            if (basePath.isEmpty() || sourceProjectPath.isEmpty() || microservices.isEmpty()) {
                model.addAttribute("error", "所有欄位都必須填寫");
                return "index";
            }

            // 呼叫原本的處理邏輯
            DatabaseSegmentationResult result = processDatabaseSegmentation(basePath, sourceProjectPath, microservices);

            model.addAttribute("result", result);
            model.addAttribute("success", "處理完成！");

            return "result";

        } catch (Exception e) {
            model.addAttribute("error", "處理過程發生錯誤: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, String> getStatus() {
        return Map.of("status", "ready", "message", "系統準備就緒");
    }

    private DatabaseSegmentationResult processDatabaseSegmentation(
            String basePath,
            String sourceProjectPath,
            List<String> microservices) throws Exception {

        // 這裡整合你原本的邏輯
        DatabaseSegmentationApplicationZ app = new DatabaseSegmentationApplicationZ();

        // 修改原本的 main 方法邏輯，讓它接受參數
        return app.processWithParameters(basePath, sourceProjectPath, microservices);
    }

    // 結果資料類別
    public static class DatabaseSegmentationResult {
        private Map<String, Map<String, List<String>>> controllerToServiceMap;
        private Map<String, String> interfaceToImplementationMap;
        private Map<String, Set<String>> microserviceToRepositoryMap;
        private boolean hasDuplicateRepositories;
        private Map<String, List<String>> duplicateRepositories;

        // Getters and Setters
        public Map<String, Map<String, List<String>>> getControllerToServiceMap() {
            return controllerToServiceMap;
        }

        public void setControllerToServiceMap(Map<String, Map<String, List<String>>> controllerToServiceMap) {
            this.controllerToServiceMap = controllerToServiceMap;
        }

        public Map<String, String> getInterfaceToImplementationMap() {
            return interfaceToImplementationMap;
        }

        public void setInterfaceToImplementationMap(Map<String, String> interfaceToImplementationMap) {
            this.interfaceToImplementationMap = interfaceToImplementationMap;
        }

        public Map<String, Set<String>> getMicroserviceToRepositoryMap() {
            return microserviceToRepositoryMap;
        }

        public void setMicroserviceToRepositoryMap(Map<String, Set<String>> microserviceToRepositoryMap) {
            this.microserviceToRepositoryMap = microserviceToRepositoryMap;
        }

        public boolean isHasDuplicateRepositories() {
            return hasDuplicateRepositories;
        }

        public void setHasDuplicateRepositories(boolean hasDuplicateRepositories) {
            this.hasDuplicateRepositories = hasDuplicateRepositories;
        }

        public Map<String, List<String>> getDuplicateRepositories() {
            return duplicateRepositories;
        }

        public void setDuplicateRepositories(Map<String, List<String>> duplicateRepositories) {
            this.duplicateRepositories = duplicateRepositories;
        }
    }
}