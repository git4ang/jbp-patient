package fr.testlab.jbp.patient.service;

import fr.testlab.jbp.patient.model.Patient;

// JUnit 5 (Jupiter) -- import différent de JUnit 4 (org.junit.Test)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pas de Mockito ici : PatientService n'a aucune dépendance externe à mocker
// ConcurrentHashMap interne = suffisant pour tester la logique métier en isolation
class PatientServiceTest {

    private PatientService service;

    // @BeforeEach (JUnit 5) = @Before (JUnit 4)
    // Garantit un service vide et un compteur à 1 avant chaque test
    @BeforeEach
    void setUp() {
        service = new PatientService();
        // Le constructeur ajoute 2 patients par défaut (Dupont id=1, Martin id=2)
    }

    @Test
    void savePatient_assignsId() {
        Patient p = new Patient(0, "Durand", "Alice", "alice@testlab.fr");
        Patient saved = service.save(p);
        // Les 2 patients du constructeur ont pris id=1 et id=2 -> prochain = 3
        assertEquals(3, saved.getId(), "L'id auto-incrémenté doit être 3");
    }

    @Test
    void savePatient_isRetrievableAfterSave() {
        Patient p = new Patient(0, "Durand", "Alice", "alice@testlab.fr");
        Patient saved = service.save(p);
        Patient found = service.findById(saved.getId());
        assertNotNull(found, "findById doit retourner le patient après save");
        assertEquals("Durand", found.getLastName());
    }

    @Test
    void findById_returnsDefaultPatient() {
        // Dupont est créé par le constructeur avec id=1
        Patient p = service.findById(1);
        assertNotNull(p);
        assertEquals("Dupont", p.getLastName());
    }

    @Test
    void findById_unknownId_returnsNull() {
        // id=999 n'existe pas -- PatientResource retourne 404 quand findById retourne null
        assertNull(service.findById(999), "Un id inexistant doit retourner null");
    }

    @Test
    void deletePatient_returnsTrueAndRemovesIt() {
        boolean deleted = service.delete(1);
        assertTrue(deleted, "delete doit retourner true si le patient existait");
        assertNull(service.findById(1), "Le patient supprimé ne doit plus être retrouvable");
    }

    @Test
    void deletePatient_unknownId_returnsFalse() {
        boolean deleted = service.delete(999);
        // assertFalse (remplace assertTrue(!deleted)) -- règle SonarQube S2701 :
        // utiliser l'assertion sémantiquement correcte pour un message d'erreur lisible
        assertFalse(deleted, "delete doit retourner false si le patient n'existait pas");
    }

    @Test
    void findAll_returnsAllPatients() {
        assertEquals(2, service.findAll().size(), "findAll doit retourner les 2 patients du constructeur");
    }
}
