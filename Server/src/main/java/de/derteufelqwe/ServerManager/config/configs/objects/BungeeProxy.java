package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BungeeProxy extends ServerObjBase {

    // Port of the proxy
    private int port;

}
