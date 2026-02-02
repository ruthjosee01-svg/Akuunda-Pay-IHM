package com.akuunda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
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
    private final String BACKEND_INVENTORY_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_INVENTORY_URL",
            BACKEND_BASE_URL + "/esim/inventory");
    private final String BACKEND_CREDIT_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_CREDIT_URL",
            BACKEND_BASE_URL + "/esim/internet-balance");
    private final String BACKEND_TERMINATE_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_TERMINATE_URL",
            BACKEND_BASE_URL + "/esim/terminate");
    private final String BACKEND_SUSPEND_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_SUSPEND_URL",
            BACKEND_BASE_URL + "/esim/suspend");
    private final String BACKEND_REACTIVATE_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_REACTIVATE_URL",
            BACKEND_BASE_URL + "/esim/reactivate");
    private final String BACKEND_RENEW_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_RENEW_URL",
            BACKEND_BASE_URL + "/esim/renew");
    private final String BACKEND_RENEW_PRODUCT_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_RENEW_PRODUCT_URL",
            BACKEND_BASE_URL + "/esim/renew-product");
    private final String BACKEND_CATALOG_PRODUCT_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_CATALOG_PRODUCT_URL",
            BACKEND_BASE_URL + "/esim/catalog");
    private final String BACKEND_MSISDN_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_MSISDN_URL",
            BACKEND_BASE_URL + "/esim/msisdn");
    private final String BACKEND_ESIM_STATUS_URL = System.getenv().getOrDefault("AKUUNDA_BACKEND_ESIM_STATUS_URL",
            BACKEND_BASE_URL + "/esim/status");
    private final String REALM_NAME = System.getenv().getOrDefault("AKUUNDA_REALM_NAME", "akuunda-realm");
    private final String BACKEND_TOKEN = System.getenv("AKUUNDA_BACKEND_TOKEN");
    private final String BACKEND_USER_ID = System.getenv("AKUUNDA_USER_ID");
    private final String BACKEND_MSISDN = System.getenv("AKUUNDA_MSISDN");
    
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
    private Label greetingLabel;
    private Label activeUserNameLabel;
    private Label activeUserPhoneLabel;
    private String cachedDisplayName = null;
    private String currentLang = "fr";
    private boolean isPlansPage = false;
    private String lastPlansCountryName = null;
    private String lastPlansCountryIso3 = null;
    private Double lastSubscribedAmount = null;
    private String lastSubscribedProductName = null;
    private String lastActiveProductId = null;
    private String lastSubscribedCurrency = null;
    private String cachedMsisdn = null;
    private String cachedSimSerial = null;
    private String cachedSimStatus = null;
    private String cachedUserStatus = null;
    private final Map<String, Map<String, String>> i18n = buildI18n();
    private List<JSONObject> catalogProducts = new ArrayList<>();
    private final List<String> popularIso3 = List.of(
            "FRA", "JPN", "ESP", "DEU", "ITA", "MAR", "CIV", "SEN",
            "BEN", "USA", "GBR", "NLD", "CHN", "BRA"
    );
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

        mainContainer.getChildren().addAll(title, subtitle, card, info);
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

        greetingLabel = new Label(buildGreetingText());
        greetingLabel.getStyleClass().add("greeting-label");
        fetchDisplayNameFromBackend();

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

        HBox bottomNav = buildBottomNav("plans");
        mainContainer.getChildren().addAll(header, backBtn, title, subtitle, greetingLabel, searchField, scopeTabs, scrollPane, nextBtn, bottomNav);
        isPlansPage = false;
        String userStatus = ensureUserStatus();
        boolean allowSubscribe = isSubscribeAllowed(userStatus);
        if (nextBtn != null) {
            boolean countryOk = selectedCountryIso3 != null && !selectedCountryIso3.isBlank();
            nextBtn.setDisable(!countryOk || !allowSubscribe);
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
        Label statusBanner = new Label("");
        statusBanner.getStyleClass().add("status-hint");

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
        HBox bottomNav = buildBottomNav("plans");
        mainContainer.getChildren().addAll(backBtn, title, subtitle, statusBanner, loader, scrollPane, bottomNav);

        String userStatus = ensureUserStatus();
        boolean allowSubscribe = isSubscribeAllowed(userStatus);
        if ("SUSPENDED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.suspended"));
        } else if ("TERMINATED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.terminated"));
        } else if ("NO_SIM".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.nosim"));
        } else if ("IN_PROGRESS".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.inprogress"));
        } else if ("SIM_NOT_READY".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.pending"));
        } else if ("NO_ACTIVE_PLAN".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.noactive"));
        } else {
            statusBanner.setText(t("plans.status.unknown"));
        }

        loadCatalogProducts(products -> {
            mainContainer.getChildren().remove(loader);
            int foundCount = 0;
            for (JSONObject p : products) {
                if (classifyProduct(p) != CoverageType.LOCAL) {
                    continue;
                }
                JSONArray countryList = getProductCountryList(p);
                if (isCountryMatching(iso3, countryList)) {
                    plansListContainer.getChildren().add(createPlanCard(p, allowSubscribe));
                    foundCount++;
                }
            }
            if (foundCount == 0) {
                plansListContainer.getChildren().add(new Label(t("plans.none")));
            }
        }, error -> {
            mainContainer.getChildren().remove(loader);
            plansListContainer.getChildren().add(new Label(error));
        });
    }

    private void showRegionPlansPage(String region) {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = true;
        String localizedRegion = localizeRegion(region);

        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label(t("plans.title") + " " + localizedRegion);
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("plans.subtitle") + " " + localizedRegion + ".");
        subtitle.getStyleClass().add("page-subtitle-dark");
        Label statusBanner = new Label("");
        statusBanner.getStyleClass().add("status-hint");

        VBox plansListContainer = new VBox(15);
        plansListContainer.setPadding(new Insets(10, 5, 10, 5));

        ScrollPane scrollPane = new ScrollPane(plansListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ProgressIndicator loader = new ProgressIndicator();
        loader.getStyleClass().add("loader");
        HBox bottomNav = buildBottomNav("plans");
        mainContainer.getChildren().addAll(backBtn, title, subtitle, statusBanner, loader, scrollPane, bottomNav);

        String userStatus = ensureUserStatus();
        boolean allowSubscribe = isSubscribeAllowed(userStatus);
        if ("SUSPENDED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.suspended"));
        } else if ("TERMINATED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.terminated"));
        } else if ("NO_SIM".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.nosim"));
        } else if ("IN_PROGRESS".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.inprogress"));
        } else if ("SIM_NOT_READY".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.pending"));
        } else if ("NO_ACTIVE_PLAN".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.noactive"));
        } else {
            statusBanner.setText(t("plans.status.unknown"));
        }

        loadCatalogProducts(products -> {
            mainContainer.getChildren().remove(loader);
            int foundCount = 0;
            for (JSONObject p : products) {
                if (classifyProduct(p) != CoverageType.REGIONAL) {
                    continue;
                }
                String productRegion = getProductRegion(p);
                if (productRegion != null && productRegion.equalsIgnoreCase(region)) {
                    plansListContainer.getChildren().add(createPlanCard(p, allowSubscribe));
                    foundCount++;
                }
            }
            if (foundCount == 0) {
                plansListContainer.getChildren().add(new Label(t("plans.none")));
            }
        }, error -> {
            mainContainer.getChildren().remove(loader);
            plansListContainer.getChildren().add(new Label(error));
        });
    }

    private void showWorldPlansPage(String search) {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = true;

        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label(t("plans.title") + " " + t("scope.world"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("plans.subtitle") + " " + t("scope.world") + ".");
        subtitle.getStyleClass().add("page-subtitle-dark");
        Label statusBanner = new Label("");
        statusBanner.getStyleClass().add("status-hint");

        VBox plansListContainer = new VBox(15);
        plansListContainer.setPadding(new Insets(10, 5, 10, 5));

        ScrollPane scrollPane = new ScrollPane(plansListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ProgressIndicator loader = new ProgressIndicator();
        loader.getStyleClass().add("loader");
        HBox bottomNav = buildBottomNav("plans");
        mainContainer.getChildren().addAll(backBtn, title, subtitle, statusBanner, loader, scrollPane, bottomNav);

        String userStatus = ensureUserStatus();
        boolean allowSubscribe = isSubscribeAllowed(userStatus);
        if ("SUSPENDED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.suspended"));
        } else if ("TERMINATED".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.terminated"));
        } else if ("NO_SIM".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.nosim"));
        } else if ("IN_PROGRESS".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.inprogress"));
        } else if ("SIM_NOT_READY".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.pending"));
        } else if ("NO_ACTIVE_PLAN".equalsIgnoreCase(userStatus)) {
            statusBanner.setText(t("plans.status.noactive"));
        } else {
            statusBanner.setText("");
        }

        loadCatalogProducts(products -> {
            mainContainer.getChildren().remove(loader);
            int foundCount = 0;
            String needle = search == null ? "" : search.trim().toLowerCase();
            for (JSONObject p : products) {
                if (classifyProduct(p) != CoverageType.WORLD) {
                    continue;
                }
                if (!needle.isEmpty() && !getProductNameForMatch(p).contains(needle)) {
                    continue;
                }
                plansListContainer.getChildren().add(createPlanCard(p, allowSubscribe));
                foundCount++;
            }
            if (foundCount == 0) {
                plansListContainer.getChildren().add(new Label(t("plans.none")));
            }
        }, error -> {
            mainContainer.getChildren().remove(loader);
            plansListContainer.getChildren().add(new Label(error));
        });
    }

    // --- ÉCRAN 3 : SOLDE INTERNET + RENOUVELLEMENT ---
    private void buildInternetBalanceScreen() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        if (primaryStage != null) {
            primaryStage.setTitle(t("balance.title"));
        }

        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("top-bar-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label topTitle = new Label(t("balance.header"));
        topTitle.getStyleClass().add("top-bar-title");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, leftSpacer, topTitle, rightSpacer);

        Label title = new Label(t("balance.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("balance.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        VBox balanceCard = new VBox(8);
        balanceCard.getStyleClass().add("balance-card");
        balanceCard.setPadding(new Insets(16));

        Label balanceLabel = new Label(t("balance.card.title"));
        balanceLabel.getStyleClass().add("balance-card-title");

        Label statusPill = new Label(t("balance.status.active"));
        statusPill.getStyleClass().add("balance-status");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, balanceLabel, headerSpacer, statusPill);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label balanceValue = new Label(t("balance.loading"));
        balanceValue.getStyleClass().add("balance-amount");

        Label balanceMeta = new Label("");
        balanceMeta.getStyleClass().add("balance-meta");

        Label balanceExpiry = new Label("");
        balanceExpiry.getStyleClass().add("balance-meta");

        balanceCard.getChildren().addAll(headerRow, balanceValue, balanceMeta, balanceExpiry);

        Button renewBtn = new Button(t("balance.renew"));
        renewBtn.setMaxWidth(Double.MAX_VALUE);
        renewBtn.setMinHeight(50);
        renewBtn.getStyleClass().add("primary-button");
        renewBtn.setDisable(true);
        renewBtn.setOnAction(e -> confirmRenew());

        VBox infoBlock = new VBox(6);
        infoBlock.getStyleClass().add("info-block");
        Label infoText = new Label(t("balance.info"));
        infoText.setWrapText(true);
        infoText.getStyleClass().add("info-block-text");
        infoBlock.getChildren().add(infoText);

        VBox noEsimCard = buildNoEsimCard();
        noEsimCard.setVisible(false);
        noEsimCard.setManaged(false);

        HBox bottomNav = buildBottomNav("balance");
        mainContainer.getChildren().addAll(topBar, title, subtitle, balanceCard, noEsimCard, renewBtn, infoBlock, bottomNav);

        fetchInternetBalance(balanceValue, balanceMeta, balanceExpiry, renewBtn, balanceCard, noEsimCard, infoBlock);
    }

    // --- ÉCRAN 4 : FORFAITS ACTIFS ---
    private void buildActiveSubscriptionsScreen() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        if (primaryStage != null) {
            primaryStage.setTitle(t("active.title"));
        }

        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("top-bar-button");
        backBtn.setOnAction(e -> buildEsimIntroScreen());

        Label topTitle = new Label(t("active.header"));
        topTitle.getStyleClass().add("top-bar-title");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, leftSpacer, topTitle, rightSpacer);

        Label title = new Label(t("active.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("active.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        VBox userInfoCard = new VBox(6);
        userInfoCard.getStyleClass().add("user-info-card");
        userInfoCard.setPadding(new Insets(12));

        Label userTitle = new Label(t("active.user.title"));
        userTitle.getStyleClass().add("user-info-title");

        activeUserNameLabel = new Label(getDisplayNameForHeader());
        activeUserNameLabel.getStyleClass().add("user-info-name");

        activeUserPhoneLabel = new Label(t("active.user.phone") + " -");
        activeUserPhoneLabel.getStyleClass().add("user-info-phone");

        userInfoCard.getChildren().addAll(userTitle, activeUserNameLabel, activeUserPhoneLabel);

        VBox list = new VBox(12);
        list.getStyleClass().add("list-container");

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label status = new Label(t("active.loading"));
        status.getStyleClass().add("status-hint");

        Button terminateBtn = new Button(t("terminate.cta"));
        terminateBtn.getStyleClass().add("secondary-button");
        terminateBtn.setDisable(true);
        terminateBtn.setOnAction(e -> confirmTerminate());

        Button suspendBtn = new Button(t("suspend.cta"));
        suspendBtn.getStyleClass().add("secondary-button");
        suspendBtn.setDisable(true);
        suspendBtn.setOnAction(e -> confirmSuspend());

        Button reactivateBtn = new Button(t("reactivate.cta"));
        reactivateBtn.getStyleClass().add("secondary-button");
        reactivateBtn.setVisible(false);
        reactivateBtn.setManaged(false);
        reactivateBtn.setOnAction(e -> {
            String simSerial = ensureSimSerial();
            if (simSerial != null && !simSerial.isBlank()) {
                reactivateSubscriber(simSerial, this::buildActiveSubscriptionsScreen);
            } else {
                showAlert(t("alert.error"), t("reactivate.missingSim"));
            }
        });

        HBox actionRow = new HBox(12, suspendBtn, reactivateBtn, terminateBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(4, 0, 10, 0));

        VBox noEsimCard = buildNoEsimCard();
        noEsimCard.setVisible(false);
        noEsimCard.setManaged(false);

        HBox bottomNav = buildBottomNav("esims");
        mainContainer.getChildren().addAll(topBar, title, subtitle, userInfoCard, status, noEsimCard, scroll,
                actionRow, bottomNav);

        fetchDisplayNameFromBackend();
        fetchActiveSubscriptions(list, status, activeUserPhoneLabel, suspendBtn, reactivateBtn, terminateBtn, scroll, noEsimCard);
    }

    private void buildSimStatusScreen() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        if (primaryStage != null) {
            primaryStage.setTitle(t("sim.status.title"));
        }

        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildEsimIntroScreen());

        Label title = new Label(t("sim.status.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("sim.status.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        VBox card = new VBox(8);
        card.getStyleClass().add("balance-card");
        card.setPadding(new Insets(16));

        Label statusLabel = new Label(t("sim.status.loading"));
        statusLabel.getStyleClass().add("status-hint");

        Label simSerialLabel = new Label(t("sim.status.simSerial") + " -");
        simSerialLabel.getStyleClass().add("status-hint");

        Label msisdnLabel = new Label(t("sim.status.msisdn") + " -");
        msisdnLabel.getStyleClass().add("status-hint");

        Label infoLabel = new Label("");
        infoLabel.getStyleClass().add("status-hint");
        infoLabel.setWrapText(true);

        Button reactivateBtn = new Button(t("reactivate.action"));
        reactivateBtn.getStyleClass().add("primary-button");
        reactivateBtn.setVisible(false);
        reactivateBtn.setManaged(false);

        Button plansBtn = new Button(t("sim.status.viewPlans"));
        plansBtn.getStyleClass().add("secondary-button");
        plansBtn.setOnAction(e -> buildMainSelectionScreen());

        card.getChildren().addAll(statusLabel, simSerialLabel, msisdnLabel, infoLabel, reactivateBtn, plansBtn);

        mainContainer.getChildren().addAll(backBtn, title, subtitle, card);

        new Thread(() -> {
            try {
                String simSerial = ensureSimSerial();
                String msisdn = ensureMsisdn();
                String status = ensureSimStatus();

                String normalized = status == null ? "" : status.trim().toUpperCase();
                boolean hasSim = simSerial != null && !simSerial.isBlank();
                boolean hasMsisdn = msisdn != null && !msisdn.isBlank();

                Platform.runLater(() -> {
                    simSerialLabel.setText(t("sim.status.simSerial") + " " + (hasSim ? simSerial : "-"));
                    msisdnLabel.setText(t("sim.status.msisdn") + " " + (hasMsisdn ? msisdn : "-"));

                    reactivateBtn.setVisible(false);
                    reactivateBtn.setManaged(false);
                    plansBtn.setDisable(false);
                    infoLabel.setText("");

                    if (!hasSim) {
                        statusLabel.setText(t("sim.status.none"));
                        infoLabel.setText(t("sim.status.none.detail"));
                        return;
                    }

                    if ("SUSPENDED".equalsIgnoreCase(normalized)) {
                        statusLabel.setText(t("sim.status.suspended"));
                        infoLabel.setText(t("sim.status.suspended.detail"));
                        plansBtn.setDisable(true);
                        reactivateBtn.setVisible(true);
                        reactivateBtn.setManaged(true);
                        reactivateBtn.setOnAction(e -> reactivateSubscriber(simSerial, this::buildSimStatusScreen));
                        return;
                    }

                    if ("TERMINATED".equalsIgnoreCase(normalized)) {
                        statusLabel.setText(t("sim.status.terminated"));
                        infoLabel.setText(t("sim.status.terminated.detail"));
                        plansBtn.setDisable(true);
                        return;
                    }

                    if (!hasMsisdn || "RESERVED".equalsIgnoreCase(normalized)) {
                        statusLabel.setText(t("sim.status.pending"));
                        infoLabel.setText(t("sim.status.pending.detail"));
                        plansBtn.setDisable(true);
                        return;
                    }

                    if ("USED".equalsIgnoreCase(normalized) || "ACTIVE".equalsIgnoreCase(normalized)) {
                        statusLabel.setText(t("sim.status.active"));
                        infoLabel.setText(t("sim.status.active.detail"));
                        plansBtn.setDisable(false);
                        return;
                    }

                    statusLabel.setText(t("sim.status.unknown"));
                    infoLabel.setText(t("sim.status.unknown.detail"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText(t("errors.network")));
            }
        }).start();
    }

    private void fetchActiveSubscriptions(VBox list, Label msisdnLabel, Label phoneLabel,
                                          Button suspendBtn, Button reactivateBtn, Button terminateBtn,
                                          ScrollPane scroll, VBox noEsimCard) {
        msisdnLabel.setText(t("active.loading"));
        new Thread(() -> {
            try {
                String msisdn = ensureMsisdn();
                if (msisdn == null || msisdn.isBlank()) {
                    String simSerial = ensureSimSerial();
                    Platform.runLater(() -> {
                        msisdnLabel.setText("");
                        phoneLabel.setText(t("active.user.phone") + " -");
                        scroll.setVisible(false);
                        scroll.setManaged(false);
                        noEsimCard.setVisible(true);
                        noEsimCard.setManaged(true);
                        terminateBtn.setDisable(true);
                        suspendBtn.setDisable(true);
                        reactivateBtn.setVisible(false);
                        reactivateBtn.setManaged(false);
                        if (simSerial != null && !simSerial.isBlank()) {
                            msisdnLabel.setText(t("active.pendingMsisdn"));
                            updateInfoCard(noEsimCard, "pendingEsim.title", "pendingEsim.body", false);
                        } else {
                            updateInfoCard(noEsimCard, "noEsim.title", "noEsim.body", true);
                        }
                    });
                    return;
                }
                String simSerial = ensureSimSerial();
                String status = ensureSimStatus();
                Platform.runLater(() -> {
                    phoneLabel.setText(t("active.user.phone") + " " + msisdn);
                    if ("TERMINATED".equalsIgnoreCase(status)) {
                        msisdnLabel.setText(t("active.status.terminated"));
                        scroll.setVisible(false);
                        scroll.setManaged(false);
                        noEsimCard.setVisible(true);
                        noEsimCard.setManaged(true);
                        terminateBtn.setDisable(true);
                        suspendBtn.setDisable(true);
                        reactivateBtn.setVisible(false);
                        reactivateBtn.setManaged(false);
                    } else if ("SUSPENDED".equalsIgnoreCase(status)) {
                        msisdnLabel.setText(t("active.status.suspended"));
                        scroll.setVisible(true);
                        scroll.setManaged(true);
                        noEsimCard.setVisible(false);
                        noEsimCard.setManaged(false);
                        terminateBtn.setDisable(simSerial == null || simSerial.isBlank());
                        suspendBtn.setDisable(true);
                        reactivateBtn.setVisible(true);
                        reactivateBtn.setManaged(true);
                    } else {
                        msisdnLabel.setText(t("active.loading"));
                        scroll.setVisible(true);
                        scroll.setManaged(true);
                        noEsimCard.setVisible(false);
                        noEsimCard.setManaged(false);
                        terminateBtn.setDisable(simSerial == null || simSerial.isBlank());
                        suspendBtn.setDisable(simSerial == null || simSerial.isBlank());
                        reactivateBtn.setVisible(false);
                        reactivateBtn.setManaged(false);
                    }
                });
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                String url = BACKEND_INVENTORY_URL + "?msisdn=" + msisdn + "&statuses=active,readyForUse&withBalances=true";
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET();
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    List<JSONObject> items = parseInventoryItems(response.body());
                    String finalStatus = status;
                    Platform.runLater(() -> {
                        list.getChildren().clear();
                        if (items.isEmpty()) {
                            if ("SUSPENDED".equalsIgnoreCase(finalStatus)) {
                                msisdnLabel.setText(t("active.status.suspended"));
                            } else {
                                msisdnLabel.setText(t("active.empty"));
                            }
                            return;
                        }
                        msisdnLabel.setText(t("active.found") + " " + items.size());
                        for (JSONObject item : items) {
                            list.getChildren().add(createActiveSubscriptionCard(item));
                        }
                    });
                    return;
                }
                if (response.statusCode() == 401) {
                    Platform.runLater(() -> msisdnLabel.setText(t("errors.token")));
                } else {
                    Platform.runLater(() -> msisdnLabel.setText(t("errors.catalog") + " " + response.statusCode()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> msisdnLabel.setText(formatNetworkError(e)));
            }
        }).start();
    }

    private List<JSONObject> parseInventoryItems(String body) {
        List<JSONObject> results = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return results;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    results.add(obj);
                }
            }
            return results;
        }
        JSONObject root = new JSONObject(trimmed);
        JSONArray arr = root.optJSONArray("productSubscriptions");
        if (arr == null) arr = root.optJSONArray("subscriptions");
        if (arr == null) arr = root.optJSONArray("items");
        if (arr == null) arr = root.optJSONArray("data");
        if (arr == null) arr = root.optJSONArray("content");
        if (arr == null) {
            JSONObject single = root.optJSONObject("subscription");
            if (single != null) {
                results.add(single);
            }
            return results;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) {
                results.add(obj);
            }
        }
        return results;
    }

    private String fetchActiveProductIdForMsisdn(String msisdn) throws Exception {
        if (msisdn == null || msisdn.isBlank()) {
            return null;
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String url = BACKEND_INVENTORY_URL + "?msisdn=" + msisdn + "&statuses=active,readyForUse&withBalances=true";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();
        if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
        }
        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
            return null;
        }
        List<JSONObject> items = parseInventoryItems(response.body());
        for (JSONObject item : items) {
            String productId = extractSubscriptionProductId(item);
            if (productId != null && !productId.isBlank()) {
                return productId;
            }
        }
        return null;
    }

    private VBox createActiveSubscriptionCard(JSONObject item) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("active-card");

        String productId = extractSubscriptionProductId(item);
        String status = extractSubscriptionStatus(item);
        String expiration = extractSubscriptionExpiration(item);
        String balance = extractSubscriptionBalance(item);
        String title = productId == null || productId.isBlank()
                ? t("active.unknown")
                : humanizeProductId(productId);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("active-title");

        Label expirationLabel = new Label(expiration == null || expiration.isBlank()
                ? ""
                : t("active.expiration") + " " + expiration);
        expirationLabel.getStyleClass().add("active-meta");

        Button renewBtn = new Button(t("active.renew"));
        renewBtn.getStyleClass().add("primary-button");
        renewBtn.setDisable(true);

        if (productId != null && !productId.isBlank()) {
            renewBtn.setDisable(false);
            renewBtn.setOnAction(e -> confirmRenewProduct(productId, null));
        }

        card.getChildren().addAll(titleLabel, expirationLabel, renewBtn);
        return card;
    }

    private String extractSubscriptionProductId(JSONObject item) {
        if (item == null) {
            return "";
        }
        String productId = item.optString("productId", "");
        if (!productId.isBlank()) return productId;
        productId = item.optString("product_id", "");
        if (!productId.isBlank()) return productId;
        JSONObject product = item.optJSONObject("product");
        if (product != null) {
            productId = product.optString("productId", "");
            if (!productId.isBlank()) return productId;
            productId = product.optString("id", "");
            if (!productId.isBlank()) return productId;
        }
        JSONObject def = item.optJSONObject("productDefinition");
        if (def != null) {
            productId = def.optString("productId", "");
        }
        return productId;
    }

    private String extractSubscriptionStatus(JSONObject item) {
        if (item == null) {
            return "";
        }
        String status = item.optString("status", "");
        if (!status.isBlank()) return status;
        status = item.optString("subscriptionStatus", "");
        if (!status.isBlank()) return status;
        status = item.optString("state", "");
        return status;
    }

    private String extractSubscriptionExpiration(JSONObject item) {
        if (item == null) {
            return "";
        }
        String expiration = item.optString("expirationDate", "");
        if (!expiration.isBlank()) return expiration;
        expiration = item.optString("expiryDate", "");
        if (!expiration.isBlank()) return expiration;
        expiration = item.optString("endDate", "");
        return expiration;
    }

    private String extractSubscriptionBalance(JSONObject item) {
        if (item == null) {
            return "";
        }
        JSONArray balances = null;
        JSONObject balancesObj = item.optJSONObject("balances");
        if (balancesObj != null) {
            balances = balancesObj.optJSONArray("balances");
            if (balances == null) balances = balancesObj.optJSONArray("data");
            if (balances == null) balances = balancesObj.optJSONArray("items");
            if (balances == null) balances = balancesObj.optJSONArray("content");
        }
        if (balances == null) {
            balances = item.optJSONArray("balances");
        }
        if (balances == null) {
            return "";
        }
        double bestValue = -1;
        String bestUnit = "";
        for (int i = 0; i < balances.length(); i++) {
            JSONObject obj = balances.optJSONObject(i);
            if (obj == null) continue;
            double value = obj.optDouble("remainingValue", obj.optDouble("value", obj.optDouble("balance", obj.optDouble("remainingAmount", -1))));
            if (value < 0) continue;
            String unit = obj.optString("resourceUnit", obj.optString("unit", obj.optString("balanceUnit", obj.optString("unitOfMeasure", ""))));
            if (bestValue < 0 || value > bestValue) {
                bestValue = value;
                bestUnit = unit;
            }
        }
        if (bestValue < 0) {
            return "";
        }
        return formatDataAmount(bestValue, bestUnit);
    }

    private String formatDataAmount(double value, String unit) {
        if (unit == null) {
            return String.format(Locale.US, "%.0f", value);
        }
        String u = unit.trim().toUpperCase(Locale.ROOT);
        double v = value;
        if (u.equals("B") || u.equals("BYTE") || u.equals("BYTES")) {
            v = v / 1024.0;
            u = "KB";
        }
        if (u.equals("KB") || u.equals("KBYTE") || u.equals("KBYTES")) {
            if (v >= 1024 * 1024) {
                return String.format(Locale.US, "%.1f GB", v / (1024.0 * 1024.0));
            }
            if (v >= 1024) {
                return String.format(Locale.US, "%.1f MB", v / 1024.0);
            }
            return String.format(Locale.US, "%.0f KB", v);
        }
        if (u.equals("MB") || u.equals("MBYTES")) {
            if (v >= 1024) {
                return String.format(Locale.US, "%.1f GB", v / 1024.0);
            }
            return String.format(Locale.US, "%.1f MB", v);
        }
        if (u.equals("GB") || u.equals("GBYTES")) {
            return String.format(Locale.US, "%.1f GB", v);
        }
        if (u.isBlank()) {
            return String.format(Locale.US, "%.0f", v);
        }
        return String.format(Locale.US, "%.0f %s", v, unit.trim());
    }

    private void fetchProductPrice(String productId, java.util.function.BiConsumer<Double, String> consumer) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                String url = BACKEND_CATALOG_PRODUCT_URL + "/" + productId;
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET();
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    JSONObject obj = new JSONObject(response.body());
                    JSONObject product = obj.has("productDefinition") ? obj : obj.optJSONObject("product");
                    if (product == null) {
                        product = obj;
                    }
                    double amount = extractAmount(product);
                    String currency = extractCurrency(product);
                    consumer.accept(amount, currency);
                    return;
                }
            } catch (Exception ignored) {
            }
            resolvePriceFromCatalog(productId, consumer);
        }).start();
    }

    private void resolvePriceFromCatalog(String productId, java.util.function.BiConsumer<Double, String> consumer) {
        JSONObject product = findProductInCatalog(productId);
        if (product != null) {
            double amount = extractAmount(product);
            String currency = extractCurrency(product);
            consumer.accept(amount, currency);
            return;
        }
        if (catalogProducts != null && !catalogProducts.isEmpty()) {
            consumer.accept(-1.0, "");
            return;
        }
        loadCatalogProducts(products -> {
            JSONObject loaded = findProductInCatalog(productId);
            if (loaded != null) {
                double amount = extractAmount(loaded);
                String currency = extractCurrency(loaded);
                consumer.accept(amount, currency);
                return;
            }
            consumer.accept(-1.0, "");
        }, error -> consumer.accept(-1.0, ""));
    }

    private JSONObject findProductInCatalog(String productId) {
        if (productId == null || productId.isBlank() || catalogProducts == null) {
            return null;
        }
        for (JSONObject p : catalogProducts) {
            if (p == null) {
                continue;
            }
            String id = p.optString("productId", "");
            if (id.isBlank()) {
                JSONObject def = p.optJSONObject("productDefinition");
                if (def != null) {
                    id = def.optString("productId", "");
                }
            }
            if (productId.equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
        logo.setFitWidth(48);
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

    private HBox buildBottomNav(String activeKey) {
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER);
        nav.getStyleClass().add("bottom-nav");

        Button home = new Button(t("nav.home"));
        Button plans = new Button(t("nav.plans"));
        Button balance = new Button(t("nav.balance"));
        Button esims = new Button(t("nav.esims"));

        home.getStyleClass().add("bottom-nav-button");
        plans.getStyleClass().add("bottom-nav-button");
        balance.getStyleClass().add("bottom-nav-button");
        esims.getStyleClass().add("bottom-nav-button");

        if ("home".equals(activeKey)) {
            home.getStyleClass().add("bottom-nav-button-active");
        } else if ("plans".equals(activeKey)) {
            plans.getStyleClass().add("bottom-nav-button-active");
        } else if ("balance".equals(activeKey)) {
            balance.getStyleClass().add("bottom-nav-button-active");
        } else if ("esims".equals(activeKey)) {
            esims.getStyleClass().add("bottom-nav-button-active");
        }

        home.setOnAction(e -> buildEsimIntroScreen());
        plans.setOnAction(e -> buildMainSelectionScreen());
        balance.setOnAction(e -> buildInternetBalanceScreen());
        esims.setOnAction(e -> {
            cachedMsisdn = null;
            cachedSimSerial = null;
            fetchMsisdnFromBackend();
            buildActiveSubscriptionsScreen();
        });

        nav.getChildren().addAll(home, plans, balance, esims);
        return nav;
    }
    private String buildGreetingText() {
        String name = cachedDisplayName;
        if (name == null || name.isBlank()) {
            return t("greeting.default");
        }
        return t("greeting.default") + ", " + name;
    }

    private void updateGreetingLabel() {
        if (greetingLabel == null) {
            return;
        }
        greetingLabel.setText(buildGreetingText());
    }

    private void updateActiveUserHeader() {
        if (activeUserNameLabel == null) {
            return;
        }
        String display = getDisplayNameForHeader();
        activeUserNameLabel.setText(display);
    }

    private String getDisplayNameForHeader() {
        if (cachedDisplayName != null && !cachedDisplayName.isBlank()) {
            return cachedDisplayName;
        }
        String fallback = t("active.user.unknown");
        return fallback == null || fallback.isBlank() ? "Utilisateur" : fallback;
    }

    private void fetchDisplayNameFromBackend() {
        if (cachedDisplayName != null && !cachedDisplayName.isBlank()) {
            Platform.runLater(() -> {
                updateGreetingLabel();
                updateActiveUserHeader();
            });
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            return;
        }
        String url = BACKEND_BASE_URL + "/users/" + REALM_NAME + "/user-id/" + BACKEND_USER_ID;
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
                        String first = data.optString("firstname", "").trim();
                        String last = data.optString("lastname", "").trim();
                        String name = (first + " " + last).trim();
                        if (name.isBlank()) {
                            name = data.optString("username", "").trim();
                        }
                        if (!name.isBlank()) {
                            cachedDisplayName = name;
                            Platform.runLater(() -> {
                                updateGreetingLabel();
                                updateActiveUserHeader();
                            });
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
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

        local.setSelected(true);
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
        if (destinationList == null) {
            return;
        }
        String search = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        String scope = null;
        if (scopeGroup != null && scopeGroup.getSelectedToggle() != null) {
            scope = scopeGroup.getSelectedToggle().getUserData().toString();
        }

        if ("WORLD".equals(scope)) {
            showWorldPlansPage(search);
            return;
        }

        if ("REGIONAL".equals(scope)) {
            updateListView(new ArrayList<>(allCountriesData), scope);
            return;
        }

        if (scope == null) {
            destinationList.getChildren().clear();
            return;
        }

        if ("LOCAL".equals(scope)) {
            renderPopularDestinations(search);
            return;
        }

        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject country : allCountriesData) {
            String name = getCountryDisplayName(country);

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

    private void renderPopularDestinations(String search) {
        destinationList.getChildren().clear();

        Label title = new Label(t("popular.title"));
        title.getStyleClass().add("popular-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        destinationList.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(c1, c2);

        int idx = 0;
        for (String iso3 : popularIso3) {
            JSONObject country = findCountryByIso3(iso3);
            if (country == null) {
                continue;
            }
            String name = getCountryDisplayName(country).toLowerCase();
            if (search != null && !search.isBlank() && !name.contains(search)) {
                continue;
            }
            HBox card = createCountryCard(country);
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.add(card, idx % 2, idx / 2);
            idx++;
        }

        if (idx == 0) {
            destinationList.getChildren().add(new Label(t("popular.none")));
            return;
        }

        destinationList.getChildren().add(grid);

        Button more = new Button(t("popular.more"));
        more.getStyleClass().add("secondary-button");
        more.setMaxWidth(Double.MAX_VALUE);
        more.setOnAction(e -> showAllCountriesPage());
        destinationList.getChildren().add(more);
    }

    private void showAllCountriesPage() {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        Button backBtn = new Button("<- " + t("nav.back"));
        backBtn.getStyleClass().addAll("link-button", "back-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label title = new Label(t("destinations.title"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("destinations.subtitle"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        TextField allSearch = new TextField();
        allSearch.setPromptText(t("search.prompt"));
        allSearch.getStyleClass().add("search-field");

        VBox listContainer = new VBox(12);
        listContainer.setPadding(new Insets(10, 5, 10, 5));

        renderAllCountriesList(listContainer, "");
        allSearch.textProperty().addListener((obs, oldVal, newVal) ->
                renderAllCountriesList(listContainer, newVal));

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mainContainer.getChildren().addAll(backBtn, title, subtitle, allSearch, scrollPane);
        if (primaryStage != null) {
            primaryStage.setTitle(t("destinations.title"));
        }
    }

    private void renderAllCountriesList(VBox listContainer, String search) {
        listContainer.getChildren().clear();
        if (allCountriesData == null || allCountriesData.isEmpty()) {
            return;
        }
        String needle = search == null ? "" : search.trim().toLowerCase();
        List<JSONObject> all = new ArrayList<>(allCountriesData);
        all.sort(Comparator.comparing(c -> getCountryDisplayName(c).toLowerCase()));
        for (JSONObject c : all) {
            String name = getCountryDisplayName(c).toLowerCase();
            if (!needle.isEmpty() && !name.contains(needle)) {
                continue;
            }
            listContainer.getChildren().add(createCountryCard(c));
        }
    }

    private enum CoverageType {
        LOCAL,
        REGIONAL,
        WORLD,
        UNKNOWN
    }

    private void renderWorldPlansInList(String search) {}

    private void loadCatalogProducts(Consumer<List<JSONObject>> onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
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
                        if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                            JSONObject catalog = new JSONObject(response.body());
                            JSONArray products = catalog.optJSONArray("products");
                            if (products == null) {
                                onError.accept(t("errors.catalog.invalid"));
                                return;
                            }
                            List<JSONObject> list = new ArrayList<>();
                            for (int i = 0; i < products.length(); i++) {
                                list.add(products.getJSONObject(i));
                            }
                            catalogProducts = list;
                            onSuccess.accept(list);
                            return;
                        }

                        String body = response.body() == null ? "" : response.body().strip();
                        if (body.length() > 200) {
                            body = body.substring(0, 200) + "...";
                        }
                        if (response.statusCode() == 401) {
                            onError.accept(t("errors.token"));
                        } else {
                            String msg = t("errors.catalog") + " " + response.statusCode();
                            if (!body.isEmpty()) {
                                msg += "\n" + body;
                            }
                            onError.accept(msg);
                        }
                    } catch (Exception ex) {
                        onError.accept(t("errors.ui") + " " + ex.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(formatNetworkError(e)));
            }
        }).start();
    }

    private JSONArray getProductCountryList(JSONObject product) {
        if (product == null) {
            return null;
        }
        JSONObject def = product.optJSONObject("productDefinition");
        return def == null ? null : def.optJSONArray("countryList");
    }

    private CoverageType classifyProduct(JSONObject product) {
        if (isWorldByName(product)) {
            return CoverageType.WORLD;
        }
        if (isRegionalByName(product)) {
            return CoverageType.REGIONAL;
        }

        JSONArray countryList = getProductCountryList(product);
        if (countryList == null || countryList.length() == 0) {
            return CoverageType.UNKNOWN;
        }
        if (countryList.length() == 1) {
            return CoverageType.LOCAL;
        }

        List<String> regions = new ArrayList<>();
        for (int i = 0; i < countryList.length(); i++) {
            String iso3 = countryList.optString(i, "");
            if (iso3 == null || iso3.isBlank()) {
                continue;
            }
            String region = getRegionForIso3(iso3);
            regions.add(region == null || region.isBlank() ? "Other" : region);
        }

        if (isWorldByCoverage(product, regions, countryList.length())) {
            return CoverageType.WORLD;
        }

        String region = regions.isEmpty() ? null : regions.get(0);
        boolean sameRegion = true;
        for (String r : regions) {
            if (region == null || !region.equalsIgnoreCase(r)) {
                sameRegion = false;
                break;
            }
        }
        return sameRegion ? CoverageType.REGIONAL : CoverageType.UNKNOWN;
    }

    private boolean isWorldByName(JSONObject product) {
        String name = getProductNameForMatch(product).toUpperCase(Locale.ROOT);
        String productId = getProductIdForMatch(product).toUpperCase(Locale.ROOT);
        return name.contains("WORLD")
                || name.contains("GLOBAL")
                || productId.contains("WORLD")
                || productId.contains("GLOBAL");
    }

    private boolean isWorldByCoverage(JSONObject product, List<String> regions, int countryCount) {
        if (isWorldByName(product)) {
            return true;
        }
        if (countryCount >= 120) {
            return true;
        }
        HashSet<String> major = new HashSet<>();
        for (String r : regions) {
            if ("Africa".equalsIgnoreCase(r)
                    || "Americas".equalsIgnoreCase(r)
                    || "Asia".equalsIgnoreCase(r)
                    || "Europe".equalsIgnoreCase(r)
                    || "Oceania".equalsIgnoreCase(r)) {
                major.add(r.toLowerCase(Locale.ROOT));
            }
        }
        return major.size() >= 5;
    }

    private String getProductRegion(JSONObject product) {
        String regionByName = getRegionFromName(product);
        if (regionByName != null) {
            return regionByName;
        }

        JSONArray countryList = getProductCountryList(product);
        if (countryList == null || countryList.length() == 0) {
            return null;
        }
        String region = null;
        for (int i = 0; i < countryList.length(); i++) {
            String iso3 = countryList.optString(i, "");
            if (iso3 == null || iso3.isBlank()) {
                continue;
            }
            String current = getRegionForIso3(iso3);
            if (current == null || current.isBlank()) {
                current = "Other";
            }
            if (region == null) {
                region = current;
            } else if (!region.equalsIgnoreCase(current)) {
                return null;
            }
        }
        return region;
    }

    private String getRegionForIso3(String iso3) {
        if (iso3 == null || iso3.isBlank() || allCountriesData == null) {
            return null;
        }
        for (JSONObject country : allCountriesData) {
            String code = country.optString("cca3", "");
            if (iso3.equalsIgnoreCase(code)) {
                return country.optString("region", null);
            }
        }
        return null;
    }

    private String getProductNameForMatch(JSONObject product) {
        if (product == null) {
            return "";
        }
        String name = "";
        JSONObject display = product.optJSONObject("display");
        if (display != null) {
            name = display.optString("name", "");
        }
        if (name == null || name.isBlank()) {
            JSONObject def = product.optJSONObject("productDefinition");
            if (def != null) {
                name = def.optString("productId", "");
            }
        }
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private ImageView buildContinentIcon(String region) {
        String path = switch (region) {
            case "Africa" -> "/images/continents/551221-carte-detaillee-du-continent-africain-en-silhouette-noire-gratuit-vectoriel.jpg";
            case "Americas" -> "/images/continents/60039370-silhouette-continent-américain-isolé-vecteur-illustration.jpg";
            case "Asia" -> "/images/continents/asie.png";
            case "Europe" -> "/images/continents/22176832-l-europe-continent-carte-noir-carte-de-l-europe-vectoriel.jpg";
            case "Oceania" -> "/images/continents/oceanie.jpg";
            case "Antarctic" -> "/images/continents/antartique.jpg";
            default -> null;
        };
        if (path == null) {
            return null;
        }
        try {
            Image raw = new Image(getClass().getResourceAsStream(path));
            Image orange = tintSilhouette(raw, Color.web("#F88809"));
            ImageView view = new ImageView(orange != null ? orange : raw);
            view.getStyleClass().add("continent-icon-image");
            view.setFitWidth(28);
            view.setFitHeight(28);
            view.setPreserveRatio(true);
            return view;
        } catch (Exception e) {
            return null;
        }
    }

    private Image tintSilhouette(Image source, Color tint) {
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
                    writer.setColor(x, y, new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 1.0));
                }
            }
        }
        return out;
    }

    private String getProductIdForMatch(JSONObject product) {
        if (product == null) {
            return "";
        }
        JSONObject def = product.optJSONObject("productDefinition");
        String id = def != null ? def.optString("productId", "") : "";
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private String getRegionFromName(JSONObject product) {
        String name = getProductNameForMatch(product).toUpperCase(Locale.ROOT);
        String productId = getProductIdForMatch(product).toUpperCase(Locale.ROOT);
        String text = name + " " + productId;
        if (text.contains("AFRICA")) {
            return "Africa";
        }
        if (text.contains("EUROPE")) {
            return "Europe";
        }
        if (text.contains("ASIA")) {
            return "Asia";
        }
        if (text.contains("AMERICA") || text.contains("AMERICAS")) {
            return "Americas";
        }
        if (text.contains("OCEANIA")) {
            return "Oceania";
        }
        return null;
    }

    private boolean isRegionalByName(JSONObject product) {
        return getRegionFromName(product) != null;
    }

    private void fetchInternetBalance(Label balanceValue, Label balanceMeta, Label balanceExpiry, Button renewBtn, VBox balanceCard, VBox noEsimCard, VBox infoBlock) {
        new Thread(() -> {
            try {
                String msisdn = ensureMsisdn();
                if (msisdn == null || msisdn.isBlank()) {
                    String simSerial = ensureSimSerial();
                    Platform.runLater(() -> {
                        balanceCard.setVisible(false);
                        balanceCard.setManaged(false);
                        infoBlock.setVisible(false);
                        infoBlock.setManaged(false);
                        noEsimCard.setVisible(true);
                        noEsimCard.setManaged(true);
                        renewBtn.setDisable(true);
                        if (simSerial != null && !simSerial.isBlank()) {
                            updateInfoCard(noEsimCard, "pendingEsim.title", "pendingEsim.body", false);
                        } else {
                            updateInfoCard(noEsimCard, "noEsim.title", "noEsim.body", true);
                        }
                    });
                    return;
                }
                Platform.runLater(() -> {
                    balanceCard.setVisible(true);
                    balanceCard.setManaged(true);
                    infoBlock.setVisible(true);
                    infoBlock.setManaged(true);
                    noEsimCard.setVisible(false);
                    noEsimCard.setManaged(false);
                });
                try {
                    String productId = fetchActiveProductIdForMsisdn(msisdn);
                    if (productId != null && !productId.isBlank()) {
                        lastActiveProductId = productId;
                        Platform.runLater(() -> {
                            renewBtn.setDisable(false);
                            renewBtn.setOnAction(e -> confirmRenewProduct(productId, null));
                        });
                    }
                } catch (Exception ignored) {
                }
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_CREDIT_URL + "?msisdn=" + msisdn))
                        .header("Accept", "application/json")
                        .GET();
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    JSONObject body = new JSONObject(response.body());
                    JSONObject credit = body.optJSONObject("credit");
                    if (credit != null) {
                        String currency = credit.optString("currency", "");
                        String unit = credit.optString("unit", "");
                        double amount = credit.optDouble("amount", -1);
                        String expiration = credit.optString("expirationDate", "");
                        String formatted = formatCreditAmount(amount, unit, currency);
                        Platform.runLater(() -> {
                            balanceValue.setText(formatted);
                            balanceMeta.setText(t("balance.meta.unit") + " " + unit);
                            if (expiration != null && !expiration.isBlank()) {
                                balanceExpiry.setText(t("balance.meta.expiration") + " " + expiration);
                            }
                            renewBtn.setDisable(false);
                        });
                        return;
                    }
                }
                Platform.runLater(() -> {
                    balanceValue.setText(t("balance.unavailable"));
                    if (lastActiveProductId == null || lastActiveProductId.isBlank()) {
                        renewBtn.setDisable(true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    balanceValue.setText(t("balance.unavailable"));
                    if (lastActiveProductId == null || lastActiveProductId.isBlank()) {
                        renewBtn.setDisable(true);
                    }
                });
            }
        }).start();
    }

    private VBox buildNoEsimCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("balance-card");
        card.setPadding(new Insets(16));

        Label title = new Label(t("noEsim.title"));
        title.getStyleClass().add("balance-card-title");

        Label body = new Label(t("noEsim.body"));
        body.getStyleClass().add("balance-meta");
        body.setWrapText(true);

        Button cta = new Button(t("noEsim.cta"));
        cta.getStyleClass().add("primary-button");
        cta.setOnAction(e -> buildMainSelectionScreen());

        card.getChildren().addAll(title, body, cta);
        return card;
    }

    private void updateInfoCard(VBox card, String titleKey, String bodyKey, boolean showCta) {
        if (card == null || card.getChildren().size() < 2) {
            return;
        }
        Node titleNode = card.getChildren().get(0);
        if (titleNode instanceof Label title) {
            title.setText(t(titleKey));
        }
        Node bodyNode = card.getChildren().get(1);
        if (bodyNode instanceof Label body) {
            body.setText(t(bodyKey));
        }
        if (card.getChildren().size() > 2) {
            Node ctaNode = card.getChildren().get(2);
            ctaNode.setVisible(showCta);
            ctaNode.setManaged(showCta);
        }
    }

    private void confirmRenew() {
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            showAlert(t("alert.error"), t("errors.userId.required"));
            return;
        }
        if (lastSubscribedAmount == null || lastSubscribedAmount <= 0) {
            showAlert(t("alert.error"), t("errors.renew.amount"));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("renew.confirm.title"));
        alert.setHeaderText(t("renew.confirm.header"));
        alert.setContentText(t("renew.confirm.body"));
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                renewSubscription();
            }
        });
    }

    private void confirmRenewProduct(String productId, Double amount) {
        if (productId == null || productId.isBlank()) {
            showAlert(t("alert.error"), t("errors.product.invalid"));
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            showAlert(t("alert.error"), t("errors.userId.required"));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("renew.confirm.title"));
        alert.setHeaderText(t("renew.confirm.header"));
        alert.setContentText(t("renew.confirm.body"));
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                renewProduct(productId, amount);
            }
        });
    }

    private void confirmTerminate() {
        String simSerial = ensureSimSerial();
        if (simSerial == null || simSerial.isBlank()) {
            showAlert(t("alert.error"), t("terminate.sim.missing"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(t("terminate.title"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyDialogTheme(dialog.getDialogPane());

        Label header = new Label(t("terminate.header"));
        header.getStyleClass().add("dialog-title");
        Label warning = new Label(t("terminate.warning"));
        warning.setWrapText(true);
        warning.getStyleClass().add("dialog-subtitle");

        Label simLabel = new Label(t("terminate.sim.label") + " " + simSerial);
        simLabel.getStyleClass().add("dialog-hint");

        CheckBox confirm = new CheckBox(t("terminate.confirm"));
        confirm.getStyleClass().add("dialog-checkbox");

        VBox content = new VBox(10, header, warning, simLabel, confirm);
        content.getStyleClass().add("dialog-content");
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(t("terminate.cta.confirm"));
        okButton.setDisable(true);
        confirm.selectedProperty().addListener((obs, oldVal, newVal) -> okButton.setDisable(!newVal));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                performTerminate(simSerial);
            }
        });
    }

    private void performTerminate(String simSerial) {
        Dialog<Void> loading = showLoading(t("terminate.loading.title"), t("terminate.loading"));
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                String url = BACKEND_TERMINATE_URL + "/" + simSerial;
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"));
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    loading.close();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        cachedMsisdn = null;
                        cachedSimSerial = null;
                        cachedSimStatus = "TERMINATED";
                        showAlert(t("alert.success"), t("terminate.success"));
                        buildActiveSubscriptionsScreen();
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else {
                        showAlert(t("alert.error"), t("terminate.failed") + " " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.close();
                    showAlert(t("alert.error"), formatNetworkError(e));
                });
            }
        }).start();
    }

    private void confirmSuspend() {
        String simSerial = ensureSimSerial();
        if (simSerial == null || simSerial.isBlank()) {
            showAlert(t("alert.error"), t("terminate.sim.missing"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(t("suspend.title"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyDialogTheme(dialog.getDialogPane());

        Label header = new Label(t("suspend.header"));
        header.getStyleClass().add("dialog-title");
        Label warning = new Label(t("suspend.warning"));
        warning.setWrapText(true);
        warning.getStyleClass().add("dialog-subtitle");

        Label simLabel = new Label(t("terminate.sim.label") + " " + simSerial);
        simLabel.getStyleClass().add("dialog-hint");

        CheckBox confirm = new CheckBox(t("suspend.confirm"));
        confirm.getStyleClass().add("dialog-checkbox");

        VBox content = new VBox(10, header, warning, simLabel, confirm);
        content.getStyleClass().add("dialog-content");
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(t("suspend.cta.confirm"));
        okButton.setDisable(true);
        confirm.selectedProperty().addListener((obs, oldVal, newVal) -> okButton.setDisable(!newVal));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                performSuspend(simSerial);
            }
        });
    }

    private void performSuspend(String simSerial) {
        Dialog<Void> loading = showLoading(t("suspend.loading.title"), t("suspend.loading"));
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                String url = BACKEND_SUSPEND_URL + "/" + simSerial;
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"));
                if (BACKEND_TOKEN != null && !BACKEND_TOKEN.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + BACKEND_TOKEN.trim());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    loading.close();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        cachedSimStatus = "SUSPENDED";
                        showAlert(t("alert.success"), t("suspend.success"));
                        buildActiveSubscriptionsScreen();
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else {
                        showAlert(t("alert.error"), t("suspend.failed") + " " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.close();
                    showAlert(t("alert.error"), formatNetworkError(e));
                });
            }
        }).start();
    }

    private void renewSubscription() {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle(t("renew.loading.title"));
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText(t("renew.loading.header"));
        applyDialogTheme(loading.getDialogPane());
        loading.show();

        new Thread(() -> {
            try {
                String msisdn = ensureMsisdn();
                if (msisdn == null || msisdn.isBlank()) {
                    Platform.runLater(() -> {
                        loading.close();
                        showAlert(t("alert.error"), t("errors.msisdn.required"));
                    });
                    return;
                }
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                JSONObject payload = new JSONObject();
                payload.put("userId", BACKEND_USER_ID);
                payload.put("msisdn", msisdn);
                payload.put("amount", lastSubscribedAmount == null ? 0 : lastSubscribedAmount);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_RENEW_URL))
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
                        showAlert(t("alert.success"), t("renew.success"));
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 400 && isInsufficientFund(response.body())) {
                        showAlert(t("alert.error"), t("errors.renew.insufficientFunds"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        showAlert(t("alert.error"), "HTTP " + response.statusCode() + "\n" + body);
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

    private void renewProduct(String productId, Double amount) {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle(t("renew.loading.title"));
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText(t("renew.loading.header"));
        applyDialogTheme(loading.getDialogPane());
        loading.show();

        new Thread(() -> {
            try {
                String msisdn = ensureMsisdn();
                if (msisdn == null || msisdn.isBlank()) {
                    Platform.runLater(() -> {
                        loading.close();
                        showAlert(t("alert.error"), t("errors.msisdn.required"));
                    });
                    return;
                }
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                JSONObject payload = new JSONObject();
                payload.put("userId", BACKEND_USER_ID);
                payload.put("productId", productId);
                payload.put("msisdn", msisdn);
                if (amount != null) {
                    payload.put("amount", amount);
                }

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_RENEW_PRODUCT_URL))
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
                        showAlert(t("alert.success"), t("renew.success"));
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 400 && isInsufficientFund(response.body())) {
                        showAlert(t("alert.error"), t("errors.renew.insufficientFunds"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        showAlert(t("alert.error"), "HTTP " + response.statusCode() + "\n" + body);
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

    private String resolveMsisdn() {
        if (cachedMsisdn != null && !cachedMsisdn.isBlank()) {
            return cachedMsisdn;
        }
        if (BACKEND_MSISDN != null && !BACKEND_MSISDN.isBlank()) {
            cachedMsisdn = BACKEND_MSISDN.replaceAll("\\s", "");
            return cachedMsisdn;
        }
        return "";
    }

    private String resolveSimSerial() {
        if (cachedSimSerial != null && !cachedSimSerial.isBlank()) {
            return cachedSimSerial;
        }
        return "";
    }

    private String resolveSimStatus() {
        if (cachedSimStatus != null && !cachedSimStatus.isBlank()) {
            return cachedSimStatus;
        }
        return "";
    }

    private String resolveUserStatus() {
        if (cachedUserStatus != null && !cachedUserStatus.isBlank()) {
            return cachedUserStatus;
        }
        return "";
    }

    private String ensureMsisdn() {
        String msisdn = resolveMsisdn();
        if (msisdn != null && !msisdn.isBlank()) {
            return msisdn;
        }
        String fetched = fetchMsisdnFromBackend();
        if (fetched != null && !fetched.isBlank()) {
            cachedMsisdn = fetched;
            return fetched;
        }
        return "";
    }

    private String ensureSimSerial() {
        String simSerial = resolveSimSerial();
        if (simSerial != null && !simSerial.isBlank()) {
            return simSerial;
        }
        fetchLineStatusFromBackend();
        fetchMsisdnFromBackend();
        return resolveSimSerial();
    }

    private String ensureSimStatus() {
        String status = resolveSimStatus();
        if (status != null && !status.isBlank()) {
            return status;
        }
        fetchLineStatusFromBackend();
        fetchMsisdnFromBackend();
        return resolveSimStatus();
    }

    private String ensureUserStatus() {
        String status = resolveUserStatus();
        if (status != null && !status.isBlank()) {
            return status;
        }
        fetchLineStatusFromBackend();
        return resolveUserStatus();
    }

    private String fetchMsisdnFromBackend() {
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            return "";
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String url = BACKEND_MSISDN_URL + "/" + BACKEND_USER_ID;
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
                String msisdn = body.optString("msisdn", "");
                String simSerial = body.optString("simSerial", "");
                String status = body.optString("status", "");
                if (simSerial != null && !simSerial.isBlank()) {
                    cachedSimSerial = simSerial.replaceAll("\\s", "");
                }
                if (status != null && !status.isBlank()) {
                    cachedSimStatus = status.trim();
                }
                if (msisdn != null && !msisdn.isBlank()) {
                    return msisdn.replaceAll("\\s", "");
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private void fetchLineStatusFromBackend() {
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String url = BACKEND_ESIM_STATUS_URL + "/" + BACKEND_USER_ID;
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
                String userStatus = body.optString("userStatus", "");
                String subscriberStatus = body.optString("subscriberStatus", "");
                String localStatus = body.optString("localStatus", "");
                String simSerial = body.optString("simSerial", "");
                String msisdn = body.optString("msisdn", "");

                if (userStatus != null && !userStatus.isBlank()) {
                    cachedUserStatus = userStatus.trim();
                }
                if (subscriberStatus != null && !subscriberStatus.isBlank()) {
                    cachedSimStatus = subscriberStatus.trim();
                } else if (localStatus != null && !localStatus.isBlank()) {
                    cachedSimStatus = localStatus.trim();
                }
                if (simSerial != null && !simSerial.isBlank()) {
                    cachedSimSerial = simSerial.replaceAll("\\s", "");
                }
                if (msisdn != null && !msisdn.isBlank()) {
                    cachedMsisdn = msisdn.replaceAll("\\s", "");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isSubscribeAllowed(String userStatus) {
        if (userStatus == null) {
            return true;
        }
        if (userStatus.isBlank()) {
            return true;
        }
        if ("SUSPENDED".equalsIgnoreCase(userStatus)) {
            return false;
        }
        if ("TERMINATED".equalsIgnoreCase(userStatus)) {
            return false;
        }
        return true;
    }

    private String formatCreditAmount(double amount, String unit, String currency) {
        if (amount < 0) {
            return t("balance.unavailable");
        }
        double value = amount;
        if ("CENTS".equalsIgnoreCase(unit)) {
            value = amount / 100.0;
        }
        String symbol = currency == null || currency.isBlank() ? "" : (" " + currency);
        return String.format("%.2f%s", value, symbol);
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

    private VBox createPlanCard(JSONObject product, boolean allowSubscribe) {
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
        boolean priceKnown = amount >= 0;
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
        b.setOnAction(e -> confirmSubscribe(productId, amount, currency, name));
        if (!allowSubscribe) {
            b.setDisable(true);
            b.setText(t("plan.unavailable"));
        } else if (!priceKnown) {
            b.setDisable(true);
            b.setText(t("plan.priceUnavailable"));
        }

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

        if ("REGIONAL".equals(scope)) {
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

        ImageView icon = buildContinentIcon(region);
        Label name = new Label(localizeRegion(region));
        name.getStyleClass().add("continent-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(String.valueOf(countries.size()));
        count.getStyleClass().add("continent-count");
        Label arrow = new Label("> ");
        arrow.getStyleClass().add("continent-arrow");

        if (icon != null) {
            card.getChildren().add(icon);
        }
        card.getChildren().addAll(name, spacer, count, arrow);
        card.setOnMouseClicked(e -> showRegionPlansPage(region));
        return card;
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

    private Dialog<Void> showLoading(String title, String header) {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle(title);
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText(header);
        applyDialogTheme(loading.getDialogPane());
        loading.show();
        return loading;
    }

    private void confirmSubscribe(String productId, double amount, String currency, String displayName) {
        if (productId == null || productId.isBlank()) {
            showAlert(t("alert.error"), t("errors.product.invalid"));
            return;
        }
        if (BACKEND_USER_ID == null || BACKEND_USER_ID.isBlank()) {
            showAlert(t("alert.error"), t("errors.userId.required"));
            return;
        }
        if (amount < 0) {
            showAlert(t("alert.error"), t("errors.price.missing"));
            return;
        }

        String status = ensureSimStatus();
        if ("TERMINATED".equalsIgnoreCase(status)) {
            showAlert(t("alert.error"), t("errors.subscriber.terminated"));
            return;
        }
        if ("SUSPENDED".equalsIgnoreCase(status)) {
            showReactivatePrompt(productId, amount, currency, displayName);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("subscribe.confirm.title"));
        alert.setHeaderText(t("subscribe.confirm.header"));
        String simSerial = resolveSimSerial();
        alert.setContentText(simSerial == null || simSerial.isBlank()
                ? t("subscribe.confirm.body.newsim")
                : t("subscribe.confirm.body"));
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                subscribeProduct(productId, amount, currency, displayName);
            }
        });
    }

    private void subscribeProduct(String productId, double amount, String currency, String displayName) {
        Dialog<Void> loading = new Dialog<>();
        loading.setTitle(t("subscribe.loading.title"));
        loading.getDialogPane().setContent(new ProgressIndicator());
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        loading.setHeaderText(t("subscribe.loading.header"));
        applyDialogTheme(loading.getDialogPane());
        loading.show();

        new Thread(() -> {
            try {
                String msisdn = resolveMsisdn();
                if (msisdn == null || msisdn.isBlank()) {
                    msisdn = fetchMsisdnFromBackend();
                }
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                JSONObject payload = new JSONObject();
                payload.put("productId", productId);
                payload.put("userId", BACKEND_USER_ID);
                payload.put("amount", amount);
                if (msisdn != null && !msisdn.isBlank()) {
                    payload.put("msisdn", msisdn);
                }

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
                            String activationMessage = body.optString("activationMessage", "");
                            String serial = body.optString("simSerial", "");
                            String qrCodeValue = body.optString("qrCodeValue", "");
                            String qrCodeDataUrl = body.optString("qrCodeDataUrl", "");

                            if ((activationCode != null && !activationCode.isBlank())
                                    || (qrCodeValue != null && !qrCodeValue.isBlank())
                                    || (qrCodeDataUrl != null && !qrCodeDataUrl.isBlank())) {
                                cachedMsisdn = null;
                                cachedSimSerial = null;
                                fetchMsisdnFromBackend();
                                buildSubscriptionSuccessScreen(displayName, amount, currency, subscriptionId, serial,
                                        activationCode, qrCodeValue, qrCodeDataUrl);
                            } else if (activationRequired && transactionId != null && !transactionId.isBlank()) {
                                cachedMsisdn = null;
                                cachedSimSerial = null;
                                fetchMsisdnFromBackend();
                                showActivationPendingAndPoll(transactionId, serial);
                            } else if (activationRequired && serial != null && !serial.isBlank()) {
                                cachedMsisdn = null;
                                cachedSimSerial = null;
                                fetchMsisdnFromBackend();
                                showActivationPendingAndPollBySimSerial(serial);
                            } else if (activationRequired && "SIM_NOT_READY".equalsIgnoreCase(activationMessage)) {
                                showAlert(t("activation.title"), t("activation.retry"));
                            } else {
                                cachedMsisdn = null;
                                cachedSimSerial = null;
                                fetchMsisdnFromBackend();
                                buildSubscriptionSuccessScreen(displayName, amount, currency, subscriptionId, serial,
                                        activationCode, "", "");
                            }
                            lastSubscribedAmount = amount;
                            lastSubscribedProductName = displayName != null && !displayName.isBlank() ? displayName : productId;
                            lastSubscribedCurrency = currency;
                        } catch (Exception ex) {
                            showAlert(t("alert.success"), t("subscribe.success.unparsed"));
                        }
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        if (response.statusCode() == 409) {
                            showAlert(t("alert.error"), t("errors.conflict"));
                        } else if (response.statusCode() == 400 && isSimNotReadyForActivation(body)) {
                            showRetryActivationPrompt(productId, amount, currency, displayName);
                        } else if (response.statusCode() == 400 && isUserNotFound(body)) {
                            showAlert(t("alert.error"), t("errors.user.notFound"));
                        } else if (response.statusCode() == 400 && isMsisdnMissing(body)) {
                            showAlert(t("alert.error"), t("errors.msisdn.missing"));
                        } else if (response.statusCode() == 400 && isNoEsim(body)) {
                            showAlert(t("alert.error"), t("errors.noEsim"));
                        } else if (response.statusCode() == 400 && isInsufficientFund(body)) {
                            showAlert(t("alert.error"), t("errors.subscribe.insufficientFunds"));
                        } else if (response.statusCode() == 400 && isSubscriberTerminated(body)) {
                            showAlert(t("alert.error"), t("errors.subscriber.terminated"));
                        } else if (response.statusCode() == 400 && (isSubscriberSuspended(body) || isSubscriberNotEligible(body))) {
                            showReactivatePrompt(productId, amount, currency, displayName);
                        } else if (response.statusCode() == 401) {
                            showAlert(t("alert.error"), t("errors.token"));
                        } else {
                            showAlert(t("alert.error"), "HTTP " + response.statusCode() + "\n" + body);
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

    private boolean isSubscriberNotEligible(String body) {
        return body != null && body.contains("SUBSCRIBER_STATUS_NOT_ELIGIBLE");
    }

    private boolean isSimNotReadyForActivation(String body) {
        return body != null && body.toLowerCase().contains("not ready for activation");
    }

    private boolean isInsufficientFund(String body) {
        return body != null && body.contains("INSUFFICIENT_FUND");
    }

    private boolean isUserNotFound(String body) {
        if (body == null) {
            return false;
        }
        return body.contains("Utilisateur introuvable") || body.toLowerCase().contains("user not found");
    }

    private boolean isMsisdnMissing(String body) {
        if (body == null) {
            return false;
        }
        String lowered = body.toLowerCase();
        return lowered.contains("msisdn") && lowered.contains("requis");
    }

    private boolean isNoEsim(String body) {
        if (body == null) {
            return false;
        }
        return body.contains("Aucune eSIM associée")
                || body.contains("Aucune eSIM associee")
                || body.contains("Aucun simSerial");
    }

    private void showRetryActivationPrompt(String productId, double amount, String currency, String displayName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("alert.error"));
        alert.setHeaderText(null);
        alert.setContentText(t("errors.sim.notReady"));
        ButtonType retryBtn = new ButtonType(t("action.retry"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(t("reactivate.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retryBtn, cancelBtn);
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == retryBtn) {
                subscribeProduct(productId, amount, currency, displayName);
            }
        });
    }

    private boolean isSubscriberSuspended(String body) {
        return body != null && body.contains("SUBSCRIBER_SUSPENDED");
    }

    private boolean isSubscriberTerminated(String body) {
        return body != null && body.contains("SUBSCRIBER_TERMINATED");
    }

    private void showReactivatePrompt(String productId, double amount, String currency, String displayName) {
        String simSerial = ensureSimSerial();
        if (simSerial == null || simSerial.isBlank()) {
            showAlert(t("alert.error"), t("reactivate.missingSim"));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("reactivate.title"));
        alert.setHeaderText(t("reactivate.header"));
        alert.setContentText(t("reactivate.body"));
        ButtonType reactivateBtn = new ButtonType(t("reactivate.action"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(t("reactivate.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(reactivateBtn, cancelBtn);
        applyDialogTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == reactivateBtn) {
                reactivateSubscriber(simSerial, () -> subscribeProduct(productId, amount, currency, displayName));
            }
        });
    }

    private void reactivateSubscriber(String simSerial, Runnable onSuccess) {
        Dialog<Void> loading = showLoading(t("reactivate.loading.title"), t("reactivate.loading.header"));
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                JSONObject payload = new JSONObject();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_REACTIVATE_URL + "/" + simSerial))
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
                        cachedSimStatus = "USED";
                        showAlert(t("alert.success"), t("reactivate.success"));
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else if (response.statusCode() == 409) {
                        showAlert(t("alert.error"), t("errors.conflict"));
                    } else if (response.statusCode() == 401) {
                        showAlert(t("alert.error"), t("errors.token"));
                    } else {
                        String body = response.body() == null ? "" : response.body();
                        showAlert(t("alert.error"), "HTTP " + response.statusCode() + "\n" + body);
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
                                buildSubscriptionSuccessScreen(lastSubscribedProductName,
                                        lastSubscribedAmount == null ? 0 : lastSubscribedAmount,
                                        lastSubscribedCurrency,
                                        "",
                                        simSerial,
                                        activationCode,
                                        qrValue,
                                        qrDataUrl);
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

    private void showActivationPendingAndPollBySimSerial(String simSerial) {
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
                    JSONObject esim = fetchEsimDetails(client, simSerial);
                    if (esim != null) {
                        String activationCode = esim.optString("activationCode", "");
                        String qrValue = "";
                        String qrDataUrl = "";
                        JSONObject qrCode = esim.optJSONObject("qrCode");
                        if (qrCode != null) {
                            qrValue = qrCode.optString("value", "");
                            qrDataUrl = qrCode.optString("dataUrl", "");
                        }
                        if ((activationCode != null && !activationCode.isBlank())
                                || (qrValue != null && !qrValue.isBlank())
                                || (qrDataUrl != null && !qrDataUrl.isBlank())) {
                            final String activationCodeFinal = activationCode;
                            final String qrValueFinal = qrValue;
                            final String qrDataUrlFinal = qrDataUrl;
                            Platform.runLater(() -> {
                                dialog.close();
                                buildSubscriptionSuccessScreen(lastSubscribedProductName,
                                        lastSubscribedAmount == null ? 0 : lastSubscribedAmount,
                                        lastSubscribedCurrency,
                                        "",
                                        simSerial,
                                        activationCodeFinal,
                                        qrValueFinal,
                                        qrDataUrlFinal);
                            });
                            return;
                        }
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
            return -1.0;
        }
        JSONArray subscriptionFee = prices.optJSONArray("subscriptionFee");
        if (subscriptionFee == null || subscriptionFee.length() == 0) {
            return -1.0;
        }
        JSONArray tier = subscriptionFee.optJSONArray(0);
        if (tier == null || tier.length() == 0) {
            return -1.0;
        }
        JSONObject priceItem = tier.optJSONObject(0);
        if (priceItem == null) {
            return -1.0;
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
        if (amount < 0) {
            return t("active.price.unavailable");
        }
        if (amount == 0) {
            return t("plan.free");
        }
        if (currency == null || currency.isBlank()) {
            return String.format("%.2f", amount);
        }
        return String.format("%.2f %s", amount, currency);
    }

    private void buildSubscriptionSuccessScreen(String productName,
                                                double amount,
                                                String currency,
                                                String subscriptionId,
                                                String simSerial,
                                                String activationCode,
                                                String qrCodeValue,
                                                String qrCodeDataUrl) {
        mainContainer.getChildren().clear();
        mainContainer.getStyleClass().setAll("screen-light");
        isPlansPage = false;

        if (primaryStage != null) {
            primaryStage.setTitle(t("success.title"));
        }

        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("top-bar-button");
        backBtn.setOnAction(e -> buildMainSelectionScreen());

        Label topTitle = new Label(t("success.title"));
        topTitle.getStyleClass().add("top-bar-title");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, leftSpacer, topTitle, rightSpacer);

        Label title = new Label(t("success.header"));
        title.getStyleClass().add("page-title-dark");
        Label subtitle = new Label(t("success.subtitle.long"));
        subtitle.getStyleClass().add("page-subtitle-dark");

        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("success-card");

        Label planName = new Label(productName == null || productName.isBlank() ? t("success.plan.unknown") : productName);
        planName.getStyleClass().add("success-plan-name");

        String priceText = amount < 0 ? t("active.price.unavailable") : formatPrice(amount, currency);
        Label price = new Label(priceText);
        price.getStyleClass().add("success-price");

        VBox meta = new VBox(4);
        meta.getStyleClass().add("success-meta");
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            meta.getChildren().add(new Label("subscriptionId: " + subscriptionId));
        }
        if (simSerial != null && !simSerial.isBlank()) {
            meta.getChildren().add(new Label("simSerial: " + simSerial));
        }
        if (activationCode != null && !activationCode.isBlank()) {
            meta.getChildren().add(new Label("activationCode: " + activationCode));
        }

        card.getChildren().addAll(planName, price, meta);

        byte[] qrBytes = decodeQrBytes(qrCodeDataUrl);
        ImageView qrView = buildQrImageView(qrBytes);
        VBox qrBox = new VBox(8);
        qrBox.setAlignment(Pos.CENTER);
        if (qrView != null) {
            StackPane qrCard = new StackPane(qrView);
            qrCard.getStyleClass().add("qr-card");
            Button download = new Button(t("success.download"));
            download.getStyleClass().addAll("secondary-button", "qr-download");
            download.setOnAction(e -> {
                Window owner = mainContainer.getScene() != null ? mainContainer.getScene().getWindow() : null;
                saveQrCode(qrBytes, owner);
            });
            qrBox.getChildren().addAll(qrCard, download);
        }
        if (qrCodeValue != null && !qrCodeValue.isBlank()) {
            Label lpa = new Label("LPA: " + qrCodeValue);
            lpa.getStyleClass().add("dialog-lpa");
            Button copy = new Button(t("success.copy"));
            copy.getStyleClass().add("secondary-button");
            copy.setOnAction(e -> copyToClipboard(qrCodeValue));
            qrBox.getChildren().addAll(lpa, copy);
        }

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        Button myEsim = new Button(t("success.myEsim"));
        myEsim.getStyleClass().add("secondary-button");
        myEsim.setOnAction(e -> {
            cachedMsisdn = null;
            cachedSimSerial = null;
            fetchMsisdnFromBackend();
            buildActiveSubscriptionsScreen();
        });
        Button home = new Button(t("success.home"));
        home.getStyleClass().add("primary-button");
        home.setOnAction(e -> buildMainSelectionScreen());
        actions.getChildren().addAll(myEsim, home);

        HBox bottomNav = buildBottomNav("esims");
        mainContainer.getChildren().addAll(topBar, title, subtitle, card, qrBox, actions, bottomNav);
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
        fr.put("nav.home", "Accueil");
        fr.put("nav.plans", "Forfaits");
        fr.put("nav.balance", "Solde");
        fr.put("nav.esims", "Mes eSIM");
        fr.put("plans.title", "Offres pour");
        fr.put("plans.subtitle", "Forfaits disponibles pour");
        fr.put("plans.none", "Aucun forfait eSIM trouvé pour ce pays.");
        fr.put("plans.status.suspended", "eSIM suspendue : réactivez-la pour souscrire à un forfait.");
        fr.put("plans.status.terminated", "eSIM résiliée : vous ne pouvez plus souscrire.");
        fr.put("plans.status.nosim", "Aucune eSIM : vous pouvez souscrire pour en créer une.");
        fr.put("plans.status.inprogress", "Forfait en cours : vous pouvez souscrire.");
        fr.put("plans.status.pending", "eSIM en cours d'activation : souscription indisponible.");
        fr.put("plans.status.noactive", "Aucun forfait actif : souscription disponible.");
        fr.put("plans.status.unknown", "Statut eSIM inconnu : accès aux forfaits maintenu.");
        fr.put("plan.unavailable", "Indisponible");
        fr.put("popular.title", "Destinations populaires");
        fr.put("popular.none", "Aucune destination populaire trouvée.");
        fr.put("popular.more", "Plus de destinations");
        fr.put("errors.catalog.invalid", "Catalogue invalide.");
        fr.put("errors.token", "401 - Token manquant ou invalide.");
        fr.put("errors.catalog", "Erreur catalogue:");
        fr.put("errors.ui", "Erreur UI:");
        fr.put("errors.network", "Erreur réseau :");
        fr.put("errors.conflict", "Une opération est déjà en cours sur votre eSIM. Veuillez patienter 1 à 2 minutes puis réessayer.");
        fr.put("plan.select", "Sélectionner");
        fr.put("plan.free", "Gratuit");
        fr.put("plan.priceUnavailable", "Prix indisponible");
        fr.put("plan.defaultMeta", "Forfait eSIM");
        fr.put("alert.error", "Erreur");
        fr.put("alert.success", "Succès");
        fr.put("alert.copied", "Copié");
        fr.put("action.retry", "Réessayer");
        fr.put("errors.product.invalid", "Produit invalide.");
        fr.put("errors.userId.required", "AKUUNDA_USER_ID est requis pour la souscription.");
        fr.put("errors.price.missing", "Prix indisponible pour ce forfait.");
        fr.put("errors.user.notFound", "Utilisateur introuvable.");
        fr.put("errors.msisdn.missing", "MSISDN requis pour souscrire a un forfait.");
        fr.put("errors.noEsim", "Aucune eSIM associee a ce compte.");
        fr.put("errors.subscribe.insufficientFunds", "Souscription impossible : solde insuffisant.");
        fr.put("subscribe.confirm.title", "Souscription eSIM");
        fr.put("subscribe.confirm.header", "Confirmer la souscription ?");
        fr.put("subscribe.confirm.body", "Vous allez activer un forfait eSIM pour ce pays.");
        fr.put("subscribe.confirm.body.newsim", "Aucune eSIM n’est encore associée à ce compte. Une nouvelle eSIM sera attribuée après la souscription.");
        fr.put("subscribe.loading.title", "Souscription");
        fr.put("subscribe.loading.header", "Souscription en cours...");
        fr.put("subscribe.success.unparsed", "Souscription OK (réponse non parsable).");
        fr.put("subscribe.success", "Souscription OK");
        fr.put("reactivate.title", "Réactiver l’eSIM");
        fr.put("reactivate.header", "Votre eSIM est suspendue");
        fr.put("reactivate.body", "Souhaitez-vous réactiver votre eSIM pour continuer la souscription ?");
        fr.put("reactivate.action", "Réactiver");
        fr.put("reactivate.cta", "Réactiver l’eSIM");
        fr.put("reactivate.cancel", "Annuler");
        fr.put("reactivate.loading.title", "Réactivation");
        fr.put("reactivate.loading.header", "Réactivation en cours...");
        fr.put("reactivate.success", "eSIM réactivée. Vous pouvez relancer la souscription.");
        fr.put("reactivate.missingSim", "Aucune eSIM associée à ce compte. Veuillez choisir un forfait pour en obtenir une.");
        fr.put("activation.title", "Activation eSIM");
        fr.put("activation.inProgress", "Activation en cours...");
        fr.put("activation.fetchingQr", "Activation terminée. Récupération du QR...");
        fr.put("activation.qr.missing", "Activation terminée, mais QR non récupéré.");
        fr.put("activation.fail", "Activation eSIM en échec.");
        fr.put("activation.retry", "Activation toujours en cours. Réessayez plus tard.");
        fr.put("activation.success", "Activation OK");
        fr.put("success.title", "Votre eSIM est prête");
        fr.put("success.header", "Souscription réussie");
        fr.put("success.subtitle.long", "Votre forfait est activé. Scannez le QR pour installer l'eSIM.");
        fr.put("success.plan.unknown", "Forfait eSIM");
        fr.put("success.subtitle", "Scannez le QR code pour installer l'eSIM.");
        fr.put("success.copy", "Copier le code LPA");
        fr.put("success.download", "Telecharger le QR");
        fr.put("success.download.done", "QR telecharge.");
        fr.put("errors.qr.download", "Echec du telechargement du QR.");
        fr.put("success.next", "Ensuite, activez les donnees mobiles sur votre iPhone/Android.");
        fr.put("success.myEsim", "Voir mes eSIM");
        fr.put("success.home", "Retour à l'accueil");
        fr.put("clipboard.copied", "Le code LPA a été copié.");
        fr.put("greeting.default", "Bonjour");
        fr.put("wallet.prefix", "Solde");
        fr.put("wallet.placeholder", "Solde: --");
        fr.put("wallet.unavailable", "Solde indisponible");
        fr.put("home.balance.label", "Votre solde disponible");
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
        fr.put("esim.intro.title", "Bienvenue dans la rubrique eSIM");
        fr.put("esim.intro.subtitle", "En cliquant sur l'option ci-dessous :");
        fr.put("esim.intro.card", "Activer votre eSIM  >");
        fr.put("esim.intro.myEsim", "Voir mes eSIM");
        fr.put("esim.intro.info", "Service uniquement disponible sur les téléphones compatibles eSim. Des frais de paiement de X € seront appliqués.");
        fr.put("active.header", "Mes eSIM");
        fr.put("active.title", "Forfaits actifs");
        fr.put("active.subtitle", "Consultez vos forfaits actifs et renouvelez-les.");
        fr.put("active.loading", "Chargement des forfaits actifs...");
        fr.put("active.empty", "Aucun forfait actif pour le moment.");
        fr.put("active.found", "Forfaits actifs :");
        fr.put("active.price.loading", "Prix en cours...");
        fr.put("active.price.unavailable", "Prix indisponible");
        fr.put("active.balance.label", "Solde restant :");
        fr.put("active.balance.unavailable", "Solde indisponible");
        fr.put("active.renew", "Renouveler");
        fr.put("active.status.unknown", "Statut inconnu");
        fr.put("active.status.label", "Statut :");
        fr.put("active.status.suspended", "Votre eSIM est suspendue. Réactivez-la pour reprendre.");
        fr.put("active.status.terminated", "eSIM résiliée. Vous ne pouvez plus souscrire.");
        fr.put("active.pendingMsisdn", "Activation en cours : numéro non encore attribué.");
        fr.put("active.expiration", "Expiration :");
        fr.put("active.msisdn", "Ma SIM :");
        fr.put("active.unknown", "Forfait eSIM");
        fr.put("active.user.title", "Utilisateur");
        fr.put("active.user.phone", "Ligne :");
        fr.put("active.user.unknown", "Utilisateur");
        fr.put("noEsim.title", "Vous n'avez pas encore d'eSIM");
        fr.put("noEsim.body", "Choisissez un forfait pour créer et activer votre eSIM.");
        fr.put("noEsim.cta", "Voir les forfaits");
        fr.put("pendingEsim.title", "eSIM en cours d'activation");
        fr.put("pendingEsim.body", "Votre eSIM est en cours d'activation. Réessayez dans quelques minutes.");
        fr.put("sim.status.cta", "Etat de la SIM");
        fr.put("sim.status.title", "Etat de la SIM");
        fr.put("sim.status.subtitle", "Consultez l'etat de votre eSIM.");
        fr.put("sim.status.loading", "Chargement de l'etat...");
        fr.put("sim.status.simSerial", "SIM :");
        fr.put("sim.status.msisdn", "MSISDN :");
        fr.put("sim.status.active", "eSIM active");
        fr.put("sim.status.active.detail", "Votre eSIM est active. Vous pouvez souscrire a des forfaits.");
        fr.put("sim.status.pending", "Activation en cours");
        fr.put("sim.status.pending.detail", "Votre eSIM est en cours d'activation. Patientez quelques minutes.");
        fr.put("sim.status.suspended", "eSIM suspendue");
        fr.put("sim.status.suspended.detail", "Votre eSIM est suspendue. Reactivez-la pour souscrire a un forfait.");
        fr.put("sim.status.terminated", "eSIM resiliee");
        fr.put("sim.status.terminated.detail", "Votre eSIM est resiliee. Contactez le support pour une nouvelle eSIM.");
        fr.put("sim.status.none", "Aucune eSIM");
        fr.put("sim.status.none.detail", "Vous n'avez pas encore d'eSIM. Choisissez un forfait pour en creer une.");
        fr.put("sim.status.unknown", "Etat inconnu");
        fr.put("sim.status.unknown.detail", "Impossible de recuperer l'etat. Reessayez plus tard.");
        fr.put("sim.status.viewPlans", "Voir les forfaits");
        fr.put("suspend.hint", "Suspendre temporairement l’eSIM.");
        fr.put("suspend.cta", "Suspendre l’eSIM");
        fr.put("suspend.title", "Suspension eSIM");
        fr.put("suspend.header", "Suspendre cette eSIM ?");
        fr.put("suspend.warning", "La SIM sera suspendue temporairement. Vous pourrez la réactiver plus tard.");
        fr.put("suspend.confirm", "Je comprends et je souhaite suspendre.");
        fr.put("suspend.cta.confirm", "Suspendre");
        fr.put("suspend.loading.title", "Suspension");
        fr.put("suspend.loading", "Suspension en cours...");
        fr.put("suspend.success", "Suspension acceptée.");
        fr.put("suspend.failed", "Suspension impossible.");
        fr.put("terminate.hint", "Supprimer l'eSIM pour ce compte.");
        fr.put("terminate.cta", "Supprimer l'eSIM");
        fr.put("terminate.title", "Résiliation eSIM");
        fr.put("terminate.header", "Supprimer cette eSIM ?");
        fr.put("terminate.warning", "Cette action résilie l'abonné. La SIM ne pourra plus accéder au réseau.");
        fr.put("terminate.confirm", "Je comprends et je souhaite résilier.");
        fr.put("terminate.sim.label", "SIM :");
        fr.put("terminate.sim.missing", "SIM inconnue.");
        fr.put("terminate.cta.confirm", "Résilier");
        fr.put("terminate.loading.title", "Résiliation");
        fr.put("terminate.loading", "Résiliation en cours...");
        fr.put("terminate.success", "Résiliation acceptée.");
        fr.put("terminate.failed", "Résiliation impossible.");
        fr.put("balance.cta", "Solde internet");
        fr.put("balance.header", "Solde internet");
        fr.put("balance.title", "Solde internet");
        fr.put("balance.subtitle", "Consultez votre crédit et renouvelez votre forfait.");
        fr.put("balance.card.title", "Votre solde Internet");
        fr.put("balance.status.active", "Actif");
        fr.put("balance.loading", "Chargement...");
        fr.put("balance.meta.unit", "Unité:");
        fr.put("balance.meta.expiration", "Expiration:");
        fr.put("balance.unavailable", "Solde indisponible");
        fr.put("balance.info", "Lorsque votre solde atteint 0, vous pouvez renouveler le forfait depuis cette page.");
        fr.put("balance.renew", "Renouveler le forfait");
        fr.put("errors.msisdn.required", "Aucune eSIM associée à ce compte. Souscrivez pour en obtenir une.");
        fr.put("errors.subscriber.terminated", "eSIM résiliée : vous ne pouvez plus souscrire.");
        fr.put("errors.sim.notReady", "Votre eSIM n’est pas encore prête. Réessayez dans quelques minutes.");
        fr.put("errors.renew.amount", "Montant du dernier forfait inconnu. Souscrivez d'abord à un forfait.");
        fr.put("errors.renew.insufficientFunds", "Renouvellement impossible : solde insuffisant.");
        fr.put("errors.subscribe.insufficientFunds", "Souscription impossible : solde insuffisant.");
        fr.put("renew.confirm.title", "Renouvellement");
        fr.put("renew.confirm.header", "Confirmer le renouvellement ?");
        fr.put("renew.confirm.body", "Vous allez renouveler votre dernier forfait.");
        fr.put("renew.loading.title", "Renouvellement");
        fr.put("renew.loading.header", "Renouvellement en cours...");
        fr.put("renew.success", "Renouvellement OK");


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
        en.put("nav.home", "Home");
        en.put("nav.plans", "Plans");
        en.put("nav.balance", "Balance");
        en.put("nav.esims", "My eSIMs");
        en.put("plans.title", "Plans for");
        en.put("plans.subtitle", "Available plans for");
        en.put("plans.none", "No eSIM plans found for this country.");
        en.put("plans.status.suspended", "eSIM suspended: reactivate it to subscribe.");
        en.put("plans.status.terminated", "eSIM terminated: you can no longer subscribe.");
        en.put("plans.status.nosim", "No eSIM: you can subscribe to create one.");
        en.put("plans.status.inprogress", "Plan in progress: you can subscribe.");
        en.put("plans.status.pending", "eSIM activation in progress: subscription unavailable.");
        en.put("plans.status.noactive", "No active plan: subscription available.");
        en.put("plans.status.unknown", "Unknown eSIM status: access to plans is allowed.");
        en.put("plan.unavailable", "Unavailable");
        en.put("popular.title", "Popular destinations");
        en.put("popular.none", "No popular destinations found.");
        en.put("popular.more", "More destinations");
        en.put("errors.catalog.invalid", "Invalid catalog.");
        en.put("errors.token", "401 - Missing or invalid token.");
        en.put("errors.catalog", "Catalog error:");
        en.put("errors.ui", "UI error:");
        en.put("errors.network", "Network error:");
        en.put("plan.select", "Select");
        en.put("plan.free", "Free");
        en.put("plan.priceUnavailable", "Price unavailable");
        en.put("plan.defaultMeta", "eSIM plan");
        en.put("alert.error", "Error");
        en.put("alert.success", "Success");
        en.put("alert.copied", "Copied");
        en.put("action.retry", "Retry");
        en.put("errors.product.invalid", "Invalid product.");
        en.put("errors.userId.required", "AKUUNDA_USER_ID is required to subscribe.");
        en.put("errors.price.missing", "Price unavailable for this plan.");
        en.put("errors.user.notFound", "User not found.");
        en.put("errors.msisdn.missing", "MSISDN is required to subscribe.");
        en.put("errors.noEsim", "No eSIM associated with this account.");
        en.put("errors.subscribe.insufficientFunds", "Subscription failed: insufficient balance.");
        en.put("errors.conflict", "An operation is already in progress on your eSIM. Please wait 1 to 2 minutes, then try again.");
        en.put("subscribe.confirm.title", "eSIM subscription");
        en.put("subscribe.confirm.header", "Confirm subscription?");
        en.put("subscribe.confirm.body", "You are about to activate an eSIM plan for this country.");
        en.put("subscribe.confirm.body.newsim", "No eSIM is associated with this account yet. A new eSIM will be assigned after subscription.");
        en.put("subscribe.loading.title", "Subscription");
        en.put("subscribe.loading.header", "Subscription in progress...");
        en.put("subscribe.success.unparsed", "Subscription OK (unparsable response).");
        en.put("subscribe.success", "Subscription OK");
        en.put("reactivate.title", "Reactivate eSIM");
        en.put("reactivate.header", "Your eSIM is suspended");
        en.put("reactivate.body", "Do you want to reactivate your eSIM to continue the subscription?");
        en.put("reactivate.action", "Reactivate");
        en.put("reactivate.cta", "Reactivate eSIM");
        en.put("reactivate.cancel", "Cancel");
        en.put("reactivate.loading.title", "Reactivation");
        en.put("reactivate.loading.header", "Reactivation in progress...");
        en.put("reactivate.success", "eSIM reactivated. You can retry the subscription.");
        en.put("reactivate.missingSim", "No eSIM associated with this account. Please choose a plan to get one.");
        en.put("activation.title", "eSIM activation");
        en.put("activation.inProgress", "Activation in progress...");
        en.put("activation.fetchingQr", "Activation complete. Fetching QR...");
        en.put("activation.qr.missing", "Activation complete, but QR could not be retrieved.");
        en.put("activation.fail", "eSIM activation failed.");
        en.put("activation.retry", "Activation still in progress. Please try again later.");
        en.put("activation.success", "Activation OK");
        en.put("success.title", "Your eSIM is ready");
        en.put("success.header", "Subscription successful");
        en.put("success.subtitle.long", "Your plan is active. Scan the QR to install the eSIM.");
        en.put("success.plan.unknown", "eSIM plan");
        en.put("success.subtitle", "Scan the QR code to install the eSIM.");
        en.put("success.copy", "Copy LPA code");
        en.put("success.download", "Download QR");
        en.put("success.download.done", "QR downloaded.");
        en.put("errors.qr.download", "Failed to download QR.");
        en.put("success.next", "Then enable cellular data on your iPhone/Android.");
        en.put("success.myEsim", "My eSIMs");
        en.put("success.home", "Back to home");
        en.put("clipboard.copied", "The LPA code has been copied.");
        en.put("greeting.default", "Hello");
        en.put("wallet.prefix", "Balance");
        en.put("wallet.placeholder", "Balance: --");
        en.put("wallet.unavailable", "Balance unavailable");
        en.put("home.balance.label", "Available balance");
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
        en.put("esim.intro.title", "Welcome to the eSIM section");
        en.put("esim.intro.subtitle", "Click the option below:");
        en.put("esim.intro.card", "Activate your eSIM  >");
        en.put("esim.intro.myEsim", "My eSIMs");
        en.put("esim.intro.info", "Service only available on eSIM compatible phones. Payment fees of X € will apply.");
        en.put("active.header", "My eSIMs");
        en.put("active.title", "Active plans");
        en.put("active.subtitle", "Check your active plans and renew them.");
        en.put("active.user.title", "Subscriber");
        en.put("active.user.phone", "Line:");
        en.put("active.user.unknown", "Subscriber");
        en.put("active.loading", "Loading active plans...");
        en.put("active.empty", "No active plan for now.");
        en.put("active.found", "Active plans:");
        en.put("active.price.loading", "Loading price...");
        en.put("active.price.unavailable", "Price unavailable");
        en.put("active.balance.label", "Remaining balance:");
        en.put("active.balance.unavailable", "Balance unavailable");
        en.put("active.renew", "Renew");
        en.put("active.status.unknown", "Unknown status");
        en.put("active.status.label", "Status:");
        en.put("active.status.suspended", "Your eSIM is suspended. Reactivate it to continue.");
        en.put("active.status.terminated", "eSIM terminated. You can no longer subscribe.");
        en.put("active.pendingMsisdn", "Activation in progress: number not assigned yet.");
        en.put("active.expiration", "Expiration:");
        en.put("active.msisdn", "My SIM:");
        en.put("active.unknown", "eSIM plan");
        en.put("noEsim.title", "You don't have an eSIM yet");
        en.put("noEsim.body", "Choose a plan to create and activate your eSIM.");
        en.put("noEsim.cta", "View plans");
        en.put("pendingEsim.title", "eSIM activation in progress");
        en.put("pendingEsim.body", "Your eSIM is being activated. Please try again in a few minutes.");
        en.put("sim.status.cta", "SIM status");
        en.put("sim.status.title", "SIM status");
        en.put("sim.status.subtitle", "Check your eSIM status.");
        en.put("sim.status.loading", "Loading status...");
        en.put("sim.status.simSerial", "SIM:");
        en.put("sim.status.msisdn", "MSISDN:");
        en.put("sim.status.active", "eSIM active");
        en.put("sim.status.active.detail", "Your eSIM is active. You can subscribe to plans.");
        en.put("sim.status.pending", "Activation in progress");
        en.put("sim.status.pending.detail", "Your eSIM is being activated. Please wait a few minutes.");
        en.put("sim.status.suspended", "eSIM suspended");
        en.put("sim.status.suspended.detail", "Your eSIM is suspended. Reactivate it to subscribe.");
        en.put("sim.status.terminated", "eSIM terminated");
        en.put("sim.status.terminated.detail", "Your eSIM is terminated. Contact support for a new eSIM.");
        en.put("sim.status.none", "No eSIM");
        en.put("sim.status.none.detail", "You don't have an eSIM yet. Choose a plan to create one.");
        en.put("sim.status.unknown", "Unknown status");
        en.put("sim.status.unknown.detail", "Unable to fetch status. Please try again later.");
        en.put("sim.status.viewPlans", "View plans");
        en.put("suspend.hint", "Temporarily suspend the eSIM.");
        en.put("suspend.cta", "Suspend eSIM");
        en.put("suspend.title", "Suspend eSIM");
        en.put("suspend.header", "Suspend this eSIM?");
        en.put("suspend.warning", "The SIM will be suspended temporarily. You can reactivate it later.");
        en.put("suspend.confirm", "I understand and want to suspend.");
        en.put("suspend.cta.confirm", "Suspend");
        en.put("suspend.loading.title", "Suspension");
        en.put("suspend.loading", "Suspension in progress...");
        en.put("suspend.success", "Suspension accepted.");
        en.put("suspend.failed", "Suspension failed.");
        en.put("terminate.hint", "Terminate this eSIM for this account.");
        en.put("terminate.cta", "Terminate eSIM");
        en.put("terminate.title", "eSIM termination");
        en.put("terminate.header", "Terminate this eSIM?");
        en.put("terminate.warning", "This will terminate the subscriber. The SIM will lose network access.");
        en.put("terminate.confirm", "I understand and want to terminate.");
        en.put("terminate.sim.label", "SIM:");
        en.put("terminate.sim.missing", "SIM unknown.");
        en.put("terminate.cta.confirm", "Terminate");
        en.put("terminate.loading.title", "Termination");
        en.put("terminate.loading", "Termination in progress...");
        en.put("terminate.success", "Termination accepted.");
        en.put("terminate.failed", "Termination failed.");
        en.put("balance.cta", "Internet balance");
        en.put("balance.header", "Internet balance");
        en.put("balance.title", "Internet balance");
        en.put("balance.subtitle", "Check your credit and renew your plan.");
        en.put("balance.card.title", "Your internet balance");
        en.put("balance.status.active", "Active");
        en.put("balance.loading", "Loading...");
        en.put("balance.meta.unit", "Unit:");
        en.put("balance.meta.expiration", "Expires:");
        en.put("balance.unavailable", "Balance unavailable");
        en.put("balance.info", "When your balance reaches 0, you can renew your plan from this page.");
        en.put("balance.renew", "Renew plan");
        en.put("errors.msisdn.required", "No eSIM linked to this account. Subscribe to get one.");
        en.put("errors.subscriber.terminated", "eSIM terminated: you can no longer subscribe.");
        en.put("errors.sim.notReady", "Your eSIM is not ready yet. Please try again in a few minutes.");
        en.put("errors.renew.amount", "Last plan amount unknown. Subscribe to a plan first.");
        en.put("errors.renew.insufficientFunds", "Renewal failed: insufficient balance.");
        en.put("errors.subscribe.insufficientFunds", "Subscription failed: insufficient balance.");
        en.put("renew.confirm.title", "Renewal");
        en.put("renew.confirm.header", "Confirm renewal?");
        en.put("renew.confirm.body", "You are about to renew your last plan.");
        en.put("renew.loading.title", "Renewal");
        en.put("renew.loading.header", "Renewal in progress...");
        en.put("renew.success", "Renewal OK");


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
        de.put("nav.home", "Start");
        de.put("nav.plans", "Tarife");
        de.put("nav.balance", "Guthaben");
        de.put("nav.esims", "Meine eSIMs");
        de.put("plans.title", "Angebote für");
        de.put("plans.subtitle", "Verfügbare Tarife für");
        de.put("plans.none", "Keine eSIM-Tarife für dieses Land gefunden.");
        de.put("plans.status.suspended", "eSIM suspendiert: Bitte reaktivieren, um zu buchen.");
        de.put("plans.status.terminated", "eSIM beendet: Keine neue Buchung möglich.");
        de.put("plans.status.nosim", "Keine eSIM: Sie können abonnieren, um eine zu erstellen.");
        de.put("plans.status.inprogress", "Tarif aktiv: Sie können abonnieren.");
        de.put("plans.status.pending", "eSIM-Aktivierung läuft: Buchung nicht verfügbar.");
        de.put("plans.status.noactive", "Kein aktiver Tarif: Buchung verfügbar.");
        de.put("plans.status.unknown", "Unbekannter eSIM-Status: Zugriff auf Tarife bleibt möglich.");
        de.put("plan.unavailable", "Nicht verfugbar");
        de.put("popular.title", "Beliebte Reiseziele");
        de.put("popular.none", "Keine beliebten Reiseziele gefunden.");
        de.put("popular.more", "Mehr Reiseziele");
        de.put("errors.catalog.invalid", "Ungültiger Katalog.");
        de.put("errors.token", "401 - Token fehlt oder ist ungültig.");
        de.put("errors.catalog", "Katalogfehler:");
        de.put("errors.ui", "UI-Fehler:");
        de.put("errors.network", "Netzwerkfehler:");
        de.put("plan.select", "Auswählen");
        de.put("plan.free", "Kostenlos");
        de.put("plan.priceUnavailable", "Preis nicht verfügbar");
        de.put("plan.defaultMeta", "eSIM-Tarif");
        de.put("alert.error", "Fehler");
        de.put("alert.success", "Erfolg");
        de.put("alert.copied", "Kopiert");
        de.put("action.retry", "Erneut versuchen");
        de.put("errors.product.invalid", "Ungültiges Produkt.");
        de.put("errors.userId.required", "AKUUNDA_USER_ID ist für die Buchung erforderlich.");
        de.put("errors.price.missing", "Preis für diesen Tarif nicht verfügbar.");
        de.put("errors.user.notFound", "Benutzer nicht gefunden.");
        de.put("errors.msisdn.missing", "MSISDN ist für die Buchung erforderlich.");
        de.put("errors.noEsim", "Keine eSIM mit diesem Konto verknüpft.");
        de.put("errors.subscribe.insufficientFunds", "Buchung fehlgeschlagen: Guthaben zu niedrig.");
        de.put("errors.conflict", "Ein Vorgang läuft bereits für Ihre eSIM. Bitte 1 bis 2 Minuten warten und erneut versuchen.");
        de.put("subscribe.confirm.title", "eSIM-Buchung");
        de.put("subscribe.confirm.header", "Buchung bestätigen?");
        de.put("subscribe.confirm.body", "Du bist dabei, einen eSIM-Tarif für dieses Land zu aktivieren.");
        de.put("subscribe.confirm.body.newsim", "Es ist noch keine eSIM mit diesem Konto verbunden. Nach der Buchung wird eine neue eSIM zugewiesen.");
        de.put("subscribe.loading.title", "Buchung");
        de.put("subscribe.loading.header", "Buchung läuft...");
        de.put("subscribe.success.unparsed", "Buchung OK (Antwort nicht interpretierbar).");
        de.put("subscribe.success", "Buchung OK");
        de.put("reactivate.title", "eSIM reaktivieren");
        de.put("reactivate.header", "Deine eSIM ist gesperrt");
        de.put("reactivate.body", "Möchtest du die eSIM reaktivieren, um die Buchung fortzusetzen?");
        de.put("reactivate.action", "Reaktivieren");
        de.put("reactivate.cta", "eSIM reaktivieren");
        de.put("reactivate.cancel", "Abbrechen");
        de.put("reactivate.loading.title", "Reaktivierung");
        de.put("reactivate.loading.header", "Reaktivierung läuft...");
        de.put("reactivate.success", "eSIM reaktiviert. Du kannst die Buchung erneut starten.");
        de.put("reactivate.missingSim", "Keine eSIM mit diesem Konto verbunden. Bitte wähle einen Tarif, um eine zu erhalten.");
        de.put("activation.title", "eSIM-Aktivierung");
        de.put("activation.inProgress", "Aktivierung läuft...");
        de.put("activation.fetchingQr", "Aktivierung abgeschlossen. QR wird geladen...");
        de.put("activation.qr.missing", "Aktivierung abgeschlossen, aber QR nicht erhalten.");
        de.put("activation.fail", "eSIM-Aktivierung fehlgeschlagen.");
        de.put("activation.retry", "Aktivierung läuft noch. Bitte später erneut versuchen.");
        de.put("activation.success", "Aktivierung OK");
        de.put("success.title", "Deine eSIM ist bereit");
        de.put("success.header", "Abo erfolgreich");
        de.put("success.subtitle.long", "Dein Tarif ist aktiv. Scanne den QR-Code, um die eSIM zu installieren.");
        de.put("success.plan.unknown", "eSIM-Tarif");
        de.put("success.subtitle", "Scanne den QR-Code, um die eSIM zu installieren.");
        de.put("success.copy", "LPA-Code kopieren");
        de.put("success.download", "QR herunterladen");
        de.put("success.download.done", "QR heruntergeladen.");
        de.put("errors.qr.download", "QR-Download fehlgeschlagen.");
        de.put("success.next", "Aktiviere anschließend mobile Daten auf deinem iPhone/Android.");
        de.put("success.myEsim", "Meine eSIMs");
        de.put("success.home", "Zurück zur Startseite");
        de.put("clipboard.copied", "Der LPA-Code wurde kopiert.");
        de.put("greeting.default", "Hallo");
        de.put("wallet.prefix", "Guthaben");
        de.put("wallet.placeholder", "Guthaben: --");
        de.put("wallet.unavailable", "Guthaben nicht verfugbar");
        de.put("home.balance.label", "Verfugbares Guthaben");
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
        de.put("esim.intro.title", "Willkommen im eSIM Bereich");
        de.put("esim.intro.subtitle", "Klicken Sie auf die folgende Option:");
        de.put("esim.intro.card", "eSIM aktivieren  >");
        de.put("esim.intro.myEsim", "Meine eSIMs");
        de.put("esim.intro.info", "Dienst nur auf eSIM-fahigen Telefonen verfugbar. Zahlungsgebuhren von X € fallen an.");
        de.put("active.header", "Meine eSIMs");
        de.put("active.title", "Aktive Tarife");
        de.put("active.subtitle", "Prufe deine aktiven Tarife und erneuere sie.");
        de.put("active.user.title", "Teilnehmer");
        de.put("active.user.phone", "Leitung:");
        de.put("active.user.unknown", "Teilnehmer");
        de.put("active.loading", "Aktive Tarife werden geladen...");
        de.put("active.empty", "Noch kein aktiver Tarif.");
        de.put("active.found", "Aktive Tarife:");
        de.put("active.price.loading", "Preis wird geladen...");
        de.put("active.price.unavailable", "Preis nicht verfugbar");
        de.put("active.balance.label", "Restguthaben:");
        de.put("active.balance.unavailable", "Guthaben nicht verfugbar");
        de.put("active.renew", "Erneuern");
        de.put("active.status.unknown", "Status unbekannt");
        de.put("active.status.label", "Status:");
        de.put("active.status.suspended", "Deine eSIM ist suspendiert. Reaktiviere sie, um fortzufahren.");
        de.put("active.status.terminated", "eSIM beendet. Du kannst nicht erneut abonnieren.");
        de.put("active.pendingMsisdn", "Aktivierung lauft: Nummer noch nicht zugewiesen.");
        de.put("active.expiration", "Ablauf:");
        de.put("active.msisdn", "Meine SIM:");
        de.put("active.unknown", "eSIM-Tarif");
        de.put("noEsim.title", "Du hast noch keine eSIM");
        de.put("noEsim.body", "Wahle einen Tarif, um deine eSIM zu erstellen und zu aktivieren.");
        de.put("noEsim.cta", "Tarife anzeigen");
        de.put("pendingEsim.title", "eSIM wird aktiviert");
        de.put("pendingEsim.body", "Deine eSIM wird gerade aktiviert. Bitte versuche es in ein paar Minuten erneut.");
        de.put("sim.status.cta", "SIM-Status");
        de.put("sim.status.title", "SIM-Status");
        de.put("sim.status.subtitle", "Status deiner eSIM anzeigen.");
        de.put("sim.status.loading", "Status wird geladen...");
        de.put("sim.status.simSerial", "SIM:");
        de.put("sim.status.msisdn", "MSISDN:");
        de.put("sim.status.active", "eSIM aktiv");
        de.put("sim.status.active.detail", "Deine eSIM ist aktiv. Du kannst Tarife buchen.");
        de.put("sim.status.pending", "Aktivierung lauft");
        de.put("sim.status.pending.detail", "Deine eSIM wird aktiviert. Bitte warte ein paar Minuten.");
        de.put("sim.status.suspended", "eSIM suspendiert");
        de.put("sim.status.suspended.detail", "Deine eSIM ist suspendiert. Reaktiviere sie, um zu buchen.");
        de.put("sim.status.terminated", "eSIM beendet");
        de.put("sim.status.terminated.detail", "Deine eSIM ist beendet. Kontaktiere den Support fur eine neue eSIM.");
        de.put("sim.status.none", "Keine eSIM");
        de.put("sim.status.none.detail", "Du hast noch keine eSIM. Wahle einen Tarif, um eine zu erstellen.");
        de.put("sim.status.unknown", "Unbekannter Status");
        de.put("sim.status.unknown.detail", "Status konnte nicht geladen werden. Bitte spater erneut versuchen.");
        de.put("sim.status.viewPlans", "Tarife anzeigen");
        de.put("suspend.hint", "eSIM vorubergehend sperren.");
        de.put("suspend.cta", "eSIM sperren");
        de.put("suspend.title", "eSIM sperren");
        de.put("suspend.header", "Diese eSIM sperren?");
        de.put("suspend.warning", "Die SIM wird vorubergehend gesperrt. Du kannst sie spater reaktivieren.");
        de.put("suspend.confirm", "Ich verstehe und mochte sperren.");
        de.put("suspend.cta.confirm", "Sperren");
        de.put("suspend.loading.title", "Sperrung");
        de.put("suspend.loading", "Sperrung laeuft...");
        de.put("suspend.success", "Sperrung akzeptiert.");
        de.put("suspend.failed", "Sperrung fehlgeschlagen.");
        de.put("terminate.hint", "eSIM fur dieses Konto beenden.");
        de.put("terminate.cta", "eSIM beenden");
        de.put("terminate.title", "eSIM-Kundigung");
        de.put("terminate.header", "Diese eSIM beenden?");
        de.put("terminate.warning", "Diese Aktion beendet den Abonnenten. Die SIM hat keinen Netzzugang mehr.");
        de.put("terminate.confirm", "Ich verstehe und mochte beenden.");
        de.put("terminate.sim.label", "SIM:");
        de.put("terminate.sim.missing", "SIM unbekannt.");
        de.put("terminate.cta.confirm", "Beenden");
        de.put("terminate.loading.title", "Kundigung");
        de.put("terminate.loading", "Kundigung laeuft...");
        de.put("terminate.success", "Kundigung akzeptiert.");
        de.put("terminate.failed", "Kundigung fehlgeschlagen.");
        de.put("balance.cta", "Internetguthaben");
        de.put("balance.header", "Internetguthaben");
        de.put("balance.title", "Internetguthaben");
        de.put("balance.subtitle", "Guthaben prüfen und Tarif erneuern.");
        de.put("balance.card.title", "Dein Internetguthaben");
        de.put("balance.status.active", "Aktiv");
        de.put("balance.loading", "Laden...");
        de.put("balance.meta.unit", "Einheit:");
        de.put("balance.meta.expiration", "Ablauf:");
        de.put("balance.unavailable", "Guthaben nicht verfugbar");
        de.put("balance.info", "Wenn dein Guthaben 0 erreicht, kannst du den Tarif hier erneuern.");
        de.put("balance.renew", "Tarif erneuern");
        de.put("errors.msisdn.required", "Keine eSIM mit diesem Konto verbunden. Bitte einen Tarif buchen.");
        de.put("errors.subscriber.terminated", "eSIM beendet: Keine neue Buchung moglich.");
        de.put("errors.sim.notReady", "Deine eSIM ist noch nicht bereit. Bitte in ein paar Minuten erneut versuchen.");
        de.put("errors.renew.amount", "Letzter Betrag unbekannt. Zuerst einen Tarif buchen.");
        de.put("errors.renew.insufficientFunds", "Verlaengerung fehlgeschlagen: Guthaben zu niedrig.");
        de.put("errors.subscribe.insufficientFunds", "Buchung fehlgeschlagen: Guthaben zu niedrig.");
        de.put("renew.confirm.title", "Verlangerung");
        de.put("renew.confirm.header", "Verlangerung bestatigen?");
        de.put("renew.confirm.body", "Du erneuerst deinen letzten Tarif.");
        de.put("renew.loading.title", "Verlangerung");
        de.put("renew.loading.header", "Verlangerung laeuft...");
        de.put("renew.success", "Verlangerung OK");


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
        es.put("nav.home", "Inicio");
        es.put("nav.plans", "Planes");
        es.put("nav.balance", "Saldo");
        es.put("nav.esims", "Mis eSIM");
        es.put("plans.title", "Planes para");
        es.put("plans.subtitle", "Planes disponibles para");
        es.put("plans.none", "No se encontraron planes eSIM para este país.");
        es.put("plans.status.suspended", "eSIM suspendida: reactívala para suscribirte.");
        es.put("plans.status.terminated", "eSIM finalizada: ya no puedes suscribirte.");
        es.put("plans.status.nosim", "Sin eSIM: puedes suscribirte para crear una.");
        es.put("plans.status.inprogress", "Plan en curso: puedes suscribirte.");
        es.put("plans.status.pending", "Activación de eSIM en curso: suscripción no disponible.");
        es.put("plans.status.noactive", "Sin plan activo: suscripción disponible.");
        es.put("plans.status.unknown", "Estado de eSIM desconocido: acceso a planes permitido.");
        es.put("plan.unavailable", "No disponible");
        es.put("popular.title", "Destinos populares");
        es.put("popular.none", "No se encontraron destinos populares.");
        es.put("popular.more", "Más destinos");
        es.put("errors.catalog.invalid", "Catálogo inválido.");
        es.put("errors.token", "401 - Token faltante o inválido.");
        es.put("errors.catalog", "Error de catálogo:");
        es.put("errors.ui", "Error de UI:");
        es.put("errors.network", "Error de red:");
        es.put("plan.select", "Seleccionar");
        es.put("plan.free", "Gratis");
        es.put("plan.priceUnavailable", "Precio no disponible");
        es.put("plan.defaultMeta", "Plan eSIM");
        es.put("alert.error", "Error");
        es.put("alert.success", "Éxito");
        es.put("alert.copied", "Copiado");
        es.put("action.retry", "Reintentar");
        es.put("errors.product.invalid", "Producto inválido.");
        es.put("errors.userId.required", "AKUUNDA_USER_ID es necesario para la suscripción.");
        es.put("errors.price.missing", "Precio no disponible para este plan.");
        es.put("errors.user.notFound", "Usuario no encontrado.");
        es.put("errors.msisdn.missing", "MSISDN es necesario para suscribirse.");
        es.put("errors.noEsim", "No hay una eSIM asociada a esta cuenta.");
        es.put("errors.subscribe.insufficientFunds", "Suscripción fallida: saldo insuficiente.");
        es.put("errors.conflict", "Ya hay una operación en curso en tu eSIM. Espera de 1 a 2 minutos y vuelve a intentarlo.");
        es.put("subscribe.confirm.title", "Suscripción eSIM");
        es.put("subscribe.confirm.header", "¿Confirmar suscripción?");
        es.put("subscribe.confirm.body", "Vas a activar un plan eSIM para este país.");
        es.put("subscribe.confirm.body.newsim", "Aún no hay una eSIM asociada a esta cuenta. Se asignará una nueva eSIM tras la suscripción.");
        es.put("subscribe.loading.title", "Suscripción");
        es.put("subscribe.loading.header", "Suscripción en curso...");
        es.put("subscribe.success.unparsed", "Suscripción OK (respuesta no interpretable).");
        es.put("subscribe.success", "Suscripción OK");
        es.put("reactivate.title", "Reactivar eSIM");
        es.put("reactivate.header", "Tu eSIM está suspendida");
        es.put("reactivate.body", "¿Quieres reactivar tu eSIM para continuar la suscripción?");
        es.put("reactivate.action", "Reactivar");
        es.put("reactivate.cta", "Reactivar eSIM");
        es.put("reactivate.cancel", "Cancelar");
        es.put("reactivate.loading.title", "Reactivación");
        es.put("reactivate.loading.header", "Reactivación en curso...");
        es.put("reactivate.success", "eSIM reactivada. Puedes reintentar la suscripción.");
        es.put("reactivate.missingSim", "No hay una eSIM asociada a esta cuenta. Elige un plan para obtener una.");
        es.put("activation.title", "Activación eSIM");
        es.put("activation.inProgress", "Activación en curso...");
        es.put("activation.fetchingQr", "Activación terminada. Cargando QR...");
        es.put("activation.qr.missing", "Activación terminada, pero no se obtuvo el QR.");
        es.put("activation.fail", "Fallo en la activación eSIM.");
        es.put("activation.retry", "Activación aún en curso. Inténtalo de nuevo más tarde.");
        es.put("activation.success", "Activación OK");
        es.put("success.title", "Tu eSIM está lista");
        es.put("success.header", "Suscripción exitosa");
        es.put("success.subtitle.long", "Tu plan está activo. Escanea el QR para instalar la eSIM.");
        es.put("success.plan.unknown", "Plan eSIM");
        es.put("success.subtitle", "Escanea el código QR para instalar la eSIM.");
        es.put("success.copy", "Copiar código LPA");
        es.put("success.download", "Descargar QR");
        es.put("success.download.done", "QR descargado.");
        es.put("errors.qr.download", "Fallo al descargar el QR.");
        es.put("success.next", "Luego activa los datos móviles en tu iPhone/Android.");
        es.put("success.myEsim", "Ver mis eSIM");
        es.put("success.home", "Volver al inicio");
        es.put("clipboard.copied", "El código LPA se ha copiado.");
        es.put("greeting.default", "Hola");
        es.put("wallet.prefix", "Saldo");
        es.put("wallet.placeholder", "Saldo: --");
        es.put("wallet.unavailable", "Saldo no disponible");
        es.put("home.balance.label", "Saldo disponible");
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
        es.put("esim.intro.title", "Bienvenido a la seccion eSIM");
        es.put("esim.intro.subtitle", "Haz clic en la opcion siguiente:");
        es.put("esim.intro.card", "Activar tu eSIM  >");
        es.put("esim.intro.myEsim", "Ver mis eSIM");
        es.put("esim.intro.info", "Servicio solo disponible en telefonos compatibles con eSIM. Se aplicaran tarifas de X €.");
        es.put("active.header", "Mis eSIM");
        es.put("active.title", "Planes activos");
        es.put("active.subtitle", "Consulta tus planes activos y renuévalos.");
        es.put("active.user.title", "Usuario");
        es.put("active.user.phone", "Línea:");
        es.put("active.user.unknown", "Usuario");
        es.put("active.loading", "Cargando planes activos...");
        es.put("active.empty", "No hay planes activos por ahora.");
        es.put("active.found", "Planes activos:");
        es.put("active.price.loading", "Cargando precio...");
        es.put("active.price.unavailable", "Precio no disponible");
        es.put("active.balance.label", "Saldo restante:");
        es.put("active.balance.unavailable", "Saldo no disponible");
        es.put("active.renew", "Renovar");
        es.put("active.status.unknown", "Estado desconocido");
        es.put("active.status.label", "Estado:");
        es.put("active.status.suspended", "Tu eSIM está suspendida. Reactívala para continuar.");
        es.put("active.status.terminated", "eSIM finalizada. Ya no puedes suscribirte.");
        es.put("active.pendingMsisdn", "Activación en curso: numero aun no asignado.");
        es.put("active.expiration", "Expira:");
        es.put("active.msisdn", "Mi SIM:");
        es.put("active.unknown", "Plan eSIM");
        es.put("noEsim.title", "Aun no tienes una eSIM");
        es.put("noEsim.body", "Elige un plan para crear y activar tu eSIM.");
        es.put("noEsim.cta", "Ver planes");
        es.put("pendingEsim.title", "Activacion de eSIM en curso");
        es.put("pendingEsim.body", "Tu eSIM se esta activando. Intentalo de nuevo en unos minutos.");
        es.put("sim.status.cta", "Estado de la SIM");
        es.put("sim.status.title", "Estado de la SIM");
        es.put("sim.status.subtitle", "Consulta el estado de tu eSIM.");
        es.put("sim.status.loading", "Cargando estado...");
        es.put("sim.status.simSerial", "SIM:");
        es.put("sim.status.msisdn", "MSISDN:");
        es.put("sim.status.active", "eSIM activa");
        es.put("sim.status.active.detail", "Tu eSIM esta activa. Puedes suscribirte a planes.");
        es.put("sim.status.pending", "Activacion en curso");
        es.put("sim.status.pending.detail", "Tu eSIM se esta activando. Espera unos minutos.");
        es.put("sim.status.suspended", "eSIM suspendida");
        es.put("sim.status.suspended.detail", "Tu eSIM esta suspendida. Reactivala para suscribirte.");
        es.put("sim.status.terminated", "eSIM finalizada");
        es.put("sim.status.terminated.detail", "Tu eSIM esta finalizada. Contacta con soporte para una nueva eSIM.");
        es.put("sim.status.none", "Sin eSIM");
        es.put("sim.status.none.detail", "No tienes una eSIM. Elige un plan para crear una.");
        es.put("sim.status.unknown", "Estado desconocido");
        es.put("sim.status.unknown.detail", "No se pudo obtener el estado. Intentalo mas tarde.");
        es.put("sim.status.viewPlans", "Ver planes");
        es.put("suspend.hint", "Suspender temporalmente la eSIM.");
        es.put("suspend.cta", "Suspender eSIM");
        es.put("suspend.title", "Suspender eSIM");
        es.put("suspend.header", "¿Suspender esta eSIM?");
        es.put("suspend.warning", "La SIM se suspenderá temporalmente. Podrás reactivarla más tarde.");
        es.put("suspend.confirm", "Entiendo y quiero suspender.");
        es.put("suspend.cta.confirm", "Suspender");
        es.put("suspend.loading.title", "Suspensión");
        es.put("suspend.loading", "Suspensión en curso...");
        es.put("suspend.success", "Suspensión aceptada.");
        es.put("suspend.failed", "Suspensión fallida.");
        es.put("terminate.hint", "Dar de baja esta eSIM para esta cuenta.");
        es.put("terminate.cta", "Dar de baja eSIM");
        es.put("terminate.title", "Baja eSIM");
        es.put("terminate.header", "¿Dar de baja esta eSIM?");
        es.put("terminate.warning", "Esta accion da de baja al abonado. La SIM no tendra acceso a la red.");
        es.put("terminate.confirm", "Entiendo y quiero darla de baja.");
        es.put("terminate.sim.label", "SIM:");
        es.put("terminate.sim.missing", "SIM desconocida.");
        es.put("terminate.cta.confirm", "Dar de baja");
        es.put("terminate.loading.title", "Baja");
        es.put("terminate.loading", "Baja en curso...");
        es.put("terminate.success", "Baja aceptada.");
        es.put("terminate.failed", "No se pudo dar de baja.");
        es.put("balance.cta", "Saldo de Internet");
        es.put("balance.header", "Saldo de Internet");
        es.put("balance.title", "Saldo de Internet");
        es.put("balance.subtitle", "Consulta tu crédito y renueva tu plan.");
        es.put("balance.card.title", "Tu saldo de Internet");
        es.put("balance.status.active", "Activo");
        es.put("balance.loading", "Cargando...");
        es.put("balance.meta.unit", "Unidad:");
        es.put("balance.meta.expiration", "Expira:");
        es.put("balance.unavailable", "Saldo no disponible");
        es.put("balance.info", "Cuando tu saldo llegue a 0, puedes renovar el plan desde esta pagina.");
        es.put("balance.renew", "Renovar plan");
        es.put("errors.msisdn.required", "No hay eSIM asociada a esta cuenta. Suscribete para obtener una.");
        es.put("errors.subscriber.terminated", "eSIM finalizada: ya no puedes suscribirte.");
        es.put("errors.sim.notReady", "Tu eSIM aún no está lista. Inténtalo de nuevo en unos minutos.");
        es.put("errors.renew.amount", "Importe del ultimo plan desconocido. Suscribete primero.");
        es.put("errors.renew.insufficientFunds", "Renovacion fallida: saldo insuficiente.");
        es.put("errors.subscribe.insufficientFunds", "Suscripcion fallida: saldo insuficiente.");
        es.put("renew.confirm.title", "Renovacion");
        es.put("renew.confirm.header", "¿Confirmar renovacion?");
        es.put("renew.confirm.body", "Vas a renovar tu ultimo plan.");
        es.put("renew.loading.title", "Renovacion");
        es.put("renew.loading.header", "Renovacion en curso...");
        es.put("renew.success", "Renovacion OK");


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
        zh.put("nav.home", "首页");
        zh.put("nav.plans", "套餐");
        zh.put("nav.balance", "余额");
        zh.put("nav.esims", "我的 eSIM");
        zh.put("plans.title", "套餐适用于");
        zh.put("plans.subtitle", "可用套餐：");
        zh.put("plans.none", "该国家暂无 eSIM 套餐。");
        zh.put("plans.status.suspended", "eSIM 已暂停：请先重新激活再订购。");
        zh.put("plans.status.terminated", "eSIM 已注销：无法再次订购。");
        zh.put("plans.status.nosim", "没有 eSIM：可以订购以创建。");
        zh.put("plans.status.inprogress", "套餐进行中：可以订购。");
        zh.put("plans.status.pending", "eSIM 正在激活：暂不可订购。");
        zh.put("plans.status.noactive", "无有效套餐：可订购。");
        zh.put("plans.status.unknown", "eSIM 状态未知：仍可查看套餐。");
        zh.put("plan.unavailable", "不可用");
        zh.put("popular.title", "热门目的地");
        zh.put("popular.none", "未找到热门目的地。");
        zh.put("popular.more", "更多目的地");
        zh.put("errors.catalog.invalid", "套餐目录无效。");
        zh.put("errors.token", "401 - 缺少或无效 token。");
        zh.put("errors.catalog", "套餐目录错误：");
        zh.put("errors.ui", "UI 错误：");
        zh.put("errors.network", "网络错误：");
        zh.put("plan.select", "选择");
        zh.put("plan.free", "免费");
        zh.put("plan.priceUnavailable", "价格不可用");
        zh.put("plan.defaultMeta", "eSIM 套餐");
        zh.put("alert.error", "错误");
        zh.put("alert.success", "成功");
        zh.put("alert.copied", "已复制");
        zh.put("action.retry", "重试");
        zh.put("errors.product.invalid", "产品无效。");
        zh.put("errors.userId.required", "需要 AKUUNDA_USER_ID 才能订阅。");
        zh.put("errors.price.missing", "该套餐价格不可用。");
        zh.put("errors.user.notFound", "用户未找到。");
        zh.put("errors.msisdn.missing", "订阅需要 MSISDN。");
        zh.put("errors.noEsim", "该账户没有关联 eSIM。");
        zh.put("errors.subscribe.insufficientFunds", "订阅失败：余额不足。");
        zh.put("errors.conflict", "您的 eSIM 正在处理另一项操作。请等待 1 到 2 分钟后重试。");
        zh.put("subscribe.confirm.title", "eSIM 订阅");
        zh.put("subscribe.confirm.header", "确认订阅？");
        zh.put("subscribe.confirm.body", "您将激活该国家的 eSIM 套餐。");
        zh.put("subscribe.confirm.body.newsim", "该账户尚未关联 eSIM，订阅后将分配新的 eSIM。");
        zh.put("subscribe.loading.title", "订阅");
        zh.put("subscribe.loading.header", "订阅处理中...");
        zh.put("subscribe.success.unparsed", "订阅成功（响应无法解析）。");
        zh.put("subscribe.success", "订阅成功");
        zh.put("reactivate.title", "重新激活 eSIM");
        zh.put("reactivate.header", "你的 eSIM 已暂停");
        zh.put("reactivate.body", "是否重新激活 eSIM 以继续订阅？");
        zh.put("reactivate.action", "重新激活");
        zh.put("reactivate.cta", "重新激活 eSIM");
        zh.put("reactivate.cancel", "取消");
        zh.put("reactivate.loading.title", "重新激活");
        zh.put("reactivate.loading.header", "正在重新激活...");
        zh.put("reactivate.success", "eSIM 已重新激活，可重新订阅。");
        zh.put("reactivate.missingSim", "该账户没有关联 eSIM，请先选择套餐以获取。");
        zh.put("activation.title", "eSIM 激活");
        zh.put("activation.inProgress", "激活中...");
        zh.put("activation.fetchingQr", "激活完成。正在获取 QR...");
        zh.put("activation.qr.missing", "激活完成，但无法获取 QR。");
        zh.put("activation.fail", "eSIM 激活失败。");
        zh.put("activation.retry", "激活仍在进行中。请稍后重试。");
        zh.put("activation.success", "激活成功");
        zh.put("success.title", "您的 eSIM 已准备就绪");
        zh.put("success.header", "订阅成功");
        zh.put("success.subtitle.long", "您的套餐已激活。扫描 QR 码安装 eSIM。");
        zh.put("success.plan.unknown", "eSIM 套餐");
        zh.put("success.subtitle", "扫描 QR 码以安装 eSIM。");
        zh.put("success.copy", "复制 LPA 代码");
        zh.put("success.download", "下载二维码");
        zh.put("success.download.done", "二维码已下载。");
        zh.put("errors.qr.download", "二维码下载失败。");
        zh.put("success.next", "然后在 iPhone/Android 上开启移动数据。");
        zh.put("success.myEsim", "查看我的 eSIM");
        zh.put("success.home", "返回首页");
        zh.put("clipboard.copied", "LPA 代码已复制。");
        zh.put("greeting.default", "你好");
        zh.put("wallet.prefix", "余额");
        zh.put("wallet.placeholder", "余额: --");
        zh.put("wallet.unavailable", "余额不可用");
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
        zh.put("esim.intro.title", "欢迎使用 eSIM");
        zh.put("esim.intro.subtitle", "请点击以下选项：");
        zh.put("esim.intro.card", "激活 eSIM  >");
        zh.put("esim.intro.myEsim", "查看我的 eSIM");
        zh.put("esim.intro.info", "仅适用于支持 eSIM 的手机。将收取 X € 费用。");
        zh.put("active.header", "我的 eSIM");
        zh.put("active.title", "活跃套餐");
        zh.put("active.subtitle", "查看活跃套餐并续订。");
        zh.put("active.user.title", "用户");
        zh.put("active.user.phone", "号码：");
        zh.put("active.user.unknown", "用户");
        zh.put("active.loading", "正在加载活跃套餐...");
        zh.put("active.empty", "暂无活跃套餐。");
        zh.put("active.found", "活跃套餐：");
        zh.put("active.price.loading", "正在加载价格...");
        zh.put("active.price.unavailable", "价格不可用");
        zh.put("active.balance.label", "剩余余额：");
        zh.put("active.balance.unavailable", "余额不可用");
        zh.put("active.renew", "续订");
        zh.put("active.status.unknown", "状态未知");
        zh.put("active.status.label", "状态：");
        zh.put("active.status.suspended", "你的 eSIM 已暂停。请先重新激活。");
        zh.put("active.status.terminated", "eSIM 已注销，无法再次订阅。");
        zh.put("active.pendingMsisdn", "激活中：号码尚未分配。");
        zh.put("active.expiration", "到期：");
        zh.put("active.msisdn", "我的SIM:");
        zh.put("active.unknown", "eSIM 套餐");
        zh.put("noEsim.title", "你还没有 eSIM");
        zh.put("noEsim.body", "请选择套餐以创建并激活 eSIM。");
        zh.put("noEsim.cta", "查看套餐");
        zh.put("pendingEsim.title", "eSIM 正在激活");
        zh.put("pendingEsim.body", "你的 eSIM 正在激活中，请稍后再试。");
        zh.put("sim.status.cta", "SIM 状态");
        zh.put("sim.status.title", "SIM 状态");
        zh.put("sim.status.subtitle", "查看你的 eSIM 状态。");
        zh.put("sim.status.loading", "正在加载状态...");
        zh.put("sim.status.simSerial", "SIM:");
        zh.put("sim.status.msisdn", "MSISDN:");
        zh.put("sim.status.active", "eSIM 已激活");
        zh.put("sim.status.active.detail", "你的 eSIM 已激活，可以订购套餐。");
        zh.put("sim.status.pending", "激活中");
        zh.put("sim.status.pending.detail", "你的 eSIM 正在激活中，请稍等。");
        zh.put("sim.status.suspended", "eSIM 已暂停");
        zh.put("sim.status.suspended.detail", "你的 eSIM 已暂停，请先重新激活。");
        zh.put("sim.status.terminated", "eSIM 已注销");
        zh.put("sim.status.terminated.detail", "你的 eSIM 已注销，请联系支持获取新 eSIM。");
        zh.put("sim.status.none", "没有 eSIM");
        zh.put("sim.status.none.detail", "你还没有 eSIM，请选择套餐创建。");
        zh.put("sim.status.unknown", "状态未知");
        zh.put("sim.status.unknown.detail", "无法获取状态，请稍后重试。");
        zh.put("sim.status.viewPlans", "查看套餐");
        zh.put("suspend.hint", "暂时停用 eSIM。");
        zh.put("suspend.cta", "暂停 eSIM");
        zh.put("suspend.title", "暂停 eSIM");
        zh.put("suspend.header", "确定暂停该 eSIM？");
        zh.put("suspend.warning", "SIM 将暂时停用，稍后可重新激活。");
        zh.put("suspend.confirm", "我已了解并确认暂停。");
        zh.put("suspend.cta.confirm", "确认暂停");
        zh.put("suspend.loading.title", "暂停中");
        zh.put("suspend.loading", "正在暂停...");
        zh.put("suspend.success", "暂停已受理。");
        zh.put("suspend.failed", "暂停失败。");
        zh.put("terminate.hint", "注销该 eSIM。");
        zh.put("terminate.cta", "注销 eSIM");
        zh.put("terminate.title", "注销 eSIM");
        zh.put("terminate.header", "确定注销该 eSIM？");
        zh.put("terminate.warning", "注销后该 SIM 将无法接入网络。");
        zh.put("terminate.confirm", "我已了解并确认注销。");
        zh.put("terminate.sim.label", "SIM：");
        zh.put("terminate.sim.missing", "未知 SIM。");
        zh.put("terminate.cta.confirm", "确认注销");
        zh.put("terminate.loading.title", "注销中");
        zh.put("terminate.loading", "正在注销...");
        zh.put("terminate.success", "注销已受理。");
        zh.put("terminate.failed", "注销失败。");
        zh.put("balance.cta", "网络余额");
        zh.put("balance.header", "网络余额");
        zh.put("balance.title", "网络余额");
        zh.put("balance.subtitle", "查看余额并续订套餐。");
        zh.put("balance.card.title", "您的网络余额");
        zh.put("balance.status.active", "启用");
        zh.put("balance.loading", "加载中...");
        zh.put("balance.meta.unit", "单位:");
        zh.put("balance.meta.expiration", "到期:");
        zh.put("balance.unavailable", "余额不可用");
        zh.put("balance.info", "当余额为 0 时，可在此页面续订套餐。");
        zh.put("balance.renew", "续订套餐");
        zh.put("errors.msisdn.required", "该账户暂无 eSIM，请先订购套餐。");
        zh.put("errors.subscriber.terminated", "eSIM 已注销：无法再次订阅。");
        zh.put("errors.sim.notReady", "eSIM 尚未就绪，请稍后再试。");
        zh.put("errors.renew.amount", "上次套餐金额未知，请先订购套餐。");
        zh.put("errors.renew.insufficientFunds", "续订失败：余额不足。");
        zh.put("errors.subscribe.insufficientFunds", "订购失败：余额不足。");
        zh.put("renew.confirm.title", "续订");
        zh.put("renew.confirm.header", "确认续订？");
        zh.put("renew.confirm.body", "您将续订上一次的套餐。");
        zh.put("renew.loading.title", "续订");
        zh.put("renew.loading.header", "续订进行中...");
        zh.put("renew.success", "续订成功");

        dict.put("fr", fr);
        dict.put("en", en);
        dict.put("de", de);
        dict.put("es", es);
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
