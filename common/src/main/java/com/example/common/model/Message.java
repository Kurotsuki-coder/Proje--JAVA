package com.example.common.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message implements Serializable{
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne //Plusieurs messages pour un seul expéditeur
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Utilisateur expediteur;

    @ManyToOne //Plusieurs messages pour un seul destinataire
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    public Message() {

    }

    public Message(Utilisateur expediteur, Utilisateur destinataire, String contenu) {
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.contenu = contenu;
        this.status = MessageStatus.ENVOYE;
        this.dateEnvoi = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getExpediteur() {
        return expediteur;
    }

    public void setExpediteur(Utilisateur expediteur) {
        this.expediteur = expediteur;
    }

    public Utilisateur getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Utilisateur destinataire) {
        this.destinataire = destinataire;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}