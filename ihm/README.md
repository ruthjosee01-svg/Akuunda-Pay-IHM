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

- `AKUUNDA_BACKEND_URL` : URL du catalogue eSIM
- `AKUUNDA_BACKEND_TOKEN` : token Keycloak (Bearer)

Exemple :

```bash
export AKUUNDA_BACKEND_URL="http://localhost:8089/api/internal/v1/esim/catalog"
export AKUUNDA_BACKEND_TOKEN="<TOKEN_KEYCLOAK>"
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
- Test rapide du backend :

```bash
curl -i -H "Authorization: Bearer $AKUUNDA_BACKEND_TOKEN" \
  "$AKUUNDA_BACKEND_URL" | head -n 20
```

## Checklist de démarrage

1. Backend lancé en `dev`
2. Token Keycloak valide
3. Variables `AKUUNDA_BACKEND_URL` et `AKUUNDA_BACKEND_TOKEN` exportées
4. `mvn clean javafx:run`
