package com.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        //charge le fichier fxml
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/example/client/frmConnexion.fxml"));

        //création de la scene
        Scene scene = new Scene(fxmlLoader.load(), 622, 674);

        stage.setTitle("Message Ma");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
