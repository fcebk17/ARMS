package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FolderAnalyzer {

    private final OpenAiService openAiService;

    public FolderAnalyzer(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public void analyzeAllFilesIndividually(String folderPath, String question) throws IOException {
        List<Path> files = listAllJavaFiles(folderPath);
        int totalApiCount = 0;
        int fileIndex = 1;

        for (Path file : files) {
            String filename = file.getFileName().toString();
            String code = Files.readString(file);

//            只需要分析 /controller /repository, /service 內的檔案
            String prompt = String.format("""
                以下是微服務中的一個 Controller, Repository 或 Service，請協助我分析：
                如果這個不是Service，請跳過
                如果這個是interface，請跳過
                如果這是Service，那這個Service是否需要透過外部呼叫去存取當前專案沒有的repository
                若需要透過 api call 去拿到其他 Service 的資料，該怎麼改寫 Service?
                檔案名稱：%s
    
                程式碼如下：
                %s
            """, filename, code);

            String reply = openAiService.chatWithGpt(prompt);

            System.out.printf("[%d/%d]  分析 %s 完成%n", fileIndex++, files.size(), filename);
            System.out.println("----------- GPT 回覆 -----------");
            System.out.println(reply);

            // 檢查 GPT 回覆中是否提及與其他服務的互動
            if (reply.contains("呼叫") || reply.contains("RestTemplate") || reply.contains("FeignClient")
                    || reply.contains("請求") || reply.contains("溝通") || reply.contains("service")) {

                System.out.println("需要檢查該 Controller 的互動行為，完整程式如下：");
                System.out.printf("=== %s ===\n", filename);
                System.out.println(code);
            }

            System.out.println();
        }
    }

    // new
    public void analyzeFilesInBatches(String folderPath, int batchSize) throws IOException {
        List<Path> allFiles = listAllJavaFiles(folderPath);  // 只抓 controller/service/repository 檔案
        List<String> batchResults = new ArrayList<>();

        int totalBatches = (int) Math.ceil((double) allFiles.size() / batchSize);

        for (int i = 0; i < allFiles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allFiles.size());
            List<Path> batch = allFiles.subList(i, end);

            StringBuilder batchPrompt = new StringBuilder();
            for (Path file : batch) {
                String filename = file.getFileName().toString();
                String code = Files.readString(file);
                batchPrompt.append("=== File: ").append(filename).append(" ===\n");
                batchPrompt.append(code).append("\n\n");
            }

            String prompt = String.format("""
                以下是微服務中的一個 Controller, Repository 或 Service，請協助我分析：
                如果這個不是Service，請跳過
                如果這個是interface，請跳過
                如果這是Service，那這個Service是否需要透過外部呼叫去存取當前專案沒有的repository
                若需要透過 api call 去拿到其他 Service 的資料，該怎麼改寫 Service?
    
                程式碼如下：
                %s
            """, batchPrompt.toString());

            System.out.printf("📦 分析第 %d/%d 批，共 %d 個檔案...\n", i / batchSize + 1, totalBatches, batch.size());
            String reply = openAiService.chatWithGpt(prompt);
            batchResults.add(reply);

            // 可以選擇立即印出每一批結果
            System.out.println("----- GPT 回覆 -----");
            System.out.println(reply);
            System.out.println("--------------------\n");
        }

        // 最後彙整總結所有批次回覆
        StringBuilder finalPrompt = new StringBuilder();
        for (int i = 0; i < batchResults.size(); i++) {
            finalPrompt.append("=== 第 ").append(i + 1).append(" 批分析結果 ===\n");
            finalPrompt.append(batchResults.get(i)).append("\n\n");
        }

        String summaryPrompt = String.format("""
        以下是多批微服務分析的回覆彙整，請綜合總結：
        - 微服務之間的溝通模式
        - 各服務（Controller）間的依賴與呼叫方式
        - 若需要透過 api call 去拿到其他 Service 的資料，該怎麼改寫 Service?

        回覆彙整如下：
        %s
        """, finalPrompt.toString());

        String summary = openAiService.chatWithGpt(summaryPrompt);
        System.out.println("==== 最終總結分析 ====");
        System.out.println(summary);
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
                請分析以下多個微服務專案微服務的 controller下的檔案，幫我改寫微服務的 controller 該如何互相溝通？\s
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
                    .filter(p -> Files.isRegularFile(p) &&
                            p.toString().endsWith(".java") &&
                            (
                                    p.toString().contains(File.separator + "controller" + File.separator) ||
                                            p.toString().contains(File.separator + "service" + File.separator) ||
                                            p.toString().contains(File.separator + "repository" + File.separator)
                            ))
                    .collect(Collectors.toList());
        }
    }

    // 萃取第一個出現的整數
    private int extractFirstInteger(String text) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
