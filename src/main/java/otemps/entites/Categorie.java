package otemps.entites;

import java.util.List;
import java.util.ArrayList;

public class Categorie {
    private int idCategorie;
    private String nomCategorie;
    private String description;
    private List<Objet> objets;

    // Constructeurs
    public Categorie() {
        this.objets = new ArrayList<>();
    }

    public Categorie(int idCategorie, String nomCategorie, String description) {
        this.idCategorie = idCategorie;
        this.nomCategorie = nomCategorie;
        this.description = description;
        this.objets = new ArrayList<>();
    }

    // Getters et Setters
    public int getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Objet> getObjets() {
        return objets;
    }

    public void setObjets(List<Objet> objets) {
        this.objets = objets;
    }

    public void addObjet(Objet objet) {
        this.objets.add(objet);
    }

    public void removeObjet(Objet objet) {
        this.objets.remove(objet);
    }

    @Override
    public String toString() {
        return "Categorie{" +
                "idCategorie=" + idCategorie +
                ", nomCategorie='" + nomCategorie + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}