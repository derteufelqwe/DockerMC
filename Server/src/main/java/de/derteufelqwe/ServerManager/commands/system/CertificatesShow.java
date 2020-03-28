package de.derteufelqwe.ServerManager.commands.system;

import de.derteufelqwe.commons.Constants;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Prints a Certificate to the console
 */
@CommandLine.Command(name = "show", description = "Shows a certificate", mixinStandardHelpOptions = true,
        subcommands = {
})
public class CertificatesShow implements Runnable {


    @Override
    public void run() {
        File certFile = new File(Constants.REGISTRY_CERT_PATH + Constants.REGISTRY_CERT_NAME);
        if (!certFile.exists()) {
            System.out.println("No certificate found. Generate one first.");
            return;
        }

        try {
            String fileContent = FileUtils.readFileToString(certFile, StandardCharsets.UTF_8);
            System.out.println(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
