package fr.testlab.jbp.patient.service;

import fr.testlab.jbp.patient.model.Patient;
import org.apache.log4j.Logger;                    // log4j 1.x (etat v5-like, G4 skippe)

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PatientService {

    // log4j 1.x — sera remplace par SLF4J/log4j2 en G4 (skippe ici)
    private static final Logger log = Logger.getLogger(PatientService.class);

    // Stockage en memoire — pas de base de donnees, pas de JPA
    // ConcurrentHashMap : thread-safe sans synchronized (plusieurs requetes simultanees)
    private final Map<Integer, Patient> store   = new ConcurrentHashMap<>();
    private final AtomicInteger         counter = new AtomicInteger(1); // id auto-incremente

    public PatientService() {
        // Donnees de depart pour tester les GET sans avoir a creer des patients
        save(new Patient(0, "Dupont", "Marie",  "marie@testlab.fr"));
        save(new Patient(0, "Martin", "Pierre", "pierre@testlab.fr"));
    }

    public List<Patient> findAll() {
        return new ArrayList<>(store.values());
    }

    public Patient findById(int id) {
        return store.get(id); // null si non trouve — PatientResource retourne 404
    }

    public Patient save(Patient p) {
        if (p.getId() == 0) {              // 0 = pas encore d'id : nouveau patient
            p.setId(counter.getAndIncrement());
        }
        store.put(p.getId(), p);
        log.info("Patient sauvegarde id=" + p.getId()); // log4j 1.x (concatenation — pas de {})
        return p;
    }

    public boolean delete(int id) {
        return store.remove(id) != null;   // true si existait, false si 404
    }
}
