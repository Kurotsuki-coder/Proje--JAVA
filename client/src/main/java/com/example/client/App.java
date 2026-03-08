package com.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/example/client/frmConnexion.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 622, 674);
        stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/images_java/icon_app.png")
        ));

        stage.setTitle("Message Ma");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
