package de.derteufelqwe.ServerManager.config.objects;

import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateCfg {

    @Comment("Country Code, 2 letters (e.g. DE or UK")
    private String countryCode;
    @Comment("State or province name")
    private String state;
    @Comment("City name")
    private String city;
    @Comment("Organization name")
    private String organizationName;
    @Comment("Email (Doesn't need to be a valid email)")
    private String email;

}
