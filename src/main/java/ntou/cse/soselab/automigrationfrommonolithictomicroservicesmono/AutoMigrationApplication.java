package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.springframework.boot.SpringApplication;

public class AutoMigrationApplication {
    public static void main(String[] args) {

        CloneProject cloneProject = new CloneProject();
        cloneProject.copyDirectory("doc/E-Commerce-Application", "test-1");
        cloneProject.copyDirectory("doc/E-Commerce-Application", "test-2");
    }
}
