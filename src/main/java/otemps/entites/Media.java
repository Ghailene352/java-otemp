package otemps.entites;

public class Media {
    private int idMedia;
    private String lienFichier;
    private String type;
    private int idObjet;
    private Objet objet;

    // Constructeurs
    public Media() {
    }

    public Media(int idMedia, String lienFichier, String type, int idObjet) {
        this.idMedia = idMedia;
        this.lienFichier = lienFichier;
        this.type = type;
        this.idObjet = idObjet;
    }

    // Getters et Setters
    public int getIdMedia() {
        return idMedia;
    }

    public void setIdMedia(int idMedia) {
        this.idMedia = idMedia;
    }

    public String getLienFichier() {
        return lienFichier;
    }

    public void setLienFichier(String lienFichier) {
        this.lienFichier = lienFichier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIdObjet() {
        return idObjet;
    }

    public void setIdObjet(int idObjet) {
        this.idObjet = idObjet;
    }

    public Objet getObjet() {
        return objet;
    }

    public void setObjet(Objet objet) {
        this.objet = objet;
    }

    @Override
    public String toString() {
        return "Media{" +
                "idMedia=" + idMedia +
                ", lienFichier='" + lienFichier + '\'' +
                ", type='" + type + '\'' +
                ", idObjet=" + idObjet +
                '}';
    }
}