package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.component;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.ServiceWriter;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.model.ChatMessage;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiChatService;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller.ChatGptRefactorController.RefactorRequest;
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
        // 原本的預設執行邏輯（如果需要的話）
        System.out.println("ChatDemoRunner 已啟動，等待重構請求...");
    }

    /**
     * 使用參數化請求進行程式碼重構
     */
    public String runRefactorWithParameters(RefactorRequest request) throws Exception {
        // 清空之前的回覆
        gptReplies.clear();

        // 建立聊天歷史
        List<ChatMessage> history = new ArrayList<>();

        // 建立四個 prompt
        List<String> prompts = buildPromptsFromRequest(request);

        // 逐一處理每個 prompt
        for (String prompt : prompts) {
            // 加入 user 訊息
            history.add(new ChatMessage("user", prompt));

            // 呼叫 OpenAI API 取得 AI 回覆
            String aiReply = openAiChatService.chat(history);

            // 加入 AI 訊息
            history.add(new ChatMessage("assistant", aiReply));
            gptReplies.add(aiReply); // 將 gpt 回覆存起來

            // 印出本次 prompt 與 AI 回覆（用於 debug）
            System.out.println("[USER] " + prompt);
            System.out.println("[GPT] " + aiReply);
            System.out.println("-------------------------------------");
        }

        // 取得最終的重構結果
        String finalResult = getGptReplies();

        // 如果有指定輸出路徑，則寫入檔案
        if (request.getOutputPath() != null && !request.getOutputPath().trim().isEmpty()) {
            ServiceWriter.writeToFile(finalResult, request.getOutputPath());
            System.out.println("重構結果已寫入: " + request.getOutputPath());
        }

        return finalResult;
    }

    /**
     * 根據請求參數建立四個 prompt
     */
    private List<String> buildPromptsFromRequest(RefactorRequest request) throws Exception {
        List<String> prompts = new ArrayList<>();

        // Prompt 1: 微服務架構說明
        String prompt1 = String.format(
                "我現在正在進行微服務切割，目前切出來的微服務有多個，我會一併附上這些微服務有哪些 Controller 與 Repository。\n" +
                        "其中 %s 是使用 Database Wrapping Service Pattern 概念切出來的微服務，先不要給我回復，我會再給你後續指令\n\n%s",
                request.getDwspServiceName(),
                request.getMicroserviceDescription()
        );
        prompts.add(prompt1);

        // Prompt 2: Repository 關係說明
        prompts.add(request.getRepositoryExplanation() + "\n你先理解這個關係");

        // Prompt 3: 程式碼分析與重構請求
        String sourceServiceCode = "";
        String repositoryControllerCode = "";

        try {
            // 讀取原始服務程式碼
            if (Files.exists(Paths.get(request.getSourceServicePath()))) {
                sourceServiceCode = Files.readString(
                        Paths.get(request.getSourceServicePath()),
                        StandardCharsets.UTF_8
                );
            } else {
                throw new Exception("找不到原始服務程式碼檔案: " + request.getSourceServicePath());
            }

            // 讀取 Repository Controller 程式碼
            if (Files.exists(Paths.get(request.getRepositoryControllerPath()))) {
                repositoryControllerCode = Files.readString(
                        Paths.get(request.getRepositoryControllerPath()),
                        StandardCharsets.UTF_8
                );
            } else {
                throw new Exception("找不到 Repository Controller 程式碼檔案: " + request.getRepositoryControllerPath());
            }

        } catch (Exception e) {
            throw new Exception("讀取程式碼檔案時發生錯誤: " + e.getMessage());
        }

        String prompt3 = String.format(
                "%s 微服務 (port number=%s) 的服務層邏輯與 %s 微服務 (port number=%s) 的 Controller 的原始碼如下，" +
                        "請幫我將服務層邏輯中有使用到 Repository 的部分改寫為呼叫 %s\n\n" +
                        "原始服務程式碼:\n%s\n\n" +
                        "Repository Controller 程式碼:\n%s",
                request.getTargetServiceName(),
                request.getTargetServicePort(),
                request.getDwspServiceName(),
                request.getDwspServicePort(),
                request.getDwspServiceName(),
                sourceServiceCode,
                repositoryControllerCode
        );
        prompts.add(prompt3);

        // Prompt 4: 請求完整程式碼
        prompts.add("請給我你修改後的完整程式碼，只給我程式碼就好，不要附加 markdown 符號");

        return prompts;
    }

    /**
     * 取得最終的 GPT 回覆（第四個回覆）
     */
    public String getGptReplies() {
        if (gptReplies.size() >= 4) {
            return gptReplies.get(3); // 回傳第四個回覆
        } else if (!gptReplies.isEmpty()) {
            return gptReplies.get(gptReplies.size() - 1); // 回傳最後一個回覆
        } else {
            return "沒有收到 GPT 回覆";
        }
    }

    /**
     * 取得所有 GPT 回覆
     */
    public List<String> getAllGptReplies() {
        return new ArrayList<>(gptReplies);
    }
}