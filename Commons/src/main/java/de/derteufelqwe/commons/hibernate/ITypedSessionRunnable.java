package de.derteufelqwe.commons.hibernate;

import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ITypedSessionRunnable<T> {

    T run(@NotNull Session session);

}
