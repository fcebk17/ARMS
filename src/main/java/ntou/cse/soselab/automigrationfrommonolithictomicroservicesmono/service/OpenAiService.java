package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.ChatRequest;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    // 讀取 OpenAI API 金鑰，從 application.properties 中透過 Spring 注入
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    // OpenAI Chat Completions 的 REST API endpoint
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public String chatWithGpt(String userPrompt) {
        RestTemplate restTemplate = new RestTemplate();

        // 建立 request payload
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userPrompt);

        ChatRequest chatRequest = new ChatRequest("gpt-4o", List.of(message), 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<ChatRequest> request = new HttpEntity<>(chatRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, Map.class);

        // 回傳第一個回應的文字
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");

        return messageObj.get("content").toString();
    }

    public String askGptWithCodeFile(String filePath, String question) throws IOException {
        String codeContent = FileUtil.readFile(filePath);

        String fullPrompt = String.format("Please read the following code of file and answer the questions: \n\n%s\n\n", codeContent, question);

        return chatWithGpt(fullPrompt);
    }
}
