package fr.testlab.jbp.patient;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

/**
 * Simulation de charge jbp-patient — G11 B27
 *
 * Scénario : opérations CRUD sur /api/patients
 * Cible    : http://localhost:8080
 * Auth     : Basic admin:admin (YWRtaW46YWRtaW4=)
 *
 * Assertions :
 *   - P95 < 500 ms
 *   - Taux de succès >= 99 %
 *
 * Lancement : ./scripts/run-gatling.sh
 */
public class PatientSimulation extends Simulation {

    // Protocole HTTP commun à tous les scénarios
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .header("Authorization", "Basic YWRtaW46YWRtaW4=");  // admin:admin

    // Corps JSON pour la création d'un patient
    String newPatient = "{\"firstName\":\"Gatling\",\"lastName\":\"Test\",\"birthDate\":\"2000-01-01\"}";

    // Scénario CRUD complet
    ScenarioBuilder scenario = scenario("Patients CRUD")
        // GET /api/patients — liste tous les patients
        .exec(
            http("GET /patients")
                .get("/api/patients")
                .check(status().is(200))
        )
        .pause(Duration.ofMillis(200))

        // GET /api/patients/1 — patient existant
        .exec(
            http("GET /patients/1")
                .get("/api/patients/1")
                .check(status().is(200))
        )
        .pause(Duration.ofMillis(200))

        // GET /api/patients/9999 — patient inexistant → 404
        .exec(
            http("GET /patients/9999 (not found)")
                .get("/api/patients/9999")
                .check(status().is(404))
        )
        .pause(Duration.ofMillis(200))

        // POST /api/patients — création
        .exec(
            http("POST /patients")
                .post("/api/patients")
                .body(StringBody(newPatient))
                .check(status().is(201))
                .check(jsonPath("$.id").saveAs("createdId"))
        )
        .pause(Duration.ofMillis(200))

        // GET sur le patient créé
        .exec(
            http("GET /patients/#{createdId}")
                .get("/api/patients/#{createdId}")
                .check(status().is(200))
        );

    {
        setUp(
            scenario.injectOpen(
                rampUsers(50).during(Duration.ofSeconds(10))
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(95).lt(500),
            global().successfulRequests().percent().gte(99.0)
        );
    }
}
