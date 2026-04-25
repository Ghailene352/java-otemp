package com.guser.config;

public final class MailConfig {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String USERNAME = "hsinigaylen@gmail.com";
    private static final String PASSWORD = "avmj ewhn ikxv prbm";
    private static final String FROM_ADDRESS = USERNAME;

    private MailConfig() {
    }

    public static String getSmtpHost() {
        return SMTP_HOST;
    }

    public static int getSmtpPort() {
        return SMTP_PORT;
    }

    public static String getUsername() {
        return USERNAME;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    public static String getFromAddress() {
        return FROM_ADDRESS;
    }
}
