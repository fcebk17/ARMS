package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import lombok.Value;
import ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.service.OpenAiService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.*;

@SpringBootApplication
public class DatabaseSegmentationApplication {

    private static final Map<String, Map<String, List<String>>> controllerToServiceMap = new LinkedHashMap<>();
    private static final Set<Map<String, List<String>>> serviceToRepositorySet = new LinkedHashSet<>();

    public static void main(String[] args) throws Exception {
//        FileProcessingApplication application = new FileProcessingApplication();
//        List<String> groupNames = application.getServiceName();
        List<String> groupNames = Arrays.asList("AccountService", "ProductService", "TransactionService");

        String BASE_PATH = "/home/popocorn/output_book_store/";
        String packageName = "";
        final String PACKAGE_NAME;

//        Map<String, Map<String, List<String>>> controllerToServiceMap = new LinkedHashMap<>();
        Map<String, String> interfaceToImplementationMap = new LinkedHashMap<>();
//        Set<Map<String, List<String>>> serviceToRepositorySet = new LinkedHashSet<>();

        Map<String, Set<String>> microserviceToRepositoryMap = new HashMap<>();

        Map<String, Map<String, List<String>>> microserviceToServiceImplToRepositoryMap = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, String>>> repositoryMethodParametersMap = new HashMap<>();

        ImportCollector collector = new ImportCollector("/home/popocorn/test-project/book-store-api/src/main/java/com/example/bookstore_api");
        Map<String, String> importMap = collector.getAllImports();
        System.out.println("Imports Map: " + importMap);

        // Search `@Autowired interfaces in each Controller
        for (String groupName : groupNames) {
            ControllerAutowiredFinder controllerAutowiredFinder = new ControllerAutowiredFinder(BASE_PATH + groupName);
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

                // 如果該 groupName 下的 Controller 已經存在，則合併 Interface 列表
                controllerToServiceMap.get(groupName).merge(controllerName, interfaces, (existing, newList) -> {
                    existing.addAll(newList);
                    return existing;
                });
            }
        }
        System.out.println("controllerToServiceMap: " + controllerToServiceMap);
        PACKAGE_NAME = removeLastPackageSegment(packageName);

        // Search each interface implementation class
        // Not yet finished
        for (String groupName : groupNames) {
            Map<String, List<String>> controllerMap = controllerToServiceMap.get(groupName);
            if (controllerMap == null) continue;

            Set<String> uniqueServiceInterfaces = new HashSet<>();
            for (List<String> serviceList : controllerMap.values()) {
                uniqueServiceInterfaces.addAll(serviceList);
            }

            for (String serviceInterface : uniqueServiceInterfaces) {
                try {
                    InterfaceImplFinder implFinder = new InterfaceImplFinder(
                            BASE_PATH + groupName,
                            serviceInterface,
                            PACKAGE_NAME
                    );
                    implFinder.printImplementations();

                    Map<String, String> partialMap = implFinder.getInterfaceToImplementationMap();
                    System.out.println("Interface to Implementation Map: " + partialMap);
                    // put into interfaceToImplementationMap
                    interfaceToImplementationMap.putAll(partialMap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("interfaceToImplementationMap: " + interfaceToImplementationMap);

        // 找到每個 ServiceImpl 中的 repository
        for (String groupName : groupNames) {
            for (String implementation : interfaceToImplementationMap.values()) {
                ServiceImplAutowiredRepositoryFinder finder = new ServiceImplAutowiredRepositoryFinder(BASE_PATH + groupName, implementation);
                finder.scan();

                Map<String, List<String>> currentResult = finder.getAutowiredRepositories();

                if(!serviceToRepositorySet.contains(currentResult)) {
                    serviceToRepositorySet.add(new LinkedHashMap<>(currentResult));
                }
                // System.out.println("AutowiredRepositories: " + finder.getAutowiredRepositories());
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
            String moduleName = moduleEntry.getKey(); // e.g., AdminService
            Map<String, List<String>> controllerMap = moduleEntry.getValue();

            // 每個模組底下的 ServiceImpl 對應 Repository 列表
            Map<String, List<String>> serviceImplToRepoMap = new LinkedHashMap<>();

            for (List<String> serviceInterfaces : controllerMap.values()) {
                for (String serviceInterface : serviceInterfaces) {
                    // 取得實作類別
                    String implClass = interfaceToImplementationMap.get(serviceInterface);
                    if (implClass == null) continue;

                    // 找出對應的 Repository 列表
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
//        System.out.println("isThereDuplicateRepositories: " + isThereDuplicateRepositories);

        // 找到所有 repository 使用到的 method
        RepositoryUsageFinder repositoryUsageFinder = new RepositoryUsageFinder(BASE_PATH, microserviceToRepositoryMap);
        repositoryUsageFinder.scan();
        repositoryMethodParametersMap = repositoryUsageFinder.getRepositoryMethodParameters();
        System.out.println("repositoryMethodParameters: " + repositoryMethodParametersMap);

        // 判斷是否有重複 repository
        if (!isThereDuplicateRepositories) {
            NoDuplicateRepositoryCleaner cleaner = new NoDuplicateRepositoryCleaner(microserviceToRepositoryMap, BASE_PATH);
            cleaner.cleanUnusedRepositories();
        } else {
            System.out.println("duplicateRepoToServices: " + getDuplicateRepoToServices());
            for (String moduleName : duplicateRepoToServices.keySet()) {
                String serviceName = moduleName.substring(moduleName.lastIndexOf(".") + 1) + "Service";
                String path = BASE_PATH + serviceName;
                String controllerName = moduleName.substring(moduleName.lastIndexOf(".") + 1) + "Controller";

                // 找到 controller 路徑
                ControllerPathFinder finder = new ControllerPathFinder(path);
                String controllerPath = finder.getControllerDirectory();
                System.out.println(controllerPath);

                // 找 controller annotation
                String controllerAnnotation = finder.getControllerAnnotationType();
                System.out.println(controllerAnnotation);

                // 建立空的 controller
                ControllerGenerator controllerGenerator = new ControllerGenerator(controllerPath, controllerName, controllerAnnotation);
                controllerGenerator.generateController();
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
            }


        }


    }


    // 刪除 package 名稱的最後一層 (e.g., com.app.controllers -> com.app)
    private static String removeLastPackageSegment(String packageName) {
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return packageName.substring(0, lastDotIndex); // 移除最後一層
        }
        return packageName; // 如果沒有 `.`，則回傳原本的 package
    }

    private static Map<String, List<String>> duplicateRepoToServices = new HashMap<>();

    // 判斷是否有重複的 repository
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

    public static Map<String, List<String>> getDuplicateRepoToServices() {
        return duplicateRepoToServices;
    }

    public static Map<String, Map<String, List<String>>> getControllerToServiceMap() {
        return controllerToServiceMap;
    }

    public static Set<Map<String, List<String>>> getServiceToRepositorySet() {
        return serviceToRepositorySet;
    }
}
