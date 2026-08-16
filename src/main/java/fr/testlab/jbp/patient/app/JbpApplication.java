package fr.testlab.jbp.patient.app;

import fr.testlab.jbp.patient.auth.AuthResource;
import fr.testlab.jbp.patient.auth.JwtAuthFilter;
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

// G6  : SLF4J + log4j2, CXF 4.x/jakarta.*, Virtual Thread watchdog
// G9  : MetricsResource expose /metrics au format Prometheus
// G13 : HealthResource (/health), APP_PORT/APP_HOST, OpenApiResource (/openapi.json)
// G11 : AuthResource (/auth/login) + JwtAuthFilter -- remplace BasicAuthFilter
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
        AuthResource    auth     = new AuthResource();
        // B16 : OpenApiResource (swagger-jaxrs2-jakarta) expose /openapi.json et /openapi.yaml
        OpenApiResource openApi  = new OpenApiResource();

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("http://" + host + ":" + port + "/api");
        // AuthResource enregistre POST /api/auth/login (public -- pas d'auth)
        factory.setServiceBeans(List.of(resource, metrics, health, openApi, auth));
        // JwtAuthFilter remplace BasicAuthFilter : valide token Bearer sur /patients
        factory.setProviders(List.of(new JacksonJsonProvider(), new JwtAuthFilter()));

        Server server = factory.create();
        log.info("jbp-patient demarre sur http://{}:{}/api", host, port);
        log.info("G9  : metriques    sur http://{}:{}/api/metrics", host, port);
        log.info("G13 : health       sur http://{}:{}/api/health", host, port);
        log.info("G13 : openapi.json sur http://{}:{}/api/openapi.json", host, port);
        log.info("G11 : login JWT    sur http://{}:{}/api/auth/login  [POST]", host, port);

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
