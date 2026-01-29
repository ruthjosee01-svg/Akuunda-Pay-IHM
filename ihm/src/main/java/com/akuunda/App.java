package com.akuunda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final String REALM_NAME = System.getenv().getOrDefault("AKUUNDA_REALM_NAME", "akuunda-realm");
    private final String BACKEND_TOKEN = System.getenv("AKUUNDA_BACKEND_TOKEN");
    private final String BACKEND_USER_ID = System.getenv("AKUUNDA_USER_ID");
    
    private VBox mainContainer;
    private Stage primaryStage;
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
    private Label walletBalanceLabel;
    private String cachedDisplayName = null;
    private String currentLang = "fr";
    private boolean isPlansPage = false;
    private String lastPlansCountryName = null;
    private String lastPlansCountryIso3 = null;
    private final Map<String, Map<String, String>> i18n = buildI18n();
    private final Map<String, String> langLabels = Map.of(
            "fr", "Francais",
            "en", "English",
            "de", "Deutsch",
            "es", "Espanol",
            "zh", "\u4e2d\u6587"
    );

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20, 25, 20, 25));
        mainContainer.getStyleClass().add("screen");

        Scene scene = new Scene(mainContainer, 420, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        buildEsimIntroScreen();

        stage.setTitle(t("app.title"));
        stage.setScene(scene);
        stage.show();

        fetchCountriesFromAPI();
    }

    // --- ÉCRAN 0 : ACCUEIL (style app principale) ---


    // --- ÉCRAN 0.5 : INTRO eSIM ---
    private void buildEsimIntroScreen() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        if (primaryStage != null) {
            primaryStage.setTitle(t("esim.intro.header"));
        }

        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("top-bar-button");
        backBtn.setDisable(true);

        Label topTitle = new Label(t("esim.intro.header"));
        topTitle.getStyleClass().add("top-bar-title");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Button settingsBtn = new Button("⚙");
        settingsBtn.getStyleClass().add("top-bar-button");
        settingsBtn.setDisable(true);

                Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, leftSpacer, topTitle, rightSpacer, settingsBtn);

        Label title = new Label(t("esim.intro.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("esim.intro.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        Button card = new Button(t("esim.intro.card"));
        card.getStyleClass().add("intro-card");
        card.setOnAction(e -> buildMainSelectionScreen());

        Label info = new Label(t("esim.intro.info"));
        info.getStyleClass().add("info-banner");
        info.setWrapText(true);

        mainContainer.getChildren().addAll(topBar, title, subtitle, card, info);
    }


// --- ÉCRAN 1 : SÉLECTION DU PAYS ---
    private void buildMainSelectionScreen() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        if (primaryStage != null) {
            primaryStage.setTitle(t("app.title"));
        }


        HBox header = buildHeader();
        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildEsimIntroScreen());

        Label title = new Label(t("destinations.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("destinations.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        Label greeting = new Label(buildGreetingText());
        greeting.getStyleClass().add("greeting-label");

        searchField = new TextField();
        searchField.setPromptText(t("search.prompt"));
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

        nextBtn = new Button(t("cta.viewPlans"));
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setMinHeight(55);
        nextBtn.setDisable(true);
        nextBtn.getStyleClass().add("primary-button");
        
        nextBtn.setOnAction(e -> showPlansPage(selectedCountryName, selectedCountryIso3));

        mainContainer.getChildren().addAll(header, backBtn, title, subtitle, greeting, searchField, scopeTabs, scrollPane, nextBtn);
        isPlansPage = false;
        if (nextBtn != null) {
            nextBtn.setDisable(selectedCountryIso3 == null || selectedCountryIso3.isBlank());
        }
        if (!allCountriesData.isEmpty()) applyFiltersAndRender();
        fetchWalletBalance();
    }

    // --- ÉCRAN 2 : LISTE DES OFFRES (Filtrée et Scrollable) ---
    private void showPlansPage(String countryName, String iso3) {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = true;
        lastPlansCountryName = countryName;
        lastPlansCountryIso3 = iso3;
        
        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label(t("plans.title") + " " + countryName);
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("plans.subtitle") + " " + countryName + ".");
        subtitle.getStyleClass().add("page-subtitle-dark");

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
                                plansListContainer.getChildren().add(new Label(t("errors.catalog.invalid")));
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
                                plansListContainer.getChildren().add(new Label(t("plans.none")));
                            }
                        } else {
                            String body = response.body() == null ? "" : response.body().strip();
                            if (body.length() > 200) {
                                body = body.substring(0, 200) + "...";
                            }
                            if (response.statusCode() == 401) {
                                plansListContainer.getChildren().add(new Label(t("errors.token")));
                            } else {
                                plansListContainer.getChildren().add(new Label(t("errors.catalog") + " " + response.statusCode()));
                            }
                            if (!body.isEmpty()) {
                                plansListContainer.getChildren().add(new Label(body));
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        plansListContainer.getChildren().add(new Label(t("errors.ui") + " " + ex.getMessage()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> plansListContainer.getChildren().add(new Label(formatNetworkError(e))));
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ChoiceBox<String> langSelect = new ChoiceBox<>();
        langSelect.getItems().addAll("Francais", "English", "Deutsch", "Espanol", "中文");
        langSelect.setValue(langLabels.getOrDefault(currentLang, "Francais"));
        langSelect.getStyleClass().add("lang-select");
        langSelect.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            currentLang = switch (newVal) {
                case "English" -> "en";
                case "Deutsch" -> "de";
                case "Espanol" -> "es";
                case "中文" -> "zh";
                default -> "fr";
            };
            if (primaryStage != null) {
                primaryStage.setTitle(t("app.title"));
            }
            refreshSelectedCountryName();
            if (isPlansPage && lastPlansCountryIso3 != null) {
                showPlansPage(lastPlansCountryName, lastPlansCountryIso3);
            } else {
                buildEsimIntroScreen();
            }
        });

        header.getChildren().addAll(logo, spacer, langSelect);
        return header;
    }

    private String buildGreetingText() {
        String name = getDisplayNameFromToken();
        if (name == null || name.isBlank()) {
            return t("greeting.default");
        }
        return t("greeting.default") + ", " + name;
    }

    private String getDisplayNameFromToken() {
        if (cachedDisplayName != null) {
            return cachedDisplayName;
        }
        if (BACKEND_TOKEN == null || BACKEND_TOKEN.isBlank()) {
            return null;
        }
        try {
            String[] parts = BACKEND_TOKEN.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = parts[1];
            int pad = (4 - payload.length() % 4) % 4;
            payload = payload + "=".repeat(pad);
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(payload);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            String name = obj.optString("name", "");
            if (name == null || name.isBlank()) {
                name = obj.optString("preferred_username", "");
            }
            if (name == null || name.isBlank()) {
                name = obj.optString("given_name", "");
            }
            if (name != null && !name.isBlank()) {
                cachedDisplayName = name;
            }
            return cachedDisplayName;
        } catch (Exception e) {
            return null;
        }
    }


    private void fetchWalletBalance() {
        if (walletBalanceLabel == null) {
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            walletBalanceLabel.setText(t("wallet.placeholder"));
                            return;
        }
        String url = BACKEND_BASE_URL + "/users/" + REALM_NAME + "/wallet-balance/user-id/" + BACKEND_USER_ID;
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET();
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    JSONObject body = new JSONObject(response.body());
                    JSONObject data = body.optJSONObject("data");
                    if (data != null) {
                        double userBalance = data.optDouble("userBalance", -1);
                        double convertedAmount = data.optDouble("convertedAmount", -1);
                        String text;
                        if (convertedAmount >= 0 && userBalance >= 0) {
                            text = String.format("%s: %.2f (≈ %.2f USDC)", t("wallet.prefix"), convertedAmount, userBalance);
                        } else {
                            text = t("wallet.unavailable");
                        }
                        final String finalText = text;
                        Platform.runLater(() -> walletBalanceLabel.setText(finalText));
                        return;
                    }
                }
                Platform.runLater(() -> walletBalanceLabel.setText(t("wallet.unavailable")));
            } catch (Exception e) {
                Platform.runLater(() -> walletBalanceLabel.setText(t("wallet.unavailable")));
            }
        }).start();
    }

    private HBox buildScopeTabs() {
        scopeGroup = new ToggleGroup();
        ToggleButton local = new ToggleButton(t("scope.local"));
        ToggleButton regional = new ToggleButton(t("scope.regional"));
        ToggleButton world = new ToggleButton(t("scope.world"));

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
            String name = getCountryDisplayName(country);
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
        filtered.sort(Comparator.comparing(c -> getCountryDisplayName(c).toLowerCase()));
        updateListView(filtered, scope);
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
            duration = durationMatcher.group(1) + " " + localizeDurationUnit();
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
        return t("plan.defaultMeta");
    }

    private String localizeDurationUnit() {
        return switch (currentLang) {
            case "fr" -> "jours";
            case "en" -> "days";
            case "de" -> "Tage";
            case "es" -> "dias";
            case "zh" -> "\u5929";
            default -> "days";
        };
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

        String productId = def.optString("productId", t("plan.defaultMeta"));
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

        Button b = new Button(t("plan.select"));
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
                        .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,translations,flags,cca3,cca2,region"))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray array = new JSONArray(response.body());
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i));
                list.sort(Comparator.comparing(c -> getCountryDisplayName(c).toLowerCase()));
                allCountriesData = list;
                detectUserRegion();
                Platform.runLater(this::applyFiltersAndRender);
            } catch (Exception e) {}
        }).start();
    }

        private void updateListView(List<JSONObject> countries, String scope) {
        destinationList.getChildren().clear();
        if (countries == null || countries.isEmpty()) {
            return;
        }

        if ("WORLD".equals(scope)) {
            List<String> order = List.of("Africa", "Americas", "Asia", "Europe", "Oceania", "Antarctic", "Other");
            Map<String, List<JSONObject>> grouped = new HashMap<>();
            for (JSONObject country : countries) {
                String region = country.optString("region", "Other");
                if (region == null || region.isBlank()) {
                    region = "Other";
                }
                grouped.computeIfAbsent(region, k -> new ArrayList<>()).add(country);
            }
            for (String region : order) {
                List<JSONObject> list = grouped.get(region);
                if (list == null || list.isEmpty()) {
                    continue;
                }
                list.sort(Comparator.comparing(c -> getCountryDisplayName(c).toLowerCase()));
                destinationList.getChildren().add(createContinentCard(region, list));
            }
            return;
        }

        countries.sort(Comparator.comparing(c -> getCountryDisplayName(c).toLowerCase()));
        for (JSONObject country : countries) {
            destinationList.getChildren().add(createCountryCard(country));
        }
    }

    private HBox createContinentCard(String region, List<JSONObject> countries) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.getStyleClass().addAll("country-card", "continent-card");

        Region icon = new Region();
        icon.getStyleClass().add("continent-icon");
        Label name = new Label(localizeRegion(region));
        name.getStyleClass().add("continent-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(String.valueOf(countries.size()));
        count.getStyleClass().add("continent-count");
        Label arrow = new Label("> ");
        arrow.getStyleClass().add("continent-arrow");

        card.getChildren().addAll(icon, name, spacer, count, arrow);
        card.setOnMouseClicked(e -> openContinentWindow(region, countries));
        return card;
    }

    private void openContinentWindow(String region, List<JSONObject> countries) {
        String localizedRegion = localizeRegion(region);
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label(t("continent.title") + " " + localizedRegion);
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("continent.subtitle") + " " + localizedRegion + ".");
        subtitle.getStyleClass().add("page-subtitle-dark");

        VBox listContainer = new VBox(12);
        listContainer.setPadding(new Insets(10, 5, 10, 5));
        for (JSONObject c : countries) {
            listContainer.getChildren().add(createCountryCard(c, false, null));
        }

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mainContainer.getChildren().addAll(backBtn, title, subtitle, scrollPane);
        if (primaryStage != null) {
            primaryStage.setTitle(t("continent.title") + " " + localizedRegion);
        }
    }

    private HBox createCountryCard(JSONObject country) {
        return createCountryCard(country, true, null);
    }

    private HBox createCountryCard(JSONObject country, boolean trackSelection, Stage stageToClose) {
        String name = getCountryDisplayName(country);
        String flagUrl = "";
        String iso3 = country.optString("cca3", "");
        String iso2 = country.optString("cca2", "");
        JSONObject flags = country.optJSONObject("flags");
        if (flags != null) {
            flagUrl = flags.optString("png", "");
        }

        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("country-card");
        
        try {
            if (flagUrl != null && !flagUrl.isBlank()) {
                ImageView iv = new ImageView(new Image(flagUrl, true));
                iv.setFitWidth(34);
                iv.setPreserveRatio(true);
                card.getChildren().add(iv);
            }
        } catch (Exception e) {}

        Label n = new Label(name);
        n.getStyleClass().add("country-name");
        card.getChildren().add(n);

        if (trackSelection && selectedCountryIso3 != null && selectedCountryIso3.equalsIgnoreCase(iso3)) {
            selectedCountryCard = card;
            card.getStyleClass().add("country-card-selected");
        }

        card.setOnMouseClicked(e -> handleCountryClick(country, card, trackSelection, stageToClose));
        return card;
    }

    private void selectCountry(JSONObject country, HBox card) {
        if (selectedCountryCard != null) {
            selectedCountryCard.getStyleClass().remove("country-card-selected");
        }
        selectedCountryCard = card;
        if (card != null) {
            card.getStyleClass().add("country-card-selected");
        }
        selectedCountryIso3 = country.optString("cca3", "");
        selectedCountryIso2 = country.optString("cca2", "");
        selectedCountryName = getCountryDisplayName(country);
        if (nextBtn != null) {
            nextBtn.setDisable(selectedCountryIso3 == null || selectedCountryIso3.isBlank());
        }
    }

    private void handleCountryClick(JSONObject country, HBox card, boolean trackSelection, Stage stageToClose) {
        selectCountry(country, trackSelection ? card : null);
        if (stageToClose != null) {
            stageToClose.close();
        }
        if (selectedCountryIso3 != null && !selectedCountryIso3.isBlank()) {
            showPlansPage(selectedCountryName, selectedCountryIso3);
        }
    }

    private void refreshSelectedCountryName() {
        if (selectedCountryIso3 != null && !selectedCountryIso3.isBlank()) {
            JSONObject country = findCountryByIso3(selectedCountryIso3);
            if (country != null) {
                selectedCountryName = getCountryDisplayName(country);
            }
        }
        if (lastPlansCountryIso3 != null && !lastPlansCountryIso3.isBlank()) {
            JSONObject country = findCountryByIso3(lastPlansCountryIso3);
            if (country != null) {
                lastPlansCountryName = getCountryDisplayName(country);
            }
        }
    }

    private JSONObject findCountryByIso3(String iso3) {
        if (iso3 == null || iso3.isBlank() || allCountriesData == null) {
            return null;
        }
        for (JSONObject country : allCountriesData) {
            String code = country.optString("cca3", "");
            if (iso3.equalsIgnoreCase(code)) {
                return country;
            }
        }
        return null;
    }

    private String getCountryDisplayName(JSONObject country) {
        if (country == null) {
            return "";
        }
        String langKey = switch (currentLang) {
            case "fr" -> "fra";
            case "en" -> "eng";
            case "de" -> "deu";
            case "es" -> "spa";
            case "zh" -> "zho";
            default -> "eng";
        };
        JSONObject translations = country.optJSONObject("translations");
        if (translations != null) {
            JSONObject tr = translations.optJSONObject(langKey);
            if (tr != null) {
                String common = tr.optString("common", "");
                if (common != null && !common.isBlank()) {
                    return common;
                }
            }
        }
        JSONObject name = country.optJSONObject("name");
        if (name != null) {
            return name.optString("common", "");
        }
        return "";
    }

    private String localizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return t("continent.other");
        }
        String normalized = region.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "africa" -> t("continent.africa");
            case "americas" -> t("continent.americas");
            case "asia" -> t("continent.asia");
            case "europe" -> t("continent.europe");
            case "oceania" -> t("continent.oceania");
            case "antarctic" -> t("continent.antarctic");
            case "other" -> t("continent.other");
            default -> region;
        };
    }

    private void applyDialogTheme(DialogPane pane) {
        if (pane == null) {
            return;
        }
        var css = getClass().getResource("/css/style.css");
        if (css != null) {
            String cssPath = css.toExternalForm();
            if (!pane.getStylesheets().contains(cssPath)) {
                pane.getStylesheets().add(cssPath);
            }
        }
    }

    private void confirmSubscribe(String productId, double amount) {
        if (productId == null || productId.isBlank()) {
            showAlert(t("alert.error"), t("errors.product.invalid"));
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            showAlert(t("alert.error"), t("errors.userId.required"));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("subscribe.confirm.title"));
        alert.setHeaderText(t("subscribe.confirm.header"));
        alert.setContentText(t("subscribe.confirm.body"));
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                subscribeProduct(productId, amount);
            }
        });
    }

    private void subscribeProduct(String productId, double amount) {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle(t("subscribe.loading.title"));
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText(t("subscribe.loading.header"));
        applyDialogTheme(loading.getDialogPane());
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

                            String message = t("subscribe.success") + "\n" +
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
                                showAlert(t("alert.success"), fallback);
                            }
                        } catch (Exception ex) {
                            showAlert(t("alert.success"), t("subscribe.success.unparsed"));
                        }
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        if (response.statusCode() == 409) {
                            showAlert(t("alert.error"), t("errors.conflict"));
                        } else if (response.statusCode() == 401) {
                            showAlert(t("alert.error"), t("errors.token"));
                        } else {
                            showAlert(t("alert.error"), "HTTP " + response.statusCode() + "
" + body);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.close();
                    showAlert(t("alert.error"), e.getMessage());
                });
            }
        }).start();
    }

    private void showActivationPendingAndPoll(String transactionId, String simSerial) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(t("activation.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Label statusLabel = new Label(t("activation.inProgress"));
        VBox content = new VBox(10, new ProgressIndicator(), statusLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content);
        applyDialogTheme(dialog.getDialogPane());

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
                        Platform.runLater(() -> statusLabel.setText(t("activation.fetchingQr")));
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
                                String message = t("activation.success") + "\n" +
                                        "simSerial: " + simSerial + "\n" +
                                        "activationCode: " + activationCode;
                                showSuccessWithQr(message, qrValue, qrDataUrl);
                            } else {
                                showAlert(t("activation.title"), t("activation.qr.missing"));
                            }
                        });
                        return;
                    }
                    if ("ERROR".equals(normalized)) {
                        Platform.runLater(() -> {
                            dialog.close();
                            showAlert(t("alert.error"), t("activation.fail"));
                        });
                        return;
                    }

                    int current = i;
                    Platform.runLater(() -> statusLabel.setText(t("activation.inProgress") + " (" + current + "/" + attempts + ")"));
                    Thread.sleep(delayMs);
                }
                Platform.runLater(() -> {
                    dialog.close();
                    showAlert(t("activation.title"), t("activation.retry"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dialog.close();
                    showAlert(t("alert.error"), e.getMessage());
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
            return t("plan.free");
        }
        if (currency == null || currency.isBlank()) {
            return String.format("%.2f", amount);
        }
        return String.format("%.2f %s", amount, currency);
    }

    private void showSuccessWithQr(String message, String qrCodeValue, String qrCodeDataUrl) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(t("alert.success"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        applyDialogTheme(dialog.getDialogPane());

        Label title = new Label(t("success.title"));
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label(t("success.subtitle"));
        subtitle.getStyleClass().add("dialog-subtitle");

        byte[] qrBytes = decodeQrBytes(qrCodeDataUrl);
        ImageView qrView = buildQrImageView(qrBytes);
        VBox content = new VBox(12, title, subtitle);
        content.getStyleClass().add("dialog-content");

        if (qrView != null) {
            StackPane qrCard = new StackPane(qrView);
            qrCard.getStyleClass().add("qr-card");

            Button download = new Button(t("success.download"));
            download.getStyleClass().addAll("secondary-button", "qr-download");
            download.setOnAction(e -> {
                Window owner = dialog.getDialogPane().getScene() != null
                        ? dialog.getDialogPane().getScene().getWindow()
                        : null;
                saveQrCode(qrBytes, owner);
            });

            VBox qrBox = new VBox(10, qrCard, download);
            qrBox.setAlignment(Pos.CENTER);
            content.getChildren().add(qrBox);
        }
        if (qrCodeValue != null && !qrCodeValue.isBlank()) {
            Label lpa = new Label("LPA: " + qrCodeValue);
            lpa.getStyleClass().add("dialog-lpa");
            Button copy = new Button(t("success.copy"));
            copy.getStyleClass().add("secondary-button");
            copy.setOnAction(e -> copyToClipboard(qrCodeValue));
            content.getChildren().addAll(lpa, copy);
        }

        Label nextSteps = new Label(t("success.next"));
        nextSteps.getStyleClass().add("dialog-hint");
        content.getChildren().add(nextSteps);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private byte[] decodeQrBytes(String dataUrl) {
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
            return java.util.Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ImageView buildQrImageView(byte[] qrBytes) {
        if (qrBytes == null || qrBytes.length == 0) {
            return null;
        }
        try {
            Image img = new Image(new ByteArrayInputStream(qrBytes));
            Image styled = stylizeQrImage(img);
            ImageView view = new ImageView(styled != null ? styled : img);
            view.setFitWidth(180);
            view.setPreserveRatio(true);
            return view;
        } catch (Exception e) {
            return null;
        }
    }

    private Image stylizeQrImage(Image source) {
        if (source == null || source.isError()) {
            return source;
        }
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        if (width <= 0 || height <= 0) {
            return source;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        WritableImage out = new WritableImage(width, height);
        PixelWriter writer = out.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = reader.getColor(x, y);
                double alpha = c.getOpacity();
                double brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                if (alpha < 0.05 || brightness > 0.9) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, Color.WHITE);
                }
            }
        }
        return out;
    }

    private void saveQrCode(byte[] qrBytes, Window owner) {
        if (qrBytes == null || qrBytes.length == 0) {
            showAlert(t("alert.error"), t("errors.qr.download"));
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("success.download"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        chooser.setInitialFileName("esim-qr.png");
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), qrBytes);
            showAlert(t("alert.success"), t("success.download.done"));
        } catch (IOException e) {
            showAlert(t("alert.error"), t("errors.qr.download"));
        }
    }

    private void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        showAlert(t("alert.copied"), t("clipboard.copied"));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    private String formatNetworkError(Exception e) {
        String message = e == null ? "" : e.getMessage();
        if (message == null || message.isBlank()) {
            String type = e == null ? "Exception" : e.getClass().getSimpleName();
            return t("errors.network") + " " + type;
        }
        return t("errors.network") + " " + message;
    }

    private Map<String, Map<String, String>> buildI18n() {
        Map<String, Map<String, String>> dict = new HashMap<>();

        Map<String, String> fr = new HashMap<>();
        fr.put("app.title", "Akuunda Pay - eSIM");
        fr.put("destinations.title", "Destinations");
        fr.put("destinations.subtitle", "Choisissez un pays pour afficher les offres eSIM.");
        fr.put("search.prompt", "Rechercher une destination");
        fr.put("scope.local", "LOCAL");
        fr.put("scope.regional", "REGIONAL");
        fr.put("scope.world", "MONDE");
        fr.put("continent.africa", "Afrique");
        fr.put("continent.americas", "Ameriques");
        fr.put("continent.asia", "Asie");
        fr.put("continent.europe", "Europe");
        fr.put("continent.oceania", "Oceanie");
        fr.put("continent.antarctic", "Antarctique");
        fr.put("continent.other", "Autres");
        fr.put("continent.title", "Continent");
        fr.put("continent.subtitle", "Choisissez un pays en");
        fr.put("cta.viewPlans", "Voir les forfaits");
        fr.put("nav.back", "Retour");
        fr.put("nav.menu", "MENU");
        fr.put("plans.title", "Offres pour");
        fr.put("plans.subtitle", "Forfaits disponibles pour");
        fr.put("plans.none", "Aucun forfait eSIM trouvé pour ce pays.");
        fr.put("errors.catalog.invalid", "Catalogue invalide.");
        fr.put("errors.token", "401 - Token manquant ou invalide.");
        fr.put("errors.catalog", "Erreur catalogue:");
        fr.put("errors.ui", "Erreur UI:");
        fr.put("errors.network", "Erreur réseau :");
        fr.put("errors.conflict", "Une souscription est déjà en cours pour cette SIM. Réessayez dans quelques minutes.");
        fr.put("plan.select", "Sélectionner");
        fr.put("plan.free", "Gratuit");
        fr.put("plan.defaultMeta", "Forfait eSIM");
        fr.put("alert.error", "Erreur");
        fr.put("alert.success", "Succès");
        fr.put("alert.copied", "Copié");
        fr.put("errors.product.invalid", "Produit invalide.");
        fr.put("errors.userId.required", "AKUUNDA_USER_ID est requis pour la souscription.");
        fr.put("subscribe.confirm.title", "Souscription eSIM");
        fr.put("subscribe.confirm.header", "Confirmer la souscription ?");
        fr.put("subscribe.confirm.body", "Vous allez activer un forfait eSIM pour ce pays.");
        fr.put("subscribe.loading.title", "Souscription");
        fr.put("subscribe.loading.header", "Souscription en cours...");
        fr.put("subscribe.success.unparsed", "Souscription OK (réponse non parsable).");
        fr.put("subscribe.success", "Souscription OK");
        fr.put("activation.title", "Activation eSIM");
        fr.put("activation.inProgress", "Activation en cours...");
        fr.put("activation.fetchingQr", "Activation terminée. Récupération du QR...");
        fr.put("activation.qr.missing", "Activation terminée, mais QR non récupéré.");
        fr.put("activation.fail", "Activation eSIM en échec.");
        fr.put("activation.retry", "Activation toujours en cours. Réessayez plus tard.");
        fr.put("activation.success", "Activation OK");
        fr.put("success.title", "Votre eSIM est prête");
        fr.put("success.subtitle", "Scannez le QR code pour installer l'eSIM.");
        fr.put("success.copy", "Copier le code LPA");
        fr.put("success.download", "Telecharger le QR");
        fr.put("success.download.done", "QR telecharge.");
        fr.put("errors.qr.download", "Echec du telechargement du QR.");
        fr.put("success.next", "Ensuite, activez les donnees mobiles sur votre iPhone/Android.");
        fr.put("clipboard.copied", "Le code LPA a été copié.");
        fr.put("greeting.default", "Bonjour");
        fr.put("wallet.prefix", "Solde");
        fr.put("wallet.placeholder", "Solde: --");
        fr.put("wallet.unavailable", "Solde indisponible");        fr.put("home.balance.label", "Votre solde disponible");
        fr.put("home.balance.add", "+ Ajouter de l'argent");
        fr.put("home.action.receive", "Recevoir");
        fr.put("home.action.pay", "Payer");
        fr.put("home.action.withdraw", "Retirer");
        fr.put("home.action.history", "Historique");
        fr.put("home.services", "Services");
        fr.put("home.service.esim", "eSIM");
        fr.put("home.service.send", "Envoyer");
        fr.put("home.service.exchange", "Change devise");
        fr.put("esim.intro.header", "eSIM");
        fr.put("esim.intro.title", "Bienvenue dans la rubrique eSIM Orange");
        fr.put("esim.intro.subtitle", "En cliquant sur l'option ci-dessous :");
        fr.put("esim.intro.card", "Activer votre eSIM Orange  >");
        fr.put("esim.intro.info", "Service uniquement disponible sur les téléphones compatibles eSim. Des frais de paiement de X € seront appliqués.");


        Map<String, String> en = new HashMap<>();
        en.put("app.title", "Akuunda Pay - eSIM");
        en.put("destinations.title", "Destinations");
        en.put("destinations.subtitle", "Pick a country to view available eSIM plans.");
        en.put("search.prompt", "Search a destination");
        en.put("scope.local", "LOCAL");
        en.put("scope.regional", "REGIONAL");
        en.put("scope.world", "WORLD");
        en.put("continent.africa", "Africa");
        en.put("continent.americas", "Americas");
        en.put("continent.asia", "Asia");
        en.put("continent.europe", "Europe");
        en.put("continent.oceania", "Oceania");
        en.put("continent.antarctic", "Antarctic");
        en.put("continent.other", "Other");
        en.put("continent.title", "Continent");
        en.put("continent.subtitle", "Choose a country in");
        en.put("cta.viewPlans", "View plans");
        en.put("nav.back", "Back");
        en.put("nav.menu", "MENU");
        en.put("plans.title", "Plans for");
        en.put("plans.subtitle", "Available plans for");
        en.put("plans.none", "No eSIM plans found for this country.");
        en.put("errors.catalog.invalid", "Invalid catalog.");
        en.put("errors.token", "401 - Missing or invalid token.");
        en.put("errors.catalog", "Catalog error:");
        en.put("errors.ui", "UI error:");
        en.put("errors.network", "Network error:");
        en.put("plan.select", "Select");
        en.put("plan.free", "Free");
        en.put("plan.defaultMeta", "eSIM plan");
        en.put("alert.error", "Error");
        en.put("alert.success", "Success");
        en.put("alert.copied", "Copied");
        en.put("errors.product.invalid", "Invalid product.");
        en.put("errors.userId.required", "AKUUNDA_USER_ID is required to subscribe.");
        en.put("errors.conflict", "A subscription is already pending for this SIM. Please try again in a few minutes.");
        en.put("subscribe.confirm.title", "eSIM subscription");
        en.put("subscribe.confirm.header", "Confirm subscription?");
        en.put("subscribe.confirm.body", "You are about to activate an eSIM plan for this country.");
        en.put("subscribe.loading.title", "Subscription");
        en.put("subscribe.loading.header", "Subscription in progress...");
        en.put("subscribe.success.unparsed", "Subscription OK (unparsable response).");
        en.put("subscribe.success", "Subscription OK");
        en.put("activation.title", "eSIM activation");
        en.put("activation.inProgress", "Activation in progress...");
        en.put("activation.fetchingQr", "Activation complete. Fetching QR...");
        en.put("activation.qr.missing", "Activation complete, but QR could not be retrieved.");
        en.put("activation.fail", "eSIM activation failed.");
        en.put("activation.retry", "Activation still in progress. Please try again later.");
        en.put("activation.success", "Activation OK");
        en.put("success.title", "Your eSIM is ready");
        en.put("success.subtitle", "Scan the QR code to install the eSIM.");
        en.put("success.copy", "Copy LPA code");
        en.put("success.download", "Download QR");
        en.put("success.download.done", "QR downloaded.");
        en.put("errors.qr.download", "Failed to download QR.");
        en.put("success.next", "Then enable cellular data on your iPhone/Android.");
        en.put("clipboard.copied", "The LPA code has been copied.");
        en.put("greeting.default", "Hello");
        en.put("wallet.prefix", "Balance");
        en.put("wallet.placeholder", "Balance: --");
        en.put("wallet.unavailable", "Balance unavailable");        en.put("home.balance.label", "Available balance");
        en.put("home.balance.add", "+ Add money");
        en.put("home.action.receive", "Receive");
        en.put("home.action.pay", "Pay");
        en.put("home.action.withdraw", "Withdraw");
        en.put("home.action.history", "History");
        en.put("home.services", "Services");
        en.put("home.service.esim", "eSIM");
        en.put("home.service.send", "Send");
        en.put("home.service.exchange", "Exchange");
        en.put("esim.intro.header", "eSIM");
        en.put("esim.intro.title", "Welcome to the Orange eSIM section");
        en.put("esim.intro.subtitle", "Click the option below:");
        en.put("esim.intro.card", "Activate your Orange eSIM  >");
        en.put("esim.intro.info", "Service only available on eSIM compatible phones. Payment fees of X € will apply.");


        Map<String, String> de = new HashMap<>();
        de.put("app.title", "Akuunda Pay - eSIM");
        de.put("destinations.title", "Reiseziele");
        de.put("destinations.subtitle", "Wähle ein Land, um verfügbare eSIM-Angebote zu sehen.");
        de.put("search.prompt", "Ziel suchen");
        de.put("scope.local", "LOKAL");
        de.put("scope.regional", "REGIONAL");
        de.put("scope.world", "WELT");
        de.put("continent.africa", "Afrika");
        de.put("continent.americas", "Amerika");
        de.put("continent.asia", "Asien");
        de.put("continent.europe", "Europa");
        de.put("continent.oceania", "Ozeanien");
        de.put("continent.antarctic", "Antarktis");
        de.put("continent.other", "Andere");
        de.put("continent.title", "Kontinent");
        de.put("continent.subtitle", "Wähle ein Land in");
        de.put("cta.viewPlans", "Tarife anzeigen");
        de.put("nav.back", "Zurück");
        de.put("nav.menu", "MENU");
        de.put("plans.title", "Angebote für");
        de.put("plans.subtitle", "Verfügbare Tarife für");
        de.put("plans.none", "Keine eSIM-Tarife für dieses Land gefunden.");
        de.put("errors.catalog.invalid", "Ungültiger Katalog.");
        de.put("errors.token", "401 - Token fehlt oder ist ungültig.");
        de.put("errors.catalog", "Katalogfehler:");
        de.put("errors.ui", "UI-Fehler:");
        de.put("errors.network", "Netzwerkfehler:");
        de.put("plan.select", "Auswählen");
        de.put("plan.free", "Kostenlos");
        de.put("plan.defaultMeta", "eSIM-Tarif");
        de.put("alert.error", "Fehler");
        de.put("alert.success", "Erfolg");
        de.put("alert.copied", "Kopiert");
        de.put("errors.product.invalid", "Ungültiges Produkt.");
        de.put("errors.userId.required", "AKUUNDA_USER_ID ist für die Buchung erforderlich.");
        de.put("errors.conflict", "Eine Anfrage ist bereits für diese SIM ausstehend. Bitte in ein paar Minuten erneut versuchen.");
        de.put("subscribe.confirm.title", "eSIM-Buchung");
        de.put("subscribe.confirm.header", "Buchung bestätigen?");
        de.put("subscribe.confirm.body", "Du bist dabei, einen eSIM-Tarif für dieses Land zu aktivieren.");
        de.put("subscribe.loading.title", "Buchung");
        de.put("subscribe.loading.header", "Buchung läuft...");
        de.put("subscribe.success.unparsed", "Buchung OK (Antwort nicht interpretierbar).");
        de.put("subscribe.success", "Buchung OK");
        de.put("activation.title", "eSIM-Aktivierung");
        de.put("activation.inProgress", "Aktivierung läuft...");
        de.put("activation.fetchingQr", "Aktivierung abgeschlossen. QR wird geladen...");
        de.put("activation.qr.missing", "Aktivierung abgeschlossen, aber QR nicht erhalten.");
        de.put("activation.fail", "eSIM-Aktivierung fehlgeschlagen.");
        de.put("activation.retry", "Aktivierung läuft noch. Bitte später erneut versuchen.");
        de.put("activation.success", "Aktivierung OK");
        de.put("success.title", "Deine eSIM ist bereit");
        de.put("success.subtitle", "Scanne den QR-Code, um die eSIM zu installieren.");
        de.put("success.copy", "LPA-Code kopieren");
        de.put("success.download", "QR herunterladen");
        de.put("success.download.done", "QR heruntergeladen.");
        de.put("errors.qr.download", "QR-Download fehlgeschlagen.");
        de.put("success.next", "Aktiviere anschließend mobile Daten auf deinem iPhone/Android.");
        de.put("clipboard.copied", "Der LPA-Code wurde kopiert.");
        de.put("greeting.default", "Hallo");
        de.put("wallet.prefix", "Guthaben");
        de.put("wallet.placeholder", "Guthaben: --");
        de.put("wallet.unavailable", "Guthaben nicht verfugbar");        de.put("home.balance.label", "Verfugbares Guthaben");
        de.put("home.balance.add", "+ Geld einzahlen");
        de.put("home.action.receive", "Empfangen");
        de.put("home.action.pay", "Bezahlen");
        de.put("home.action.withdraw", "Abheben");
        de.put("home.action.history", "Historie");
        de.put("home.services", "Services");
        de.put("home.service.esim", "eSIM");
        de.put("home.service.send", "Senden");
        de.put("home.service.exchange", "Wechsel");
        de.put("esim.intro.header", "eSIM");
        de.put("esim.intro.title", "Willkommen im Orange eSIM Bereich");
        de.put("esim.intro.subtitle", "Klicken Sie auf die folgende Option:");
        de.put("esim.intro.card", "Orange eSIM aktivieren  >");
        de.put("esim.intro.info", "Dienst nur auf eSIM-fahigen Telefonen verfugbar. Zahlungsgebuhren von X € fallen an.");


        Map<String, String> es = new HashMap<>();
        es.put("app.title", "Akuunda Pay - eSIM");
        es.put("destinations.title", "Destinos");
        es.put("destinations.subtitle", "Elige un país para ver los planes eSIM disponibles.");
        es.put("search.prompt", "Buscar destino");
        es.put("scope.local", "LOCAL");
        es.put("scope.regional", "REGIONAL");
        es.put("scope.world", "MUNDO");
        es.put("continent.africa", "Africa");
        es.put("continent.americas", "America");
        es.put("continent.asia", "Asia");
        es.put("continent.europe", "Europa");
        es.put("continent.oceania", "Oceania");
        es.put("continent.antarctic", "Antartida");
        es.put("continent.other", "Otros");
        es.put("continent.title", "Continente");
        es.put("continent.subtitle", "Elige un país en");
        es.put("cta.viewPlans", "Ver planes");
        es.put("nav.back", "Volver");
        es.put("nav.menu", "MENU");
        es.put("plans.title", "Planes para");
        es.put("plans.subtitle", "Planes disponibles para");
        es.put("plans.none", "No se encontraron planes eSIM para este país.");
        es.put("errors.catalog.invalid", "Catálogo inválido.");
        es.put("errors.token", "401 - Token faltante o inválido.");
        es.put("errors.catalog", "Error de catálogo:");
        es.put("errors.ui", "Error de UI:");
        es.put("errors.network", "Error de red:");
        es.put("plan.select", "Seleccionar");
        es.put("plan.free", "Gratis");
        es.put("plan.defaultMeta", "Plan eSIM");
        es.put("alert.error", "Error");
        es.put("alert.success", "Éxito");
        es.put("alert.copied", "Copiado");
        es.put("errors.product.invalid", "Producto inválido.");
        es.put("errors.userId.required", "AKUUNDA_USER_ID es necesario para la suscripción.");
        es.put("errors.conflict", "Ya hay una suscripción pendiente para esta SIM. Inténtalo de nuevo en unos minutos.");
        es.put("subscribe.confirm.title", "Suscripción eSIM");
        es.put("subscribe.confirm.header", "¿Confirmar suscripción?");
        es.put("subscribe.confirm.body", "Vas a activar un plan eSIM para este país.");
        es.put("subscribe.loading.title", "Suscripción");
        es.put("subscribe.loading.header", "Suscripción en curso...");
        es.put("subscribe.success.unparsed", "Suscripción OK (respuesta no interpretable).");
        es.put("subscribe.success", "Suscripción OK");
        es.put("activation.title", "Activación eSIM");
        es.put("activation.inProgress", "Activación en curso...");
        es.put("activation.fetchingQr", "Activación terminada. Cargando QR...");
        es.put("activation.qr.missing", "Activación terminada, pero no se obtuvo el QR.");
        es.put("activation.fail", "Fallo en la activación eSIM.");
        es.put("activation.retry", "Activación aún en curso. Inténtalo de nuevo más tarde.");
        es.put("activation.success", "Activación OK");
        es.put("success.title", "Tu eSIM está lista");
        es.put("success.subtitle", "Escanea el código QR para instalar la eSIM.");
        es.put("success.copy", "Copiar código LPA");
        es.put("success.download", "Descargar QR");
        es.put("success.download.done", "QR descargado.");
        es.put("errors.qr.download", "Fallo al descargar el QR.");
        es.put("success.next", "Luego activa los datos móviles en tu iPhone/Android.");
        es.put("clipboard.copied", "El código LPA se ha copiado.");
        es.put("greeting.default", "Hola");
        es.put("wallet.prefix", "Saldo");
        es.put("wallet.placeholder", "Saldo: --");
        es.put("wallet.unavailable", "Saldo no disponible");        es.put("home.balance.label", "Saldo disponible");
        es.put("home.balance.add", "+ Agregar dinero");
        es.put("home.action.receive", "Recibir");
        es.put("home.action.pay", "Pagar");
        es.put("home.action.withdraw", "Retirar");
        es.put("home.action.history", "Historial");
        es.put("home.services", "Servicios");
        es.put("home.service.esim", "eSIM");
        es.put("home.service.send", "Enviar");
        es.put("home.service.exchange", "Cambio");
        es.put("esim.intro.header", "eSIM");
        es.put("esim.intro.title", "Bienvenido a la seccion eSIM Orange");
        es.put("esim.intro.subtitle", "Haz clic en la opcion siguiente:");
        es.put("esim.intro.card", "Activar tu eSIM Orange  >");
        es.put("esim.intro.info", "Servicio solo disponible en telefonos compatibles con eSIM. Se aplicaran tarifas de X €.");


        Map<String, String> zh = new HashMap<>();
        zh.put("continent.africa", "非洲");
        zh.put("continent.americas", "美洲");
        zh.put("continent.asia", "亚洲");
        zh.put("continent.europe", "欧洲");
        zh.put("continent.oceania", "大洋洲");
        zh.put("continent.antarctic", "南极洲");
        zh.put("continent.other", "其他");
        zh.put("continent.title", "大洲");
                zh.put("continent.subtitle", "请选择位于");
        zh.put("app.title", "Akuunda Pay - eSIM");
        zh.put("destinations.title", "目的地");
        zh.put("destinations.subtitle", "选择一个国家查看 eSIM 套餐。");
        zh.put("search.prompt", "搜索目的地");
        zh.put("scope.local", "本地");
        zh.put("scope.regional", "区域");
        zh.put("scope.world", "全球");
        zh.put("cta.viewPlans", "查看套餐");
        zh.put("nav.back", "返回");
        zh.put("nav.menu", "菜单");
        zh.put("plans.title", "套餐适用于");
        zh.put("plans.subtitle", "可用套餐：");
        zh.put("plans.none", "该国家暂无 eSIM 套餐。");
        zh.put("errors.catalog.invalid", "套餐目录无效。");
        zh.put("errors.token", "401 - 缺少或无效 token。");
        zh.put("errors.catalog", "套餐目录错误：");
        zh.put("errors.ui", "UI 错误：");
        zh.put("errors.network", "网络错误：");
        zh.put("plan.select", "选择");
        zh.put("plan.free", "免费");
        zh.put("plan.defaultMeta", "eSIM 套餐");
        zh.put("alert.error", "错误");
        zh.put("alert.success", "成功");
        zh.put("alert.copied", "已复制");
        zh.put("errors.product.invalid", "产品无效。");
        zh.put("errors.userId.required", "需要 AKUUNDA_USER_ID 才能订阅。");
        zh.put("errors.conflict", "该 SIM 已有待处理的订阅，请稍后再试。");
        zh.put("subscribe.confirm.title", "eSIM 订阅");
        zh.put("subscribe.confirm.header", "确认订阅？");
        zh.put("subscribe.confirm.body", "您将激活该国家的 eSIM 套餐。");
        zh.put("subscribe.loading.title", "订阅");
        zh.put("subscribe.loading.header", "订阅处理中...");
        zh.put("subscribe.success.unparsed", "订阅成功（响应无法解析）。");
        zh.put("subscribe.success", "订阅成功");
        zh.put("activation.title", "eSIM 激活");
        zh.put("activation.inProgress", "激活中...");
        zh.put("activation.fetchingQr", "激活完成。正在获取 QR...");
        zh.put("activation.qr.missing", "激活完成，但无法获取 QR。");
        zh.put("activation.fail", "eSIM 激活失败。");
        zh.put("activation.retry", "激活仍在进行中。请稍后重试。");
        zh.put("activation.success", "激活成功");
        zh.put("success.title", "您的 eSIM 已准备就绪");
        zh.put("success.subtitle", "扫描 QR 码以安装 eSIM。");
        zh.put("success.copy", "复制 LPA 代码");
        zh.put("success.download", "下载二维码");
        zh.put("success.download.done", "二维码已下载。");
        zh.put("errors.qr.download", "二维码下载失败。");
        zh.put("success.next", "然后在 iPhone/Android 上开启移动数据。");
        zh.put("clipboard.copied", "LPA 代码已复制。");
        zh.put("greeting.default", "你好");
        zh.put("wallet.prefix", "余额");
        zh.put("wallet.placeholder", "余额: --");
        zh.put("wallet.unavailable", "余额不可用");

        dict.put("fr", fr);
        dict.put("en", en);
        dict.put("de", de);
        dict.put("es", es);
                zh.put("home.balance.label", "可用余额");
        zh.put("home.balance.add", "+ 充值");
        zh.put("home.action.receive", "收款");
        zh.put("home.action.pay", "支付");
        zh.put("home.action.withdraw", "提现");
        zh.put("home.action.history", "历史");
        zh.put("home.services", "服务");
        zh.put("home.service.esim", "eSIM");
        zh.put("home.service.send", "转账");
        zh.put("home.service.exchange", "换汇");
        zh.put("esim.intro.header", "eSIM");
        zh.put("esim.intro.title", "欢迎使用 Orange eSIM");
        zh.put("esim.intro.subtitle", "请点击以下选项：");
        zh.put("esim.intro.card", "激活 Orange eSIM  >");
        zh.put("esim.intro.info", "仅适用于支持 eSIM 的手机。将收取 X € 费用。");
        dict.put("zh", zh);
        return dict;
    }

    private String t(String key) {
        Map<String, String> lang = i18n.getOrDefault(currentLang, i18n.get("fr"));
        if (lang == null) {
            return key;
        }
        return lang.getOrDefault(key, key);
    }

    public static void main(String[] args) { launch(); }
}
