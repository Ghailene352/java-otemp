package com.guser.config;

public final class DatabaseConfig {

    private static final String URL = "jdbc:mysql://localhost:3306/guser_db?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return URL;
    }

    public static String getUsername() {
        return USERNAME;
    }

    public static String getPassword() {
        return PASSWORD;
    }
}
