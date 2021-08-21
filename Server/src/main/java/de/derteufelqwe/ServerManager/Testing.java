package de.derteufelqwe.ServerManager;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Testing {

    public void write(int b) throws IOException {
        target.write(b);
    }

    public void write(@NotNull byte[] b) throws IOException {
        target.write(b);
    }

    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        target.write(b, off, len);
    }

    public void flush() throws IOException {
        target.flush();
    }

    public void close() throws IOException {
        target.close();
    }

    private OutputStream target;

    @SneakyThrows
    public static void main(String[] args) {


    }

}
