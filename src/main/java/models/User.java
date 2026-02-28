package models;

import java.time.LocalDate;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private LocalDate date;
    private String adresse;
    private String role;
    private String numeroT;
    private String email;
    private String image;
    private String password;
    private String genre;

    public User() {
    }

    public User(String prenom, String nom) {
        this.prenom = prenom;
        this.nom = nom;
    }

    public User(int id, String prenom, String nom, LocalDate date, String adresse, String role,
                String numeroT, String email, String image, String password, String genre) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.date = date;
        this.adresse = adresse;
        this.role = role;
        this.numeroT = numeroT;
        this.email = email;
        this.image = image;
        this.password = password;
        this.genre = genre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNumeroT() {
        return numeroT;
    }

    public void setNumeroT(String numeroT) {
        this.numeroT = numeroT;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getFirstName() {
        return prenom;
    }

    public void setFirstName(String firstName) {
        this.prenom = firstName;
    }

    public String getLastName() {
        return nom;
    }

    public void setLastName(String lastName) {
        this.nom = lastName;
    }

    @Override
    public String toString() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }
}
