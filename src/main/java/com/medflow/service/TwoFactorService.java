package com.medflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de autenticacao em 2 fatores via email.
 *
 * Fluxo:
 *   1. generateAndSend()  -> gera codigo 6 digitos, envia email, retorna sessionToken
 *   2. verify()           -> confere sessionToken + codigo, retorna userId se valido
 */
@Service
public class TwoFactorService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${twofa.expiry-minutes:5}")
    private int expiryMinutes;

    private final Map<String, PendingSession> sessions = new ConcurrentHashMap<>();

    public TwoFactorService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Gera e envia o codigo; retorna o sessionToken temporario. */
    public String generateAndSend(String userId, String userName, String email) {
        String code         = generateCode();
        String sessionToken = UUID.randomUUID().toString();
        Instant expiry      = Instant.now().plusSeconds(expiryMinutes * 60L);
        sessions.put(sessionToken, new PendingSession(userId, code, expiry));
        sendEmail(email, userName, code);
        return sessionToken;
    }

    /** Verifica sessionToken + codigo. Retorna userId se correto. Lanca excecao se invalido. */
    public String verify(String sessionToken, String code) {
        PendingSession s = sessions.get(sessionToken);
        if (s == null)
            throw new IllegalArgumentException("Sessao invalida ou expirada. Faca login novamente.");
        if (Instant.now().isAfter(s.expiry())) {
            sessions.remove(sessionToken);
            throw new IllegalArgumentException("Codigo expirado. Faca login novamente.");
        }
        if (!s.code().equals(code.trim()))
            throw new IllegalArgumentException("Codigo incorreto.");
        sessions.remove(sessionToken);
        return s.userId();
    }

    private String generateCode() {
        return String.valueOf(new SecureRandom().nextInt(900_000) + 100_000);
    }

    private void sendEmail(String to, String userName, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("MedFlow - Codigo de verificacao");
        msg.setText(
            "Ola, " + userName + "!\n\n" +
            "Seu codigo de verificacao e:\n\n" +
            "        " + code + "\n\n" +
            "Valido por " + expiryMinutes + " minutos.\n" +
            "Se nao foi voce, ignore este email.\n\n" +
            "--- Neocore Hospital"
        );
        mailSender.send(msg);
    }

    /** Mascara o email: maria@email.com -> ma***@email.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] p = email.split("@");
        String vis = p[0].length() > 2 ? p[0].substring(0, 2) : p[0].substring(0, 1);
        return vis + "***@" + p[1];
    }

    private record PendingSession(String userId, String code, Instant expiry) {}
}
