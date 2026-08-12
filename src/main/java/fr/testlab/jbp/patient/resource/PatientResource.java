package fr.testlab.jbp.patient.resource;

import fr.testlab.jbp.patient.model.Patient;
import fr.testlab.jbp.patient.service.PatientService;

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
@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    private final PatientService service;

    public PatientResource(PatientService service) {
        this.service = service;
    }

    @GET
    public List<Patient> getAll() {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        Patient p = service.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(p).build();
    }

    @POST
    public Response create(Patient p) {
        return Response.status(Response.Status.CREATED).entity(service.save(p)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (service.delete(id)) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
