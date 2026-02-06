import { keycloakConfig } from "@/keycloak"

export function login() {
  const state = crypto.randomUUID()
  sessionStorage.setItem('oauth_state', state)

  const { baseUrl, realm, clientId, redirectUri } = keycloakConfig

  const url =
    `${baseUrl}/realms/${realm}/protocol/openid-connect/auth` +
    `?client_id=${clientId}` +
    `&response_type=code` +
    `&scope=openid` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&state=${state}`

  window.location.href = url
}
