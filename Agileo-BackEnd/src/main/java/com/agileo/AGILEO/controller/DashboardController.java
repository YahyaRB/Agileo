package com.agileo.AGILEO.controller;


import com.agileo.AGILEO.Dtos.response.ConsommationStatsDTO;
import com.agileo.AGILEO.Dtos.response.DaStatsDTO;
import com.agileo.AGILEO.Dtos.response.DashboardDTO;
import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.service.DashboardService;
import com.agileo.AGILEO.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    /**
     * Récupère les statistiques pour l'admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDTO> getStatistiquesAdmin() {
        DashboardDTO stats = dashboardService.getStatistiquesAdmin();
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les statistiques pour un utilisateur (chef de projet/magasinier)
     */
    @GetMapping("/user/{accessorId}")
    @PreAuthorize("hasAnyRole('USER', 'CHEF_PROJET', 'MAGASINIER')")
    public ResponseEntity<DashboardDTO> getStatistiquesUtilisateur(@PathVariable Integer accessorId) {
        DashboardDTO stats = dashboardService.getStatistiquesUtilisateur(accessorId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les statistiques pour l'utilisateur connecté
     */
    /**
     * Récupère les statistiques pour l'utilisateur connecté
     */
    @GetMapping("/me")
    public ResponseEntity<DashboardDTO> getMesStatistiques(
            @RequestParam(required = false) Integer accessorId,
            @RequestParam(required = false) String role) {

        // Si pas d'accessorId fourni, utiliser une valeur par défaut ou récupérer du token
        if (accessorId == null) {
            // Pour le moment, retourner les stats admin par défaut
            // Vous pourrez récupérer l'ID depuis le JWT plus tard
            DashboardDTO stats = dashboardService.getStatistiquesAdmin();
            return ResponseEntity.ok(stats);
        }

        // Si admin, retourne toutes les stats
        if ("ADMIN".equals(role)) {
            DashboardDTO stats = dashboardService.getStatistiquesAdmin();
            return ResponseEntity.ok(stats);
        }

        // Sinon, stats filtrées par utilisateur
        DashboardDTO stats = dashboardService.getStatistiquesUtilisateur(accessorId);
        return ResponseEntity.ok(stats);
    }
    @GetMapping("/stats/consommation/global")
    @PreAuthorize("hasAnyRole('MAGASINIER', 'ADMIN')") // Rôles qui voient le global
    public ResponseEntity<ConsommationStatsDTO> getConsommationStatsGlobal() {
        return ResponseEntity.ok(dashboardService.getConsommationStatsGlobal());
    }

    @GetMapping("/stats/consommation/user")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés
    public ResponseEntity<ConsommationStatsDTO> getConsommationStatsByUser(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getConsommationStatsByLogin(authentication.getName()));
    }

    // --- Stats Demande Achat ---

    @GetMapping("/stats/da/global")
    @PreAuthorize("hasAnyRole('MAGASINIER', 'ADMIN')") // Rôles qui voient le global
    public ResponseEntity<DaStatsDTO> getDaStatsGlobal() {
        return ResponseEntity.ok(dashboardService.getDaStatsGlobal());
    }

    @GetMapping("/stats/da/user")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés
    public ResponseEntity<DaStatsDTO> getDaStatsByUser(Authentication authentication) {
      /*  return ResponseEntity.ok(dashboardService.getDaStatsByLogin(authentication.getName()));*/
    return null;
    }
    @GetMapping("/stats/TotalDAUser")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés
    public ResponseEntity<Long> getTotalDaStatsByUser(Authentication authentication) {
        User user=userService.getUserByLogin(authentication.getName());
        return ResponseEntity.ok(dashboardService.countTotalDAByLogin(Integer.valueOf(user.getIdAgelio())));
    }
    @GetMapping("/stats/TotalConsommationUser")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés
    public ResponseEntity<Long> getTotalConsommationStatsByUser(Authentication authentication) {
        User user=userService.getUserByLogin(authentication.getName());
        return ResponseEntity.ok(dashboardService.countTotalConsommationByLogin(Integer.valueOf(user.getIdAgelio())));
    }
    @GetMapping("/stats/TotalReceptionUser")
    @PreAuthorize("isAuthenticated()") // Tous les utilisateurs connectés
    public ResponseEntity<Long> getTotalReceptionStatsByUser(Authentication authentication) {
        User user=userService.getUserByLogin(authentication.getName());
        return ResponseEntity.ok(dashboardService.countTotalReceptionByLogin(Integer.valueOf(user.getIdAgelio())));
    }
}