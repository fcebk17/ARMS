package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service;

import com.fasterxml.jackson.databind.JsonNode;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAiChatService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 接收聊天紀錄
    public String chat(List<ChatMessage> history) {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> body = new HashMap<>(); // 準備一個 map 當成 HTTP 請求的 body
        body.put("model", "gpt-4.1"); // 指定使用的 OpenAI 模型

        // 將聊天歷史 (ChatMessage List) 轉換成 OpenAI API 規定的 messages 格式（每個元素都是一個 map，key 有 role 和 content）
        List<Map<String, String>> messages = history.stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());
        body.put("messages", messages); // 把 messages 加入到 body

        HttpHeaders headers = new HttpHeaders(); // 建立 HTTP header 物件
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); // 設定 Authorization: Bearer <API_KEY>

        // 將 body 和 headers 組成一個 HTTP 請求物件
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 用 RestTemplate 發送 POST 請求，回傳型別是 JsonNode（方便直接取 JSON 欄位）
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JsonNode.class);

        return response.getBody()
                .get("choices").get(0).get("message").get("content")
                .asText();
    }
}
