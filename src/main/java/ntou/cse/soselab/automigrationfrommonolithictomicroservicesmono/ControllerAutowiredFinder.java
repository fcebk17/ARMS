package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ControllerAutowiredFinder {
    private final String basePath;
    private final Map<String, List<String>> controllerAutowiredMap;

    public ControllerAutowiredFinder(String basePath) {
        this.basePath = basePath;
        this.controllerAutowiredMap = new LinkedHashMap<>();
    }

    public void process() throws IOException {
        List<File> javaFiles = getJavaFiles();
        for (File file : javaFiles) {
            processJavaFile(file, javaFiles);
        }
    }

    public Map<String, List<String>> getControllerAutowiredMap() {
        return controllerAutowiredMap;
    }

    private List<File> getJavaFiles() throws IOException {
        List<File> fileList = new ArrayList<>();
        Files.walk(Paths.get(basePath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> fileList.add(path.toFile()));
        return fileList;
    }

    private void processJavaFile(File file, List<File> javaFiles) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            // 確認是否是 Controller
            boolean isController = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .anyMatch(cls -> cls.getAnnotations().stream()
                            .anyMatch(a -> a.getNameAsString().equals("RestController") ||
                                    a.getNameAsString().equals("Controller")));

            if (!isController) return;

            // 儲存所有 @Autowired 且為 Interface 的變數型別
            List<String> autowiredInterfaces = new ArrayList<>();

            // 找出 @Autowired 且為 Interface 的成員變數
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(FieldDeclaration fd, Void arg) {
                    super.visit(fd, arg);
                    if (fd.isAnnotationPresent("Autowired")) {
                        fd.getVariables().forEach(var -> {
                            String fieldType = var.getTypeAsString();
                            if (isInterface(fieldType, javaFiles)) {
                                autowiredInterfaces.add(fieldType);
                            }
                        });
                    }
                }
            }, null);

            // 如果有找到 @Autowired Interface，存入 Map
            if (!autowiredInterfaces.isEmpty()) {
                controllerAutowiredMap.put(file.getName(), autowiredInterfaces);
            }
        } catch (Exception e) {
            System.err.println("Error parsing file: " + file.getAbsolutePath());
        }
    }

    private boolean isInterface(String className, List<File> javaFiles) {
        for (File file : javaFiles) {
            if (file.getName().equals(className + ".java")) {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(file);
                    return cu.findFirst(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::isInterface)
                            .orElse(false);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
}
