package fr.testlab.jbp.patient.service;

import fr.testlab.jbp.patient.model.Patient;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// G6 : SLF4J (remplace org.apache.log4j.Logger de G1)
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final Map<Integer, Patient> store   = new ConcurrentHashMap<>();
    private final AtomicInteger         counter = new AtomicInteger(1);

    public PatientService() {
        save(new Patient(0, "Dupont", "Marie",  "marie@testlab.fr"));
        save(new Patient(0, "Martin", "Pierre", "pierre@testlab.fr"));
    }

    // G10 : @WithSpan cree un span enfant sous le span de PatientResource.getAll()
    @WithSpan("PatientService.findAll")
    public List<Patient> findAll() {
        Span.current().setAttribute("patient.count", store.size()); // attribut visible dans Jaeger
        return new ArrayList<>(store.values());
    }

    // G10 : @WithSpan cree un span enfant sous le span de PatientResource.getById()
    // Span.current().setAttribute() ajoute l'id recherche comme attribut de trace
    @WithSpan("PatientService.findById")
    public Patient findById(int id) {
        Patient p = store.get(id);
        Span.current().setAttribute("patient.id", id);          // id recherche
        Span.current().setAttribute("patient.found", p != null); // true/false visible dans Jaeger
        return p;
    }

    public Patient save(Patient p) {
        if (p.getId() == 0) {
            p.setId(counter.getAndIncrement());
        }
        store.put(p.getId(), p);
        log.info("Patient sauvegarde id={}", p.getId());  // SLF4J : {} au lieu de +
        return p;
    }

    public boolean delete(int id) {
        return store.remove(id) != null;
    }
}
