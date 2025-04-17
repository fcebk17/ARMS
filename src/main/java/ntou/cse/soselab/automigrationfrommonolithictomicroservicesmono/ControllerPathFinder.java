package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.File;

public class ControllerPathFinder {
    private final String BASE_PATH;

    public ControllerPathFinder(String basePath) {
        // 確保路徑以 "/" 結尾
        this.BASE_PATH = basePath.endsWith("/") ? basePath : basePath + "/";
    }

    /**
     * 找出第一個 Controller 檔案的目錄路徑
     * @return 第一個 Controller 檔案所在的目錄路徑，未找到則返回 null
     */
    public String getControllerDirectory() {
        File controllerFile = findFirstControllerFile();

        if (controllerFile == null) {
            return null;
        }

        // 返回不包含檔案名稱的目錄路徑
        return controllerFile.getParent() + "/";
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
     * 列印第一個找到的 Controller 檔案的目錄路徑
     */
    public void printFirstControllerDirectory() {
        String controllerDirectory = getControllerDirectory();

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

        // 列印找到的 Controller 檔案的目錄路徑
        System.out.println("找到的第一個 Controller 檔案目錄路徑:");
        System.out.println(controllerDirectory);
    }

    // 主方法示範使用
    public static void main(String[] args) {
        String basePath = "/home/popocorn/output/UserDaoService";

        ControllerPathFinder finder = new ControllerPathFinder(basePath);
        finder.printFirstControllerDirectory();
    }
}