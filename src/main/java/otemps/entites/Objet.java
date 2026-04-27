package otemps.entites;

import java.util.List;
import java.util.ArrayList;

public class Objet {
    private int idObjet;
    private String nom;
    private String description;
    private String epoque;
    private String origine;
    private String materiaux;
    private int idCategorie;
    private Categorie categorie;
    private List<Media> medias;

    // Constructeurs
    public Objet() {
        this.medias = new ArrayList<>();
    }

    public Objet(int idObjet, String nom, String description, String epoque,
                 String origine, String materiaux, int idCategorie) {
        this.idObjet = idObjet;
        this.nom = nom;
        this.description = description;
        this.epoque = epoque;
        this.origine = origine;
        this.materiaux = materiaux;
        this.idCategorie = idCategorie;
        this.medias = new ArrayList<>();
    }

    // Getters et Setters
    public int getIdObjet() {
        return idObjet;
    }

    public void setIdObjet(int idObjet) {
        this.idObjet = idObjet;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEpoque() {
        return epoque;
    }

    public void setEpoque(String epoque) {
        this.epoque = epoque;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getMateriaux() {
        return materiaux;
    }

    public void setMateriaux(String materiaux) {
        this.materiaux = materiaux;
    }

    public int getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public List<Media> getMedias() {
        return medias;
    }

    public void setMedias(List<Media> medias) {
        this.medias = medias;
    }

    public void addMedia(Media media) {
        this.medias.add(media);
    }

    public void removeMedia(Media media) {
        this.medias.remove(media);
    }

    @Override
    public String toString() {
        return "Objet{" +
                "idObjet=" + idObjet +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", epoque='" + epoque + '\'' +
                ", origine='" + origine + '\'' +
                ", materiaux='" + materiaux + '\'' +
                ", idCategorie=" + idCategorie +
                '}';
    }

}