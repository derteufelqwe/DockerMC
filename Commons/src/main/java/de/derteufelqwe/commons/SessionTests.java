package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile;
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SessionTests {

    public static SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", 5432);


    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("############## Started #############");




    }

}
