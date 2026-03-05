package com.example.client.controller;

import com.example.common.model.Utilisateur;
import com.example.client.network.ClientManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class frmInscriptionController {

    @FXML private TextField txtIdentifiant;
    @FXML private PasswordField txtMotsdepasse;
    @FXML private Button btnRetour;
    @FXML private Button btnSInscrire;
    @FXML private Label lblMessage;
    @FXML private Button btnInscription;
    @FXML private Button btnConnexion;

    @FXML
    public void initialize() {
        btnSInscrire.setOnAction(event -> {
            String identifiant = txtIdentifiant.getText().trim();
            String motdepasse = txtMotsdepasse.getText().trim();

            if (identifiant.isEmpty() || motdepasse.isEmpty()) {
                lblMessage.setText("Veuillez remplir tous les champs !");
                return;
            }

            // On prépare l'objet Utilisateur
            Utilisateur newUser = new Utilisateur();
            newUser.setNom(identifiant);
            newUser.setMotsdepasse(motdepasse);

            String reponse = (String) ClientManager.envoyerRequete("INSCRIPTION", newUser);

            if ("SUCCESS".equals(reponse)) {
                lblMessage.setText("Inscription réussie !");
                System.out.println("Compte créé pour : " + identifiant);
                // Optionnel : rediriger vers la connexion après 2 secondes
            } else if ("DEJA_EXISTANT".equals(reponse)) {
                lblMessage.setText("Identifiant déjà utilisé !");
            } else {
                lblMessage.setText("Erreur lors de l'inscription.");
            }
        });

        btnRetour.setOnAction(event -> chargerFenetre("/com/example/client/frmConnexion.fxml", "Connexion"));

        if (btnConnexion != null) {
            btnConnexion.setOnMouseClicked(event -> chargerFenetre("/com/example/client/frmConnexion.fxml", "Connexion"));
        }
    }

    // Petite méthode utilitaire pour éviter de répéter le code de changement de fenêtre
    private void chargerFenetre(String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.setScene(new Scene(root));
            stage.show();

            // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) btnSInscrire.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            System.err.println("Erreur chargement fenêtre : " + e.getMessage());
        }
    }
}