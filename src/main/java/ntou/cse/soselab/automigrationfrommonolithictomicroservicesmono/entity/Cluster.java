package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "clusters")
public class Cluster {

    private String projectName;
    private String concept;
    private int numberOfGroups;
    private List<GroupName> groupNames;
    private List<EndpointGroup> endpointGroupMapping;

    public Cluster(String projectName, String concept, int numberOfGroups, List<GroupName> groupNames, List<EndpointGroup> endpointGroupMapping) {
        this.projectName = projectName;
        this.concept = concept;
        this.numberOfGroups = numberOfGroups;
        this.groupNames = groupNames;
        this.endpointGroupMapping = endpointGroupMapping;
    }

    public static class GroupName {
        private String id;
        private String serviceName;

        public GroupName(String id, String serviceName) {
            this.id = id;
            this.serviceName = serviceName;
        }
    }

    public static class EndpointGroup {
        private String id;
        private String endpoint;
        private int service;

        public EndpointGroup(String id, String endpoint, int service) {
            this.id = id;
            this.endpoint = endpoint;
            this.service = service;
        }
    }
}
