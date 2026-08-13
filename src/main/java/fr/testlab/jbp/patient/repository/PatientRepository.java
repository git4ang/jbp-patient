package fr.testlab.jbp.patient.repository;

import fr.testlab.jbp.patient.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Accès PostgreSQL via JDBC pur (pas d'ORM)
// Reçoit une Connection JDBC à la construction — c'est le test qui la fournit
// (pattern "injection de dépendance" : la classe ne crée pas elle-même sa connexion)
public class PatientRepository {

    private static final Logger log = LoggerFactory.getLogger(PatientRepository.class);

    // La connexion JDBC vers PostgreSQL (fournie par Testcontainers dans les tests)
    private final Connection connection;

    public PatientRepository(Connection connection) {
        this.connection = connection;
    }

    // Crée la table si elle n'existe pas encore
    // SERIAL = type PostgreSQL pour un id auto-incrémenté (équivalent de AtomicInteger en SQL)
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS patients (
                    id        SERIAL PRIMARY KEY,
                    last_name  VARCHAR(100) NOT NULL,
                    first_name VARCHAR(100) NOT NULL,
                    email      VARCHAR(200)
                )
                """;
        // try-with-resources : ferme automatiquement le Statement après exécution
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            log.info("Table patients prête");
        }
    }

    // INSERT -- retourne le patient avec son id généré par PostgreSQL
    // RETURN_GENERATED_KEYS : demande à JDBC de récupérer l'id assigné par SERIAL
    public Patient save(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (last_name, first_name, email) VALUES (?, ?, ?)";

        // (1) PreparedStatement avec RETURN_GENERATED_KEYS pour récupérer l'id SERIAL
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getLastName());   // (2) ? remplacé par la valeur réelle
            ps.setString(2, p.getFirstName());  //     évite les injections SQL
            ps.setString(3, p.getEmail());
            ps.executeUpdate();                 // (3) exécute le INSERT

            // (4) Lire l'id généré par PostgreSQL et l'assigner au patient
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                }
            }
        }
        log.info("Patient inséré id={}", p.getId());
        return p;
    }

    // SELECT par id -- retourne null si non trouvé (PatientResource renvoie 404)
    public Patient findById(int id) throws SQLException {
        String sql = "SELECT id, last_name, first_name, email FROM patients WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);  // convertit une ligne SQL en objet Patient
                }
            }
        }
        return null;  // aucune ligne trouvée
    }

    // DELETE -- retourne true si une ligne a été supprimée, false sinon
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM patients WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();  // nombre de lignes supprimées
            return affected > 0;
        }
    }

    // SELECT toutes les lignes
    public List<Patient> findAll() throws SQLException {
        String sql = "SELECT id, last_name, first_name, email FROM patients";
        List<Patient> result = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    // Convertit une ligne du ResultSet en objet Patient
    // Factorisé ici pour ne pas répéter rs.getString("last_name") dans chaque méthode
    private Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
            rs.getInt("id"),
            rs.getString("last_name"),
            rs.getString("first_name"),
            rs.getString("email")
        );
    }
}
