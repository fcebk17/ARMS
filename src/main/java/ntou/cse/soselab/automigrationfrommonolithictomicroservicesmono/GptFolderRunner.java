package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.config.GptConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

public class GptFolderRunner {
    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GptConfig.class);
        FolderAnalyzer folderAnalyzer = context.getBean(FolderAnalyzer.class);

        String filePath = "/home/popocorn/output/UserDaoService/OnlineShopingApp/src/main/java/com/project/controller";
        String question = "告訴我這個微服務內有幾個 api 端點？";

//        folderAnalyzer.analyzeAllFiles(filePath, question);
        folderAnalyzer.analyzeAllFilesTogether(filePath, question);
    }
}
