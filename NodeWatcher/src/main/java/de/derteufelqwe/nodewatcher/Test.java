package de.derteufelqwe.nodewatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {


    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("([.+]?% Total +% Received +% Xferd +Average +Speed +Time +Time +Time +Current.+(curl:.+))", Pattern.DOTALL);
        String msg = "% Total    % Received % Xferd  Average Speed   Time    Time     Time  Current\n" +
                "                                 Dload  Upload   Total   Spent    Left  Speed\n" +
                "\n" +
                "  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0\n" +
                "  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0\n" +
                "curl: (7) Failed to connect to localhost port 8001: Connection refused";

        Matcher m = pattern.matcher(msg);
        boolean matches = m.matches();
        System.out.println("");
    }

}
