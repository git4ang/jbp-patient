package fr.testlab.jbp.patient.app;

import fr.testlab.jbp.patient.auth.BasicAuthFilter;
import fr.testlab.jbp.patient.resource.HealthResource;
import fr.testlab.jbp.patient.resource.MetricsResource;
import fr.testlab.jbp.patient.resource.PatientResource;
import fr.testlab.jbp.patient.service.PatientService;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// G6  : SLF4J + log4j2, CXF 4.x/jakarta.*, Virtual Thread watchdog, auth basique
// G9  : MetricsResource expose /metrics au format Prometheus
// G11 : HealthResource (/health), APP_PORT/APP_HOST, OpenApiResource (/openapi.json)
public class JbpApplication {

    private static final Logger log = LoggerFactory.getLogger(JbpApplication.class);

    public static void main(String[] args) throws Exception {

        // B15 : port et host configurables via variables d'environnement
        String port = System.getenv().getOrDefault("APP_PORT", "8080");
        String host = System.getenv().getOrDefault("APP_HOST", "0.0.0.0");

        PatientService  service  = new PatientService();
        PatientResource resource = new PatientResource(service);
        MetricsResource metrics  = new MetricsResource();
        HealthResource  health   = new HealthResource();
        // B16 : OpenApiResource (swagger-jaxrs2-jakarta) expose /openapi.json et /openapi.yaml
        // scanne automatiquement les classes @Path enregistrées dans le serveur CXF
        OpenApiResource openApi  = new OpenApiResource();

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("http://" + host + ":" + port + "/api");
        factory.setServiceBeans(List.of(resource, metrics, health, openApi));
        factory.setProviders(List.of(new JacksonJsonProvider(), new BasicAuthFilter()));

        Server server = factory.create();
        log.info("jbp-patient demarre sur http://{}:{}/api", host, port);
        log.info("G9  : metriques Prometheus sur http://{}:{}/api/metrics", host, port);
        log.info("G11 : health       sur http://{}:{}/api/health", host, port);
        log.info("G11 : openapi.json sur http://{}:{}/api/openapi.json", host, port);

        Thread.ofVirtual().name("jbp-watchdog").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30_000);
                    log.info("[watchdog] serveur actif - virtual thread");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        server.start();
        Thread.currentThread().join();
    }
}
