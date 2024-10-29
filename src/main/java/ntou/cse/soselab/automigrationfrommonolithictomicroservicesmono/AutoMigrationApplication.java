package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import java.util.List;

public class AutoMigrationApplication {
    public static void main(String[] args) {

        CloneProject cloneProject = new CloneProject();
        List<String> groupNames = cloneProject.getServiceName("A_E-Commerce", "User Role-Based");

        for (String groupName : groupNames) {
            cloneProject.copyDirectory("doc/E-Commerce-Application", groupName);

            // modify pom.xml by JDOM
            try {
                ModifyMavenSetting modifyMavenSetting = new ModifyMavenSetting(groupName +"/ECommerceApplication/pom.xml");
                modifyMavenSetting.loadPomFile();
                modifyMavenSetting.modifyArtifactId(groupName);
                modifyMavenSetting.modifyName(groupName);
                modifyMavenSetting.savePomFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
