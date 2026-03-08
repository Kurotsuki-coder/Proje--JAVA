package com.example.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public class GifPickerController {

    @FXML private TextField txt_search_gif;
    @FXML private TilePane tile_gifs;

    //private static final String API_KEY = "kCKihBAAqdGTB33Mw0mNkB8FnumWreK6";
    private static final String API_KEY = "DcPigCkxbh4CLWF291RgvMHiF9Sep8TG";

    private static final String GIPHY_URL = "https://api.giphy.com/v1/gifs/search?api_key="
            + API_KEY + "&limit=12&rating=g&q=";

    private Consumer<String> onGifSelected;

    public void setOnGifSelected(Consumer<String> callback) {
        this.onGifSelected = callback;
    }

    @FXML
    public void initialize() {
        // Recherche en temps réel quand on tape
        txt_search_gif.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                rechercherGifs(newVal.trim());
            }
        });

        rechercherGifsTendance();
    }

    private void rechercherGifsTendance() {
        String url = "https://api.giphy.com/v1/gifs/trending?api_key=" + API_KEY + "&limit=12&rating=g";
        lancerRequete(url);
    }

    private void rechercherGifs(String query) {
        try {
            String urlEncodee = query.replace(" ", "+");
            lancerRequete(GIPHY_URL + urlEncodee);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lancerRequete(String url) {
        // On fait une requête HTTP dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Parser le JSON
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode data = root.get("data");

                Platform.runLater(() -> {
                    tile_gifs.getChildren().clear();
                    for (JsonNode gif : data) {
                        // On récupère l'URL du GIF en taille réduite (preview)
                        String gifUrl = gif.get("images").get("fixed_height_small").get("url").asText();
                        String gifUrlOriginal = gif.get("images").get("original").get("url").asText();

                        afficherGif(gifUrl, gifUrlOriginal);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void afficherGif(String previewUrl, String originalUrl) {
        // WebView pour afficher le GIF animé
        WebView webView = new WebView();
        webView.setPrefSize(100, 100);
        webView.setMaxSize(100, 100);

        // HTML minimal pour afficher le GIF
        String html = "<html><body style='margin:0;padding:0;background:black;'>"
                + "<img src='" + previewUrl + "' width='100' height='100' style='object-fit:cover;'/>"
                + "</body></html>";
        webView.getEngine().loadContent(html);

        // Au clic → envoyer l'URL originale et fermer le popup
        webView.setOnMouseClicked(e -> {
            if (onGifSelected != null) {
                onGifSelected.accept(originalUrl);
            }
            ((Stage) tile_gifs.getScene().getWindow()).close();
        });

        TilePane.setMargin(webView, new Insets(3));
        tile_gifs.getChildren().add(webView);
    }
}