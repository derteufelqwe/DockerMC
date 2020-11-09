package de.derteufelqwe.bungeeplugin.utils;

public class Logger {


    public void info(String text, Object... args) {
        System.out.println("[Info] " + String.format(text, args));
    }

    public void warning(String text, Object... args) {
        System.out.println("[Warning] " + String.format(text, args));
    }

    public void error(String text, Object... args) {
        System.err.println("[Error] " + String.format(text, args));
    }

    public void fatalError(String text, Object... args) {
        System.err.println("[FATAL ERROR] " + String.format(text, args));
    }

}
