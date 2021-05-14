package de.derteufelqwe.plugin.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.plugin.DMCLogDriver;
import de.derteufelqwe.plugin.exceptions.InvalidAPIDataException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public abstract class Endpoint<REQ extends Serializable, RESP extends Serializable> {

    protected Gson gson = DMCLogDriver.GSON;
    private String data;


    public Endpoint(String data) {
        this.data = data;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected REQ parseRequest() throws InvalidAPIDataException {
        Serializable result = gson.fromJson(data, getRequestType());
        if (result == null) {
            try {
                result = getRequestType().newInstance();

            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        return (REQ) result;
    }

    public String getResponse() throws InvalidAPIDataException {
        return gson.toJson(process(parseRequest()), getResponseType());
    }


    protected abstract RESP process(REQ request);

    protected abstract Class<? extends Serializable> getRequestType();

    protected abstract Class<? extends Serializable> getResponseType();

}
