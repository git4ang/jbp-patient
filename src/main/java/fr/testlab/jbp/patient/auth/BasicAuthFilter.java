package fr.testlab.jbp.patient.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

// Filtre JAX-RS : intercepte chaque requête avant qu'elle atteigne PatientResource
// Vérifie l'en-tête HTTP "Authorization: Basic <base64(user:pass)>"
@Provider
public class BasicAuthFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthFilter.class);

    private static final String VALID_USER = "admin";
    private static final String VALID_PASS = "admin";

    @Override
    public void filter(ContainerRequestContext ctx) {

        // ① Chemins publics -- pas d'auth requise
        // /metrics  : Prometheus scrape sans credentials
        // /health   : liveness probe Jenkins/K8s/load balancer
        // /openapi* : documentation API publique (openapi.json, openapi.yaml)
        String path = ctx.getUriInfo().getPath();
        if (path.equals("metrics")  || path.startsWith("metrics/")  ||
            path.equals("health")   || path.startsWith("health/")   ||
            path.startsWith("openapi")) {
            return;
        }

        // ② Lire l'en-tête Authorization pour les autres chemins
        String authHeader = ctx.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Accès refusé : en-tête Authorization absent ou invalide");
            ctx.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Basic realm=\"jbp-patient\"")
                        .build()
            );
            return;
        }

        // ③ Décoder le Base64 : "Basic YWRtaW46YWRtaW4=" → "admin:admin"
        String encoded = authHeader.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(encoded));
        String[] parts = decoded.split(":", 2);

        if (parts.length != 2 || !VALID_USER.equals(parts[0]) || !VALID_PASS.equals(parts[1])) {
            log.warn("Accès refusé : credentials invalides pour '{}'", parts.length > 0 ? parts[0] : "?");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        log.debug("Accès autorisé pour '{}'", parts[0]);
        // ④ Pas d'abort = la requête continue vers PatientResource
    }
}
