package fr.testlab.jbp.patient.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

// Filtre JAX-RS : intercepte chaque requete avant qu'elle atteigne PatientResource
// Verifie l'en-tete HTTP "Authorization: Basic <base64(user:pass)>"
@Provider
public class BasicAuthFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthFilter.class);

    // Identifiants en dur pour le projet de test (equivalent simplifie de principals.xml Jeebop v5)
    private static final String VALID_USER = "admin";
    private static final String VALID_PASS = "admin";

    @Override
    public void filter(ContainerRequestContext ctx) {

        // (1) Lire l'en-tete Authorization
        String authHeader = ctx.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Acces refuse : en-tete Authorization absent ou invalide");
            // (2) Renvoyer 401 avec WWW-Authenticate pour signaler l'auth requise
            ctx.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Basic realm=\"jbp-patient\"")
                        .build()
            );
            return;
        }

        // (3) Decoder le Base64 : "Basic YWRtaW46YWRtaW4=" -> "admin:admin"
        String encoded = authHeader.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(encoded));
        String[] parts = decoded.split(":", 2);

        if (parts.length != 2 || !VALID_USER.equals(parts[0]) || !VALID_PASS.equals(parts[1])) {
            log.warn("Acces refuse : credentials invalides pour '{}'", parts.length > 0 ? parts[0] : "?");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        log.debug("Acces autorise pour '{}'", parts[0]);
        // (4) Pas d'abort = la requete continue vers PatientResource
    }
}
