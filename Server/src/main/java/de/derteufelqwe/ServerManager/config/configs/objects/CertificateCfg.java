package de.derteufelqwe.ServerManager.config.configs.objects;

import de.derteufelqwe.ServerManager.config.YAMLComment;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertificateCfg {

    @YAMLComment("Country Code, 2 letters (can be empty)")
    private String countryCode = "DE";
    @YAMLComment("State or province name (can be empty)")
    private String state = "";
    @YAMLComment("City name (can be empty)")
    private String city = "";
    @YAMLComment("Organization name (can be empty, but should be filled)")
    private String organizationName = "DockerMCTest";
    @YAMLComment("Email (can be empty, doesn't need to be a valid email)")
    private String email = "test@test.com";

    /*
     Signature Algorithm: sha256WithRSAEncryption
        Issuer: C = AA, ST = BB, L = CC, O = DD, OU = EE, CN = FF, emailAddress = GG
        Validity
     */

}
