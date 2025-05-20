package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

public class GptRunner {
    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AutoMigrationApplication.class);
        OpenAiService gptService = context.getBean(OpenAiService.class);

        String filePath = "/home/popocorn/output/UserDaoService/OnlineShopingApp/src/main/java/com/project/controller/CartController.java";
        String question = "告訴我這段程式有幾個 api 端點？";

        String result = gptService.askGptWithCodeFile(filePath, question);
        System.out.println(result);
    }
}
