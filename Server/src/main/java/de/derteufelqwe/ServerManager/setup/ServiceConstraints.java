package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.exceptions.DockerMCException;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ServiceConstraints {

    private List<String> idConstraints = new ArrayList<>();
    private List<String> nameConstraints = new ArrayList<>();
    private List<String> roleConstraints = new ArrayList<>();
    private int nodeLimit = 0;

    public ServiceConstraints(@Nullable List<String> idConstraints, @Nullable List<String> nameConstraints,
                              @Nullable List<String> roleConstraints, int nodeLimit) {
        if (idConstraints != null) {
            this.idConstraints = idConstraints;
        }
        if (nameConstraints != null) {
            this.nameConstraints = nameConstraints;
        }
        if (roleConstraints != null) {
            this.roleConstraints = roleConstraints;
        }

        if (nodeLimit < 0) {
            throw new DockerMCException("Nodelimit cant be negative.");
        }
    }

    public ServiceConstraints(int nodeLimit) {
        this(null, null, null, nodeLimit);
    }

    /**
     * Formats the constraints to the docker format.
     *
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

        // Role constraints
        for (String roleConstraint : this.roleConstraints) {
            if (roleConstraint.substring(0, 1).equals("!")) {
                constraintList.add("node.role!=" + roleConstraint);

            } else {
                constraintList.add("node.role==" + roleConstraint);
            }
        }

        return constraintList;
    }

    public int getNodeLimit() {
        return nodeLimit;
    }

}
