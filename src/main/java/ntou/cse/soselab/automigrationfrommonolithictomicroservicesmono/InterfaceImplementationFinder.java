package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

public class InterfaceImplementationFinder {

    private static ClassLoader loadExternalClasses(String projectPath) throws Exception {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project directory does not exist: " + projectPath);
        }

        File targetClassesDir = findTargetClassesDir(projectDir);
        if (targetClassesDir == null) {
            throw new IllegalArgumentException("No valid target/classes directory found in: " + projectPath);
        }

        URL url = targetClassesDir.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

        // 設定當前執行緒的 ClassLoader，確保 Reflections 使用此 ClassLoader
        Thread.currentThread().setContextClassLoader(classLoader);
        return classLoader;
    }

    // 遞迴尋找 `target/classes/` 目錄
    private static File findTargetClassesDir(File rootDir) {
        File[] files = rootDir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getAbsolutePath().endsWith("target/classes")) {
                    return file;
                }
                File found = findTargetClassesDir(file);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static Set<Class<?>> findImplementations(Class<?> interfaceClass, String basePackage) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(basePackage))
                        .setScanners(new SubTypesScanner())
        );

        return reflections.getSubTypesOf((Class<Object>) interfaceClass);
    }

    public static void main(String[] args) throws Exception {
        String adminServicePath = "/home/popocorn/output/CustomerService";  // 目標專案的根目錄
        String interfaceClassName = "com.app.services.ProductService";  // 目標 Interface (請修改)
        String targetPackage = "com.app";  // 目標 package (請修改)

        // 1. 動態加載外部專案 classes
        ClassLoader classLoader = loadExternalClasses(adminServicePath);

        // 2. 透過動態加載取得 Interface 的 Class 物件
        Class<?> targetInterface = Class.forName(interfaceClassName, true, classLoader);

        // 3. 搜尋 interface 的所有實作類
        Set<Class<?>> implementations = findImplementations(targetInterface, targetPackage);

        // 4. 列出所有找到的 class
        implementations.forEach(impl -> System.out.println("Found: " + impl.getName()));
    }
}

