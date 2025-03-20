package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.io.IOException;
import java.util.*;

public class AutoMigrationApplication {
    public static void main(String[] args) throws IOException {

        CloneProject cloneProject = new CloneProject();
        List<String> groupNames = cloneProject.getServiceName("A_E-Commerce", "User Role-Based");
        String BASE_PATH = "/home/popocorn/output/";
        Map<String, Map<String, List<String>>> serviceMap = new LinkedHashMap<>();
        String packageName = "ntou.cse.soselab";
        final String PACKAGE_NAME;


        for (String groupName : groupNames) {
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
            serviceMap.putIfAbsent(groupName, new LinkedHashMap<>());

            // 將新結果合併到 serviceMap
            for (Map.Entry<String, List<String>> entry : newResults.entrySet()) {
                String controllerName = entry.getKey();
                List<String> interfaces = entry.getValue();

                // 如果該 groupName 下的 Controller 已經存在，則合併 Interface 列表
                serviceMap.get(groupName).merge(controllerName, interfaces, (existing, newList) -> {
                    existing.addAll(newList);
                    return existing;
                });
            }
        }
        System.out.println(serviceMap);
        PACKAGE_NAME = removeLastPackageSegment(packageName);
    }

    // 刪除 package 名稱的最後一層 (e.g., com.app.controllers -> com.app)
    private static String removeLastPackageSegment(String packageName) {
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return packageName.substring(0, lastDotIndex); // 移除最後一層
        }
        return packageName; // 如果沒有 `.`，則回傳原本的 package
    }

}
