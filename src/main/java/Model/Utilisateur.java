package Model;

import java.time.LocalDate;

public class Utilisateur {

    private static int id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String adresse;
    private int phone;
    private String genre;
    private int role;
    private String email;
    private String password;
    private boolean profileComplete = true;

    // ✅ NEW
    private byte[] image;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, LocalDate dateNaissance,
                       String genre, String adresse, int phone, int role,
                       String email, String password, byte[] image) {

        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.genre = genre;
        this.adresse = adresse;
        this.phone = phone;
        this.role = role;
        this.email = email;
        this.password = password;
        this.image = image;
    }

    // Getters & Setters

    public static int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public int getPhone() { return phone; }
    public void setPhone(int phone) { this.phone = phone; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // ✅ NEW
    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
}