package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.util.List;
import java.util.Map;

public class FileProcessingApplication {
    public static void main(String[] args) {
        List<String> groupNames = getServiceName();

        CloneProject cloneProject = new CloneProject();
        String BASE_PATH = "/home/popocorn/output_test";

        for (String groupName : groupNames) {
            cloneProject.copyDirectory("/home/popocorn/test-project/Online-Shopping-App-SpringBoot-", BASE_PATH + groupName);

            // modify pom.xml by JDOM
            try {
                ModifyMavenSetting modifyMavenSetting = new ModifyMavenSetting(BASE_PATH + groupName +"/OnlineShopingApp/pom.xml");
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
    }

    // temporary function
    public static List<String> getServiceName() {
        DatabaseAccessing databaseAccessing = new DatabaseAccessing();
        return databaseAccessing.getServiceName("A_E-Commerce", "User Role-Based");
    }
}
