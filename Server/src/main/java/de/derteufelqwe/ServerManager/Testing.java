package de.derteufelqwe.ServerManager;

import lombok.SneakyThrows;
import sun.security.x509.X509CertImpl;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.X509Certificate;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {
        File certFile = new File("C:/Users/Arne/Desktop/ServerManager/Server/server/internal/security/registry-certs/ca.crt");
        X509Certificate certificate = new X509CertImpl(new FileInputStream(certFile));
        System.out.println(certificate.getNotAfter());

        System.out.println(System.getProperty("user.dir"));
    }

}
