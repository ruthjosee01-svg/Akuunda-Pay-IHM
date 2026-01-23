package com.akuunda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class App extends Application {

    private final String PRIMARY_PURPLE = "#391659";
    private final String ACCENT_ORANGE = "#F88809";

    // Backend + token Keycloak passent via variables d'environnement
    private final String BACKEND_BASE_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_BASE_URL",
            "http://localhost:8089/api/internal/v1");
    private final String BACKEND_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_URL",
            BACKEND_BASE_URL + "/esim/catalog");
    private final String BACKEND_SUBSCRIBE_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_SUBSCRIBE_URL",
            BACKEND_BASE_URL + "/esim/subscribe");
    private final String BACKEND_TRANSACTION_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_TRANSACTION_URL",
            BACKEND_BASE_URL + "/esim/transactions");
    private final String BACKEND_ESIM_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_ESIM_URL",
            BACKEND_BASE_URL + "/esim");
    private final String BACKEND_TOKEN = System.getenv("AKUUNDA_BACKEND_TOKEN");
    private final String BACKEND_USER_ID = System.getenv("AKUUNDA_USER_ID");
    
    private VBox mainContainer;
    private VBox destinationList;
    private TextField searchField;
    private ToggleGroup scopeGroup;
    private List<JSONObject> allCountriesData = new ArrayList<>();
    private HBox selectedCountryCard = null;
    private String selectedCountryName = null;
    private String selectedCountryIso3 = null; // Code 3 lettres (ex: CIV, FRA)
    private String selectedCountryIso2 = null;
    private String userCountryIso2 = null;
    private String userRegion = null;
    private Button nextBtn;

    @Override
    public void start(Stage stage) {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20, 25, 20, 25));
        mainContainer.getStyleClass().add("screen");

        Scene scene = new Scene(mainContainer, 420, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        buildMainSelectionScreen();

        stage.setTitle("Akuunda Pay - eSIM");
        stage.setScene(scene);
        stage.show();

        fetchCountriesFromAPI();
    }

    // --- ÉCRAN 1 : SÉLECTION DU PAYS ---
    private void buildMainSelectionScreen() {
        mainContainer.getChildren().clear();

        HBox header = buildHeader();
        Label title = new Label("Destinations");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Choisissez un pays pour afficher les offres eSIM.");
        subtitle.getStyleClass().add("page-subtitle");

        searchField = new TextField();
        searchField.setPromptText("Rechercher une destination");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndRender());

        HBox scopeTabs = buildScopeTabs();

        destinationList = new VBox(15);
        destinationList.getStyleClass().add("list-container");
        
        // ScrollPane pour la liste des pays
        ScrollPane scrollPane = new ScrollPane(destinationList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        nextBtn = new Button("Continuer");
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setMinHeight(55);
        nextBtn.setDisable(true);
        nextBtn.getStyleClass().add("primary-button");
        
        nextBtn.setOnAction(e -> showPlansPage(selectedCountryName, selectedCountryIso3));

        mainContainer.getChildren().addAll(header, title, subtitle, searchField, scopeTabs, scrollPane, nextBtn);
        if (!allCountriesData.isEmpty()) applyFiltersAndRender();
    }

    // --- ÉCRAN 2 : LISTE DES OFFRES (Filtrée et Scrollable) ---
    private void showPlansPage(String countryName, String iso3) {
        mainContainer.getChildren().clear();
        
        Button backBtn = new Button("← Retour");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label("Offres pour " + countryName);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Forfaits disponibles pour " + countryName + ".");
        subtitle.getStyleClass().add("page-subtitle");

        // Conteneur interne pour les cartes
        VBox plansListContainer = new VBox(15);
        plansListContainer.setPadding(new Insets(10, 5, 10, 5));

        // ScrollPane CRUCIAL pour ne pas que la page se coupe
        ScrollPane scrollPane = new ScrollPane(plansListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ProgressIndicator loader = new ProgressIndicator();
        loader.getStyleClass().add("loader");
        mainContainer.getChildren().addAll(backBtn, title, subtitle, loader, scrollPane);

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

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
        logo.setFitWidth(34);
        logo.setPreserveRatio(true);
        logo.getStyleClass().add("logo");

        Label brand = new Label("Akuunda Pay");
        brand.getStyleClass().add("brand-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button menu = new Button("≡");
        menu.getStyleClass().add("ghost-button");

        header.getChildren().addAll(logo, brand, spacer, menu);
        return header;
    }

    private HBox buildScopeTabs() {
        scopeGroup = new ToggleGroup();
        ToggleButton local = new ToggleButton("Local");
        ToggleButton regional = new ToggleButton("Régional");
        ToggleButton world = new ToggleButton("Monde");

        local.setToggleGroup(scopeGroup);
        regional.setToggleGroup(scopeGroup);
        world.setToggleGroup(scopeGroup);

        local.setUserData("LOCAL");
        regional.setUserData("REGIONAL");
        world.setUserData("WORLD");

        world.setSelected(true);

        scopeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndRender());

        HBox tabs = new HBox(8, local, regional, world);
        tabs.getStyleClass().add("segmented");
        local.getStyleClass().add("segmented-button");
        regional.getStyleClass().add("segmented-button");
        world.getStyleClass().add("segmented-button");
        return tabs;
    }

    private void applyFiltersAndRender() {
        if (allCountriesData == null || allCountriesData.isEmpty()) {
            return;
        }
        String search = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        String scope = "WORLD";
        if (scopeGroup != null && scopeGroup.getSelectedToggle() != null) {
            scope = scopeGroup.getSelectedToggle().getUserData().toString();
        }

        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject country : allCountriesData) {
            String name = country.getJSONObject("name").getString("common");
            String iso2 = country.optString("cca2", "");
            String region = country.optString("region", "");

            if ("LOCAL".equals(scope)) {
                if (userCountryIso2 != null && !userCountryIso2.isBlank()) {
                    if (!userCountryIso2.equalsIgnoreCase(iso2)) {
                        continue;
                    }
                }
            } else if ("REGIONAL".equals(scope)) {
                if (userRegion != null && !userRegion.isBlank()) {
                    if (!userRegion.equalsIgnoreCase(region)) {
                        continue;
                    }
                }
            }

            if (!search.isEmpty() && !name.toLowerCase().contains(search)) {
                continue;
            }
            filtered.add(country);
        }
        updateListView(filtered);
    }

    private void detectUserRegion() {
        userCountryIso2 = Locale.getDefault().getCountry();
        if (userCountryIso2 == null || userCountryIso2.isBlank()) {
            return;
        }
        for (JSONObject country : allCountriesData) {
            String iso2 = country.optString("cca2", "");
            if (userCountryIso2.equalsIgnoreCase(iso2)) {
                userRegion = country.optString("region", null);
                return;
            }
        }
    }

    private String humanizeProductId(String productId) {
        String cleaned = productId.replace("_", " ");
        cleaned = cleaned.replace("WW 9010 STACK ONEOFF ", "");
        cleaned = cleaned.replace("WW 9010 STACK ", "");
        return cleaned.trim();
    }

    private String extractPlanMeta(String productId) {
        String data = "";
        String duration = "";

        java.util.regex.Matcher dataMatcher = java.util.regex.Pattern.compile("(\\d+)(GB|MB)").matcher(productId);
        if (dataMatcher.find()) {
            data = dataMatcher.group(1) + dataMatcher.group(2);
        }

        java.util.regex.Matcher durationMatcher = java.util.regex.Pattern.compile("(\\d+)D").matcher(productId);
        if (durationMatcher.find()) {
            duration = durationMatcher.group(1) + " jours";
        }

        if (!data.isEmpty() && !duration.isEmpty()) {
            return data + " \u2022 " + duration;
        }
        if (!data.isEmpty()) {
            return data;
        }
        if (!duration.isEmpty()) {
            return duration;
        }
        return "Forfait eSIM";
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
        card.getStyleClass().add("plan-card");

        String productId = def.optString("productId", "Forfait eSIM");
        String name = humanizeProductId(productId);
        double amount = extractAmount(product);
        String currency = extractCurrency(product);
        Label lbl = new Label(name);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("plan-title");

        Label priceLabel = new Label(formatPrice(amount, currency));
        priceLabel.getStyleClass().add("plan-price");

        String meta = extractPlanMeta(productId);
        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("plan-meta");

        Button b = new Button("Sélectionner");
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("primary-button");
        b.setOnAction(e -> confirmSubscribe(productId, amount));

        card.getChildren().addAll(lbl, metaLabel, priceLabel, b);
        return card;
    }

    // --- RÉCUPÉRATION DES PAYS VIA API (restcountries) ---
    private void fetchCountriesFromAPI() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                // On récupère cca2/cca3 + region pour filtrer
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,flags,cca3,cca2,region"))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray array = new JSONArray(response.body());
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i));
                list.sort(Comparator.comparing(c -> c.getJSONObject("name").getString("common").toLowerCase()));
                allCountriesData = list;
                detectUserRegion();
                Platform.runLater(this::applyFiltersAndRender);
            } catch (Exception e) {}
        }).start();
    }

    private void updateListView(List<JSONObject> countries) {
        destinationList.getChildren().clear();
        for (JSONObject country : countries) {
            String name = country.getJSONObject("name").getString("common");
            String flag = country.getJSONObject("flags").getString("png");
            String iso3 = country.getString("cca3");
            String iso2 = country.optString("cca2", "");
            String region = country.optString("region", "");
            destinationList.getChildren().add(createCountryCard(name, flag, iso3, iso2, region));
        }
    }

    private HBox createCountryCard(String name, String flagUrl, String iso3, String iso2, String region) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("country-card");
        
        try {
            ImageView iv = new ImageView(new Image(flagUrl, true));
            iv.setFitWidth(34);
            iv.setPreserveRatio(true);
            card.getChildren().add(iv);
        } catch (Exception e) {}

        Label n = new Label(name);
        n.getStyleClass().add("country-name");
        card.getChildren().add(n);

        card.setOnMouseClicked(e -> {
            if (selectedCountryCard != null) {
                selectedCountryCard.getStyleClass().remove("country-card-selected");
            }
            selectedCountryCard = card;
            this.selectedCountryName = name;
            this.selectedCountryIso3 = iso3;
            this.selectedCountryIso2 = iso2;
            card.getStyleClass().add("country-card-selected");
            nextBtn.setDisable(false);
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
        alert.setHeaderText("Confirmer la souscription ?");
        alert.setContentText("Vous allez activer un forfait eSIM pour ce pays.");
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
                            String transactionId = body.optString("transactionId", "");
                            String transactionStatus = body.optString("transactionStatus", "");
                            boolean activationRequired = body.optBoolean("activationRequired", false);
                            String serial = body.optString("simSerial", "");
                            String qrCodeValue = body.optString("qrCodeValue", "");
                            String qrCodeDataUrl = body.optString("qrCodeDataUrl", "");

                            String message = "Souscription OK\n" +
                                    "subscriptionId: " + subscriptionId + "\n" +
                                    "simSerial: " + serial + "\n" +
                                    "activationCode: " + activationCode;

                            if ((activationCode != null && !activationCode.isBlank())
                                    || (qrCodeValue != null && !qrCodeValue.isBlank())
                                    || (qrCodeDataUrl != null && !qrCodeDataUrl.isBlank())) {
                                showSuccessWithQr(message, qrCodeValue, qrCodeDataUrl);
                            } else if (activationRequired && transactionId != null && !transactionId.isBlank()) {
                                showActivationPendingAndPoll(transactionId, serial);
                            } else {
                                String fallback = message;
                                if (transactionStatus != null && !transactionStatus.isBlank()) {
                                    fallback += "\ntransactionStatus: " + transactionStatus;
                                }
                                showAlert("Succès", fallback);
                            }
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

    private void showActivationPendingAndPoll(String transactionId, String simSerial) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Activation eSIM");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Label statusLabel = new Label("Activation en cours...");
        VBox content = new VBox(10, new ProgressIndicator(), statusLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content);

        final boolean[] canceled = {false};
        dialog.setOnCloseRequest(e -> canceled[0] = true);
        dialog.show();

        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                int attempts = 20;
                int delayMs = 3000;
                for (int i = 1; i <= attempts; i++) {
                    if (canceled[0]) {
                        return;
                    }

                    JSONObject status = fetchTransactionStatus(client, transactionId);
                    String asyncStatus = "";
                    if (status != null) {
                        asyncStatus = status.optString("asyncStatus",
                                status.optString("transactionStatus", ""));
                    }
                    String normalized = asyncStatus == null ? "" : asyncStatus.trim().toUpperCase();
                    if ("DONE".equals(normalized) || "SUCCESS".equals(normalized)) {
                        Platform.runLater(() -> statusLabel.setText("Activation terminée. Récupération du QR..."));
                        JSONObject esim = fetchEsimDetails(client, simSerial);
                        Platform.runLater(() -> {
                            dialog.close();
                            if (esim != null) {
                                String activationCode = esim.optString("activationCode", "");
                                String qrValue = "";
                                String qrDataUrl = "";
                                JSONObject qrCode = esim.optJSONObject("qrCode");
                                if (qrCode != null) {
                                    qrValue = qrCode.optString("value", "");
                                    qrDataUrl = qrCode.optString("dataUrl", "");
                                }
                                String message = "Activation OK\n" +
                                        "simSerial: " + simSerial + "\n" +
                                        "activationCode: " + activationCode;
                                showSuccessWithQr(message, qrValue, qrDataUrl);
                            } else {
                                showAlert("Activation", "Activation terminée, mais QR non récupéré.");
                            }
                        });
                        return;
                    }
                    if ("ERROR".equals(normalized)) {
                        Platform.runLater(() -> {
                            dialog.close();
                            showAlert("Erreur", "Activation eSIM en échec.");
                        });
                        return;
                    }

                    int current = i;
                    Platform.runLater(() -> statusLabel.setText("Activation en cours... (" + current + "/" + attempts + ")"));
                    Thread.sleep(delayMs);
                }
                Platform.runLater(() -> {
                    dialog.close();
                    showAlert("Activation", "Activation toujours en cours. Réessayez plus tard.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dialog.close();
                    showAlert("Erreur", e.getMessage());
                });
            }
        }).start();
    }

    private JSONObject fetchTransactionStatus(HttpClient client, String transactionId) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_TRANSACTION_URL + "/" + transactionId))
                .header("Accept", "application/json")
                .GET();
        if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
        }
        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
            return new JSONObject(response.body());
        }
        return null;
    }

    private JSONObject fetchEsimDetails(HttpClient client, String simSerial) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_ESIM_URL + "/" + simSerial))
                .header("Accept", "application/json")
                .GET();
        if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
        }
        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
            return new JSONObject(response.body());
        }
        return null;
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
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Succès");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Label title = new Label("Votre eSIM est prête");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("Scannez le QR code pour installer l'eSIM.");
        subtitle.getStyleClass().add("dialog-subtitle");

        ImageView qrView = buildQrImageView(qrCodeDataUrl);
        VBox content = new VBox(12, title, subtitle);
        content.getStyleClass().add("dialog-content");

        if (qrView != null) {
            content.getChildren().add(qrView);
        }
        if (qrCodeValue != null && !qrCodeValue.isBlank()) {
            Label lpa = new Label("LPA: " + qrCodeValue);
            lpa.getStyleClass().add("dialog-lpa");
            Button copy = new Button("Copier le code LPA");
            copy.getStyleClass().add("secondary-button");
            copy.setOnAction(e -> copyToClipboard(qrCodeValue));
            content.getChildren().addAll(lpa, copy);
        }

        Label nextSteps = new Label("Ensuite, activez les donnees mobiles sur votre iPhone/Android.");
        nextSteps.getStyleClass().add("dialog-hint");
        content.getChildren().add(nextSteps);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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

    private void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        showAlert("Copié", "Le code LPA a été copié.");
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
