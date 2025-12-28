package com.akuunda;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;

public class TransatelService {
    private static String accessToken = null;
    private static Instant expirationTime = null;

    private static final String AUTH_URL = "https://api.transatel.com/auth/v1/token";
    // URL exacte de ton catalogue Postman
    private static final String CATALOG_URL = "https://api.transatel.com/ocs/catalog/api/cos/WW_DEMO_COS_SPC/products";

    private static void ensureAuthenticated() throws Exception {
        // Renouvellement automatique si le token expire (toutes les 1h)
        if (accessToken == null || expirationTime == null || Instant.now().isAfter(expirationTime.minusSeconds(300))) {
            HttpClient client = HttpClient.newHttpClient();
            String authBody = "{\"username\": \"aman.kouassi\", \"password\": \"dKPia3M9NSMv@mz\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                accessToken = json.getString("access_token");
                expirationTime = Instant.now().plusSeconds(3600);
            } else {
                throw new Exception("Erreur Auth: " + response.body());
            }
        }
    }

    public static JSONArray getRealCatalog() throws Exception {
        ensureAuthenticated();
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(CATALOG_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JSONObject res = new JSONObject(response.body());
            // On renvoie la liste "products" du JSON
            return res.getJSONArray("products");
        } else {
            throw new Exception("Erreur Catalogue: " + response.statusCode());
        }
    }
}