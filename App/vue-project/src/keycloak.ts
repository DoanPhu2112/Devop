import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
    clientId: "vue-app",
    realm: "devop-realm",
    url: "http://localhost:8080",
})

export async function initializeKeycloak() {

    const authenticated = await keycloak.init({
        onLoad: "login-required",
        pkceMethod: "S256",
        checkLoginIframe: false
    });
    if (!authenticated) {
        window.location.reload();
    }
}