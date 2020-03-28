package de.derteufelqwe.ServerManager.setup.objects;

import de.derteufelqwe.commons.Constants;

import java.io.File;

/**
 * Utility class to work with the certificates for the registry
 */
public class RegistryCertFiles {

    private File caCrt = new File(Constants.REGISTRY_CERT_PATH + "ca.crt");
    private File caKey = new File(Constants.REGISTRY_CERT_PATH + "ca.key");
    private File htpasswd = new File(Constants.REGISTRY_CERT_PATH + "htpasswd");

    public RegistryCertFiles() {
    }

    public boolean filesExist() {
        return caCrt.exists() && caKey.exists() && htpasswd.exists();
    }

    public void deleteFiles() {
        caCrt.delete();
        caKey.delete();
        htpasswd.delete();
    }

}
