package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.config.GptConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GptFolderRunner {
    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GptConfig.class);
        FolderAnalyzer folderAnalyzer = context.getBean(FolderAnalyzer.class);

        Map<String, Map<String, List<String>>> controllerToService = DatabaseSegmentationApplication.getControllerToServiceMap();
        Set<Map<String, List<String>>> serviceToRepo = DatabaseSegmentationApplication.getServiceToRepositorySet();

        // 將 map 轉成格式化字串
        StringBuilder contextInfo = new StringBuilder();
        contextInfo.append("以下是 controller 到 service 的對應關係：\n")
                .append(controllerToService.toString()).append("\n\n")
                .append("以下是 service 到 repository 的對應關係：\n")
                .append(serviceToRepo.toString());



        String filePath = "/home/popocorn/output";
        // 插入 contextInfo 至 question 內容中
        String question = String.format("""
                請分析以下多個微服務專案的 controller, repository, service 資料夾下的檔案，
                
                並根據下列資訊，協助改寫每個微服務的service邏輯，應如何互相溝通，該如何用api call拿到其他微服務的資料

                %s

                """, contextInfo.toString());

//        folderAnalyzer.analyzeAllFiles(filePath, question);
//        folderAnalyzer.analyzeAllFilesIndividually(filePath, question);
        folderAnalyzer.analyzeFilesInBatches(filePath,40);
    }
}
