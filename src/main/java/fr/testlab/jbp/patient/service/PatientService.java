package fr.testlab.jbp.patient.service;

import fr.testlab.jbp.patient.model.Patient;
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

    public List<Patient> findAll() {
        return new ArrayList<>(store.values());
    }

    public Patient findById(int id) {
        return store.get(id);
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
