package de.derteufelqwe.ServerManager.setup.objects;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import lombok.Data;

import java.io.File;

/**
 * Utility class to work with the Files for the API-Proxy
 */
public class APIProxyCertFiles {

    private File clientFolder = new File(Constants.API_CERTS_PATH + "client");
    private File clientCa = new File(Constants.API_CERTS_PATH + "client/ca.pem");
    private File clientCert = new File(Constants.API_CERTS_PATH + "client/cert.pem");
    private File clientKey = new File(Constants.API_CERTS_PATH + "client/key.pem");

    private File caCert = new File(Constants.API_CERTS_PATH + "ca-cert.pem");
    private File caKey = new File(Constants.API_CERTS_PATH + "ca-key.pem");
    private File serverCert = new File(Constants.API_CERTS_PATH + "server-cert.pem");
    private File serverKey = new File(Constants.API_CERTS_PATH + "server-key.pem");


    public APIProxyCertFiles() {
    }

    public boolean filesExist() {
        return clientFolder.exists() && clientCa.exists() && clientCert.exists() && clientKey.exists() &&
                caCert.exists() && caKey.exists() && serverCert.exists() && serverKey.exists();
    }

    public void deleteFiles() {
        clientCa.delete();
        clientCert.delete();
        clientKey.delete();
        clientFolder.delete();

        caCert.delete();
        caKey.delete();
        serverCert.delete();
        serverKey.delete();
    }

}
