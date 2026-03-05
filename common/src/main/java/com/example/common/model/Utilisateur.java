package com.example.common.model;

import jakarta.persistence.*;
import java.io.Serializable; //Important pour le réseau

@Entity
@Table(name = "utilisateurs")
public class Utilisateur implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nom;

    @Column(nullable = false)
    private String motsdepasse;

    @Enumerated(EnumType.STRING) //Ici j'ai utilisé énumération à la place de String
    private Status status;

    public Utilisateur() {
        //Pour hibernate
    }

    public Utilisateur(String nom, String motsdepasse) { //On mets pas le paramètre status pour garantir que tout nouveau compte commence en mode hors ligne par défaut
        this.nom = nom;
        this.motsdepasse = motsdepasse;
        this.status = Status.OFFLINE; //Si on ne le mets pas son champ sera null dans la bd à la création de l'utilisateur
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getMotsdepasse() {
        return motsdepasse;
    }

    public void setMotsdepasse(String motsdepasse) {
        this.motsdepasse = motsdepasse;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}