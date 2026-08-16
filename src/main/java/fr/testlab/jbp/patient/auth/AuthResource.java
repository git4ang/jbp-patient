package fr.testlab.jbp.patient.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

// G11 JWT -- endpoint de login : échange credentials → token JWT signé
// POST /api/auth/login  {"username":"admin","password":"admin"}
//                    →  {"token":"eyJ..."}
//
// Ce endpoint est public (pas d'auth requise) -- voir JwtAuthFilter
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger log = LoggerFactory.getLogger(AuthResource.class);

    // Clé secrète lue depuis variable d'environnement -- jamais en dur en production
    // JWT_SECRET doit faire au moins 32 caractères (256 bits) pour HMAC-SHA256
    // Valeur par défaut : dev uniquement -- NE PAS utiliser en production
    static final String SECRET = System.getenv()
        .getOrDefault("JWT_SECRET", "dev-secret-32-chars-minimum-here!!");

    // Durée de validité du token : 1 heure (en millisecondes)
    private static final long EXPIRATION_MS = 3_600_000L;

    // Credentials autorisés -- remplacé par BCrypt + DB dans Jeebop v6 (T730)
    private static final String VALID_USER = "admin";
    private static final String VALID_PASS = "admin";

    @POST
    @Path("/login")
    public Response login(Map<String, String> creds) {

        // ① Extraire username/password du corps JSON
        String username = creds == null ? null : creds.get("username");
        String password = creds == null ? null : creds.get("password");

        if (username == null || password == null) {
            return Response.status(400)
                .entity(Map.of("error", "Champs username et password obligatoires"))
                .build();
        }

        // ② Vérifier les credentials (simple ici -- BCrypt+DB en v6)
        if (!VALID_USER.equals(username) || !VALID_PASS.equals(password)) {
            log.warn("Echec login pour '{}'", username);
            return Response.status(401)
                .entity(Map.of("error", "Identifiants invalides"))
                .build();
        }

        // ③ Construire le token JWT
        //    Structure d'un JWT : header.payload.signature
        //    header  : {"alg":"HS256","typ":"JWT"}
        //    payload : {"sub":"admin","iat":1720000000,"exp":1720003600}
        //    signature : HMAC-SHA256(header + "." + payload, SECRET)
        Date now     = new Date();
        Date expires = new Date(now.getTime() + EXPIRATION_MS);

        String token = Jwts.builder()
            .subject(username)                              // sub : qui est authentifié
            .issuedAt(now)                                  // iat : quand le token a été émis
            .expiration(expires)                            // exp : quand le token expire
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes())) // signature HMAC-SHA256
            .compact();                                     // sérialise en "header.payload.sig"

        log.info("Token JWT emis pour '{}' -- expire dans 1h", username);

        // ④ Retourner le token + méta-infos utiles au client
        return Response.ok(Map.of(
            "token",     token,
            "type",      "Bearer",
            "expiresIn", 3600,
            "username",  username
        )).build();
    }
}
