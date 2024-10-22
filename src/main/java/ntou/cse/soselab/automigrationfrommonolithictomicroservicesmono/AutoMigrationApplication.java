package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.springframework.boot.SpringApplication;

public class AutoMigrationApplication {
    public static void main(String[] args) {

        CloneProject cloneProject = new CloneProject();

        int numberOfGroups = cloneProject.getNumberOfGroups("A_E-Commerce", "User Role-Based");

        for (int i = 1; i <= numberOfGroups; i++) {
            cloneProject.copyDirectory("doc/E-Commerce-Application", "test-" + i);
        }


    }
}
