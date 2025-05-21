package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChatRequest {
    private String model;
    private List<Map<String, String>> messages;
    private double temperature;

    public ChatRequest(String model, List<Map<String, String>> messages, double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }
}
