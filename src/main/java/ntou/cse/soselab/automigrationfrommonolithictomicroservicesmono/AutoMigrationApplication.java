package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.xml.crypto.Data;
import java.util.*;

@SpringBootApplication
public class AutoMigrationApplication {
    public static void main(String[] args) throws Exception {

        DatabaseAccessing databaseAccessing = new DatabaseAccessing();
        List<String> groupNames = databaseAccessing.getServiceName("A_E-Commerce", "User Role-Based");

        String BASE_PATH = "/home/popocorn/output/";
        String packageName = "";
        final String PACKAGE_NAME;

        Map<String, Map<String, List<String>>> controllerToServiceMap = new LinkedHashMap<>();
        Map<String, String> interfaceToImplementationMap = new LinkedHashMap<>();
        Set<Map<String, List<String>>> serviceToRepositorySet = new LinkedHashSet<>();

        Map<String, Set<String>> microserviceToRepositoryMap = new HashMap<>();

        Map<String, Map<String, List<String>>> microserviceToServiceImplToRepositoryMap = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, String>>> repositoryMethodParametersMap = new HashMap<>();

        for (String groupName : groupNames) {
            CloneProject cloneProject = new CloneProject();
            cloneProject.copyDirectory("/home/popocorn/test-project/E-Commerce-Application", BASE_PATH + groupName);

            // modify pom.xml by JDOM
            try {
                ModifyMavenSetting modifyMavenSetting = new ModifyMavenSetting(BASE_PATH + groupName +"/ECommerceApplication/pom.xml");
                modifyMavenSetting.loadPomFile();
                modifyMavenSetting.modifyArtifactId(groupName);
                modifyMavenSetting.modifyName(groupName);
                modifyMavenSetting.savePomFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Use JavaParser to delete endpoint of controller
        DeleteEndpointByJavaParser deleteEndpointByJavaParser = new DeleteEndpointByJavaParser();
        List<Map<String, Object>> endpointGroupNames = deleteEndpointByJavaParser.getEndpointGroupMapping("A_E-Commerce", "User Role-Based");
        Map<String, List<String>> groupEndpointsByKey = deleteEndpointByJavaParser.classifyEndpoints(endpointGroupNames, groupNames);
        deleteEndpointByJavaParser.RestfulMethodRemoval(groupEndpointsByKey);

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
                    InterfaceImplementationFinder finder = new InterfaceImplementationFinder(
                            BASE_PATH + groupName,
                            serviceInterface,
                            PACKAGE_NAME
                    );
                    finder.printImplementations();

                    Map<String, String> partialMap = finder.getInterfaceToImplementationMap();
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

        if (!isThereDuplicateRepositories) {
            NoDuplicateRepositoryCleaner cleaner = new NoDuplicateRepositoryCleaner(microserviceToRepositoryMap, BASE_PATH);
            cleaner.cleanUnusedRepositories();
        }

        RepositoryUsageFinder finder = new RepositoryUsageFinder(BASE_PATH, microserviceToRepositoryMap);
        finder.scan();
        // finder.printRepositoryMethodUsage();
        repositoryMethodParametersMap = finder.getRepositoryMethodParameters();
        System.out.println("repositoryMethodParameters: " + repositoryMethodParametersMap);
    }


    // 刪除 package 名稱的最後一層 (e.g., com.app.controllers -> com.app)
    private static String removeLastPackageSegment(String packageName) {
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return packageName.substring(0, lastDotIndex); // 移除最後一層
        }
        return packageName; // 如果沒有 `.`，則回傳原本的 package
    }

    // 判斷是否有重複的 repository
    public static boolean checkForDuplicateRepositories(Map<String, Set<String>> serviceRepoMap) {
        Set<String> allRepositories = new HashSet<>();

        for (Set<String> repoSet : serviceRepoMap.values()) {
            for (String repo : repoSet) {
                if (!allRepositories.add(repo)) {
                    return true; // 重複出現
                }
            }
        }

        return false; // 沒有重複
    }

}
