package de.derteufelqwe.ServerManager;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.checkerframework.checker.units.qual.K;
import org.jetbrains.annotations.NotNull;
import sun.security.pkcs.PKCS8Key;
import sun.security.provider.DSAPrivateKey;
import sun.security.x509.X509Key;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class Testing {

    @SneakyThrows
    public static void createCerts(Path tmpDir) {
        String sslCertConfig = IOUtils.resourceToString("sslCert.cnf", StandardCharsets.UTF_8, Testing.class.getClassLoader());
        sslCertConfig = sslCertConfig
                .replaceFirst("\\{C\\}", "DE")
                .replaceFirst("\\{ST\\}", "SH")
                .replaceFirst("\\{L\\}", "BS")
                .replaceFirst("\\{O\\}", "DockerMC")
                .replaceFirst("\\{M\\}", "test@test.de");

        File file = new File(tmpDir + "/sslCert.cnf");
        FileUtils.write(file, sslCertConfig, StandardCharsets.UTF_8);

        String res = Utils.executeCommandOnHost("openssl", "req", "-x509", "-newkey", "rsa:4096", "-nodes", "-sha256", "-days", "356",
                "-out", tmpDir + "/" + Constants.REGISTRY_CERT_NAME, "-keyout", tmpDir + "/" + Constants.REGISTRY_KEY_NAME,
                "-config", file.getAbsolutePath());

        System.out.println("Done");
    }

    public static void createHtpasswd(Path tmpDir) {
        String name = "admin";
        String password = "admin";

        String res = name + ":" + Md5Crypt.apr1Crypt(password.getBytes(StandardCharsets.UTF_8));
        System.out.println(res);
    }

    @SneakyThrows
    public static void main(String[] args) {
        Path tmpDir = Files.createTempDirectory("DMC");

        createHtpasswd(tmpDir);
    }

}
