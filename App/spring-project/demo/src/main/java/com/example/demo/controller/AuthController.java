package com.example.demo.controller;

import com.example.demo.dto.UserAuthEvent;
import com.example.demo.dto.UserAuthEventType;
import com.example.demo.service.KurrentDbService;
import com.example.demo.service.UserAuthEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

//    private final KurrentDbService kurrentDbService;

    private final UserAuthEventPublisher userAuthEventPublisher;

    public AuthController(UserAuthEventPublisher userAuthEventPublisher) {
        this.userAuthEventPublisher = userAuthEventPublisher;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> request,
                                      HttpServletResponse response,
                                      HttpSession session) {
        try {
            String code = request.get("code");

            // Exchange code for tokens
            String tokenUrl = keycloakAuthServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, String.class);

            JsonNode tokenData = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenData.get("access_token").asText();
            String refreshToken = tokenData.has("refresh_token") ? tokenData.get("refresh_token").asText() : null;

            // Get user info
            String userInfoUrl = keycloakAuthServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            HttpEntity<String> userInfoEntity = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userInfoEntity,
                    String.class
            );

            JsonNode userInfo = objectMapper.readTree(userInfoResponse.getBody());

            // Store in session
            session.setAttribute("access_token", accessToken);
            session.setAttribute("refresh_token", refreshToken);
            session.setAttribute("user_info", userInfo.toString());

            // Set cookie with session ID
            Cookie cookie = new Cookie("JSESSIONID", session.getId());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);

            // publish auth event
            UserAuthEvent userAuthEvent = UserAuthEvent.builder()
                    .userId(userInfo.has("sub") ? userInfo.get("sub").asText() : null)
                    .username(userInfo.has("preferred_username") ? userInfo.get("preferred_username").asText() : null)
                    .eventType(UserAuthEventType.LOGIN)
                    .build();
            userAuthEventPublisher.publish(userAuthEvent);

            return ResponseEntity.ok().body(Map.of("success", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        try {
            String userInfoJson = (String) session.getAttribute("user_info");
            System.out.println("User Info JSON: " + userInfoJson);
            if (userInfoJson == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // Trả về raw JSON string để frontend parse
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userInfoJson);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session, HttpServletResponse response) {
        try {
            String accessToken = (String) session.getAttribute("access_token");
            String refreshToken = (String) session.getAttribute("refresh_token");

            // Logout from Keycloak
            if (refreshToken != null) {
                String logoutUrl = keycloakAuthServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("client_id", clientId);
                params.add("client_secret", clientSecret);
                params.add("refresh_token", refreshToken);

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
                restTemplate.postForEntity(logoutUrl, entity, String.class);
            }

            // Clear session
            session.invalidate();

            // Clear cookie
            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            // Even if Keycloak logout fails, clear local session
            session.invalidate();
            return ResponseEntity.ok(Map.of("success", true));
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpSession session) {
        try {
            String refreshToken = (String) session.getAttribute("refresh_token");

            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No refresh token"));
            }

            String tokenUrl = keycloakAuthServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, String.class);

            JsonNode tokenData = objectMapper.readTree(tokenResponse.getBody());
            String newAccessToken = tokenData.get("access_token").asText();
            String newRefreshToken = tokenData.has("refresh_token") ? tokenData.get("refresh_token").asText() : refreshToken;

            // Update session
            session.setAttribute("access_token", newAccessToken);
            session.setAttribute("refresh_token", newRefreshToken);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}