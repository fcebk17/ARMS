package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SpringDependencyParser {
    private static final Map<String, Set<String>> controllerToServiceMap = new HashMap<>();
    private static final Map<String, Set<String>> serviceToRepositoryMap = new HashMap<>();
    private static final Map<String, String> serviceInterfaceToImplMap = new HashMap<>(); // 介面 -> 具體實作
    private static final Set<String> controllerClasses = new HashSet<>();
    private static final Set<String> serviceClasses = new HashSet<>();
    private static final Set<String> repositoryClasses = new HashSet<>();
    private static final Set<String> services = new HashSet<>();

    private static final List<CompilationUnit> compilationUnits = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // 設定要掃描的專案根目錄 (請修改為你的 Spring Boot 專案的路徑)
        String projectRoot = "/home/popocorn/output/AdminService";

        Files.walk(Paths.get(projectRoot))
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(SpringDependencyParser::parseJavaFile);

        System.out.println("\n==== Finished ClassVisitor, now running ControllerVisitor ====\n");

        // First time visit, 輸出結果
        System.out.println("controllerToServiceMap: " + controllerToServiceMap);
        System.out.println("serviceToRepositoryMap: " + serviceToRepositoryMap);
        System.out.println("serviceInterfaceToImplMap :" + serviceInterfaceToImplMap);
        System.out.println("controllerClasses: " + controllerClasses);
        System.out.println("serviceClasses: " + serviceClasses);
        System.out.println("repositoryClasses: " + repositoryClasses);

        // 所有 ClassVisitor 執行完後，統一執行 ControllerVisitor
        for (CompilationUnit cu : compilationUnits) {
            new ControllerVisitor().visit(cu, null);
        }

        printDependencyMapping();
    }

    private static void parseJavaFile(Path path) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(path);

            new ClassVisitor().visit(cu, null);
            compilationUnits.add(cu);

        } catch (IOException | ParseProblemException e) {
            System.err.println("Error parsing file: " + path);
        }
    }
    static int t = 1;

    private static class ControllerVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
            String className = classDecl.getNameAsString();

            System.out.println("Second visit");
            System.out.println(controllerClasses +" and " + className);
            if (controllerClasses.contains(className.trim())) {
                System.out.println(className + " is a controller");
                System.out.println(serviceInterfaceToImplMap);
                System.out.println(serviceInterfaceToImplMap.containsKey(className));
                System.out.println(serviceInterfaceToImplMap.containsKey(className + "Impl"));

                // **如果 `@Autowired` 是 interface，找到對應的 `@Service`**
                if (serviceInterfaceToImplMap.containsKey(className)) { // if any key of serviceType in serviceInterfaceToImplMap, do
                    System.out.println("a");
                    services.add(serviceInterfaceToImplMap.get(className));
                } else if (serviceClasses.contains(className + "Impl")) {
                    services.add(className + "Impl");
                }
                controllerToServiceMap.put(className, services);
            }

            // 找出 `Controller` 內的 `@Autowired Service`
//            if (hasAnnotation(classDecl, "RestController") || hasAnnotation(classDecl, "Controller")) {
//                Set<String> services = new HashSet<>();
//                System.out.println("serviceInterfaceToImplMap: " + serviceInterfaceToImplMap);
//                serviceInterfaceToImplMap.keySet().forEach(System.out::println);
//
//                classDecl.findAll(FieldDeclaration.class).forEach(field -> {
//                    if (hasAnnotation(field, "Autowired")) {
//                        field.getVariables().forEach(var -> {
//                            String serviceType = var.getType().toString();
//
//                            // **如果 `@Autowired` 是 interface，找到對應的 `@Service`**
//                            if (serviceInterfaceToImplMap.containsKey(serviceType)) { // if any key of serviceType in serviceInterfaceToImplMap, do
//                                System.out.println("a");
//                                services.add(serviceInterfaceToImplMap.get(serviceType));
//                            } else if (serviceClasses.contains(serviceType + "Impl")) {
//                                services.add(serviceType);
//                            }
//                        });
//                    }
//                });
//
//                controllerToServiceMap.put(className, services);
//            }
            super.visit(classDecl, arg);
        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
            String className = classDecl.getNameAsString();

            System.out.println(t + ": " + className);
            t++;
            // 找出 @Repository，並加入 repositoryClasses
            if (hasAnnotation(classDecl, "Repository")) {
                repositoryClasses.add(className);
            }

            // 找出 @Service，並加入 serviceClasses
            if (hasAnnotation(classDecl, "Service")) {
                serviceClasses.add(className);

                // **檢查是否 implements 了某個介面**
                classDecl.getImplementedTypes().forEach(implType -> {
                    String interfaceName = implType.getNameAsString();
                    serviceInterfaceToImplMap.put(interfaceName, className);
                });
            }

            // 找出 `Controller` 內的 `@Autowired Service`
            if (hasAnnotation(classDecl, "RestController") || hasAnnotation(classDecl, "Controller")) {
                Set<String> services = new HashSet<>();
                controllerClasses.add(className);
                System.out.println("controllerClasses: " + controllerClasses);
                serviceInterfaceToImplMap.keySet().forEach(System.out::println);

                classDecl.findAll(FieldDeclaration.class).forEach(field -> {
                    if (hasAnnotation(field, "Autowired")) {
                        field.getVariables().forEach(var -> {
                            String serviceType = var.getType().toString();

                            // **如果 `@Autowired` 是 interface，找到對應的 `@Service`**
                            if (serviceInterfaceToImplMap.containsKey(serviceType)) { // if any key of serviceType in serviceInterfaceToImplMap, do
                                System.out.println("a");
                                services.add(serviceInterfaceToImplMap.get(serviceType));
                            } else if (serviceClasses.contains(serviceType + "Impl")) {
                                services.add(serviceType);
                            }
                        });
                    }
                });
                controllerToServiceMap.put(className, services);
            }


            // 找出 `Service` 內的 `@Autowired Repository`
            if (serviceClasses.contains(className)) {
                Set<String> repositories = new HashSet<>();

                classDecl.findAll(FieldDeclaration.class).forEach(field -> {
                    if (hasAnnotation(field, "Autowired")) {
                        field.getVariables().forEach(var -> {
                            String repoType = var.getType().toString();
                            if (repositoryClasses.contains(repoType)) {
                                repositories.add(repoType);
                            }
                        });
                    }
                });

                serviceToRepositoryMap.put(className, repositories);
            }

            super.visit(classDecl, arg);
        }
    }

    private static boolean hasAnnotation(NodeWithAnnotations<?> node, String annotation) {
        return node.getAnnotations().stream()
                .anyMatch(a -> a.getNameAsString().equals(annotation));
    }

    private static String getFullClassName(ClassOrInterfaceDeclaration classDecl) {
        return classDecl.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pkg -> pkg.getNameAsString() + "." + classDecl.getNameAsString())
                .orElse(classDecl.getNameAsString());
    }

    private static void printDependencyMapping() {
        System.out.println("\nSpring Dependency Mapping:");
        for (Map.Entry<String, Set<String>> entry : controllerToServiceMap.entrySet()) {
            String controller = entry.getKey();
            Set<String> services = entry.getValue();

            System.out.println("Controller: " + controller);
            for (String service : services) {
                Set<String> repositories = serviceToRepositoryMap.getOrDefault(service, Collections.emptySet());
                System.out.println("  → Service: " + service);
                for (String repository : repositories) {
                    System.out.println("    → Repository: " + repository);
                }
            }
            System.out.println("-----------------------------------");
        }
    }
}

