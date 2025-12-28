package com.akuunda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class App extends Application {

    private final String PRIMARY_PURPLE = "#391659";
    private final String ACCENT_ORANGE = "#F88809";
    
    // Changement du port vers 8089 (détecté par ta commande lsof)
    private final String BACKEND_URL = "http://localhost:8089/api/v1/esim/catalog";
    
    private VBox mainContainer;
    private VBox destinationList;
    private List<JSONObject> allCountriesData = new ArrayList<>();
    private HBox selectedCountryCard = null;
    private String selectedCountryName = null;
    private Button nextBtn;

    @Override
    public void start(Stage stage) {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20, 25, 20, 25));
        mainContainer.setStyle("-fx-background-color: #FFFFFF;");

        Scene scene = new Scene(mainContainer, 420, 750);
        buildMainSelectionScreen();

        stage.setTitle("Akuunda Pay - eSIM");
        stage.setScene(scene);
        stage.show();

        fetchCountriesFromAPI();
    }

    private void buildMainSelectionScreen() {
        mainContainer.getChildren().clear();
        Label brand = new Label("Akuunda Pay");
        brand.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        Label title = new Label("Choisissez une destination");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        destinationList = new VBox(15);
        ScrollPane scrollPane = new ScrollPane(destinationList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        nextBtn = new Button("Continuer");
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setMinHeight(55);
        nextBtn.setDisable(true);
        nextBtn.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        
        nextBtn.setOnAction(e -> showPlansPage(selectedCountryName));

        mainContainer.getChildren().addAll(brand, title, scrollPane, nextBtn);
        if (!allCountriesData.isEmpty()) updateListView(allCountriesData);
    }

    private void showPlansPage(String countryName) {
        mainContainer.getChildren().clear();
        Button backBtn = new Button("← Retour");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label("Offres pour " + countryName);
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        VBox plansContainer = new VBox(15);
        ProgressIndicator loader = new ProgressIndicator();
        Label loadingMsg = new Label("Chargement des offres réelles...");
        
        mainContainer.getChildren().addAll(backBtn, title, loader, loadingMsg);

        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_URL))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    mainContainer.getChildren().removeAll(loader, loadingMsg);
                    // On vérifie si le corps de la réponse n'est pas vide
                    if (response.statusCode() == 200 && !response.body().trim().equals("[]") && !response.body().isEmpty()) {
                        JSONArray products = new JSONArray(response.body());
                        for (int i = 0; i < products.length(); i++) {
                            JSONObject p = products.getJSONObject(i);
                            // On sécurise l'extraction pour éviter les plantages si Transatel change le format
                            String productId = p.optJSONObject("productDefinition") != null 
                                               ? p.getJSONObject("productDefinition").optString("productId", "Forfait " + countryName)
                                               : "Offre eSIM " + (i+1);
                            plansContainer.getChildren().add(createPlanCard(productId));
                        }
                    } else {
                        // Si Safari était blanc, on arrive ici
                        Label errorLabel = new Label("Aucune offre trouvée.\nVérifiez la connexion Transatel dans le Backend.");
                        errorLabel.setStyle("-fx-text-fill: red; -fx-alignment: center;");
                        plansContainer.getChildren().add(errorLabel);
                    }
                    mainContainer.getChildren().add(plansContainer);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingMsg.setText("Erreur : Le serveur sur le port 8089 ne répond pas.");
                    loader.setVisible(false);
                });
            }
        }).start();
    }

    private VBox createPlanCard(String name) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: #E5E5EA; -fx-border-radius: 12;");
        Label lbl = new Label(name.replace("_", " "));
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        Button b = new Button("Sélectionner");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + PRIMARY_PURPLE + "; -fx-text-fill: white; -fx-background-radius: 8;");
        card.getChildren().addAll(lbl, b);
        return card;
    }

    private void fetchCountriesFromAPI() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://restcountries.com/v3.1/all?fields=name,flags")).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray array = new JSONArray(response.body());
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i));
                list.sort(Comparator.comparing(c -> c.getJSONObject("name").getString("common").toLowerCase()));
                allCountriesData = list;
                Platform.runLater(() -> updateListView(allCountriesData));
            } catch (Exception e) {}
        }).start();
    }

    private void updateListView(List<JSONObject> countries) {
        destinationList.getChildren().clear();
        for (JSONObject country : countries) {
            String name = country.getJSONObject("name").getString("common");
            String flag = country.getJSONObject("flags").getString("png");
            destinationList.getChildren().add(createCountryCard(name, flag));
        }
    }

    private HBox createCountryCard(String name, String flagUrl) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #E5E5EA; -fx-border-radius: 10;");
        try {
            ImageView iv = new ImageView(new Image(flagUrl, true));
            iv.setFitWidth(30); iv.setPreserveRatio(true);
            card.getChildren().add(iv);
        } catch (Exception e) {}
        Label n = new Label(name);
        n.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        card.getChildren().add(n);
        card.setOnMouseClicked(e -> {
            if (selectedCountryCard != null) selectedCountryCard.setStyle("-fx-background-color: white; -fx-border-color: #E5E5EA; -fx-border-radius: 10;");
            selectedCountryCard = card;
            this.selectedCountryName = name;
            card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: " + ACCENT_ORANGE + "; -fx-border-width: 2; -fx-border-radius: 10;");
            nextBtn.setDisable(false);
            nextBtn.setStyle("-fx-background-color: " + ACCENT_ORANGE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        });
        return card;
    }

    public static void main(String[] args) { launch(); }
}