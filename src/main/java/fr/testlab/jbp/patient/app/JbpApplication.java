package fr.testlab.jbp.patient.app;

import fr.testlab.jbp.patient.resource.PatientResource;
import fr.testlab.jbp.patient.service.PatientService;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.log4j.Logger;

import java.util.List;

public class JbpApplication {

    private static final Logger log = Logger.getLogger(JbpApplication.class);

    public static void main(String[] args) throws Exception {

        // (1) Un seul PatientService partage entre toutes les requetes (singleton)
        // Le ConcurrentHashMap est initialise une seule fois, les donnees persistent
        PatientService service = new PatientService();
        PatientResource resource = new PatientResource(service);

        // (2) Fabrique CXF : configure le serveur JAX-RS embarque (Jetty sur port 8080)
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("http://localhost:8080/api");

        // (3) setServiceBeans() = instances singletons - CXF reutilise le meme objet
        // Differe de setResourceClasses() qui creerait une nouvelle instance par requete
        factory.setServiceBeans(List.of(resource));

        // (4) Jackson convertit automatiquement Patient <-> JSON sur chaque requete
        factory.setProviders(List.of(new JacksonJsonProvider()));

        Server server = factory.create();
        log.info("jbp-patient demarre sur http://localhost:8080/api");
        log.info("Endpoints : GET/POST /api/patients  GET/DELETE /api/patients/{id}");

        // (5) Watchdog - Platform Thread - etat v5-like, sera migre en G3
        // daemon=true : ce thread ne bloque pas l'arret de la JVM
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30_000);
                    log.info("[watchdog] serveur actif - platform thread");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "jbp-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        server.start();
        Thread.currentThread().join(); // (6) maintenir le processus principal actif
    }
}
