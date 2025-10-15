package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.response.ArticleDisponibleDTO;
import com.agileo.AGILEO.Dtos.response.ConsommationResponseDTO;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.service.CommandesService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@RequestMapping("/api/commandes")
public class CommandeController {
    private CommandesService commandesService;

    @GetMapping("/lignesByCommande/{id}")
    public ResponseEntity<List<ArticleDisponibleDTO>> getAllLignesByCommande(@PathVariable Long id) {
        try {
            List<ArticleDisponibleDTO> lignes = commandesService.getArticlesDisponibles(id);
            System.out.println(lignes);
            return ResponseEntity.ok(lignes);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur récupération Ligne de bon de commande: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    }
