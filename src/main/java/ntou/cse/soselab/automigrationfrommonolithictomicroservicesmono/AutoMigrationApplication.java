package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoMigrationApplication {
    public static void main(String[] args) {

        CloneProject cloneProject = new CloneProject();
        List<String> groupNames = cloneProject.getServiceName("A_E-Commerce", "User Role-Based");

        for (String groupName : groupNames) {
            cloneProject.copyDirectory("/home/popocorn/test-project/E-Commerce-Application", "/home/popocorn/output/" + groupName);

            // modify pom.xml by JDOM
            try {
                ModifyMavenSetting modifyMavenSetting = new ModifyMavenSetting("/home/popocorn/output/" + groupName +"/ECommerceApplication/pom.xml");
                modifyMavenSetting.loadPomFile();
                modifyMavenSetting.modifyArtifactId(groupName);
                modifyMavenSetting.modifyName(groupName);
                modifyMavenSetting.savePomFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        DeleteEndpointByJavaParser deleteEndpointByJavaParser = new DeleteEndpointByJavaParser();
        List<Map<String, Object>> endpointGroupNames = deleteEndpointByJavaParser.getEndpointGroupMapping("A_E-Commerce", "User Role-Based");
        Map<String, List<String>> groupEndpointsByKey = deleteEndpointByJavaParser.classifyEndpoints(endpointGroupNames, groupNames);
        deleteEndpointByJavaParser.RestfulMethodRemoval(groupEndpointsByKey);
    }
}
