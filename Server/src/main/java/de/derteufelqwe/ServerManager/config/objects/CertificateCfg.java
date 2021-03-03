package de.derteufelqwe.ServerManager.config.objects;

import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertificateCfg {

    @Comment("Country Code, 2 letters (can be empty)")
    private String countryCode = "DE";
    @Comment("State or province name (can be empty)")
    private String state = "";
    @Comment("City name (can be empty)")
    private String city = "";
    @Comment("Organization name (can be empty, but should be filled)")
    private String organizationName = "DockerMCTest";
    @Comment("Email (can be empty, doesn't need to be a valid email)")
    private String email = "test@test.com";

    /*
     Signature Algorithm: sha256WithRSAEncryption
        Issuer: C = AA, ST = BB, L = CC, O = DD, OU = EE, CN = FF, emailAddress = GG
        Validity
     */



}
