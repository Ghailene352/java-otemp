package com.guser.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String prenoms;
    private String email;
    private String telephone;
    private String motDePasse;
    private String role;
    private String faceTemplate;
    private LocalDateTime dateCreation;

    public User() {
    }

    public User(int id, String nom, String prenoms, String email, String telephone, String motDePasse,
                String role, LocalDateTime dateCreation) {
        this(id, nom, prenoms, email, telephone, motDePasse, role, dateCreation, null);
    }

    public User(int id, String nom, String prenoms, String email, String telephone, String motDePasse,
                String role, LocalDateTime dateCreation, String faceTemplate) {
        this.id = id;
        this.nom = nom;
        this.prenoms = prenoms;
        this.email = email;
        this.telephone = telephone;
        this.motDePasse = motDePasse;
        this.role = role;
        this.dateCreation = dateCreation;
        this.faceTemplate = faceTemplate;
    }

    public User(String nom, String prenoms, String email, String telephone, String motDePasse,
                String role) {
        this(nom, prenoms, email, telephone, motDePasse, role, null);
    }

    public User(String nom, String prenoms, String email, String telephone, String motDePasse,
                String role, String faceTemplate) {
        this.nom = nom;
        this.prenoms = prenoms;
        this.email = email;
        this.telephone = telephone;
        this.motDePasse = motDePasse;
        this.role = role;
        this.faceTemplate = faceTemplate;
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

    public String getPrenoms() {
        return prenoms;
    }

    public void setPrenoms(String prenoms) {
        this.prenoms = prenoms;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getFaceTemplate() {
        return faceTemplate;
    }

    public void setFaceTemplate(String faceTemplate) {
        this.faceTemplate = faceTemplate;
    }

    public boolean hasFaceTemplate() {
        return faceTemplate != null && !faceTemplate.isBlank();
    }

    public String getNomComplet() {
        return nom + " " + prenoms;
    }
}
