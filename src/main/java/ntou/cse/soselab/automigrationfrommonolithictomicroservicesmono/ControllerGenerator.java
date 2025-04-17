package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ControllerGenerator {

    private String baseDirectory;
    private String controllerName;
    private String controllerAnnotation;

    public ControllerGenerator(String baseDirectory, String controllerName, String controllerAnnotation) {
        this.baseDirectory = baseDirectory;
        this.controllerName = controllerName;
        this.controllerAnnotation = controllerAnnotation;

        // 驗證Controller
        if (!controllerAnnotation.equals("@RestController") && !controllerAnnotation.equals("@Controller")) {
            System.out.println("警告：不支持的控制器註解類型，默認使用 @RestController");
            this.controllerAnnotation = "@RestController";
        }
    }

    public boolean generateController() {
        // 確保目錄存在
        File directory = new File(baseDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("目錄已創建：" + baseDirectory);
            } else {
                System.err.println("無法創建目錄：" + baseDirectory);
                return false;
            }
        }

        // 從路徑中提取包名
        String packageName = extractPackageFromPath(baseDirectory);

        String importStatement;
        if (controllerAnnotation.equals("@RestController")) {
            importStatement = "import org.springframework.web.bind.annotation.RestController;\n";
        } else {
            importStatement = "import org.springframework.stereotype.Controller;\n";
        }

        // 創建控制器檔案
        File controllerFile = new File(baseDirectory + controllerName + ".java");

        try (FileWriter writer = new FileWriter(controllerFile)) {
            String controllerContent = "package " + packageName + ";\n\n"
                    + importStatement + "\n"
                    + controllerAnnotation + "\n"
                    + "public class " + controllerName + " {\n"
                    + "}\n";

            writer.write(controllerContent);
            System.out.println("成功建立Controller：" + controllerFile.getAbsolutePath());
            System.out.println("使用的package：" + packageName);
            return true;

        } catch (IOException e) {
            System.err.println("寫入檔案時發生錯誤：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String extractPackageFromPath(String path) {
        // 移除路徑前的 /home/ 等前綴，只保留 Java 原始碼目錄之後的部分
        String javaSourceDir = "java/";
        int javaIndex = path.indexOf(javaSourceDir);

        if (javaIndex != -1) {
            // 取得 java/ 目錄之後的路徑
            String packagePath = path.substring(javaIndex + javaSourceDir.length());

            // 將路徑分隔符替換為package分隔符
            packagePath = packagePath.replace('/', '.');

            // 移除末尾的分隔符（如果有）
            if (packagePath.endsWith(".")) {
                packagePath = packagePath.substring(0, packagePath.length() - 1);
            }

            return packagePath;
        }

        // 如果找不到 java/ 目錄，則使用 default package name
        return "com.project.controller";
    }

    public static void main(String[] args) {

        String baseDirectory = "/home/popocorn/output/CustomerDaoService/OnlineShopingApp/src/main/java/com/project/controller/";
        String controllerName = "CustomerDaoController";
        String controllerAnnotation = "@RestController";

        ControllerGenerator generator = new ControllerGenerator(baseDirectory, controllerName, controllerAnnotation);
        if (generator.generateController()) {
            System.out.println("Controller build success");
        } else {
            System.out.println("Controller build failed");
        }
    }
}