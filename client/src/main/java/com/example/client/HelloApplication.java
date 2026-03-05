package com.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("frmChat.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 806, 502);
        stage.setTitle("Message ma!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}

