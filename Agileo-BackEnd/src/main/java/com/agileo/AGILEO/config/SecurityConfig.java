package com.agileo.AGILEO.config;

import com.agileo.AGILEO.security.CustomJwtAuthenticationConverter;
import com.agileo.AGILEO.service.UserService;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// Mise à jour de l'import : Remplacement de l'ancien @EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// Annotation conservée
@Configuration
// Annotation conservée
@EnableWebSecurity
// Mise à jour de l'annotation : Remplacement de @EnableGlobalMethodSecurity par @EnableMethodSecurity (Spring Security 6+)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${keycloak.admin.server-url:http://192.168.77.21:8081}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:RB-realm}")
    private String keycloakRealm;

    @Value("${cors.allowed-origins:http://192.168.77.17:8080,http://localhost:4200}")
    private String allowedOrigins;

    /**
     * Chaîne de filtres de sécurité configurée pour Spring Boot 3.5 (Spring Security 6+).
     * Utilise le nouveau DSL de configuration par lambda et remplace les méthodes dépréciées.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Configuration CSRF : désactivée pour une API REST sans état
                .csrf(csrf -> csrf.disable())

                // Configuration CORS : utilise la source de configuration définie ci-dessous
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Gestion des sessions : politique STATELESS (pour JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configuration des autorisations de requêtes (remplace authorizeRequests())
                .authorizeHttpRequests(auth -> auth
                        // Chemins d'accès publics
                        .requestMatchers("/api/diagnostic/**").permitAll() // Remplacement de antMatchers() par requestMatchers()
                        .requestMatchers("/api/test/**").permitAll()

                        // Chemins pour les ressources statiques Angular/Frontend
                        .requestMatchers( // Remplacement de antMatchers() par requestMatchers()
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/*.js",
                                "/*.css",
                                "/*.map",
                                "/*.txt",
                                "/*.json",
                                "/*.ico",
                                "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg",
                                "/*.woff", "/*.woff2", "/*.ttf", "/*.eot",
                                "/assets/**",
                                "/static/**"
                        ).permitAll()

                        // Autres chemins publics
                        .requestMatchers("/api/health", "/actuator/health").permitAll() // Remplacement de antMatchers() par requestMatchers()

                        // Chemins nécessitant une authentification simple
                        .requestMatchers("/api/files/download/**").authenticated() // Remplacement de antMatchers() par requestMatchers()
                        .requestMatchers("/api/files/view/**").authenticated()
                        .requestMatchers("/api/files/preview/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/auth/user-info").authenticated()
                        .requestMatchers("/api/auth/user-permissions").authenticated()

                        // Chemins nécessitant un rôle spécifique
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Remplacement de antMatchers() par requestMatchers()

                        // Toutes les autres API nécessitent une authentification
                        .requestMatchers("/api/**").authenticated()

                        // Autorise toutes les autres requêtes non-API (e.g. pour le frontend)
                        .anyRequest().permitAll()
                )

                // Configuration du serveur de ressources OAuth2 pour traiter les JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(customJwtAuthenticationConverter())
                                .decoder(jwtDecoder())
                        )
                )

                // Configuration des Headers (pour la sécurité)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self' " + allowedOrigins))
                )
                // Gestion des exceptions (remplace .exceptionHandling())
                .exceptionHandling(exceptions -> exceptions
                        // Gestion de l'échec d'authentification (jeton invalide/manquant)
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();

                            if (requestUri.startsWith("/api/")) {
                                // Affichage d'erreur pour les appels API
                                System.err.println("Erreur d'authentification API: " + authException.getMessage());
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write(String.format(
                                        "{\"error\":\"Authentication required\",\"message\":\"%s\",\"path\":\"%s\"}",
                                        authException.getMessage(),
                                        requestUri
                                ));
                            } else {
                                // Redirection/simple 401 pour les autres (typiquement le frontend)
                                response.setStatus(401);
                            }
                        })
                        // Gestion de l'accès refusé (permissions insuffisantes)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            System.err.println("Acces refuse: " + accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(String.format(
                                    "{\"error\":\"Access denied\",\"message\":\"Insufficient permissions\",\"path\":\"%s\"}",
                                    request.getRequestURI()
                            ));
                        })
                );

        // La méthode build() est nécessaire pour finaliser la configuration avec le DSL par lambda
        return http.build();
    }

    /**
     * Définit le bean de conversion personnalisé pour extraire les rôles/permissions du jeton JWT.
     */
    @Bean
    public CustomJwtAuthenticationConverter customJwtAuthenticationConverter() {
        try {
            return new CustomJwtAuthenticationConverter(userService, roleRepository);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de configurer CustomJwtAuthenticationConverter", e);
        }
    }

    /**
     * Définit le bean pour le décodage des jetons JWT, en utilisant l'URL d'émetteur Keycloak.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = keycloakServerUrl + "/realms/" + keycloakRealm;

        try {
            // Tente la découverte automatique via l'URL de l'émetteur
            return JwtDecoders.fromIssuerLocation(issuerUri);
        } catch (Exception e) {
            // Tente une configuration manuelle si la découverte échoue
            try {
                String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
                return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        // L'algorithme RS256 est standard pour Keycloak
                        .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                        .build();
            } catch (Exception ex) {
                throw new RuntimeException("Impossible de configurer JwtDecoder", ex);
            }
        }
    }

    /**
     * Fournit un AuthenticationManager pour l'intégration, si nécessaire (souvent utilisé pour les tests).
     * @param config configuration de l'authentification
     * @return AuthenticationManager
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Fournit le bean {@code JwtAuthenticationProvider} (facultatif car géré implicitement, mais conservé).
     * @return JwtAuthenticationProvider
     */
    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        try {
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder());
            provider.setJwtAuthenticationConverter(customJwtAuthenticationConverter());
            return provider;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de configurer JwtAuthenticationProvider", e);
        }
    }

    /**
     * Définit la source de configuration CORS pour autoriser l'accès depuis l'application frontend.
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Divise les origines autorisées et utilise setAllowedOriginPatterns (plus flexible)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
                "X-Auth-Token", "Cache-Control"
        ));

        // Headers exposés (pour la lecture par le client)
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Authorization"
        ));

        // Autorise les credentials (cookies, headers d'autorisation, etc.)
        configuration.setAllowCredentials(true);
        // Durée maximale de mise en cache du résultat du pré-vol (3600 secondes = 1 heure)
        configuration.setMaxAge(3600L);

        // Applique la configuration à tous les chemins ("/**")
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}