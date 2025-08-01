package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.component;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.ServiceWriter;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.model.ChatMessage;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChatDemoRunner implements CommandLineRunner {

    @Autowired
    private OpenAiChatService openAiChatService;

    private List<String> gptReplies = new ArrayList<>();

    @Override
    public void run(String... args) {
        // 1. 建立聊天歷史
        List<ChatMessage> history = new ArrayList<>();
        String targetServiceName = "TransactionService";
        String DWSPServiceName = "ProductRepositoryService";

        // 2. 寫死幾個 prompt
        List<String> prompts = new ArrayList<>();
        prompts.add(
                "我現在正在進行微服務切割，目前切出來的微服務有四個，我會一併附上這四個微服務有哪些 Controller 與 Repository。\n" +
                        "其中第四個 ProductRepositoryService 是使用 Database Wrapping Service Pattern 概念切出來的微服務，先不要給我回復，我會再給你後續指令\n" +
                        "\n" +
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

        prompts.add(
                "因為在 ProductService 與 TransactionService 微服務中，都有用到 ProductRepository 這個資料儲存庫\n" +
                        "但 ProductService 與 TransactionService 中並沒有 ProductRepository，因此需要使用 api call 來呼叫要使用的 ProductRepository\n" +
                        "你先理解這個關係"
        );

        try {
            String ProductServiceCode = Files.readString(
                    Paths.get("/home/popocorn/test-project/book-store-api/src/main/java/com/example/bookstore_api/service/ProductService.java"),
                    StandardCharsets.UTF_8
            );

            String ProductRepositoryControllerCode = Files.readString(
                    Paths.get("/home/popocorn/output_book_store/ProductRepositoryService/src/main/java/com/example/bookstore_api/controller/ProductRepositoryController.java"),
                    StandardCharsets.UTF_8
            );

            String prompt = String.format("""
                    ProductService 微服務 (port number=8889) 的 ProductService (Service 層邏輯) 與 ProductRepositoryService  微服務 (port number=8891) 的 ProductRepositoryController 的原始碼如下，請幫我將 ProductService (Service 層邏輯) 有使用到 ProductRepository 的部分改寫為呼叫 ProductRepositoryService
                    \n
                    %s
                    \n
                    %s
                    """, ProductServiceCode, ProductRepositoryControllerCode);

            prompts.add(prompt);

        } catch (Exception e) {
            e.printStackTrace();
        }

        prompts.add("請給我你修改後的完整 ProductService 程式碼，只給我程式碼就好，不要附加 markdown 符號");

        for (String prompt : prompts) {
            // 加入 user 訊息
            history.add(new ChatMessage("user", prompt));

            // 呼叫 OpenAI API 取得 AI 回覆
            String aiReply = openAiChatService.chat(history);

            // 加入 AI 訊息
            history.add(new ChatMessage("assistant", aiReply));
            gptReplies.add(aiReply); // 將 gpt 回覆存起來

            // 印出本次 prompt 與 AI 回覆
            System.out.println("[USER] " + prompt);
            System.out.println("[GPT] " + aiReply);
            System.out.println("-------------------------------------");
        }

        System.out.println("[GPT REPLIES] " + getGptReplies());

        String targetfile = "/home/popocorn/ARMS_experience/output_classroom_scheduler/CourseService/src/main/java/my/projects/classroomschedulerapp/service/CourseService.java";
        ServiceWriter.writeToFile(getGptReplies(), targetfile);
    }

    public String getGptReplies() {
        return gptReplies.get(3);
    }
}
