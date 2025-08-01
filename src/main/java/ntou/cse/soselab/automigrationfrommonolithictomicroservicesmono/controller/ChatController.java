package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.model.ChatMessage;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OpenAiChatService chatService;
    // 用來暫存每個 sessionId 對應的聊天歷史（用 Map 存起來，key 是 sessionId）
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    @Autowired
    public ChatController(OpenAiChatService chatService) {
        this.chatService = chatService;
    }

    // 這個方法處理 POST /api/chat/{sessionId} 請求，實現對話功能
    @PostMapping("/{sessionId}")
    public Map<String, String> chat(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> payload) { // 輸入內容，預期有一個 key: "message"

        String userMessage = payload.get("message"); // 取得用戶訊息
        sessions.putIfAbsent(sessionId, new ArrayList<>()); // 如果 sessionId 沒有歷史，就初始化一個空 List
        List<ChatMessage> history = sessions.get(sessionId); // 取出這個 session 的對話紀錄

        // 把這次的使用者訊息加到歷史
        history.add(new ChatMessage("user", userMessage));

        // 呼叫 OpenAI，呼叫 Service 把整個歷史傳給 GPT，取得 AI 回應
        String aiResponse = chatService.chat(history);

        // 把 AI 回覆也加到歷史
        history.add(new ChatMessage("assistant", aiResponse));

        // 以 map 回傳 response 給前端（{ "response": ... }）
        return Map.of("response", aiResponse);
    }

    // 可加這個 GET endpoint 來查 chat history
    @GetMapping("/{sessionId}/history")
    public List<ChatMessage> getHistory(@PathVariable String sessionId) {
        return sessions.getOrDefault(sessionId, Collections.emptyList());
    }
}
