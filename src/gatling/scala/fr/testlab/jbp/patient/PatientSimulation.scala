package fr.testlab.jbp.patient

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

// G11 B27 : simulation de charge jbp-patient
// Lance 50 utilisateurs progressivement sur 10s, chacun exécute le scénario 3 fois
// Critère de passage : responseTime.percentile95 < 500ms, succès >= 99%
class PatientSimulation extends Simulation {

  // configuration HTTP commune à tous les scénarios
  val httpProtocol = http
    .baseUrl("http://localhost:8080")            // adresse de l'app locale
    .acceptHeader("application/json")
    .header("Authorization", "Basic YWRtaW46YWRtaW4=") // admin:admin en base64

  // scénario : enchaîne GET /patients, GET /patients/1, POST /patients
  val scénario = scenario("Patients CRUD")
    .exec(
      http("GET /patients")
        .get("/api/patients")
        .check(status.is(200))
    )
    .pause(200.milliseconds)
    .exec(
      http("GET /patients/1")
        .get("/api/patients/1")
        .check(status.is(200))
    )
    .pause(200.milliseconds)
    .exec(
      http("GET /patients/999")          // id inexistant -> 404 accepté
        .get("/api/patients/999")
        .check(status.in(200, 404))
    )
    .pause(200.milliseconds)
    .exec(
      http("POST /patients")
        .post("/api/patients")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"firstName":"Test","lastName":"Gatling"}"""))
        .check(status.is(201))
    )

  // injection : 50 utilisateurs injectés progressivement sur 10 secondes
  setUp(
    scénario.inject(rampUsers(50).during(10.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(95).lt(500),  // 95e percentile < 500ms
     global.successfulRequests.percent.gte(99)     // taux de succès >= 99%
   )
}
