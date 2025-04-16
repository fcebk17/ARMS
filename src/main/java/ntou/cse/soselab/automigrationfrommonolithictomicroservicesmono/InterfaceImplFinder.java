package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InterfaceImplFinder {
    private final String projectPath;
    private final String interfaceName;
    private final String basePackage;
    private final Map<String, String> interfaceToImplementationMap = new HashMap<>();

    public InterfaceImplFinder(String projectPath, String interfaceName, String basePackage) {
        this.projectPath = projectPath;
        this.interfaceName = extractSimpleName(interfaceName);
        this.basePackage = basePackage;
    }

    private String extractSimpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    public Map<String, String> getInterfaceToImplementationMap() {
        return interfaceToImplementationMap;
    }

    public void findImplementations() {
        try {
            // 找到所有Java源代碼文件
            List<File> javaFiles = findJavaFiles(projectPath);

            // 找到介面定義文件
            File interfaceFile = findInterfaceFile(javaFiles);
            if (interfaceFile == null) {
                System.err.println("Interface file not found for: " + interfaceName);
                return;
            }

            String interfaceFullName = getFullyQualifiedName(interfaceFile);
            System.out.println("Interface found: " + interfaceFullName);

            // 尋找所有實現該介面的類
            for (File file : javaFiles) {
                if (isImplementationOf(file, interfaceName)) {
                    String implFullName = getFullyQualifiedName(file);
                    boolean isService = hasServiceAnnotation(file);

                    System.out.println("Found implementation: " + implFullName +
                            (isService ? " (@Service)" : ""));

                    interfaceToImplementationMap.put(interfaceFullName, implFullName);
                }
            }

            if (interfaceToImplementationMap.isEmpty()) {
                System.out.println("No implementations found for interface: " + interfaceName);
            }

        } catch (IOException e) {
            System.err.println("Error scanning files: " + e.getMessage());
        }
    }

    private List<File> findJavaFiles(String rootPath) throws IOException {
        List<File> javaFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
            javaFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
        return javaFiles;
    }

    private File findInterfaceFile(List<File> javaFiles) {
        for (File file : javaFiles) {
            try {
                if (isInterface(file, interfaceName)) {
                    return file;
                }
            } catch (IOException e) {
                // 忽略解析錯誤，繼續檢查下一個文件
            }
        }
        return null;
    }

    private boolean isInterface(File file, String interfaceName) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) type;
                if (declaration.isInterface() && declaration.getNameAsString().equals(interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isImplementationOf(File file, String interfaceName) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            for (TypeDeclaration<?> type : cu.getTypes()) {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
                    if (!classDecl.isInterface()) {
                        for (com.github.javaparser.ast.type.Type implementedType : classDecl.getImplementedTypes()) {
                            if (implementedType.asString().equals(interfaceName)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // 忽略解析錯誤
        }
        return false;
    }

    private String getFullyQualifiedName(File file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            for (TypeDeclaration<?> type : cu.getTypes()) {
                return packageName.isEmpty()
                        ? type.getNameAsString()
                        : packageName + "." + type.getNameAsString();
            }
        } catch (IOException e) {
            // 忽略解析錯誤
        }
        return file.getName().replace(".java", "");
    }

    private boolean hasServiceAnnotation(File file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            for (TypeDeclaration<?> type : cu.getTypes()) {
                for (AnnotationExpr annotation : type.getAnnotations()) {
                    String name = annotation.getNameAsString();
                    if (name.equals("Service") || name.equals("org.springframework.stereotype.Service")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            // 忽略解析錯誤
        }
        return false;
    }

    public void printImplementations() {
        findImplementations();
        System.out.println("\n結果摘要:");
        interfaceToImplementationMap.forEach((key, value) ->
                System.out.println(key + " -> " + value)
        );
    }
}