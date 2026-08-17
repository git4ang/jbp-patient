package fr.testlab.jbp.patient.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

// G15 : Correlation ID -- injecte un UUID par requete dans le MDC SLF4J
//
// MDC (Mapped Diagnostic Context) = dictionnaire thread-local SLF4J :
//   chaque trace emise apres MDC.put() inclut automatiquement l'entree dans le JSON ECS
//   --> champ "labels.correlationId" dans Kibana (EcsLayout inclut le MDC sous "labels")
//
// Flux complet :
//   Client --> [X-Correlation-Id: abc] ou absent
//     --> filter(request) : lit ou genere UUID, MDC.put("correlationId", id)
//     --> JwtAuthFilter (priority 1000) : logs incluent deja correlationId
//     --> PatientResource, PatientService : idem
//     --> filter(response) : ajoute X-Correlation-Id dans la reponse
//     --> Client recoit l'id dans les headers --> peut le logguer de son cote
//
// Priority 500 : s'execute AVANT JwtAuthFilter (priority 1000)
//   --> les rejections JWT ont aussi un correlationId dans leurs logs
@Provider
@Priority(500)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

    // Header HTTP standard de correlation inter-services (convention W3C/OpenTelemetry)
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    // Cle MDC SLF4J -- apparait dans EcsLayout sous labels.correlationId dans Kibana
    public static final String MDC_KEY = "correlationId";
    // Propriete CXF pour transferer l'id du filtre request vers le filtre response
    private static final String REQUEST_PROP = "jbp.correlationId";

    @Override
    public void filter(ContainerRequestContext req) {
        // ① Lire le header entrant ou generer un UUID (premier service de la chaine)
        String id = req.getHeaderString(CORRELATION_HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        // ② Injecter dans le MDC : TOUS les logs suivants dans ce thread ont l'id
        MDC.put(MDC_KEY, id);

        // ③ Stocker en propriete de requete pour le filtre de reponse
        req.setProperty(REQUEST_PROP, id);

        log.debug("Entree : {} {} [correlationId={}]", req.getMethod(),
                  req.getUriInfo().getPath(), id);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        // ④ Ajouter X-Correlation-Id dans la reponse HTTP
        String id = (String) req.getProperty(REQUEST_PROP);
        if (id != null) {
            resp.getHeaders().add(CORRELATION_HEADER, id);
        }

        // ⑤ Nettoyer le MDC -- OBLIGATOIRE pour les Virtual Threads (thread recycle)
        //    Sans nettoyage : le correlationId d'une requete precedente contaminerait
        //    la suivante si le thread est reutilise
        MDC.remove(MDC_KEY);
    }
}
