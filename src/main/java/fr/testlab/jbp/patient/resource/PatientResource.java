package fr.testlab.jbp.patient.resource;

import fr.testlab.jbp.patient.model.Patient;
import fr.testlab.jbp.patient.service.PatientService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

// javax.ws.rs.* - etat v5-like, sera migre vers jakarta.ws.rs.* en G2
@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    // service injecte depuis JbpApplication (singleton partage entre toutes les requetes)
    private final PatientService service;

    public PatientResource(PatientService service) {
        this.service = service;
    }

    // GET /patients - liste complete
    @GET
    public List<Patient> getAll() {
        return service.findAll();
    }

    // GET /patients/{id} - un patient ou 404
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        Patient p = service.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(p).build();
    }

    // POST /patients - creer un patient, retourne 201 + le patient cree
    @POST
    public Response create(Patient p) {
        return Response.status(Response.Status.CREATED).entity(service.save(p)).build();
    }

    // DELETE /patients/{id} - supprimer ou 404
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (service.delete(id)) {
            return Response.noContent().build();                       // 204
        }
        return Response.status(Response.Status.NOT_FOUND).build();    // 404
    }
}
