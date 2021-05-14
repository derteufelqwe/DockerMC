package de.derteufelqwe.ServerManager;

import lombok.SneakyThrows;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {

        try {
            throw new RuntimeException("lol");

        } finally {
            System.out.println("hallo");
        }

    }

}
