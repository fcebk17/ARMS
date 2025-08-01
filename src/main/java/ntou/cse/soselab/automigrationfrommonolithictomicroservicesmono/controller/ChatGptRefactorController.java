package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.component.ChatDemoRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chatgpt")
public class ChatGptRefactorController {

    @Autowired
    private ChatDemoRunner chatDemoRunner;

    @GetMapping("/refactor")
    public String showRefactorPage(Model model) {
        // 設定預設值
        RefactorRequest defaultRequest = new RefactorRequest();
        defaultRequest.setTargetServiceName("TransactionService");
        defaultRequest.setDwspServiceName("ProductRepositoryService");
        defaultRequest.setTargetServicePort("8890");
        defaultRequest.setDwspServicePort("8891");

        // 設定預設的微服務架構描述
        defaultRequest.setMicroserviceDescription(
                "1. AccountService (port number=8888)\n" +
                        "  - AdminController\n" +
                        "  - UserController \n" +
                        "  - AdminRepository\n" +
                        "  - UserRepository\n" +
                        "\n" +
                        "2. ProductService (port number=8889)\n" +
                        "  - ProductController\n" +
                        "  - FeedbackController\n" +
                        "  - FeedbackRepository\n" +
                        "\n" +
                        "3. TransactionService (port number=8890)\n" +
                        "  - WishlistController\n" +
                        "  - CartController\n" +
                        "  - OrderController\n" +
                        "  - WishlishRepository\n" +
                        "  - CartRepository\n" +
                        "  - OrderRepository\n" +
                        "\n" +
                        "4. ProductRepositoryService (port number=8891)\n" +
                        "  - ProductRepositoryController\n" +
                        "  - ProductRepository"
        );

        defaultRequest.setRepositoryExplanation(
                "因為在 ProductService 與 TransactionService 微服務中，都有用到 ProductRepository 這個資料儲存庫\n" +
                        "但 ProductService 與 TransactionService 中並沒有 ProductRepository，因此需要使用 api call 來呼叫要使用的 ProductRepository"
        );

        defaultRequest.setSourceServicePath("/home/popocorn/test-project/book-store-api/src/main/java/com/example/bookstore_api/service/ProductService.java");
        defaultRequest.setRepositoryControllerPath("/home/popocorn/output_book_store/ProductRepositoryService/src/main/java/com/example/bookstore_api/controller/ProductRepositoryController.java");
        defaultRequest.setOutputPath("/home/popocorn/ARMS_experience/output_classroom_scheduler/CourseService/src/main/java/my/projects/classroomschedulerapp/service/CourseService.java");

        model.addAttribute("request", defaultRequest);
        return "chatgpt-refactor";
    }

    @PostMapping("/refactor")
    public String processRefactor(@ModelAttribute RefactorRequest request, Model model) {
        try {
            // 驗證輸入
            if (request.getTargetServiceName() == null || request.getTargetServiceName().trim().isEmpty() ||
                    request.getDwspServiceName() == null || request.getDwspServiceName().trim().isEmpty() ||
                    request.getSourceServicePath() == null || request.getSourceServicePath().trim().isEmpty() ||
                    request.getRepositoryControllerPath() == null || request.getRepositoryControllerPath().trim().isEmpty()) {
                model.addAttribute("error", "所有必填欄位都必須填寫");
                model.addAttribute("request", request);
                return "chatgpt-refactor";
            }

            // 驗證檔案路徑是否存在
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get(request.getSourceServicePath()))) {
                model.addAttribute("error", "找不到原始服務程式碼檔案: " + request.getSourceServicePath());
                model.addAttribute("request", request);
                return "chatgpt-refactor";
            }

            if (!java.nio.file.Files.exists(java.nio.file.Paths.get(request.getRepositoryControllerPath()))) {
                model.addAttribute("error", "找不到 Repository Controller 程式碼檔案: " + request.getRepositoryControllerPath());
                model.addAttribute("request", request);
                return "chatgpt-refactor";
            }

            // 呼叫 ChatDemoRunner 進行重構
            String result = chatDemoRunner.runRefactorWithParameters(request);

            model.addAttribute("success", "Code Refactoring Successful！");
            model.addAttribute("result", result);
            model.addAttribute("request", request);
            return "chatgpt-refactor-result";

        } catch (Exception e) {
            e.printStackTrace(); // 用於 debug
            model.addAttribute("error", "重構過程發生錯誤: " + e.getMessage());
            model.addAttribute("request", request);
            return "chatgpt-refactor";
        }
    }

    // 請求資料類別
    public static class RefactorRequest {
        private String targetServiceName;
        private String dwspServiceName;
        private String targetServicePort;
        private String dwspServicePort;
        private String microserviceDescription;
        private String repositoryExplanation;
        private String sourceServicePath;
        private String repositoryControllerPath;
        private String outputPath;

        // Getters and Setters
        public String getTargetServiceName() {
            return targetServiceName;
        }

        public void setTargetServiceName(String targetServiceName) {
            this.targetServiceName = targetServiceName;
        }

        public String getDwspServiceName() {
            return dwspServiceName;
        }

        public void setDwspServiceName(String dwspServiceName) {
            this.dwspServiceName = dwspServiceName;
        }

        public String getTargetServicePort() {
            return targetServicePort;
        }

        public void setTargetServicePort(String targetServicePort) {
            this.targetServicePort = targetServicePort;
        }

        public String getDwspServicePort() {
            return dwspServicePort;
        }

        public void setDwspServicePort(String dwspServicePort) {
            this.dwspServicePort = dwspServicePort;
        }

        public String getMicroserviceDescription() {
            return microserviceDescription;
        }

        public void setMicroserviceDescription(String microserviceDescription) {
            this.microserviceDescription = microserviceDescription;
        }

        public String getRepositoryExplanation() {
            return repositoryExplanation;
        }

        public void setRepositoryExplanation(String repositoryExplanation) {
            this.repositoryExplanation = repositoryExplanation;
        }

        public String getSourceServicePath() {
            return sourceServicePath;
        }

        public void setSourceServicePath(String sourceServicePath) {
            this.sourceServicePath = sourceServicePath;
        }

        public String getRepositoryControllerPath() {
            return repositoryControllerPath;
        }

        public void setRepositoryControllerPath(String repositoryControllerPath) {
            this.repositoryControllerPath = repositoryControllerPath;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }
    }
}