import { keycloakConfig } from "@/keycloak"

export async function getUserInfo(accessToken: string) {
  const { baseUrl, realm } = keycloakConfig

  const response = await fetch(
    `${baseUrl}/realms/${realm}/protocol/openid-connect/userinfo`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error('Failed to get user info')
  }

  return response.json()
}
