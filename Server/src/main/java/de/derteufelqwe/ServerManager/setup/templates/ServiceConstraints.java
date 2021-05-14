package de.derteufelqwe.ServerManager.setup.templates;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ServiceConstraints implements Cloneable {

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

        this.nodeLimit = nodeLimit;
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


    /**
     * Custom clone implementation to clone the lists.
     * @return
     */
    @Override
    @SneakyThrows
    public ServiceConstraints clone() {
        ServiceConstraints constraints = (ServiceConstraints) super.clone();

        constraints.setIdConstraints(new ArrayList<>(this.idConstraints));
        constraints.setNameConstraints(new ArrayList<>(this.nameConstraints));
        constraints.setRoleConstraints(new ArrayList<>(this.roleConstraints));

        return constraints;
    }

}
