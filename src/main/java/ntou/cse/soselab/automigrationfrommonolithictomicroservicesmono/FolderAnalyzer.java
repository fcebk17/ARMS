package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FolderAnalyzer {

    private final OpenAiService openAiService;

    public FolderAnalyzer(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public void analyzeAllFilesTogether(String folderPath, String question) throws IOException {
        List<Path> files = listAllJavaFiles(folderPath);
        StringBuilder combinedCode = new StringBuilder();

        for (Path file : files) {
            String filename = file.getFileName().toString();
            String code = Files.readString(file);
            combinedCode.append("=== file name: ").append(filename).append(" ===\n");
            combinedCode.append(code).append("\n\n");
        }

        String fullPrompt = String.format("""
                請分析以下多個 Spring Boot Controller 程式碼，回答：「這些程式總共提供幾個 API 端點？」請列出總數，並簡要說明怎麼計算。
                       \s
                        %s
                """, combinedCode);

        String result = openAiService.chatWithGpt(fullPrompt);
        System.out.println(result);
    }

    public void analyzeAllFiles(String folderPath, String question) throws IOException {
        List<Path> files = listAllJavaFiles(folderPath);
        int count = 1;

        for (Path file : files) {
            String code = Files.readString(file);
            String prompt = String.format("請分析以下 Java 程式碼，並回答：%s\n\n%s", question, code);

            String result = openAiService.chatWithGpt(prompt);

//            String outputPath = file.toString().replace(".java", "_gpt_analysis.txt");
//            Files.writeString(Paths.get(outputPath), result);

            System.out.printf("[%d/%d] 分析完成：%s%n", count++, files.size(), file.getFileName());
            System.out.println(result);
        }
    }

    private List<Path> listAllJavaFiles(String folderPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            return paths
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }
}
