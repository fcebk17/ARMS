package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.springframework.boot.SpringApplication;

import java.util.List;

public class AutoMigrationApplication {
    public static void main(String[] args) {

        CloneProject cloneProject = new CloneProject();
        List<String> groupNames = cloneProject.getServiceName("A_E-Commerce", "User Role-Based");

        for (String groupName : groupNames) {
            cloneProject.copyDirectory("doc/E-Commerce-Application", groupName);
        }


    }
}
