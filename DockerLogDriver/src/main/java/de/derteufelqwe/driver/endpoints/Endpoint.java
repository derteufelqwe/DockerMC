package de.derteufelqwe.driver.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;

public abstract class Endpoint<REQ extends Serializable, RESP extends Serializable> {

    protected Gson gson = new GsonBuilder().create();
    private String data;


    public Endpoint(String data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    protected REQ parseRequest() {
        return (REQ) gson.fromJson(data, getRequestType());
    }

    public String getResponse() {
        return gson.toJson(process(parseRequest()), getResponseType());
    }


    protected abstract RESP process(REQ request);

    protected abstract Class<? extends Serializable> getRequestType();

    protected abstract Class<? extends Serializable> getResponseType();

}
