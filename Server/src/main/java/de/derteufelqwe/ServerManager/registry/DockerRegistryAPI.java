package de.derteufelqwe.ServerManager.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.istack.NotNull;
import de.derteufelqwe.ServerManager.registry.deserializers.HistoryDeserializer;
import de.derteufelqwe.ServerManager.registry.deserializers.RESTErrorDeserializer;
import de.derteufelqwe.ServerManager.registry.deserializers.V1CompatibilityDeserializer;
import de.derteufelqwe.ServerManager.registry.objects.*;
import de.derteufelqwe.commons.Constants;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Base64;
import sun.security.x509.X509CertImpl;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

public class DockerRegistryAPI {

    private final Type TYPE_LIST_STRING = new TypeToken<ArrayList<String>>(){}.getType();

    private Gson gson = createGson();
    private HttpClientBuilder clientBuilder;

    private String url;
    private String username;
    private String password;


    public DockerRegistryAPI(String url, @NotNull String username, @NotNull String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.clientBuilder = createClientBuilder();
    }

    public DockerRegistryAPI(String url) {
        this(url, null, null);
    }


    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(RESTError.class, new RESTErrorDeserializer())
                .registerTypeAdapter(History.class, new HistoryDeserializer())
                .registerTypeAdapter(V1Compatibility.class, new V1CompatibilityDeserializer())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
    }

    private SSLContext createSSLContext() {
        try {
            return SSLContexts.custom()
                    .loadTrustMaterial(this.loadCustomKeystore(), null)
                    .build();

        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore loadCustomKeystore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry(this.url, new X509CertImpl(new FileInputStream(Constants.REGISTRY_CERT_PATH_1 + Constants.REGISTRY_CERT_NAME)));
            return keyStore;

        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClientBuilder createClientBuilder() {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setSSLContext(createSSLContext());

        if (!(this.username == null && this.password == null))
                builder.setDefaultCredentialsProvider(this.getCredentialsProvider());

        return builder;
    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        provider.setCredentials(AuthScope.ANY, credentials);

        return provider;
    }


    private String createUrl(String url) {
        return this.url + "/v2/" + url;
    }

    private boolean isSuccessful(int statusCode) {
        return (statusCode / 100) == 2;
    }

    private RESTResponse readData(HttpRequestBase requestBase, String method) throws RegistryAPIException {
        try {
            try (CloseableHttpClient httpClient = this.clientBuilder.build()) {

                try (CloseableHttpResponse response = httpClient.execute(requestBase)) {
                    if (response.getEntity() != null) {
                        String pageData = EntityUtils.toString(response.getEntity());

                        // Return the page result
                        if (isSuccessful(response.getStatusLine().getStatusCode())) {
                            return new RESTResponse(response, pageData);

                        } else {
                            // Handle the '404 not found' message
                            if (pageData.startsWith("404")) {
                                return new RESTResponse(response, pageData);

                            // Return the error response
                            } else {
                                return new RESTResponse(response, gson.fromJson(pageData, RESTResponse.class).getErrors());
                            }
                        }

                    } else {
                        throw new RegistryAPIException("Registry didn't return entity. Returned: " + response);
                    }

                }
            }

        } catch (IOException e) {
            throw new RegistryAPIException("Failed to %s url %s: %s.", e, method, url, e.getMessage());
        }
    }

    private RESTResponse get(String url, Header[] headers) {
        HttpGet httpGet = new HttpGet(createUrl(url));
        httpGet.setHeaders(headers);

        return readData(httpGet, HttpGet.METHOD_NAME);
    }

    private RESTResponse delete(String url, Header[] headers) {
        HttpDelete httpDelete = new HttpDelete(createUrl(url));
        httpDelete.setHeaders(headers);

        return readData(httpDelete, HttpDelete.METHOD_NAME);
    }

    private void checkResponse(RESTResponse restResponse) throws RegistryAPIException {
        if (!restResponse.success()) {
            throw new RegistryAPIException("Registry returned status %s (%s)'. Errors: %s.",
                    restResponse.getStatusCode(), restResponse.getStatusReason(), restResponse.getErrors()
            );
        }
    }


    public Catalog getCatalog() {
        RESTResponse restResponse = this.get("_catalog", new Header[0]);
        this.checkResponse(restResponse);

        return gson.fromJson(restResponse.getResponse(), Catalog.class);
    }

    public Tags getTags(String image) {
        RESTResponse restResponse = this.get(image + "/tags/list", new Header[0]);
        this.checkResponse(restResponse);

        return gson.fromJson(restResponse.getResponse(), Tags.class);
    }

    public ImageManifest getManifest(String image, String tag) {
        String pwd = "Basic " + new String(Base64.encode("admin:root".getBytes(StandardCharsets.UTF_8)));
        Header[] headers = new Header[] {
                new BasicHeader("Authorization", pwd)
        };

        RESTResponse restResponse = this.get(image + "/manifests/" + tag, headers);
        this.checkResponse(restResponse);

        ImageManifest manifest = gson.fromJson(restResponse.getResponse(), ImageManifest.class);
        manifest.setContentDigest(restResponse.getHeaders().get("Docker-Content-Digest"));
        return manifest;
    }

    public DeleteManifest getDeleteManifest(String image, String tag) {
        Header[] headers = new Header[] {
                new BasicHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json")
        };

        RESTResponse restResponse = this.get(image + "/manifests/" + tag, headers);
        this.checkResponse(restResponse);

        DeleteManifest manifest = gson.fromJson(restResponse.getResponse(), DeleteManifest.class);
        manifest.setContentDigest(restResponse.getHeaders().get("Docker-Content-Digest"));
        return manifest;
    }

    public void deleteManifest(String image, String digest) {
        Header[] headers = new Header[] {
//                new BasicHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json")
        };

        RESTResponse restResponse = this.delete(image + "/manifests/" + digest, headers);
        this.checkResponse(restResponse);
    }

    public void deleteBlob(String image, String digest) {
        RESTResponse restResponse = this.delete(image + "/blobs/" + digest, new Header[0]);
        this.checkResponse(restResponse);
    }

}
