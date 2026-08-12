package fr.testlab.jbp.patient.auth;

import java.security.MessageDigest;
import java.util.Map;

// Auth v5-like : hash MD5 + map en memoire (equivalent principals.xml Jeebop v5)
// Vulnerabilite connue : MD5 sensible aux rainbow tables
// G5 skippe - reste en etat v5-like
public class Md5Auth {

    // admin -> hash MD5 de "admin" = 21232f297a57a5a743894a0e4a801fc3
    private static final Map<String, String> USERS = Map.of(
        "admin", "21232f297a57a5a743894a0e4a801fc3"
    );

    public static boolean authenticate(String username, String password) {
        String expected = USERS.get(username);
        if (expected == null) return false;
        return expected.equals(md5(password));
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 indisponible", e);
        }
    }
}
