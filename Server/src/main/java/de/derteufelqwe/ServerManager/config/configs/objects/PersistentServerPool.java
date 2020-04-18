package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PersistentServerPool extends ServerBase {

    // Soft playerlimit
    private int softPlayerLimit;

    @Override
    public ValidationResponse valid() {
        return null;
    }

    @Override
    public FindResponse find() {
        return null;
    }

    @Override
    public CreateResponse create() {
        return null;
    }

    @Override
    public DestroyResponse destroy() {
        return null;
    }
}
