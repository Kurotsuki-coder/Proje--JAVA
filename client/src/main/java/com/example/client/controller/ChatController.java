package com.example.client.controller;

import com.example.client.network.ClientManager;
import com.example.common.model.*;
import com.example.common.network.Payload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

    private Utilisateur currentUser;

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;

        ClientManager.setOnMessageReceived(obj -> {
            Platform.runLater(() -> {
                if (obj instanceof Payload) {
                    Payload p = (Payload) obj;
                    if ("UPDATE_USER_LIST".equals(p.getAction())) {
                        mettreAJourListeContacts((List<Utilisateur>) p.getData());
                    } else if ("HISTORY_DATA".equals(p.getAction())) {
                        vbox_messages.getChildren().clear();
                        ((List<Message>) p.getData()).forEach(this::afficherNouveauMessage);
                    }
                } else if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    String expediteur = msg.getExpediteur().getNom();
                    if (expediteur.equals(lbl_current_contact.getText()) || expediteur.equals(currentUser.getNom())) {
                        afficherNouveauMessage(msg);
                    }
                }
            });
        });
        ClientManager.envoyerMessage("REQUEST_USER_LIST", null);
    }

    private void mettreAJourListeContacts(List<Utilisateur> users) {
        vbox_contacts.getChildren().clear();
        for (Utilisateur user : users) {
            // Empêche de s'afficher soi-même
            if (currentUser != null && user.getNom().equals(currentUser.getNom())) continue;
            vbox_contacts.getChildren().add(creerItemContact(user));
        }
    }

    private HBox creerItemContact(Utilisateur user) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(10, 15, 10, 15));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;");

        boolean estEnLigne = user.getStatus() == Status.ONLINE;
        Circle statusCircle = new Circle(5, estEnLigne ? Color.web("#50c984") : Color.web("#888888"));

        Label lblNom = new Label(user.getNom());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        item.getChildren().addAll(statusCircle, lblNom);

        item.setOnMouseClicked(e -> {
            lbl_current_contact.setText(user.getNom());
            lbl_status.setText(estEnLigne ? "En ligne" : "Hors ligne");
            circle_status.setFill(statusCircle.getFill());
            vbox_messages.getChildren().clear();
            ClientManager.envoyerMessage("GET_HISTORY", user);
        });
        return item;
    }

    @FXML
    private void handleSendMessage() {
        String texte = txt_message_input.getText().trim();
        if (texte.isEmpty() || lbl_current_contact.getText().contains("En attente")) return;

        Message msg = new Message(currentUser, new Utilisateur(lbl_current_contact.getText(), ""), texte);
        ClientManager.envoyerMessage("SEND_PRIVATE_MESSAGE", msg);
        afficherNouveauMessage(msg);
        txt_message_input.clear();
    }

    private void afficherNouveauMessage(Message msg) {
        Label bulle = new Label(msg.getContenu());
        bulle.setPadding(new Insets(8, 12, 8, 12));
        boolean moi = msg.getExpediteur().getNom().equals(currentUser.getNom());
        bulle.setStyle(moi ? "-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15;"
                : "-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15;");

        HBox box = new HBox(moi ? bulle : new VBox(new Label(msg.getExpediteur().getNom() + ":"), bulle));
        box.setAlignment(moi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        vbox_messages.getChildren().add(box);
        scroll_messages.setVvalue(1.0);
    }

    @FXML
    private void handleLogout() throws IOException {
        ClientManager.deconnecter();
        System.exit(0);
    }
}