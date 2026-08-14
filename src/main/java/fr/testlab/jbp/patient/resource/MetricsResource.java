package fr.testlab.jbp.patient.resource;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.StringWriter;

// Endpoint /metrics -- expose les métriques au format Prometheus (texte scrape)
// Prometheus scrape ce endpoint toutes les 15s et stocke les séries temporelles
@Path("/metrics")
public class MetricsResource {

    // Compteur HTTP par méthode + code -- incrémenté dans PatientResource
    // static : partagé entre toutes les instances (singleton Prometheus)
    public static final Counter HTTP_REQUESTS = Counter.build()
        .name("jbp_http_requests_total")        // nom de la métrique dans Prometheus
        .help("Nombre total de requêtes HTTP")
        .labelNames("method", "path", "status") // labels pour filtrer dans Grafana
        .register();

    static {
        // Enregistre les métriques JVM par défaut : heap, GC, threads, classes
        DefaultExports.initialize();
    }

    // GET /api/metrics -- retourne toutes les métriques au format texte Prometheus
    @GET
    @Produces(TextFormat.CONTENT_TYPE_004)  // "text/plain; version=0.0.4; charset=utf-8"
    public Response metrics() throws Exception {
        StringWriter writer = new StringWriter();
        // Écrit toutes les métriques enregistrées dans le CollectorRegistry par défaut
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        return Response.ok(writer.toString()).build();
    }
}
