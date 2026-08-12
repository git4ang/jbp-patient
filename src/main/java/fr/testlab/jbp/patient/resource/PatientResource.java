package fr.testlab.jbp.patient.resource;

import fr.testlab.jbp.patient.model.Patient;
import fr.testlab.jbp.patient.service.PatientService;

import javax.ws.rs.Consumes;        // javax.* -- sera migre en G2 vers jakarta.*
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)   // toutes les methodes retournent du JSON
@Consumes(MediaType.APPLICATION_JSON)   // toutes les methodes acceptent du JSON
public class PatientResource {

    // une instance de service par resource (pas de singleton ici — stockage en memoire partage)
    private final PatientService service = new PatientService();

    // GET /patients → liste complete
    @GET
    public List<Patient> getAll() {
        return service.findAll();
    }

    // GET /patients/{id} → un patient ou 404
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        Patient p = service.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();  // 404
        }
        return Response.ok(p).build();                                  // 200 + JSON
    }

    // POST /patients → creer un patient, retourne 201 + le patient cree
    @POST
    public Response create(Patient p) {
        Patient created = service.save(p);
        return Response.status(Response.Status.CREATED).entity(created).build(); // 201
    }

    // DELETE /patients/{id} → supprimer ou 404
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        if (service.delete(id)) {
            return Response.noContent().build();               // 204 — suppression OK
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // 404 — inexistant
    }
}
