package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class InterfaceImplementationFinder {
    private final String projectPath;
    private final String interfaceClassName;
    private final String basePackage;
    private ClassLoader classLoader;

    public InterfaceImplementationFinder(String projectPath, String interfaceClassName, String basePackage) throws Exception {
        this.projectPath = projectPath;
        this.interfaceClassName = interfaceClassName;
        this.basePackage = basePackage;
        this.classLoader = loadExternalClasses();
    }

    private ClassLoader loadExternalClasses() throws Exception {
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
    private File findTargetClassesDir(File rootDir) {
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

    public Set<Class<?>> findImplementations() throws ClassNotFoundException {
        // 透過動態加載取得 Interface 的 Class 物件
        Class<?> targetInterface = Class.forName(interfaceClassName, true, classLoader);

        System.out.println("targetInterface: " + targetInterface);

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(basePackage, classLoader))
                        .setScanners(new SubTypesScanner())
        );

        return reflections.getSubTypesOf((Class<Object>) targetInterface);
    }

    public void printImplementations() {
        try {
            Set<Class<?>> implementations = findImplementations();
            implementations.forEach(impl -> System.out.println("Found: " + impl.getName()));
            // 要再加上判斷條件，如果 implementations 為 @Service
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Interface class not found - " + interfaceClassName);
        }
    }
}

