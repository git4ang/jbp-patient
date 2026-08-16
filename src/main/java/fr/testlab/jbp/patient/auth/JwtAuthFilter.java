package fr.testlab.jbp.patient.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// G11 JWT -- filtre de validation : vérifie le token Bearer sur chaque requête protégée
// Remplace BasicAuthFilter (credentials Base64 sur chaque requête)
//
// Flux :
//   Client → Authorization: Bearer eyJ... → JwtAuthFilter
//     → valide signature + expiration
//     → OK : requête continue vers PatientResource
//     → KO : 401 Unauthorized
//
// Chemins publics (pas de token requis) :
//   /metrics   -- Prometheus scrape
//   /health    -- liveness probe
//   /openapi*  -- documentation API
//   /auth/*    -- login lui-même (sinon boucle infinie !)
@Provider
@Priority(Priorities.AUTHENTICATION)     // s'exécute avant les filtres métier
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    public void filter(ContainerRequestContext ctx) {

        // ① Chemins publics -- pas de token requis
        String path = ctx.getUriInfo().getPath();
        if (path.equals("metrics")    || path.startsWith("metrics/")  ||
            path.equals("health")     || path.startsWith("health/")   ||
            path.startsWith("openapi")|| path.startsWith("auth")) {
            return;
        }

        // ② Lire l'en-tête Authorization
        String header = ctx.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("Requete sans token JWT : {} {}", ctx.getMethod(), path);
            ctx.abortWith(Response.status(401)
                .entity(Map.of(
                    "error",  "Token manquant",
                    "detail", "Authorization: Bearer <token> requis"))
                .build());
            return;
        }

        // ③ Extraire le token (supprimer le préfixe "Bearer ")
        String token = header.substring("Bearer ".length()).trim();

        try {
            // ④ Valider la signature ET la date d'expiration
            //    parseSignedClaims lève une exception si :
            //    - la signature ne correspond pas à la clé secrète (token falsifié)
            //    - le token est expiré (exp < maintenant)
            //    - le token est malformé
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(AuthResource.SECRET.getBytes()))
                .build()
                .parseSignedClaims(token);

            // ⑤ Token valide -- la requête continue normalement
            // (on pourrait extraire claims.getPayload().getSubject() pour obtenir le username)

        } catch (JwtException e) {
            // Couvre : signature invalide, token expiré, token malformé
            log.warn("Token JWT invalide : {}", e.getMessage());
            ctx.abortWith(Response.status(401)
                .entity(Map.of(
                    "error",  "Token invalide ou expiré",
                    "detail", e.getMessage()))
                .build());
        }
    }
}
