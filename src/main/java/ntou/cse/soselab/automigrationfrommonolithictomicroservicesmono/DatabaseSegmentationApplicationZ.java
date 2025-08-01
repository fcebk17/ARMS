package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.controller.WebController.DatabaseSegmentationResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SpringBootApplication
public class DatabaseSegmentationApplicationZ {

    private static Map<String, List<String>> duplicateRepoToServices = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(DatabaseSegmentationApplicationZ.class, args);
    }

    /**
     * 處理資料庫分割的主要方法 - 接受前端參數
     */
    public DatabaseSegmentationResult processWithParameters(String basePath, String sourceProjectPath, List<String> groupNames) throws Exception {
        System.out.println("開始處理資料庫分割...");
        System.out.println("Base Path: " + basePath);
        System.out.println("Source Project Path: " + sourceProjectPath);
        System.out.println("Microservices: " + groupNames);

        // 確保路徑以 / 結尾
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        String packageName = "";
        final String PACKAGE_NAME;

        Map<String, Map<String, List<String>>> controllerToServiceMap = new LinkedHashMap<>();
        Map<String, String> interfaceToImplementationMap = new LinkedHashMap<>();
        Set<Map<String, List<String>>> serviceToRepositorySet = new LinkedHashSet<>();
        Map<String, Set<String>> microserviceToRepositoryMap = new HashMap<>();
        Map<String, Map<String, List<String>>> microserviceToServiceImplToRepositoryMap = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, String>>> repositoryMethodParametersMap = new HashMap<>();

        // 收集 import 資訊
        ImportCollector collector = new ImportCollector(sourceProjectPath);
        Map<String, String> importMap = collector.getAllImports();
        System.out.println("Imports Map: " + importMap);

        // 搜尋每個 Controller 中的 @Autowired 介面
        for (String groupName : groupNames) {
            String groupPath = basePath + groupName;

            // 檢查目錄是否存在，如果不存在則建立
            File groupDir = new File(groupPath);
            if (!groupDir.exists()) {
                groupDir.mkdirs();
                System.out.println("建立目錄: " + groupPath);
            }

            ControllerAutowiredFinder controllerAutowiredFinder = new ControllerAutowiredFinder(groupPath);
            controllerAutowiredFinder.process();
            packageName = controllerAutowiredFinder.getPackageName();

            // 取得新的結果
            Map<String, List<String>> newResults = controllerAutowiredFinder.getControllerAutowiredMap();

            // 如果 serviceMap 內還沒有這個 groupName，則新增
            controllerToServiceMap.putIfAbsent(groupName, new LinkedHashMap<>());

            // 將新結果合併到 serviceMap
            for (Map.Entry<String, List<String>> entry : newResults.entrySet()) {
                String controllerName = entry.getKey();
                List<String> interfaces = entry.getValue();

                controllerToServiceMap.get(groupName).merge(controllerName, interfaces, (existing, newList) -> {
                    existing.addAll(newList);
                    return existing;
                });
            }
        }
        System.out.println("controllerToServiceMap: " + controllerToServiceMap);
        PACKAGE_NAME = removeLastPackageSegment(packageName);

        // 搜尋每個介面的實作類別
        for (String groupName : groupNames) {
            Map<String, List<String>> controllerMap = controllerToServiceMap.get(groupName);
            if (controllerMap == null) continue;

            Set<String> uniqueServiceInterfaces = new HashSet<>();
            for (List<String> serviceList : controllerMap.values()) {
                uniqueServiceInterfaces.addAll(serviceList);
            }

            for (String serviceType : uniqueServiceInterfaces) {
                if (isInterface(serviceType, basePath + groupName, PACKAGE_NAME)) {
                    InterfaceImplFinder implFinder = new InterfaceImplFinder(
                            basePath + groupName,
                            serviceType,
                            PACKAGE_NAME
                    );
                    implFinder.printImplementations();
                    Map<String, String> partialMap = implFinder.getInterfaceToImplementationMap();
                    System.out.println("Interface to Implementation Map: " + partialMap);
                    interfaceToImplementationMap.putAll(partialMap);
                }
                else {
                    System.out.println("I'm normal class");
                    interfaceToImplementationMap.put(serviceType, serviceType);
                }
            }
        }

        System.out.println("interfaceToImplementationMap: " + interfaceToImplementationMap);

        // 找到每個 ServiceImpl 中的 repository
        for (String groupName : groupNames) {
            for (String implementation : interfaceToImplementationMap.values()) {
                ServiceImplAutowiredRepositoryFinder finder = new ServiceImplAutowiredRepositoryFinder(basePath + groupName, implementation);
                finder.scan();

                Map<String, List<String>> currentResult = finder.getAutowiredRepositories();

                if(!serviceToRepositorySet.contains(currentResult)) {
                    serviceToRepositorySet.add(new LinkedHashMap<>(currentResult));
                }
            }
        }
        System.out.println("serviceToRepositorySet: " + serviceToRepositorySet);

        // 將每個 microservice 中所使用到的 repository 記錄下來
        for (String moduleName : controllerToServiceMap.keySet()) {
            Set<String> repositories = new HashSet<>();
            Map<String, List<String>> controllers = controllerToServiceMap.get(moduleName);

            for (List<String> serviceInterfaces : controllers.values()) {
                for (String serviceInterface : serviceInterfaces) {
                    String impl = interfaceToImplementationMap.get(serviceInterface);
                    if (impl != null) {
                        for (Map<String, List<String>> implRepoMap : serviceToRepositorySet) {
                            if(implRepoMap.containsKey(impl)) {
                                repositories.addAll(implRepoMap.get(impl));
                            }
                        }
                    }
                }
            }
            microserviceToRepositoryMap.put(moduleName, repositories);
        }

        System.out.println("microserviceToRepositoryMap: " + microserviceToRepositoryMap);

        // 生成 module -> ServiceImpl -> repository 資料結構
        for (Map.Entry<String, Map<String, List<String>>> moduleEntry : controllerToServiceMap.entrySet()) {
            String moduleName = moduleEntry.getKey();
            Map<String, List<String>> controllerMap = moduleEntry.getValue();

            Map<String, List<String>> serviceImplToRepoMap = new LinkedHashMap<>();

            for (List<String> serviceInterfaces : controllerMap.values()) {
                for (String serviceInterface : serviceInterfaces) {
                    String implClass = interfaceToImplementationMap.get(serviceInterface);
                    if (implClass == null) continue;

                    for (Map<String, List<String>> serviceRepoMap : serviceToRepositorySet) {
                        if (serviceRepoMap.containsKey(implClass)) {
                            serviceImplToRepoMap.put(implClass, serviceRepoMap.get(implClass));
                            break;
                        }
                    }
                }
            }

            microserviceToServiceImplToRepositoryMap.put(moduleName, serviceImplToRepoMap);
        }

        System.out.println("microserviceToServiceImplToRepositoryMap: " + microserviceToServiceImplToRepositoryMap);

        boolean isThereDuplicateRepositories = checkForDuplicateRepositories(microserviceToRepositoryMap);

        // 找到所有 repository 使用到的 method
        RepositoryUsageFinder repositoryUsageFinder = new RepositoryUsageFinder(basePath, microserviceToRepositoryMap);
        repositoryUsageFinder.scan();
        repositoryMethodParametersMap = repositoryUsageFinder.getRepositoryMethodParameters();
        System.out.println("repositoryMethodParameters: " + repositoryMethodParametersMap);

        // 處理重複或非重複的情況
        if (!isThereDuplicateRepositories) {
            System.out.println("沒有重複的 Repository，進行清理作業");
            NoDuplicateRepositoryCleaner cleaner = new NoDuplicateRepositoryCleaner(microserviceToRepositoryMap, basePath);
            cleaner.cleanUnusedRepositories();
        } else {
            System.out.println("發現重複的 Repository，建立專用 Controller");
            System.out.println("duplicateRepoToServices: " + getDuplicateRepoToServices());

            // 處理重複 Repository 的邏輯
            processeDuplicateRepositories(basePath, repositoryMethodParametersMap, importMap);
        }

        // 建立結果物件
        DatabaseSegmentationResult result = new DatabaseSegmentationResult();
        result.setControllerToServiceMap(controllerToServiceMap);
        result.setInterfaceToImplementationMap(interfaceToImplementationMap);
        result.setMicroserviceToRepositoryMap(microserviceToRepositoryMap);
        result.setHasDuplicateRepositories(isThereDuplicateRepositories);
        result.setDuplicateRepositories(getDuplicateRepoToServices());

        System.out.println("資料庫分割處理完成！");
        return result;
    }

    /**
     * 處理重複 Repository 的邏輯
     */
    private void processeDuplicateRepositories(String basePath,
                                               Map<String, Map<String, Map<String, String>>> repositoryMethodParametersMap,
                                               Map<String, String> importMap) {
        for (String moduleName : duplicateRepoToServices.keySet()) {
            String serviceName = moduleName.substring(moduleName.lastIndexOf(".") + 1) + "Service";
            String path = basePath + serviceName;
            String controllerName = moduleName.substring(moduleName.lastIndexOf(".") + 1) + "Controller";

            System.out.println("path: " + path);

            try {
                // 找到 controller 路徑
                ControllerPathFinder finder = new ControllerPathFinder(path);
                String controllerPath = finder.getControllerDirectory();
                System.out.println("Controller Path: " + controllerPath);

                // 找 controller annotation
                String controllerAnnotation = finder.getControllerAnnotationType();
                System.out.println("Controller Annotation: " + controllerAnnotation);

                // 建立空的 controller
                ControllerGenerator controllerGenerator = new ControllerGenerator(controllerPath, controllerName, controllerAnnotation);
                if (controllerGenerator.generateController()) {
                    System.out.println("Controller build success");

                    // 呼叫 RestApiMethodInjector，自動插入對應的 REST API
                    String repoName = moduleName.substring(moduleName.lastIndexOf(".") + 1);
                    System.out.println("repoName: "+ repoName);
                    Map<String, Map<String, String>> methodMap = repositoryMethodParametersMap.get(repoName);
                    System.out.println("methodMap: " + methodMap);
                    if (methodMap != null) {
                        RestApiMethodInjector injector = new RestApiMethodInjector(controllerPath, controllerName, methodMap, importMap);
                        injector.inject();
                    } else {
                        System.out.println("No method injected into controller");
                    }

                } else {
                    System.out.println("Controller build failed");
                }
            } catch (Exception e) {
                System.err.println("處理重複 Repository 時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 判斷是否為介面
     */
    public static boolean isInterface(String typeName, String basePath, String packageName) {
        String path = basePath + "/" + typeName.replace('.', '/') + ".java";
        File file = new File(path);
        if (!file.exists()) return false;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("public interface " + typeName) || line.startsWith("interface " + typeName)) {
                    return true;
                }
                if (line.startsWith("public class " + typeName) || line.startsWith("class " + typeName)) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 移除 package 名稱的最後一層
     */
    private static String removeLastPackageSegment(String packageName) {
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return packageName.substring(0, lastDotIndex);
        }
        return packageName;
    }

    /**
     * 判斷是否有重複的 repository
     */
    public static boolean checkForDuplicateRepositories(Map<String, Set<String>> serviceRepoMap) {
        duplicateRepoToServices.clear();

        Map<String, List<String>> repoToServices = new HashMap<>();

        // 遍歷每個 Service 及其 repositories
        for (Map.Entry<String, Set<String>> entry : serviceRepoMap.entrySet()) {
            String serviceName = entry.getKey();
            Set<String> repositories = entry.getValue();

            // 記錄每個 repository 對應的 Service
            for (String repo : repositories) {
                if (!repoToServices.containsKey(repo)) {
                    repoToServices.put(repo, new ArrayList<>());
                }
                repoToServices.get(repo).add(serviceName);
            }
        }

        // 找出重複的 repository（出現在多個 Service 中的）
        boolean hasDuplicate = false;

        for (Map.Entry<String, List<String>> entry : repoToServices.entrySet()) {
            String repo = entry.getKey();
            List<String> services = entry.getValue();

            if (services.size() > 1) {
                hasDuplicate = true;
                duplicateRepoToServices.put(repo, services);
            }
        }

        if (!hasDuplicate) {
            System.out.println("No duplicate repositories");
        }

        return hasDuplicate;
    }

    /**
     * 取得重複的 Repository 對應的服務列表
     */
    public static Map<String, List<String>> getDuplicateRepoToServices() {
        return duplicateRepoToServices;
    }
}