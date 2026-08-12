package fr.testlab.jbp.patient.model;

// POJO simple - pas d'annotations JPA (@Entity, @Table...)
// Stockage en memoire - pas de base de donnees, pas d'ORM.
public class Patient {

    private int    id;
    private String lastName;
    private String firstName;
    private String email;

    public Patient() {}

    public Patient(int id, String lastName, String firstName, String email) {
        this.id        = id;
        this.lastName  = lastName;
        this.firstName = firstName;
        this.email     = email;
    }

    public int    getId()        { return id; }
    public String getLastName()  { return lastName; }
    public String getFirstName() { return firstName; }
    public String getEmail()     { return email; }

    public void setId(int id)         { this.id = id; }
    public void setLastName(String v)  { this.lastName = v; }
    public void setFirstName(String v) { this.firstName = v; }
    public void setEmail(String v)     { this.email = v; }
}
