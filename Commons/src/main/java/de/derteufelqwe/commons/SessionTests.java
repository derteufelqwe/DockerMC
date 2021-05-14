package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;

import java.io.File;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("admin", "ubuntu1");


    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("############## Started #############");

        System.out.println(new File("").getAbsolutePath());
        System.out.println(System.getProperty("user.dir"));

    }

}
