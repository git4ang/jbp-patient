package fr.testlab.jbp.patient.service;

import fr.testlab.jbp.patient.model.Patient;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PatientService {

    // log4j 1.x - etat v5-like, G4 skippe
    private static final Logger log = Logger.getLogger(PatientService.class);

    // Stockage en memoire - ConcurrentHashMap : thread-safe sans synchronized
    private final Map<Integer, Patient> store   = new ConcurrentHashMap<>();
    private final AtomicInteger         counter = new AtomicInteger(1);

    public PatientService() {
        // Donnees de depart pour tester les GET sans creer de patients
        save(new Patient(0, "Dupont", "Marie",  "marie@testlab.fr"));
        save(new Patient(0, "Martin", "Pierre", "pierre@testlab.fr"));
    }

    public List<Patient> findAll() {
        return new ArrayList<>(store.values());
    }

    public Patient findById(int id) {
        return store.get(id); // null si non trouve - PatientResource retourne 404
    }

    public Patient save(Patient p) {
        if (p.getId() == 0) {             // 0 = nouveau patient, pas encore d'id
            p.setId(counter.getAndIncrement());
        }
        store.put(p.getId(), p);
        log.info("Patient sauvegarde id=" + p.getId());
        return p;
    }

    public boolean delete(int id) {
        return store.remove(id) != null;  // true si existait, false si absent
    }
}
