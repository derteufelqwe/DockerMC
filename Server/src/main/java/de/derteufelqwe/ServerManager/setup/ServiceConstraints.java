package de.derteufelqwe.ServerManager.setup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConstraints {

    private List<String> idConstraints = new ArrayList<>();
    private List<String> nameConstraints = new ArrayList<>();
    private int nodeLimit = 0;

    public ServiceConstraints(int nodeLimit) {
        this.nodeLimit = nodeLimit;
    }

    /**
     * Formats the constraints to the docker format.
     * @return List of constraints
     */
    public List<String> getDockerConstraints() {
        List<String> constraintList = new ArrayList<>();

        // ID constraints
        for (String idConstr : this.idConstraints) {
            if (idConstr.substring(0, 1).equals("!")) {
                constraintList.add("node.id!=" + idConstr.substring(1));

            } else {
                constraintList.add("node.id==" + idConstr);
            }
        }

        // Name constraints
        for (String nameConstr : this.nameConstraints) {
            if (nameConstr.substring(0, 1).equals("!")) {
               constraintList.add("node.labels.name!=" + nameConstr.substring(1));

            } else {
                constraintList.add("node.labels.name==" + nameConstr);
            }
        }

        return constraintList;
    }

    public int getNodeLimit() {
        return nodeLimit;
    }

}
