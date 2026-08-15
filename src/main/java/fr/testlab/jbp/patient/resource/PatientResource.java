package fr.testlab.jbp.patient.resource;

import fr.testlab.jbp.patient.model.Patient;
import fr.testlab.jbp.patient.service.PatientService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

// G6 : jakarta.ws.rs.* (remplace javax.ws.rs.* de G1)
// G10 : @WithSpan sur chaque methode -- JAXRSAnnotationsInstrumentationModule desactive
//        en mode programmatique (JAXRSServerFactoryBean + Jetty embarque sans servlet container)
@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    private final PatientService service;

    public PatientResource(PatientService service) {
        this.service = service;
    }

    @WithSpan("GET /patients")
    @GET
    public List<Patient> getAll() {
        return service.findAll();
    }

    @WithSpan("GET /patients/{id}")
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        Span.current().setAttribute("http.request.patient_id", id); // attribut visible dans Jaeger
        Patient p = service.findById(id);
        if (p == null) {
            Span.current().setAttribute("http.response.status", 404);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Span.current().setAttribute("http.response.status", 200);
        return Response.ok(p).build();
    }

    @WithSpan("POST /patients")
    @POST
    public Response create(Patient p) {
        Patient saved = service.save(p);
        Span.current().setAttribute("patient.id.created", saved.getId());
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @WithSpan("DELETE /patients/{id}")
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        Span.current().setAttribute("http.request.patient_id", id);
        if (service.delete(id)) {
            return Response.noContent().build();
        }
        Span.current().setAttribute("http.response.status", 404);
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
