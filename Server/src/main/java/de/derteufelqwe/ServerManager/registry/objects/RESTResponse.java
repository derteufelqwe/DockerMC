package de.derteufelqwe.ServerManager.registry.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RESTResponse {

    private String response;
    private int statusCode;
    private String statusReason;
    private Map<String, String> headers = new HashMap<>();

    private List<RESTError> errors;


    private RESTResponse(CloseableHttpResponse response) {
        this.statusCode = response.getStatusLine().getStatusCode();
        this.statusReason = response.getStatusLine().getReasonPhrase();
        for (Header header : response.getAllHeaders()) {
            this.headers.put(header.getName(), header.getValue());
        }
    }

    public RESTResponse(CloseableHttpResponse response, String content) {
        this(response);
        this.response = content;
    }

    public RESTResponse(CloseableHttpResponse response, List<RESTError> errors) {
        this(response);
        this.errors = errors;
    }


    public boolean success() {
        return ((int) (this.statusCode / 100.0)) == 2;
    }

}
