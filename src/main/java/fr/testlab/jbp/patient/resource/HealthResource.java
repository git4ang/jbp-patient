package fr.testlab.jbp.patient.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

// G11 B14 : endpoint /health standard DevOps
// Appele par Jenkins, Kubernetes liveness probe, load balancers
// pour verifier que l'app est vivante avant de router du trafic
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Response health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status",  "UP");
        body.put("service", "jbp-patient");
        body.put("version", System.getenv().getOrDefault("APP_VERSION", "2.0.0")); // B15 : version via env var
        return Response.ok(body).build();
    }
}
