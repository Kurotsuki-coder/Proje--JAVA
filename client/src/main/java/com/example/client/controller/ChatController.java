package com.example.client.controller;

import com.example.client.network.ClientManager;
import com.example.common.model.Message;
import com.example.common.model.Status;
import com.example.common.model.Utilisateur;
import com.example.common.network.Payload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.scene.web.WebView;

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
    @FXML private TextField txt_search;
    @FXML private javafx.scene.image.ImageView btn_gif;

    private Utilisateur currentUser;

    @FXML
    public void initialize() {
        // Bouton GIF
        btn_gif.setOnMouseClicked(e -> ouvrirGifPicker());
        btn_gif.setCursor(Cursor.HAND);
        vbox_messages.heightProperty().addListener((obs, oldVal, newVal) -> {
            scroll_messages.setVvalue(1.0);
        });

        txt_search.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtre = newVal.trim().toLowerCase();
            vbox_contacts.getChildren().forEach(node -> {
                if (node instanceof HBox hbox) {
                    hbox.getChildren().stream()
                            .filter(child -> child instanceof Label)
                            .map(child -> (Label) child)
                            .findFirst()
                            .ifPresent(label -> {
                                boolean visible = label.getText().toLowerCase().contains(filtre);
                                hbox.setVisible(visible);
                                hbox.setManaged(visible);
                            });
                }
            });
        });
    }

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
        System.out.println("Chat démarré pour: " + user.getNom());

        // 1. Callback messages
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
                    String expediteur = msg.getExpediteur().getNom().trim();
                    String correspondantActuel = lbl_current_contact.getText().trim();

                    // Si on reçoit un message alors qu'on attend encore un contact
                    if (correspondantActuel.contains("En attente")) {
                        lbl_current_contact.setText(expediteur);
                        lbl_status.setText("En ligne");
                        circle_status.setFill(Color.web("#50c984"));
                        correspondantActuel = expediteur;
                    }
                    if (expediteur.equals(correspondantActuel) || expediteur.equals(currentUser.getNom())) {
                        afficherNouveauMessage(msg);
                    } else {
                        // ✅ Ajouter un point de notification sur le contact
                        ajouterNotification(expediteur);
                    }
                }
            });
        });

        // 2. Callback perte de connexion
        ClientManager.setOnDeconnexion(() -> {
            Platform.runLater(() -> {
                // Mettre tous les cercles en gris
                vbox_contacts.getChildren().forEach(node -> {
                    if (node instanceof HBox hbox) {
                        hbox.getChildren().forEach(child -> {
                            if (child instanceof Circle circle) {
                                circle.setFill(Color.web("#888888"));
                            }
                        });
                    }
                });

                // Désactiver l'envoi
                txt_message_input.setDisable(true);
                lbl_status.setText("Hors ligne");
                circle_status.setFill(Color.web("#888888"));

                // Afficher l'alerte
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connexion perdue");
                alert.setHeaderText("Vous avez été déconnecté du serveur");
                alert.setContentText("Vérifiez votre connexion et relancez l'application.");
                alert.showAndWait();
            });
        });

        // 3. Demander la liste des utilisateurs connectés
        ClientManager.envoyerMessage("REQUEST_USER_LIST", null);
    }

    private void mettreAJourListeContacts(List<Utilisateur> users) {
        vbox_contacts.getChildren().clear();
        for (Utilisateur user : users) {
            if (currentUser != null && user.getNom().equals(currentUser.getNom())) continue;
            vbox_contacts.getChildren().add(creerItemContact(user));
        }

        // ✅ Sélectionner automatiquement le premier contact
        if (!vbox_contacts.getChildren().isEmpty()) {
            HBox premier = (HBox) vbox_contacts.getChildren().get(0);
            premier.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1,
                    true, true, true, true, true, true, true, true, true, true, null));
        }
    }

    private HBox creerItemContact(Utilisateur user) {
        HBox itemContact = new HBox(10);
        itemContact.setAlignment(Pos.CENTER_LEFT);
        itemContact.setPadding(new Insets(10, 15, 10, 15));
        itemContact.setCursor(Cursor.HAND);
        itemContact.setStyle("-fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;");

        boolean estEnLigne = user.getStatus() != null && user.getStatus() == Status.ONLINE;
        Circle statusCircle = new Circle(5, estEnLigne ? Color.web("#50c984") : Color.web("#888888"));

        Label lblNom = new Label(user.getNom());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: 'Segoe UI';");

        itemContact.getChildren().addAll(statusCircle, lblNom);

        // Clic : charger la conversation
        itemContact.setOnMouseClicked(e -> {
            lbl_current_contact.setText(user.getNom());
            lbl_status.setText(estEnLigne ? "En ligne" : "Hors ligne");
            circle_status.setFill(estEnLigne ? Color.web("#50c984") : Color.web("#888888"));

            vbox_messages.getChildren().clear();
            ClientManager.envoyerMessage("GET_HISTORY", user);

            //Supprimer le point de notification au clic
            itemContact.getChildren().removeIf(child ->
                    (child instanceof Circle
                            && ((Circle) child).getFill().equals(Color.web("#6CA651"))
                            && ((Circle) child).getRadius() == 6)
                            || (child instanceof javafx.scene.layout.Region
                            && HBox.getHgrow((javafx.scene.layout.Region) child) == javafx.scene.layout.Priority.ALWAYS)
            );

            vbox_contacts.getChildren().forEach(node ->
                    node.setStyle("-fx-background-color: transparent; -fx-border-color: #3e3e42; -fx-border-width: 0 0 1 0;")
            );
            itemContact.setStyle("-fx-background-color: #3e3e42; -fx-background-radius: 5;");
        });

        // Effet hover
        itemContact.setOnMouseEntered(e -> {
            if (!lbl_current_contact.getText().equals(user.getNom())) {
                itemContact.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 5;");
            }
        });
        itemContact.setOnMouseExited(e -> {
            if (!lbl_current_contact.getText().equals(user.getNom())) {
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
        ClientManager.envoyerMessage("SEND_PRIVATE_MESSAGE", messageAEnvoyer);
        afficherNouveauMessage(messageAEnvoyer);
        txt_message_input.clear();
    }

    private void afficherNouveauMessage(Message msg) {
        HBox conteneurBulle = new HBox();
        boolean cestMoi = msg.getExpediteur().getNom().equals(currentUser.getNom());
        boolean estUnGif = msg.getContenu().startsWith("https://media") && msg.getContenu().contains("giphy.com");

        if (estUnGif) {
            // Afficher le GIF avec un WebView
            WebView webView = new WebView();
            webView.setPrefSize(200, 200);
            webView.setMaxSize(200, 200);
            String html = "<html><body style='margin:0;padding:0;background:transparent;'>"
                    + "<img src='" + msg.getContenu() + "' width='200' style='border-radius:10px;'/>"
                    + "</body></html>";
            webView.getEngine().loadContent(html);
            conteneurBulle.getChildren().add(webView);
        } else {
            // Message texte normal
            Label bulle = new Label(msg.getContenu());
            bulle.setWrapText(true);
            bulle.setMaxWidth(300);
            bulle.setPadding(new Insets(8, 12, 8, 12));

            if (cestMoi) {
                bulle.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15 15 2 15;");
            } else {
                bulle.setText(msg.getExpediteur().getNom() + ":\n" + msg.getContenu());
                bulle.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15 15 15 2;");
            }
            conteneurBulle.getChildren().add(bulle);
        }

        conteneurBulle.setAlignment(cestMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        vbox_messages.getChildren().add(conteneurBulle);
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            ClientManager.deconnecter();

            // Charger la page de connexion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/frmConnexion.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) vbox_messages.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ouvrirGifPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/frmGifPicker.fxml"));
            Parent root = loader.load();
            GifPickerController ctrl = loader.getController();

            // Quand un GIF est sélectionné → l'envoyer comme message
            ctrl.setOnGifSelected(gifUrl -> {
                String destinataireNom = lbl_current_contact.getText();
                if (destinataireNom.contains("En attente")) return;

                Utilisateur destinataire = new Utilisateur();
                destinataire.setNom(destinataireNom);

                Message msg = new Message(currentUser, destinataire, gifUrl);
                ClientManager.envoyerMessage("SEND_PRIVATE_MESSAGE", msg);
                afficherNouveauMessage(msg);
            });

            Stage stage = new Stage();
            stage.setTitle("Choisir un GIF");
            stage.setScene(new Scene(root));
            stage.initOwner(vbox_messages.getScene().getWindow()); // Attaché à la fenêtre principale
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ajouterNotification(String nomExpediteur) {
        vbox_contacts.getChildren().forEach(node -> {
            if (node instanceof HBox hbox) {
                // Trouver le bon contact par son nom
                hbox.getChildren().stream()
                        .filter(child -> child instanceof Label)
                        .map(child -> (Label) child)
                        .findFirst()
                        .ifPresent(label -> {
                            if (label.getText().equals(nomExpediteur)) {
                                // Vérifier si le point existe déjà
                                /*boolean dejaDot = hbox.getChildren().stream()
                                        .anyMatch(child -> child instanceof Circle
                                                && ((Circle) child).getFill().equals(Color.web("#FF3B30"))
                                                && ((Circle) child).getRadius() == 6);*/
                                boolean dejaDot = hbox.getChildren().stream()
                                        .anyMatch(child -> child instanceof Circle
                                                && ((Circle) child).getFill().equals(Color.web("#6CA651"))
                                                && ((Circle) child).getRadius() == 6);

                                if (!dejaDot) {
                                    Circle dot = new Circle(6, Color.web("#6CA651"));

                                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                                    hbox.getChildren().add(spacer);
                                    hbox.getChildren().add(dot);
                                    HBox.setMargin(dot, new Insets(0, 10, 0, 0));
                                }
                            }
                        });
            }
        });
    }
}