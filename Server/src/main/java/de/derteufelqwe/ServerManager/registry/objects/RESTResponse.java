package de.derteufelqwe.ServerManager.registry.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.StatusLine;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RESTResponse {

    private String response;
    private int statusCode;
    private String statusReason;

    private List<RESTError> errors;

    public RESTResponse(StatusLine statusLine, String response) {
        this.statusCode = statusLine.getStatusCode();
        this.statusReason = statusLine.getReasonPhrase();
        this.response = response;
    }

    public RESTResponse(StatusLine statusLine, List<RESTError> errors) {
        this.statusCode = statusLine.getStatusCode();
        this.statusReason = statusLine.getReasonPhrase();
        this.errors = errors;
    }


    public boolean success() {
        return ((int) (this.statusCode / 100.0)) == 2;
    }

}
