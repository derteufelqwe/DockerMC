package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServerPool extends ServerObjBase {

    // Soft playerlimit
    private int softPlayerLimit;

}
