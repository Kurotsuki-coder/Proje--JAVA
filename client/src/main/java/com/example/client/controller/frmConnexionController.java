package com.example.client.controller;

import com.example.common.model.Utilisateur;
import com.example.client.network.ClientManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class frmConnexionController {
    @FXML private TextField txtIdentifiant;
    @FXML private PasswordField txtMotsdepasse;
    @FXML private Button btnSeConnecter;
    @FXML private Button btnQuitter;
    @FXML private Label lblMessage;
    @FXML private Button btnInscription;
    @FXML private Label lblMotdepasseOublie;

    @FXML
    public void initialize() {
        // Action de se connecter
        btnSeConnecter.setOnAction(event -> {
            String identifiant = txtIdentifiant.getText().trim();
            String motdepasse = txtMotsdepasse.getText().trim();

            if (identifiant.isEmpty() || motdepasse.isEmpty()) {
                lblMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            Utilisateur userTrial = new Utilisateur();
            userTrial.setNom(identifiant);
            userTrial.setMotsdepasse(motdepasse);

            // Envoi au serveur
            Object reponse = ClientManager.envoyerRequete("CONNEXION", userTrial);

            if (reponse instanceof Utilisateur) {
                Utilisateur userConnecte = (Utilisateur) reponse;
                //ClientManager.startListening();
                Platform.runLater(() -> {
                    lblMessage.setText("Connexion réussie !");
                    //Pour la redirection
                    redirectionVersChat(event, userConnecte);
                });
            } else {
                Platform.runLater(() -> lblMessage.setText("Identifiant ou mot de passe incorrect"));
            }
        });

        // Quitter
        btnQuitter.setOnAction(event -> {
            ((Stage) btnQuitter.getScene().getWindow()).close();
        });

        // Navigation vers Inscription
        btnInscription.setOnAction(event -> chargerFenetre("/com/example/client/frmInscription.fxml", "Inscription"));

        // Redirection vers mot de passe oublié
        lblMotdepasseOublie.setOnMouseClicked(event -> chargerFenetre("/com/example/client/frmMotsDePasseOublie.fxml", "Récupération"));
    }

    private void chargerFenetre(String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.setScene(new Scene(root));
            stage.show();

            // Fermer la fenêtre de connexion
            ((Stage) lblMessage.getScene().getWindow()).close();
        } catch (Exception e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Changement de scène vers le Chat
    private void redirectionVersChat(javafx.event.ActionEvent event, Utilisateur userConnecte) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/frmChat.fxml"));
            Parent root = loader.load();
            ChatController chatCtrl = loader.getController();
            chatCtrl.setCurrentUser(userConnecte);

            Stage stage = (Stage) btnSeConnecter.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
            System.out.println("Redirection effectuée avec succès");
        } catch (IOException e) {
            System.err.println("Erreur de redirection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}