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
    
    // Backend + token Keycloak passent via variables d'environnement
    private final String BACKEND_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_URL",
            "http://localhost:8089/api/internal/v1/esim/catalog");
    private final String BACKEND_SUBSCRIBE_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_SUBSCRIBE_URL",
            "http://localhost:8089/api/internal/v1/esim/subscribe");
    private final String BACKEND_TOKEN = System.getenv("AKUUNDA_BACKEND_TOKEN");
    private final String BACKEND_USER_ID = System.getenv("AKUUNDA_USER_ID");
    
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
                // Appel du catalogue côté backend (token requis)
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_URL))
                        .header("Accept", "application/json")
                        .GET();
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    try {
                        mainContainer.getChildren().remove(loader);
                        System.err.println("[IHM] Catalogue status=" + response.statusCode());
                        if (response.body() != null) {
                            System.err.println("[IHM] Catalogue body length=" + response.body().length());
                        }

                        if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                            // Réponse Transatel = objet { products: [...] }
                            JSONObject catalog = new JSONObject(response.body());
                            JSONArray products = catalog.optJSONArray("products");
                            int foundCount = 0;

                            if (products == null) {
                                plansListContainer.getChildren().add(new Label("Catalogue invalide."));
                                return;
                            }

                            for (int i = 0; i < products.length(); i++) {
                                JSONObject p = products.getJSONObject(i);
                                JSONObject def = p.optJSONObject("productDefinition");

                                if (def != null) {
                                    JSONArray countryList = def.optJSONArray("countryList");
                                    // Filtre par pays (code ISO3 présent dans countryList)
                                    if (isCountryMatching(iso3, countryList)) {
                                        plansListContainer.getChildren().add(createPlanCard(p));
                                        foundCount++;
                                    }
                                }
                            }

                            if (foundCount == 0) {
                                plansListContainer.getChildren().add(new Label("Aucun forfait eSIM trouvé pour ce pays."));
                            }
                        } else {
                            String body = response.body() == null ? "" : response.body().strip();
                            if (body.length() > 200) {
                                body = body.substring(0, 200) + "...";
                            }
                            if (response.statusCode() == 401) {
                                plansListContainer.getChildren().add(new Label("401 - Token manquant ou invalide."));
                            } else {
                                plansListContainer.getChildren().add(new Label("Erreur catalogue: " + response.statusCode()));
                            }
                            if (!body.isEmpty()) {
                                plansListContainer.getChildren().add(new Label(body));
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        plansListContainer.getChildren().add(new Label("Erreur UI: " + ex.getMessage()));
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

    private VBox createPlanCard(JSONObject product) {
        JSONObject def = product.optJSONObject("productDefinition");
        if (def == null) {
            return new VBox();
        }
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #FAF9FB; -fx-border-color: #E5E5EA; -fx-border-radius: 12;");

        String name = def.optString("productId", "Forfait eSIM").replace("_", " ");
        double amount = extractAmount(product);
        String currency = extractCurrency(product);
        Label lbl = new Label(name);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_PURPLE + "; -fx-font-size: 14;");

        Label priceLabel = new Label(formatPrice(amount, currency));
        priceLabel.setStyle("-fx-text-fill: #5A5A5A; -fx-font-size: 12;");

        Button b = new Button("Sélectionner");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + PRIMARY_PURPLE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        b.setOnAction(e -> confirmSubscribe(def.optString("productId", ""), amount));

        card.getChildren().addAll(lbl, priceLabel, b);
        return card;
    }

    // --- RÉCUPÉRATION DES PAYS VIA API (restcountries) ---
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

    private void confirmSubscribe(String productId, double amount) {
        if (productId == null || productId.isBlank()) {
            showAlert("Erreur", "Produit invalide.");
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            showAlert("Erreur", "AKUUNDA_USER_ID est requis pour la souscription.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Souscription eSIM");
        alert.setHeaderText(null);
        alert.setContentText("Confirmer la souscription de ce forfait ?");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                subscribeProduct(productId, amount);
            }
        });
    }

    private void subscribeProduct(String productId, double amount) {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle("Souscription");
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText("Souscription en cours...");
        loading.show();

        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                JSONObject payload = new JSONObject();
                payload.put("productId", productId);
                payload.put("userId", BACKEND_USER_ID);
                payload.put("amount", amount);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_SUBSCRIBE_URL))
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }

                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    loading.close();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            JSONObject body = new JSONObject(response.body());
                            String activationCode = body.optString("activationCode", "");
                            String subscriptionId = body.optString("subscriptionId", "");
                            String serial = body.optString("simSerial", "");
                            String qrCodeValue = body.optString("qrCodeValue", "");
                            String qrCodeDataUrl = body.optString("qrCodeDataUrl", "");

                            String message = "Souscription OK\n" +
                                    "subscriptionId: " + subscriptionId + "\n" +
                                    "simSerial: " + serial + "\n" +
                                    "activationCode: " + activationCode;
                            showSuccessWithQr(message, qrCodeValue, qrCodeDataUrl);
                        } catch (Exception ex) {
                            showAlert("Succès", "Souscription OK (réponse non parsable).");
                        }
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        showAlert("Erreur", "HTTP " + response.statusCode() + "\n" + body);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.close();
                    showAlert("Erreur", e.getMessage());
                });
            }
        }).start();
    }

    private double extractAmount(JSONObject product) {
        JSONObject prices = product.optJSONObject("prices");
        if (prices == null) {
            return 0.0;
        }
        JSONArray subscriptionFee = prices.optJSONArray("subscriptionFee");
        if (subscriptionFee == null || subscriptionFee.length() == 0) {
            return 0.0;
        }
        JSONArray tier = subscriptionFee.optJSONArray(0);
        if (tier == null || tier.length() == 0) {
            return 0.0;
        }
        JSONObject priceItem = tier.optJSONObject(0);
        if (priceItem == null) {
            return 0.0;
        }
        double amountCents = priceItem.optDouble("amount", 0.0);
        return amountCents / 100.0;
    }

    private String extractCurrency(JSONObject product) {
        JSONObject prices = product.optJSONObject("prices");
        if (prices == null) {
            return "";
        }
        JSONArray subscriptionFee = prices.optJSONArray("subscriptionFee");
        if (subscriptionFee == null || subscriptionFee.length() == 0) {
            return "";
        }
        JSONArray tier = subscriptionFee.optJSONArray(0);
        if (tier == null || tier.length() == 0) {
            return "";
        }
        JSONObject priceItem = tier.optJSONObject(0);
        if (priceItem == null) {
            return "";
        }
        return priceItem.optString("currency", "");
    }

    private String formatPrice(double amount, String currency) {
        if (amount <= 0) {
            return "Gratuit";
        }
        if (currency == null || currency.isBlank()) {
            return String.format("%.2f", amount);
        }
        return String.format("%.2f %s", amount, currency);
    }

    private void showSuccessWithQr(String message, String qrCodeValue, String qrCodeDataUrl) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);

        ImageView qrView = buildQrImageView(qrCodeDataUrl);
        if (qrView != null) {
            VBox content = new VBox(10);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().addAll(new Label(message), qrView);
            if (qrCodeValue != null && !qrCodeValue.isBlank()) {
                content.getChildren().add(new Label(qrCodeValue));
            }
            alert.getDialogPane().setContent(content);
        }
        alert.showAndWait();
    }

    private ImageView buildQrImageView(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        String prefix = "base64,";
        int idx = dataUrl.indexOf(prefix);
        if (idx < 0) {
            return null;
        }
        String base64 = dataUrl.substring(idx + prefix.length());
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            Image img = new Image(new java.io.ByteArrayInputStream(bytes));
            ImageView view = new ImageView(img);
            view.setFitWidth(180);
            view.setPreserveRatio(true);
            return view;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(); }
}
