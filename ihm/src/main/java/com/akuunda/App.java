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
    
    // URL pointant vers ton Backend Akuunda Wallet
    private final String BACKEND_URL = "http://localhost:8089/api/internal/v1/auth/esim/catalog";
    
    private VBox mainContainer;
    private VBox destinationList;
    private List<JSONObject> allCountriesData = new ArrayList<>();
    private HBox selectedCountryCard = null;
    private String selectedCountryName = null;
    private String selectedCountryIso3 = null; // Code 3 lettres (ex: CIV, FRA)
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

    // --- ÉCRAN 1 : SÉLECTION DU PAYS ---
    private void buildMainSelectionScreen() {
        mainContainer.getChildren().clear();
        
        Label brand = new Label("Akuunda Pay");
        brand.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        
        Label title = new Label("Choisissez une destination");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        destinationList = new VBox(15);
        
        // ScrollPane pour la liste des pays
        ScrollPane scrollPane = new ScrollPane(destinationList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        nextBtn = new Button("Continuer");
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setMinHeight(55);
        nextBtn.setDisable(true);
        nextBtn.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        
        nextBtn.setOnAction(e -> showPlansPage(selectedCountryName, selectedCountryIso3));

        mainContainer.getChildren().addAll(brand, title, scrollPane, nextBtn);
        if (!allCountriesData.isEmpty()) updateListView(allCountriesData);
    }

    // --- ÉCRAN 2 : LISTE DES OFFRES (Filtrée et Scrollable) ---
    private void showPlansPage(String countryName, String iso3) {
        mainContainer.getChildren().clear();
        
        Button backBtn = new Button("← Retour");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + PRIMARY_PURPLE + "; -fx-cursor: hand;");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label("Offres pour " + countryName);
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        // Conteneur interne pour les cartes
        VBox plansListContainer = new VBox(15);
        plansListContainer.setPadding(new Insets(10, 5, 10, 5));

        // ScrollPane CRUCIAL pour ne pas que la page se coupe
        ScrollPane scrollPane = new ScrollPane(plansListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ProgressIndicator loader = new ProgressIndicator();
        mainContainer.getChildren().addAll(backBtn, title, loader, scrollPane);

        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BACKEND_URL)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    mainContainer.getChildren().remove(loader);
                    if (response.statusCode() == 200 && !response.body().isEmpty()) {
                        JSONArray products = new JSONArray(response.body());
                        int foundCount = 0;

                        for (int i = 0; i < products.length(); i++) {
                            JSONObject p = products.getJSONObject(i);
                            JSONObject def = p.optJSONObject("productDefinition");
                            
                            if (def != null) {
                                JSONArray countryList = def.optJSONArray("countryList");
                                // On filtre : le code ISO3 du pays sélectionné est-il dans la liste Transatel ?
                                if (isCountryMatching(iso3, countryList)) {
                                    plansListContainer.getChildren().add(createPlanCard(def));
                                    foundCount++;
                                }
                            }
                        }

                        if (foundCount == 0) {
                            plansListContainer.getChildren().add(new Label("Aucun forfait eSIM trouvé pour ce pays."));
                        }
                    } else {
                        plansListContainer.getChildren().add(new Label("Erreur : Impossible de charger le catalogue."));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> plansListContainer.getChildren().add(new Label("Erreur réseau : " + e.getMessage())));
            }
        }).start();
    }

    private boolean isCountryMatching(String target, JSONArray list) {
        if (list == null) return false;
        for (int i = 0; i < list.length(); i++) {
            if (list.getString(i).equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    private VBox createPlanCard(JSONObject def) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: #E5E5EA; -fx-border-radius: 12;");
        
        String name = def.optString("productId", "Forfait eSIM").replace("_", " ");
        Label lbl = new Label(name);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + "; -fx-font-size: 14;");
        
        Button b = new Button("Sélectionner");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + PRIMARY_PURPLE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        
        card.getChildren().addAll(lbl, b);
        return card;
    }

    // --- RÉCUPÉRATION DES PAYS VIA API ---
    private void fetchCountriesFromAPI() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                // On récupère cca3 pour avoir les codes 3 lettres (ex: CIV, FRA)
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,flags,cca3"))
                        .build();
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
            String iso3 = country.getString("cca3");
            destinationList.getChildren().add(createCountryCard(name, flag, iso3));
        }
    }

    private HBox createCountryCard(String name, String flagUrl, String iso3) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #E5E5EA; -fx-border-radius: 10; -fx-cursor: hand;");
        
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
            this.selectedCountryIso3 = iso3;
            card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: " + ACCENT_ORANGE + "; -fx-border-width: 2; -fx-border-radius: 10;");
            nextBtn.setDisable(false);
            nextBtn.setStyle("-fx-background-color: " + ACCENT_ORANGE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        });
        return card;
    }

    public static void main(String[] args) { launch(); }
}