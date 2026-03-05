package com.example.client.controller;

import com.example.client.network.ClientManager;
import com.example.common.model.Message;
import com.example.common.model.Utilisateur;
import com.example.common.network.Payload;
import com.sun.javafx.fxml.builder.JavaFXSceneBuilder;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.List;

public class ChatController {
    @FXML private VBox vbox_messages;
    @FXML private TextField txt_message_input;
    @FXML private ScrollPane scroll_messages;
    @FXML private VBox vbox_contacts;
    @FXML private Label lbl_current_contact;
    @FXML private Label lbl_status;
    @FXML private Circle circle_status;


    private Utilisateur currentUser = new Utilisateur("Moi", "pass");

    @FXML
    public void initialize() {
        ClientManager.setOnMessageReceived(obj -> {
            Platform.runLater(() -> { // On met TOUT dans Platform.runLater pour être serein
                if (obj instanceof Payload) {
                    Payload p = (Payload) obj;
                    if ("UPDATE_USER_LIST".equals(p.getAction())) {
                        List<String> utilisateurs = (List<String>) p.getData();
                        mettreAJourListeContacts(utilisateurs);
                    } else if ("HISTORY_DATA".equals(p.getAction())) {
                        vbox_messages.getChildren().clear(); // On vide avant de charger l'historique
                        List<Message> messages = (List<Message>) p.getData();
                        for (Message m : messages) {
                            afficherNouveauMessage(m);
                        }
                    }
                } else if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    String expediteur = msg.getExpediteur().getNom().trim();
                    String correspondantActuel = lbl_current_contact.getText().trim();

                    // Si on attendait un contact, on ouvre la conv avec celui qui nous parle
                    if (correspondantActuel.contains("En attente")) {
                        lbl_current_contact.setText(expediteur);
                        lbl_status.setText("En ligne");
                        circle_status.setFill(Color.web("#50c984"));
                        correspondantActuel = expediteur;
                    }

                    // On affiche SEULEMENT si c'est la bonne conversation
                    if (expediteur.equals(correspondantActuel) || expediteur.equals(currentUser.getNom())) {
                        afficherNouveauMessage(msg);
                    } else {
                        System.out.println("Message reçu en arrière-plan de : " + expediteur);
                    }
                }
            });
        });
        ClientManager.envoyerMessage("REQUEST_USER_LIST", null);
    }

    // Dans ChatController.java -> mettreAJourListeContacts
    private void mettreAJourListeContacts(List<String> noms) {
        Platform.runLater(() -> {
            vbox_contacts.getChildren().clear();
            for (String nom : noms) {
                if (nom.equals(currentUser.getNom())) continue;

                HBox itemContact = creerItemContact(nom); // On extrait la création pour plus de clarté
                vbox_contacts.getChildren().add(itemContact);
            }

            // Si on vient de se connecter et qu'on n'a pas encore de conversation ouverte
            if (lbl_current_contact.getText().equals("En attente...") && !vbox_contacts.getChildren().isEmpty()) {
                // On simule un clic sur le premier contact de la liste pour charger l'historique
                MouseEvent.fireEvent(vbox_contacts.getChildren().get(0), new MouseEvent(MouseEvent.MOUSE_CLICKED, 0,
                        0, 0, 0, MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null));
            }
        });
    }

    private HBox creerItemContact(String nom) {
        // 1. Création du conteneur horizontal
        HBox itemContact = new HBox();
        itemContact.setAlignment(Pos.CENTER_LEFT);
        itemContact.setSpacing(10);
        itemContact.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
        itemContact.setCursor(javafx.scene.Cursor.HAND);
        itemContact.setStyle("-fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;"); // Petite ligne de séparation

        // 2. Indicateur de statut (Cercle vert)
        Circle statusCircle = new Circle(5, Color.web("#50c984"));

        // 3. Nom du contact
        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: 'Segoe UI';");

        itemContact.getChildren().addAll(statusCircle, lblNom);

        // 4. ÉVÉNEMENT AU CLIC (Chargement de la conversation)
        itemContact.setOnMouseClicked(e -> {
            // Mettre à jour l'interface du haut
            lbl_current_contact.setText(nom);
            lbl_status.setText("En ligne");
            circle_status.setFill(Color.web("#50c984"));

            // Nettoyer la zone de messages AVANT de charger l'historique
            vbox_messages.getChildren().clear();

            // Demander l'historique au serveur pour ce contact
            Utilisateur cible = new Utilisateur();
            cible.setNom(nom);
            ClientManager.envoyerMessage("GET_HISTORY", cible);

            // Effet visuel de sélection (on remet les autres en transparent)
            vbox_contacts.getChildren().forEach(node ->
                    node.setStyle("-fx-background-color: transparent; -fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;")
            );
            itemContact.setStyle("-fx-background-color: #3e3e42; -fx-background-radius: 5;");
        });

        // Effet de survol (Hover)
        itemContact.setOnMouseEntered(e -> {
            if (!lbl_current_contact.getText().equals(nom)) {
                itemContact.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 5;");
            }
        });
        itemContact.setOnMouseExited(e -> {
            if (!lbl_current_contact.getText().equals(nom)) {
                itemContact.setStyle("-fx-background-color: transparent; -fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;");
            }
        });

        return itemContact;
    }

    @FXML
    private void handleSendMessage() {
        String texte = txt_message_input.getText().trim();
        String destinataireNom = lbl_current_contact.getText();

        if (texte.isEmpty() || destinataireNom.contains("En attente")) return;

        Utilisateur destinataire = new Utilisateur();
        destinataire.setNom(destinataireNom);

        Message messageAEnvoyer = new Message(currentUser, destinataire, texte);

        // 1. Envoyer au serveur
        ClientManager.envoyerMessage("SEND_PRIVATE_MESSAGE", messageAEnvoyer);

        // 2. Afficher localement
        afficherNouveauMessage(messageAEnvoyer);

        // 3. Effacer le champ
        txt_message_input.clear();
    }

    private void afficherNouveauMessage(Message msg) {
        HBox conteneurBulle = new HBox();
        Label bulle = new Label(msg.getContenu());
        bulle.setWrapText(true);
        bulle.setMaxWidth(300);
        bulle.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        //Label bulle = new Label(msg.getExpediteur().getNom() + " : " + msg.getContenu());

        //vérifier si c'est l'utilisateur connecté
        boolean cestMoi = msg.getExpediteur().getNom().equals(currentUser.getNom());

        if (cestMoi) {
            conteneurBulle.setAlignment(Pos.CENTER_RIGHT);
            bulle.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; " + "-fx-background-radius: 15 15 2 15;");
        } else {
            conteneurBulle.setAlignment(Pos.CENTER_LEFT);
            bulle.setText(msg.getExpediteur().getNom() + ":\n" + msg.getContenu());
            bulle.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; " + "-fx-background-radius: 15 15 15 2;");
        }

        conteneurBulle.getChildren().add(bulle);
        vbox_messages.getChildren().add(conteneurBulle);
        scroll_messages.setVvalue(1.0);
    }

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
        System.out.println("Chat démarré pour: " + user.getNom());
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            ClientManager.deconnecter();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}