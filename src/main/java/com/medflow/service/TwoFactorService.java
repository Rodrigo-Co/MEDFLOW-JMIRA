package com.medflow.service;

import com.medflow.config.AppConfig;
import com.medflow.http.HttpException;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwoFactorService {

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final int expiryMinutes;
    private final Map<String, PendingSession> sessions = new ConcurrentHashMap<>();

    public TwoFactorService(AppConfig config) {
        this.smtpHost = config.getRequired("app.mail.host");
        this.smtpPort = config.getInt("app.mail.port", 587);
        this.smtpUsername = config.getRequired("app.mail.username");
        this.smtpPassword = config.getRequired("app.mail.password");
        this.expiryMinutes = config.getInt("twofa.expiry-minutes", 5);
    }

    public String generateAndSend(String userId, String userName, String email) {
        String code = String.valueOf(new SecureRandom().nextInt(900_000) + 100_000);
        String sessionToken = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(expiryMinutes * 60L);
        sessions.put(sessionToken, new PendingSession(userId, code, expiry));
        sendEmail(email, userName, code);
        return sessionToken;
    }

    public String verify(String sessionToken, String code) {
        PendingSession session = sessions.get(sessionToken);
        if (session == null) {
            throw new HttpException(401, "Sessao invalida ou expirada. Faca login novamente.");
        }
        if (Instant.now().isAfter(session.expiry())) {
            sessions.remove(sessionToken);
            throw new HttpException(401, "Codigo expirado. Faca login novamente.");
        }
        if (!session.code().equals(code == null ? "" : code.trim())) {
            throw new HttpException(401, "Codigo incorreto.");
        }
        sessions.remove(sessionToken);
        return session.userId();
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() > 2 ? local.substring(0, 2) : local.substring(0, 1);
        return visible + "***@" + parts[1];
    }

    private void sendEmail(String to, String userName, String code) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUsername));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("MedFlow - Codigo de verificacao");
            message.setText(
                    "Ola, " + userName + "!\n\n" +
                            "Seu codigo de verificacao e:\n\n" +
                            "        " + code + "\n\n" +
                            "Valido por " + expiryMinutes + " minutos.\n" +
                            "Se nao foi voce, ignore este email.\n\n" +
                            "--- Neocore Hospital"
            );
            Transport.send(message);
        } catch (MessagingException e) {
            throw new HttpException(500, "Nao foi possivel enviar o codigo 2FA por email");
        }
    }

    private record PendingSession(String userId, String code, Instant expiry) {
    }
}
