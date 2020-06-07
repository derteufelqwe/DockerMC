package de.derteufelqwe.commons.consul;

public interface ICacheChangeListener<A, B> {

    void onAddEntry(A key, B value);
    void onModifyEntry(A key, B value);
    void onRemoveEntry(A key, B value);

}
