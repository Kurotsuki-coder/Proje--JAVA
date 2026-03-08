package com.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("frmConnexion.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/images_java/icon_app.png")
        ));

        stage.setTitle("Message ma!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void ajouterIcone(Stage stage) {
        java.io.InputStream is = HelloApplication.class.getResourceAsStream("/images_java/icon_app.png");
        if (is != null) {
            stage.getIcons().add(new javafx.scene.image.Image(is));
        }
    }
}

