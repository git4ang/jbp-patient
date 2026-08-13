package fr.testlab.jbp.patient.repository;

import fr.testlab.jbp.patient.model.Patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @Testcontainers : active l'extension JUnit 5 qui gère le cycle de vie des conteneurs
// Elle détecte les champs @Container et les démarre/arrête automatiquement
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

// PostgreSQLContainer : sait comment démarrer l'image postgres:16
// configure automatiquement le port, le user, le password, la base de données
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers  // active la gestion automatique des conteneurs Docker pour cette classe
class PatientRepositoryIT {

    // @Container : Testcontainers démarre ce conteneur avant les tests et le détruit après
    // static : un seul conteneur partagé pour tous les tests de la classe (plus rapide)
    //          si non static : un conteneur par test (isolation maximale mais lent)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")   // nom de la base créée dans le conteneur
            .withUsername("testuser")     // identifiants de connexion JDBC
            .withPassword("testpass");

    // Repository testé — recréé avant chaque test avec une table vide
    private PatientRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        // (1) Récupérer l'URL JDBC dynamique assignée par Testcontainers
        //     Le port est aléatoire à chaque run pour éviter les conflits
        //     Exemple : jdbc:postgresql://localhost:54321/testdb
        String url = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String pass = postgres.getPassword();

        // (2) Ouvrir une connexion JDBC vers le conteneur PostgreSQL
        Connection connection = DriverManager.getConnection(url, user, pass);

        // (3) Créer le repository avec cette connexion
        repository = new PatientRepository(connection);

        // (4) Créer la table et la vider avant chaque test (isolation)
        //     Sans ce TRUNCATE, les ids s'accumulent entre tests
        repository.createTableIfNotExists();
        connection.createStatement().execute("TRUNCATE TABLE patients RESTART IDENTITY");
    }

    @Test
    void savePatient_assignsIdFromPostgres() throws Exception {
        // SERIAL PostgreSQL commence à 1 après TRUNCATE RESTART IDENTITY
        Patient p = new Patient(0, "Dupont", "Marie", "marie@testlab.fr");

        Patient saved = repository.save(p);

        // L'id est assigné par PostgreSQL (SERIAL), pas par AtomicInteger
        assertEquals(1, saved.getId(), "Le premier id PostgreSQL doit être 1");
    }

    @Test
    void saveAndFindById_returnsCorrectPatient() throws Exception {
        Patient p = new Patient(0, "Martin", "Pierre", "pierre@testlab.fr");
        Patient saved = repository.save(p);

        Patient found = repository.findById(saved.getId());

        assertNotNull(found, "findById doit retourner le patient après INSERT");
        assertEquals("Martin", found.getLastName());
        assertEquals("pierre@testlab.fr", found.getEmail());
    }

    @Test
    void findById_unknownId_returnsNull() throws Exception {
        // Aucun INSERT avant ce test (table vidée par setUp)
        Patient found = repository.findById(999);

        assertNull(found, "Un id inexistant doit retourner null");
    }

    @Test
    void deletePatient_removesItFromDatabase() throws Exception {
        Patient saved = repository.save(new Patient(0, "Durand", "Alice", "alice@testlab.fr"));

        boolean deleted = repository.delete(saved.getId());

        assertTrue(deleted, "delete doit retourner true si la ligne existait");
        assertNull(repository.findById(saved.getId()), "Le patient supprimé ne doit plus exister en base");
    }

    @Test
    void findAll_returnsAllInsertedPatients() throws Exception {
        repository.save(new Patient(0, "Dupont", "Marie", "marie@testlab.fr"));
        repository.save(new Patient(0, "Martin", "Pierre", "pierre@testlab.fr"));

        List<Patient> all = repository.findAll();

        assertEquals(2, all.size(), "findAll doit retourner les 2 patients insérés");
    }
}
