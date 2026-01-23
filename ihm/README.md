# Akuunda Pay IHM

Petit client JavaFX pour afficher le catalogue eSIM.

## Liens utiles

- Backend : https://github.com/David-akuundapay/akuunda-wallet
- IHM : https://github.com/ruthjosee01-svg/Akuunda-Pay-IHM

## Prérequis

- macOS (testé sur Apple Silicon)
- Java 17
- Maven
- Backend Akuunda Wallet lancé (port 8089)

## Lancer le backend (rappel)

Depuis le repo `akuunda-wallet` :

```bash
cd <CHEMIN_VERS_AKUUNDA_WALLET>
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

### Installer Java 17

Si besoin :

```bash
brew install --cask temurin
```

Vérifie :

```bash
java -version
```

### Installer Maven

```bash
brew install maven
```

Vérifie :

```bash
mvn -v
```

## Variables d'environnement

L'IHM appelle le backend et doit envoyer le token Keycloak dans l'en-tête Authorization.

- `AKUUNDA_BACKEND_BASE_URL` : URL base backend
- `AKUUNDA_BACKEND_TRANSACTION_URL` : URL transactions
- `AKUUNDA_BACKEND_ESIM_URL` : URL eSIM details
- `AKUUNDA_BACKEND_TOKEN` : token Keycloak (Bearer)
- `AKUUNDA_USER_ID` : user de test pour la souscription

Exemple :

```bash
export AKUUNDA_BACKEND_BASE_URL="http://localhost:8089/api/internal/v1"
export AKUUNDA_BACKEND_TRANSACTION_URL="http://localhost:8089/api/internal/v1/esim/transactions"
export AKUUNDA_BACKEND_ESIM_URL="http://localhost:8089/api/internal/v1/esim"
export AKUUNDA_BACKEND_TOKEN="<TOKEN_KEYCLOAK>"
export AKUUNDA_USER_ID="<USER_ID_TEST>"
```

## Lancer l'application

```bash
cd <CHEMIN_VERS_AKUUNDA_PAY_IHM>/ihm
mvn clean javafx:run
```

## Dépannage rapide

- Si la liste est vide :
  - vérifier que le backend répond bien sur `http://localhost:8089`
  - vérifier que le token est complet et non expiré
  - vérifier que le token Transatel est valide côté backend (sinon 500)
- Test rapide du backend :

```bash
curl -i -H "Authorization: Bearer $AKUUNDA_BACKEND_TOKEN" \
  "$AKUUNDA_BACKEND_BASE_URL/esim/catalog" | head -n 20
```

## Checklist de démarrage

1. Backend lancé en `dev`
2. Token Keycloak valide
3. Variables `AKUUNDA_BACKEND_*` et `AKUUNDA_USER_ID` exportées
4. `mvn clean javafx:run`

## Notes UI / UX (attendus commanditaire)

- Interface plus "operator grade" (style Ubigi) avec plusieurs écrans.
- Respect de la DA Akuunda Pay (couleurs + typo Montserrat/Nunito).
- Messages d'erreurs plus clairs et orientés utilisateur.
- Affichage des infos techniques (subscriptionId, simSerial, LPA) dans une UI propre.
 - Gérer les erreurs techniques (401/403/409/500) avec des messages compréhensibles.
