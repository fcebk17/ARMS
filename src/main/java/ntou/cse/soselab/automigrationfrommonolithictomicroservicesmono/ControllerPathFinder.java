package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControllerPathFinder {
    private final String BASE_PATH;

    public ControllerPathFinder(String basePath) {
        // 確保路徑以 "/" 結尾
        this.BASE_PATH = basePath.endsWith("/") ? basePath : basePath + "/";
    }

    public String getControllerDirectory() {
        File controllerFile = findFirstControllerFile();

        if (controllerFile == null) {
            return null;
        }

        // 返回不包含檔案名稱的目錄路徑
        return controllerFile.getParent() + "/";
    }

    /**
     * 找出第一個 Controller 檔案的控制器類型
     * @return 控制器類型（@Controller 或 @RestController）
     */
    public String getControllerAnnotationType() {
        File controllerFile = findFirstControllerFile();

        if (controllerFile == null) {
            return "未找到 Controller";
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(controllerFile.getAbsolutePath())));

            // 使用正則表達式檢查控制器註解
            Pattern restControllerPattern = Pattern.compile("@RestController\\s");
            Pattern controllerPattern = Pattern.compile("@Controller\\s");

            Matcher restControllerMatcher = restControllerPattern.matcher(content);
            Matcher controllerMatcher = controllerPattern.matcher(content);

            if (restControllerMatcher.find()) {
                return "@RestController";
            } else if (controllerMatcher.find()) {
                return "@Controller";
            }
        } catch (IOException e) {
            System.err.println("讀取檔案時發生錯誤: " + e.getMessage());
        }

        return "未找到控制器註解";
    }

    /**
     * 找出第一個 Controller 檔案
     * @return 找到的第一個 Controller 檔案
     */
    private File findFirstControllerFile() {
        File baseDir = new File(BASE_PATH);
        return findFirstControllerFileRecursive(baseDir);
    }

    /**
     * 遞迴搜尋第一個 Controller 檔案
     * @param dir 搜尋起始目錄
     * @return 找到的第一個 Controller 檔案，未找到則返回 null
     */
    private File findFirstControllerFileRecursive(File dir) {
        if (!dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File controllerInSubdir = findFirstControllerFileRecursive(file);
                if (controllerInSubdir != null) {
                    return controllerInSubdir;
                }
            } else if (file.getName().endsWith("Controller.java")) {
                return file;
            }
        }

        return null;
    }

    /**
     * 列印第一個找到的 Controller 檔案的目錄路徑和控制器類型
     */
    public void printControllerInfo() {
        String controllerDirectory = getControllerDirectory();
        String controllerAnnotationType = getControllerAnnotationType();

        if (controllerDirectory == null) {
            System.out.println("偵錯資訊:");
            System.out.println("Base Path: " + BASE_PATH);
            System.out.println("未找到任何 Controller 檔案");

            // 列出目錄中的所有檔案，協助偵錯
            File baseDir = new File(BASE_PATH);
            File[] files = baseDir.listFiles();
            if (files != null) {
                System.out.println("目錄中的檔案:");
                for (File file : files) {
                    System.out.println(file.getName());
                }
            }
            return;
        }

        // 列印找到的 Controller 檔案的目錄路徑和控制器類型
        System.out.println("找到的第一個 Controller 檔案目錄路徑:");
        System.out.println(controllerDirectory);
        System.out.println("控制器類型:");
        System.out.println(controllerAnnotationType);
    }

    // 主方法示範使用
    public static void main(String[] args) {
        String basePath = "/home/popocorn/output/UserDaoService";

        ControllerPathFinder finder = new ControllerPathFinder(basePath);
        finder.printControllerInfo();
    }
}