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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class App extends Application {

    // COULEURS OFFICIELLES AKUUNDA PAY [cite: 165, 173]
    private final String PRIMARY_PURPLE = "#391659";
    private final String ACCENT_ORANGE = "#F88809";
    private final String LIGHT_GRAY = "#F2F2F7";
    private final String TEXT_GRAY = "#8E8E93";
    
    private VBox mainContainer;
    private VBox destinationList;
    private List<JSONObject> allCountriesData = new ArrayList<>();
    private HBox selectedCountryCard = null;
    private Button nextBtn;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20, 25, 20, 25));
        mainContainer.setStyle("-fx-background-color: #FFFFFF;");

        scene = new Scene(mainContainer, 420, 750);
        try {
            var cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) {
            System.err.println("Style CSS non trouvé.");
        }

        buildMainSelectionScreen();

        stage.setTitle("Akuunda Pay - eSIM");
        stage.setScene(scene);
        stage.show();

        fetchCountriesFromAPI();
    }

    /**
     * ÉCRAN 1 : SÉLECTION DES PAYS
     */
    private void buildMainSelectionScreen() {
        mainContainer.getChildren().clear();

        // Logo [cite: 74, 554]
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
            ImageView logoView = new ImageView(logoImg);
            logoView.setFitHeight(40);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            Label brand = new Label("Akuunda Pay");
            brand.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: " + PRIMARY_PURPLE + ";");
            header.getChildren().add(brand);
        }

        VBox titles = new VBox(5);
        Label title = new Label("Choisissez une destination");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        Label subtitle = new Label("Où souhaitez-vous activer votre eSIM ?");
        subtitle.setStyle("-fx-text-fill: " + TEXT_GRAY + "; -fx-font-size: 14;");
        titles.getChildren().addAll(title, subtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un pays...");
        searchField.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-background-radius: 12; -fx-padding: 12;");
        searchField.textProperty().addListener((obs, old, newValue) -> filterCountries(newValue));

        destinationList = new VBox(15);
        ScrollPane scrollPane = new ScrollPane(destinationList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        nextBtn = new Button("Continuer");
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setMinHeight(55);
        nextBtn.setDisable(true);
        nextBtn.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        
        nextBtn.setOnAction(e -> {
            if (selectedCountryCard != null) {
                Label lbl = (Label) ((VBox) selectedCountryCard.getChildren().get(1)).getChildren().get(0);
                showPlansPage(lbl.getText());
            }
        });

        mainContainer.getChildren().addAll(header, titles, searchField, scrollPane, nextBtn);
        if (!allCountriesData.isEmpty()) updateListView(allCountriesData);
    }

    /**
     * ÉCRAN 2 : LISTE DES FORFAITS [cite: 373]
     */
    private void showPlansPage(String countryName) {
        mainContainer.getChildren().clear();

        Button backBtn = new Button("← Retour");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + PRIMARY_PURPLE + "; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label("Forfaits " + countryName);
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        VBox plansList = new VBox(15);
        // Simulation de données qui viendront de l'API Backend plus tard
        plansList.getChildren().addAll(
            createPlanCard("Pack Voyageur", "5 Go - 30 Jours", "12.99 €", countryName),
            createPlanCard("Pack Business", "20 Go - 30 Jours", "29.99 €", countryName),
            createPlanCard("Découverte", "1 Go - 7 Jours", "4.99 €", countryName)
        );

        mainContainer.getChildren().addAll(backBtn, title, plansList);
    }

    private VBox createPlanCard(String name, String detail, String price, String country) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: #E5E5EA; -fx-border-radius: 15; -fx-background-radius: 15;");

        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        
        HBox row = new HBox();
        Label lblDetail = new Label(detail);
        lblDetail.setStyle("-fx-text-fill: " + TEXT_GRAY + ";");
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Label lblPrice = new Label(price);
        lblPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ACCENT_ORANGE + "; -fx-font-size: 18;");
        row.getChildren().addAll(lblDetail, s, lblPrice);

        Button buy = new Button("Choisir ce forfait");
        buy.setMaxWidth(Double.MAX_VALUE);
        buy.setStyle("-fx-background-color: " + PRIMARY_PURPLE + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10; -fx-cursor: hand;");
        
        // Action : Aller au récapitulatif
        buy.setOnAction(e -> showCheckoutPage(country, name, detail, price));

        card.getChildren().addAll(lblName, row, buy);
        return card;
    }

    /**
     * ÉCRAN 3 : RÉCAPITULATIF (CHECKOUT) [cite: 20, 27]
     */
    private void showCheckoutPage(String country, String planName, String detail, String price) {
        mainContainer.getChildren().clear();

        Button backBtn = new Button("← Modifier le forfait");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + PRIMARY_PURPLE + "; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> showPlansPage(country));

        Label title = new Label("Récapitulatif");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");

        // Panneau récapitulatif stylisé
        VBox recapBox = new VBox(15);
        recapBox.setPadding(new Insets(25));
        recapBox.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-background-radius: 20;");

        Label lblDest = new Label("Destination : " + country);
        lblDest.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        
        Label lblPlan = new Label("Forfait : " + planName + " (" + detail + ")");
        
        Separator sep = new Separator();
        
        HBox totalRow = new HBox();
        Label lblTotalTxt = new Label("Total à payer");
        lblTotalTxt.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Label lblTotalPrice = new Label(price);
        lblTotalPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 22; -fx-text-fill: " + ACCENT_ORANGE + ";");
        totalRow.getChildren().addAll(lblTotalTxt, s, lblTotalPrice);

        recapBox.getChildren().addAll(lblDest, lblPlan, sep, totalRow);

        // Badge Sécurité [cite: 27]
        Label securityLabel = new Label("✓ Paiement sécurisé par Blockchain");
        securityLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 12; -fx-font-weight: bold;");
        securityLabel.setAlignment(Pos.CENTER);
        securityLabel.setMaxWidth(Double.MAX_VALUE);

        Button payBtn = new Button("Confirmer et Payer");
        payBtn.setMaxWidth(Double.MAX_VALUE);
        payBtn.setMinHeight(60);
        payBtn.setStyle("-fx-background-color: " + ACCENT_ORANGE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18; -fx-background-radius: 15; -fx-cursor: hand;");
        
        payBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText("Paiement réussi !");
            alert.setContentText("Votre eSIM pour " + country + " est en cours d'activation.");
            alert.showAndWait();
            buildMainSelectionScreen(); // Retour au début
        });

        mainContainer.getChildren().addAll(backBtn, title, recapBox, securityLabel, payBtn);
    }

    // --- LOGIQUE API (Inchangée) ---

    private void fetchCountriesFromAPI() {
        Thread thread = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,region,flags"))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray array = new JSONArray(response.body());
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i));
                list.sort(Comparator.comparing(c -> c.getJSONObject("name").getString("common").toLowerCase()));
                allCountriesData = list;
                Platform.runLater(() -> updateListView(allCountriesData));
            } catch (Exception e) { e.printStackTrace(); }
        });
        thread.start();
    }

    private void updateListView(List<JSONObject> countries) {
        destinationList.getChildren().clear();
        for (JSONObject country : countries) {
            String name = country.getJSONObject("name").getString("common");
            String region = country.getString("region");
            String flag = country.getJSONObject("flags").getString("png");
            destinationList.getChildren().add(createCountryCard(name, region, flag));
        }
    }

    private void filterCountries(String query) {
        if (query == null || query.isEmpty()) { updateListView(allCountriesData); return; }
        List<JSONObject> filtered = allCountriesData.stream()
            .filter(c -> c.getJSONObject("name").getString("common").toLowerCase().contains(query.toLowerCase()))
            .toList();
        updateListView(filtered);
    }

    private HBox createCountryCard(String name, String region, String flagUrl) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #E5E5EA; -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");

        StackPane flagBox = new StackPane();
        flagBox.setMinSize(40, 25);
        try {
            ImageView iv = new ImageView(new Image(flagUrl, true));
            iv.setFitWidth(35); iv.setPreserveRatio(true);
            flagBox.getChildren().add(iv);
        } catch (Exception e) {}

        VBox texts = new VBox(2);
        Label n = new Label(name); n.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + ";");
        Label r = new Label(region); r.setStyle("-fx-text-fill: " + TEXT_GRAY + "; -fx-font-size: 11;");
        texts.getChildren().addAll(n, r);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("eSIM");
        badge.setStyle("-fx-text-fill: " + ACCENT_ORANGE + "; -fx-font-size: 10; -fx-font-weight: bold; -fx-background-color: #FFF0E0; -fx-padding: 4 8; -fx-background-radius: 8;");

        card.getChildren().addAll(flagBox, texts, spacer, badge);

        card.setOnMouseClicked(e -> {
            if (selectedCountryCard != null) {
                selectedCountryCard.setStyle("-fx-background-color: white; -fx-border-color: #E5E5EA; -fx-border-radius: 15; -fx-background-radius: 15;");
            }
            selectedCountryCard = card;
            card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: " + ACCENT_ORANGE + "; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");
            nextBtn.setDisable(false);
            nextBtn.setStyle("-fx-background-color: " + ACCENT_ORANGE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
        });
        return card;
    }

    public static void main(String[] args) {
        launch();
    }
}