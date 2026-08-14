package fr.testlab.jbp.patient.app;

import fr.testlab.jbp.patient.auth.BasicAuthFilter;
import fr.testlab.jbp.patient.resource.MetricsResource;
import fr.testlab.jbp.patient.resource.PatientResource;
import fr.testlab.jbp.patient.service.PatientService;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// G6 : SLF4J + log4j2, CXF 4.x/jakarta.*, Virtual Thread watchdog, auth basique
// G9 : MetricsResource expose /metrics au format Prometheus
public class JbpApplication {

    // ① SLF4J façade : l'implémentation (log4j2) est choisie via log4j-slf4j2-impl dans le classpath
    private static final Logger log = LoggerFactory.getLogger(JbpApplication.class);

    public static void main(String[] args) throws Exception {

        PatientService service = new PatientService();
        PatientResource resource = new PatientResource(service);
        MetricsResource metrics = new MetricsResource(); // G9 : endpoint /metrics Prometheus

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        // 0.0.0.0 : écoute sur toutes les interfaces réseau du conteneur
        // localhost : écoute uniquement sur la loopback -- inaccessible depuis l'extérieur
        factory.setAddress("http://0.0.0.0:8080/api");
        // G9 : MetricsResource ajouté dans la liste des ressources CXF
        factory.setServiceBeans(List.of(resource, metrics));

        // ② jackson-jakarta-rs remplace jackson-jaxrs (namespace jakarta vs javax)
        //    BasicAuthFilter : filtre JAX-RS qui vérifie l'en-tête Authorization
        factory.setProviders(List.of(new JacksonJsonProvider(), new BasicAuthFilter()));

        Server server = factory.create();
        log.info("jbp-patient démarré sur http://0.0.0.0:8080/api");
        log.info("Stack : CXF 4.x + jakarta.* + log4j2 + SLF4J + Virtual Thread");
        log.info("G9 : métriques Prometheus disponibles sur http://0.0.0.0:8080/api/metrics");

        // ③ Virtual Thread watchdog (remplace Platform Thread de G1)
        //    Thread.ofVirtual() : JDK 21+ standard (pas de preview en Java 25)
        Thread watchdog = Thread.ofVirtual().name("jbp-watchdog").start(() -> {
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
        Thread.currentThread().join(); // maintenir le processus principal actif
    }
}
