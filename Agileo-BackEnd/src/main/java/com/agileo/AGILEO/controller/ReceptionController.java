package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.DemandeAchat;
import com.agileo.AGILEO.entity.primary.Reception;
import com.agileo.AGILEO.entity.secondary.PieceJointe;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.ReceptionRepository;
import com.agileo.AGILEO.repository.secondary.PieceJointeRepository;
import com.agileo.AGILEO.service.FileService;
import com.agileo.AGILEO.service.Impl.ReceptionServiceImpl;
import com.agileo.AGILEO.service.ReceptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/receptions")
public class ReceptionController {

    private final ReceptionService receptionService;
    private final FileService fileService; // CORRECTION: FileService au lieu de FileController
    private final PieceJointeRepository pieceJointeRepository;
    private final ReceptionRepository receptionRepository;
    private static final Logger log = LoggerFactory.getLogger(ReceptionController.class);
    public ReceptionController(ReceptionService receptionService, FileService fileService, PieceJointeRepository pieceJointeRepository, ReceptionRepository receptionRepository) {
        this.receptionService = receptionService;
        this.fileService = fileService;
        this.pieceJointeRepository = pieceJointeRepository;
        this.receptionRepository = receptionRepository;
    }
    @PostMapping("/{receptionId}/files/upload")
    public ResponseEntity<ResponseMessage> uploadFilesForReception(
            @PathVariable Integer receptionId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {

        try {
            log.info("=== UPLOAD FICHIERS RÉCEPTION ===");
            log.info("Réception ID: {}", receptionId);
            log.info("Nombre de fichiers reçus: {}", files != null ? files.size() : 0);

            if (files == null || files.isEmpty()) {
                log.error("Aucun fichier reçu dans la requête");
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Aucun fichier fourni"));
            }

            // Log détaillé des fichiers
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                log.info("Fichier {}: {} ({} bytes)",
                        i + 1,
                        file.getOriginalFilename(),
                        file.getSize());
            }

            String currentUsername = authentication != null ?
                    authentication.getName() : "system";

            log.info("Utilisateur: {}", currentUsername);

            // CORRECTION : Utiliser fileService au lieu de receptionService
            ResponseMessage response = fileService.attachFilesToReception(
                    receptionId,
                    files,
                    currentUsername
            );

            log.info("Upload terminé avec succès: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Erreur de validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage("Erreur de validation: " + e.getMessage()));

        } catch (Exception e) {
            log.error("ERREUR lors de l'upload des fichiers pour la réception {}: {}",
                    receptionId, e.getMessage());
            log.error("Stack trace:", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur serveur: " + e.getMessage()));
        }
    }

// AJOUTER ces méthodes pour la gestion complète des fichiers KDN_FILE :

    @GetMapping("/{receptionId}/files")
    public ResponseEntity<List<DemandeAchatFileResponseDTO>> getFilesByReception(
            @PathVariable Integer receptionId) {

        try {
            log.info("Récupération des fichiers pour la réception: {}", receptionId);

            // CORRECTION : Utiliser fileService au lieu de receptionService
            List<DemandeAchatFileResponseDTO> files =
                    fileService.findFilesByReception(receptionId);

            log.info("Nombre de fichiers trouvés: {}", files.size());
            return ResponseEntity.ok(files);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fichiers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{receptionId}/pieces")
    public ResponseEntity<List<PieceJointeDTO>> getPiecesByReception(@PathVariable Integer receptionId) {
        try {
            System.out.println("🔍 Récupération des pièces jointes (ancien système) pour réception: " + receptionId);

            List<PieceJointe> pieces = pieceJointeRepository.findByReceptionId(receptionId);

            System.out.println("📄 Pièces jointes trouvées: " + pieces.size());
            for (PieceJointe piece : pieces) {
                System.out.println("   - " + piece.getNom() + " (ID: " + piece.getId() + ")");
            }

            List<PieceJointeDTO> result = pieces.stream()
                    .map(pj -> new PieceJointeDTO(
                            pj.getId(),
                            pj.getNom(),
                            pj.getType(),
                            pj.getUrl()
                    ))
                    .collect(Collectors.toList()); // CHANGÉ .toList() en .collect(Collectors.toList()) pour compatibilité

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des pièces jointes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }


    @PostMapping
    public ResponseEntity<ReceptionResponseDTO> createReception(
            @RequestBody ReceptionRequestDTO receptionDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(receptionService.createReception(receptionDto, currentUsername));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    //Les pieces joints
    @PostMapping("/upload/{receptionId}")
    public ResponseEntity<? > uploadFiles(
            @PathVariable Integer receptionId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("generatedNames") String[] generatedNames
    ) {
        try {
            Reception reception = receptionRepository.findById(receptionId)
                    .orElseThrow(() -> new RuntimeException("Reception non trouvée"));
            System.out.println("Files's length is = "+files.length);
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String generatedName = "RC_"+receptionId+"_" + generatedNames[i];
                String filePath = fileService.saveFile("receptions", receptionId, file, generatedName);
                PieceJointe pj = new PieceJointe();
                pj.setNom(generatedName);
                pj.setType(file.getContentType());
                pj.setUrl(filePath);
                pj.setReceptionId(reception.getNumero());
                pieceJointeRepository.save(pj);
            }
            return ResponseEntity.ok().body("Fichiers uploadés avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload : " + e.getMessage());
        }
    }

    @DeleteMapping("/pieces/{pieceId}")
    public ResponseEntity<Map<String, String>> deletePieceJointe(@PathVariable Long pieceId) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<PieceJointe> pieceOptional = pieceJointeRepository.findById(pieceId);
            if (!pieceOptional.isPresent()) {
                response.put("status", "error");
                response.put("message", "Pièce jointe non trouvée avec l'ID: " + pieceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            PieceJointe pieceJointe = pieceOptional.get();
            String filePath = pieceJointe.getUrl();
            System.out.println("Tentative de suppression de la pièce jointe:");
            System.out.println("ID: " + pieceJointe.getId());
            System.out.println("Nom: " + pieceJointe.getNom());
            System.out.println("Chemin: " + filePath);
            boolean fileDeleted = false;
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    Path path = Paths.get(filePath);
                    if (Files.exists(path)) {
                        Files.delete(path);
                        fileDeleted = true;
                        System.out.println("Fichier supprimé avec succès: " + path.toString());
                    } else {
                        System.out.println("Fichier non trouvé: " + path.toString());
                        fileDeleted = true;
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
                    response.put("status", "warning");
                    response.put("message", "Pièce jointe supprimée de la base de données mais erreur lors de la suppression du fichier: " + e.getMessage());
                }
            }
            pieceJointeRepository.deleteById(pieceId);
            System.out.println("Pièce jointe supprimée de la base de données");
            if (response.isEmpty()) {
                response.put("status", "success");
                response.put("message", "Pièce jointe supprimée avec succès (base de données et fichier)");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la pièce jointe: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Erreur interne lors de la suppression: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/pieces/{pieceId}/download")
    public ResponseEntity<Resource> downloadPiece(@PathVariable Long pieceId) {
        try {
            System.out.println("📥 Téléchargement de la pièce jointe: " + pieceId);

            PieceJointe pj = pieceJointeRepository.findById(pieceId)
                    .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

            System.out.println("📁 Pièce trouvée: " + pj.getNom() + " à " + pj.getUrl());

            Path path = Paths.get(pj.getUrl());

            if (!Files.exists(path)) {
                System.err.println("❌ Fichier physique non trouvé: " + path.toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(pj.getType() != null ? pj.getType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pj.getNom() + "\"")
                    .body(resource);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du téléchargement de la pièce jointe " + pieceId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




    @GetMapping("/pieces/{pieceId}/view")
    public ResponseEntity<Resource> viewPiece(@PathVariable Long pieceId) {
        try {
            System.out.println("👁️ Prévisualisation de la pièce jointe: " + pieceId);

            PieceJointe pj = pieceJointeRepository.findById(pieceId)
                    .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

            System.out.println("📁 Pièce trouvée: " + pj.getNom() + " à " + pj.getUrl());

            Path path = Paths.get(pj.getUrl());

            if (!Files.exists(path)) {
                System.err.println("❌ Fichier physique non trouvé: " + path.toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(pj.getType() != null ? pj.getType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pj.getNom() + "\"")
                    .body(resource);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la prévisualisation de la pièce jointe " + pieceId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ReceptionResponseDTO>> getAllReceptions(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isManager = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("MANAGER"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isManager || isConsulteur) {
                List<ReceptionResponseDTO> allReceptions = receptionService.getAllReceptions();
                return ResponseEntity.ok(allReceptions);
            } else {
                String username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                List<ReceptionResponseDTO> userReceptions = receptionService.getCurrentUserReceptions(username);
                return ResponseEntity.ok(userReceptions);
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(Collections.emptyList());
        } catch (BadRequestException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réceptions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/paginated")
    public ResponseEntity<PagedResponse<ReceptionResponseDTO>> getAllReceptionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "numero") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search,  // ✅ AJOUT
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isManager = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("MANAGER"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isManager || isConsulteur) {
                PagedResponse<ReceptionResponseDTO> response =
                        receptionService.getAllReceptionsPaginated(page, size, sortBy, sortDirection, search);  // ✅ PASSAGE DU PARAMÈTRE
                return ResponseEntity.ok(response);
            } else {
                String username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                PagedResponse<ReceptionResponseDTO> userReceptions =
                        receptionService.getCurrentUserReceptionsPaginated(page, size, sortBy, sortDirection, username, search);  // ✅ PASSAGE DU PARAMÈTRE
                return ResponseEntity.ok(userReceptions);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réceptions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceptionResponseDTO> getReceptionById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(receptionService.getReceptionById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/affaire/{affaireId}")
    public ResponseEntity<List<ReceptionResponseDTO>> getReceptionsByAffaire(@PathVariable Integer affaireId) {
        try {
            return ResponseEntity.ok(receptionService.getReceptionsByAffaire(affaireId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/current/receptions")
    public ResponseEntity<List<ReceptionResponseDTO>> getCurrentUserReceptions(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String username = authentication.getName();
            return ResponseEntity.ok(receptionService.getCurrentUserReceptions(username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateReception(
            @PathVariable Integer id,
            @RequestBody ReceptionRequestDTO receptionDto,
            Authentication authentication) {
        System.out.println("Reception vienne de front end : "+receptionDto.toString());
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            return ResponseEntity.ok(receptionService.updateReception(id, receptionDto, currentUsername));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteReception(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(receptionService.deleteReception(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTION DES ARTICLES DISPONIBLES ====================

    @GetMapping("/{receptionId}/articles-disponibles")
    public ResponseEntity<List<ArticleDisponibleDTO>> getArticlesDisponibles(@PathVariable Integer receptionId) {
        try {
            return ResponseEntity.ok(receptionService.getArticlesDisponibles(receptionId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{receptionId}/valider-quantite")
    public ResponseEntity<ValidationQuantiteDTO> validerQuantiteArticle(
            @PathVariable Integer receptionId,
            @RequestBody ValidateQuantiteRequestDTO request) {
        try {
            ValidationQuantiteDTO validation = receptionService.validerQuantiteArticle(
                    receptionId,
                    request.getReferenceArticle(),
                    request.getQuantiteDemandee(),
                    request.getLigneReceptionId()
            );
            return ResponseEntity.ok(validation);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTION DES LIGNES DE RÉCEPTION ====================

    @PostMapping("/{receptionId}/lignes")
    public ResponseEntity<ResponseMessage> addLignesReception(
            @PathVariable Integer receptionId,
            @RequestBody List<LigneReceptionRequestDTO> lignesDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            return ResponseEntity.ok(receptionService.addLignesReception(receptionId, lignesDto, currentUsername));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{receptionId}/lignes")
    public ResponseEntity<List<LigneReceptionResponseDTO>> getLignesReception(@PathVariable Integer receptionId) {
        try {
            return ResponseEntity.ok(receptionService.getLignesReceptionByReceptionId(receptionId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> updateLigneReception(
            @PathVariable Integer ligneId,
            @RequestBody LigneReceptionRequestDTO ligneDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            return ResponseEntity.ok(receptionService.updateLigneReception(ligneId, ligneDto, currentUsername));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> deleteLigneReception(
            @PathVariable Integer ligneId,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            return ResponseEntity.ok(receptionService.deleteLigneReception(ligneId, currentUsername));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{receptionId}/files/validate")
    public ResponseEntity<ResponseMessage> validateFilesBeforeUpload(
            @PathVariable Integer receptionId,
            @RequestParam Integer fileCount) {

        try {
            // CORRECTION : Utiliser fileService
            List<DemandeAchatFileResponseDTO> existingFiles =
                    fileService.findFilesByReception(receptionId);

            int currentCount = existingFiles.size();
            int maxFiles = 3;

            if (currentCount + fileCount > maxFiles) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage(
                                String.format("Limite dépassée: %d fichiers existants + %d nouveaux > %d maximum",
                                        currentCount, fileCount, maxFiles)
                        ));
            }

            return ResponseEntity.ok(
                    new ResponseMessage("Validation OK - " + (maxFiles - currentCount) + " emplacements disponibles")
            );

        } catch (Exception e) {
            log.error("Erreur validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{receptionId}/files/{fileId}")
    public ResponseEntity<ResponseMessage> removeFileFromReception(
            @PathVariable Integer receptionId,
            @PathVariable Integer fileId,
            Authentication authentication) {

        try {
            String currentUsername = authentication != null ?
                    authentication.getName() : "system";

            log.info("Suppression du fichier {} de la réception {} par {}",
                    fileId, receptionId, currentUsername);

            // CORRECTION : Utiliser fileService
            ResponseMessage response = fileService.removeFileFromReception(
                    receptionId,
                    fileId,
                    currentUsername
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur suppression fichier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFileFromReception(
            @PathVariable Integer fileId,
            Authentication authentication) {

        try {
            String currentUsername = authentication != null ?
                    authentication.getName() : "system";

            log.info("Téléchargement du fichier {} par {}", fileId, currentUsername);

            // CORRECTION : Utiliser fileService
            byte[] fileData = fileService.downloadFile(fileId, currentUsername);

            // Récupérer les informations du fichier
            var fileInfo = fileService.findFileById(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileInfo.getFullFileName());
            headers.setContentLength(fileData.length);

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erreur téléchargement fichier {}: {}", fileId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint pour changer le statut d'une réception
     */
    @PutMapping("/{id}/statut")
    public ResponseEntity<ResponseMessage> updateReceptionStatut(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {  // ✅ CHANGEMENT ICI

        System.out.println("========================================");
        System.out.println("🎯 DÉBUT ENDPOINT /statut");
        System.out.println("ID reçu: " + id);
        System.out.println("Request body: " + request);

        try {
            System.out.println("✅ Extraction du statut...");
            Integer newStatut = request.get("statut");
            System.out.println("Nouveau statut: " + newStatut);

            if (newStatut == null) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Le statut est obligatoire"));
            }

            // ✅ MÊME PATTERN QUE DemandeAchatController
            String currentUsername = authentication != null ? authentication.getName() : "system";
            System.out.println("✅ Username: " + currentUsername);

            System.out.println("✅ Appel du service...");
            ResponseMessage response = receptionService.updateReceptionStatut(id, newStatut, currentUsername);

            System.out.println("✅ SUCCESS - Retour du service OK");
            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            System.err.println("❌ BadRequestException: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseMessage(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            System.err.println("❌ ResourceNotFoundException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseMessage(e.getMessage()));
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌❌❌ ERREUR CAPTURÉE ❌❌❌");
            System.err.println("Message: " + e.getMessage());
            System.err.println("Type: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("========================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur: " + e.getMessage()));
        }
    }
}