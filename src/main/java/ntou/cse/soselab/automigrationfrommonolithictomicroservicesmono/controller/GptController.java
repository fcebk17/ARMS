package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gpt")
public class GptController {

    private final OpenAiService openAiService;

    public GptController(OpenAiService generatorService, OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String prompt) {
        return openAiService.chatWithGpt(prompt);
    }
}
