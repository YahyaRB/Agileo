package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.DemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.request.LigneDemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.DemandeAchat;
import com.agileo.AGILEO.entity.secondary.PieceJointe;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.DemandeAchatRepository;
import com.agileo.AGILEO.repository.secondary.PieceJointeRepository;
import com.agileo.AGILEO.service.DemandeAchatService;
import com.agileo.AGILEO.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/demandes-achat")
public class DemandeAchatController {
    private final DemandeAchatService demandeAchatService;
    private final FileService fileService; // CORRECTION: FileService au lieu de FileController
    private final PieceJointeRepository pieceJointeRepository;
    private final DemandeAchatRepository demandeAchatRepository;

    public DemandeAchatController(DemandeAchatService demandeAchatService, FileService fileService, PieceJointeRepository pieceJointeRepository, DemandeAchatRepository demandeAchatRepository) {
        this.demandeAchatService = demandeAchatService;
        this.fileService = fileService;
        this.pieceJointeRepository = pieceJointeRepository;
        this.demandeAchatRepository = demandeAchatRepository;
    }

    @PostMapping
    public ResponseEntity<DemandeAchatResponseDTO> createDemandeAchat(
            @RequestBody DemandeAchatRequestDTO demandeDto,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeAchatService.createDemandeAchat(demandeDto, currentUsername));
    }
//Les pieces joints
@PostMapping("/upload/{demandeId}")
public ResponseEntity<? > uploadFiles(
        @PathVariable Integer demandeId,
        @RequestParam("files") MultipartFile[] files,
        @RequestParam("generatedNames") String[] generatedNames
) {
    try {
        DemandeAchat demandeAchat = demandeAchatRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        System.out.println("Files's length is = "+files.length);
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String generatedName = "DA_"+demandeId+"_" + generatedNames[i];
            String filePath = fileService.saveFile("demandes-achat", demandeId, file, generatedName);
            PieceJointe pj = new PieceJointe();
            pj.setNom(generatedName);
            pj.setType(file.getContentType());
            pj.setUrl(filePath);
            pj.setDemandeAchatId(demandeAchat.getId());
            pieceJointeRepository.save(pj);
        }
        return ResponseEntity.ok().body("Fichiers uploadés avec succès");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de l'upload : " + e.getMessage());
    }
}
    @GetMapping("/{demandeId}/pieces")
    public ResponseEntity<List<PieceJointeDTO>> getPiecesByDemande(@PathVariable Integer demandeId) {
        try {
            List<PieceJointe> pieces = pieceJointeRepository.findByDemandeAchatId(demandeId);
            for (PieceJointe piece : pieces) {
                System.out.println("Le path : " + piece.getUrl());
            }
            List<PieceJointeDTO> result = pieces.stream()
                    .map(pj -> new PieceJointeDTO(
                            pj.getId(),
                            pj.getNom(),
                            pj.getType(),
                            pj.getUrl()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
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
        PieceJointe pj = pieceJointeRepository.findById(pieceId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));
        try {
            Path path = Paths.get(pj.getUrl());
            Resource resource = new UrlResource(path.toUri());

            // EXTRACTION DU NOM ORIGINAL depuis le nom généré
            String originalFileName = extractOriginalFileName(pj.getNom());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(pj.getType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/pieces/{pieceId}/view")
    public ResponseEntity<Resource> viewPiece(@PathVariable Long pieceId) {
        PieceJointe pj = pieceJointeRepository.findById(pieceId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));
        try {
            Path path = Paths.get(pj.getUrl());
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(pj.getType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pj.getNom() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping
    public ResponseEntity<List<DemandeAchatResponseDTO>> getAllDemandes(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isConsulteur) {
                List<DemandeAchatResponseDTO> allDemandes = demandeAchatService.findAllDemandes();
                return ResponseEntity.ok(allDemandes);
            } else {
                String username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }

                System.out.println("Recherche des demandes pour l'utilisateur: " + username);
                List<DemandeAchatResponseDTO> userDemandes = demandeAchatService.findDemandesByLogin(username);
                System.out.println("Nombre de demandes trouvées: " + userDemandes.size());

                return ResponseEntity.ok(userDemandes);
            }
        } catch (ResourceNotFoundException e) {
            System.err.println("Utilisateur non trouvé: " + e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        } catch (BadRequestException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des demandes d'achat: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    //Liste de demandes d'achat avec pagination
    @GetMapping("/paginated")
    public ResponseEntity<PagedResponse<DemandeAchatResponseDTO>> getAllDemandesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            Authentication authentication
            ) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));
            if (isAdmin || isConsulteur) {
                PagedResponse<DemandeAchatResponseDTO> response =
                        demandeAchatService.findAllDemandesPaginated(page, size, sortBy, sortDirection);
                return ResponseEntity.ok(response);
            } else {
                String username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                PagedResponse<DemandeAchatResponseDTO> userDemandes =
                        demandeAchatService.findAllDemandesByLoginPaginated(page, size, sortBy, sortDirection, username);
                return ResponseEntity.ok(userDemandes);
            }
        } catch (ResourceNotFoundException e) {
            System.err.println("Utilisateur non trouvé: " + e.getMessage());
            return ResponseEntity.ok((PagedResponse<DemandeAchatResponseDTO>) Collections.emptyList());
        } catch (BadRequestException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((PagedResponse<DemandeAchatResponseDTO>) Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des demandes d'achat: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeAchatResponseDTO> getDemandeById(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeAchatService.findDemandeById(id));
    }

    @GetMapping("/login/{login}")
    public ResponseEntity<List<DemandeAchatResponseDTO>> getDemandesByLogin(@PathVariable String login) {
        return ResponseEntity.ok(demandeAchatService.findDemandesByLogin(login));
    }

    @GetMapping("/chantier/{chantier}")
    public ResponseEntity<List<DemandeAchatResponseDTO>> getDemandesByChantier(@PathVariable String chantier) {
        return ResponseEntity.ok(demandeAchatService.findDemandesByChantier(chantier));
    }

    @GetMapping("/numda/{numDa}")
    public ResponseEntity<DemandeAchatResponseDTO> getDemandeByNumDa(@PathVariable String numDa) {
        return ResponseEntity.ok(demandeAchatService.findDemandeByNumDa(numDa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateDemandeAchat(
            @PathVariable Integer id,
            @RequestBody DemandeAchatRequestDTO demandeDto,
            Authentication authentication) {
        return ResponseEntity.ok(
                demandeAchatService.updateDemandeAchat(id, demandeDto, authentication.getName())
        );
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<ResponseMessage> updateDemandeAchatStatut(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";

            ResponseMessage response = demandeAchatService.updateDemandeAchatStatut(id, currentUsername);

            System.out.println("Statut mis à jour avec succès");
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            System.err.println("Demande non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseMessage("Demande d'achat non trouvée: " + e.getMessage()));
        } catch (BadRequestException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseMessage(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erreur interne: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur lors de la mise à jour du statut"));
        }
    }


    @PutMapping("/{id}/statut/{newStatus}")
    public ResponseEntity<ResponseMessage> updateDemandeAchatStatut(
            @PathVariable Integer id,
            @PathVariable Integer newStatus,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";

            // Validation du statut
            if (newStatus < -1 || newStatus > 3) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Statut invalide. Valeurs acceptées: -1 (Rejeté), 0 (Brouillon), 1 (Envoyé), 2 (Reçu), 3 (Approuvé)"));
            }

            ResponseMessage response = demandeAchatService.updateDemandeAchatStatut(id, newStatus, currentUsername);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseMessage("Demande d'achat non trouvée"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseMessage(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur lors de la mise à jour du statut"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteDemandeAchat(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeAchatService.deleteDemandeAchat(id));
    }

    // ==================== ENDPOINTS POUR LES FICHIERS ====================
    /**
     * Validation avant upload (endpoint de pré-validation)
     */
    @PostMapping("/{demandeId}/files/validate")
    public ResponseEntity<Map<String, Object>> validateFilesBeforeUpload(
            @PathVariable Integer demandeId,
            @RequestParam("fileCount") Integer fileCount) {
        try {
            System.out.println("Validation avant upload - Demande: " + demandeId + ", Fichiers: " + fileCount);

            DemandeAchat demande = demandeAchatRepository.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            Map<String, Object> validation = new HashMap<>();

            // Vérifier que la demande n'est pas envoyée
            if (demande.getStatut() != null && demande.getStatut() != 0) {
                validation.put("valid", false);
                validation.put("error", "Impossible d'ajouter des fichiers à une demande envoyée");
                return ResponseEntity.ok(validation);
            }

            // Compter les fichiers existants
            int existingFiles = 0;
            if (demande.getPjDa() != null) {
                List<DemandeAchatFileResponseDTO> files = demandeAchatService.getDemandeFiles(demandeId);
                existingFiles = files.size();
            }

            final int MAX_FILES = 3;

            if (existingFiles >= MAX_FILES) {
                validation.put("valid", false);
                validation.put("error", String.format("Limite atteinte: Maximum %d fichiers autorisés", MAX_FILES));
                return ResponseEntity.ok(validation);
            }

            if (existingFiles + fileCount > MAX_FILES) {
                int remaining = MAX_FILES - existingFiles;
                validation.put("valid", false);
                validation.put("error", String.format("Limite dépassée: Vous pouvez ajouter au maximum %d fichier(s) supplémentaire(s)", remaining));
                validation.put("maxAllowed", remaining);
                return ResponseEntity.ok(validation);
            }

            validation.put("valid", true);
            validation.put("message", String.format("Validation OK: %d fichier(s) peuvent être ajoutés", fileCount));
            validation.put("currentCount", existingFiles);
            validation.put("newTotal", existingFiles + fileCount);

            return ResponseEntity.ok(validation);

        } catch (Exception e) {
            System.err.println("Erreur validation upload: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", "Erreur lors de la validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    /**
     * Vérifier le nombre de fichiers et la capacité d'upload pour une demande
     */
    @GetMapping("/{demandeId}/files/info")
    public ResponseEntity<Map<String, Object>> getFilesInfo(@PathVariable Integer demandeId) {
        try {
            System.out.println("Récupération des informations fichiers pour demande: " + demandeId);

            // Vérifier que la demande existe
            DemandeAchat demande = demandeAchatRepository.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            // Compter les fichiers existants
            int fileCount = 0;
            if (demande.getPjDa() != null) {
                List<DemandeAchatFileResponseDTO> files = demandeAchatService.getDemandeFiles(demandeId);
                fileCount = files.size();
            }

            final int MAX_FILES = 3;
            int remainingSlots = Math.max(0, MAX_FILES - fileCount);
            boolean canUpload = remainingSlots > 0 && (demande.getStatut() == null || demande.getStatut() == 0);

            Map<String, Object> info = new HashMap<>();
            info.put("currentFileCount", fileCount);
            info.put("maxFiles", MAX_FILES);
            info.put("remainingSlots", remainingSlots);
            info.put("canUpload", canUpload);
            info.put("isDemandeEnvoyee", demande.getStatut() != null && demande.getStatut() != 0);
            info.put("limitReached", fileCount >= MAX_FILES);

            System.out.println("Informations fichiers: " + info);
            return ResponseEntity.ok(info);

        } catch (Exception e) {
            System.err.println("Erreur récupération infos fichiers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Upload de fichiers pour une demande d'achat
    @PostMapping("/{demandeId}/files/upload")
    public ResponseEntity<ResponseMessage> uploadFilesForDemande(
            @PathVariable Integer demandeId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        try {
            System.out.println("=== DÉBUT ENDPOINT UPLOAD AVEC VALIDATION ===");
            System.out.println("ID de la demande reçu: " + demandeId);
            System.out.println("Nombre de fichiers reçus: " + (files != null ? files.size() : "null"));

            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Aucun fichier fourni"));
            }

            if (demandeId == null || demandeId <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("ID de demande invalide"));
            }

            // Validation préliminaire avec l'endpoint de validation
            ResponseEntity<Map<String, Object>> validationResult = validateFilesBeforeUpload(demandeId, files.size());

            if (validationResult.getStatusCode() != HttpStatus.OK) {
                return ResponseEntity.status(validationResult.getStatusCode())
                        .body(new ResponseMessage("Erreur de validation"));
            }

            Map<String, Object> validationData = validationResult.getBody();
            if (validationData == null || !(Boolean) validationData.get("valid")) {
                String errorMessage = validationData != null ? (String) validationData.get("error") : "Validation échouée";
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage(errorMessage));
            }

            String currentUsername = authentication != null ? authentication.getName() : "system";
            System.out.println("Utilisateur authentifié: " + currentUsername);

            // UTILISATION DU SYSTÈME KDN_FILE avec validation
            ResponseMessage response = fileService.attachFilesToDemandeAchat(demandeId, files, currentUsername);

            System.out.println("Fichiers attachés avec succès au système KDN_FILE");
            System.out.println("=== FIN ENDPOINT UPLOAD AVEC VALIDATION ===");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResourceNotFoundException e) {
            System.err.println("RESOURCE_NOT_FOUND: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseMessage("Demande d'achat non trouvée: " + e.getMessage()));
        } catch (BadRequestException e) {
            System.err.println("BAD_REQUEST: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseMessage(e.getMessage()));
        } catch (Exception e) {
            System.err.println("INTERNAL_ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur interne du serveur: " + e.getMessage()));
        }
    }

    // Récupérer tous les fichiers d'une demande d'achat
    @GetMapping("/{demandeId}/files")
    public ResponseEntity<List<DemandeAchatFileResponseDTO>> getFilesByDemande(
            @PathVariable Integer demandeId) {
        try {
            List<DemandeAchatFileResponseDTO> files = demandeAchatService.getDemandeFiles(demandeId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Dans DemandeAchatController.java
    @DeleteMapping("/{demandeId}/files/{fileId}")
    public ResponseEntity<ResponseMessage> deleteFileFromDemande(
            @PathVariable Integer demandeId,
            @PathVariable Integer fileId,
            Authentication authentication) {
        try {
            System.out.println("=== SUPPRESSION FICHIER ===");
            System.out.println("Demande ID: " + demandeId);
            System.out.println("Fichier ID: " + fileId);

            String currentUsername = authentication != null ? authentication.getName() : "system";

            // Vérifier que le fichier appartient à cette demande
            DemandeAchat demande = demandeAchatRepository.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            if (demande.getPjDa() == null) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Aucun groupe de fichiers associé à cette demande"));
            }

            // Vérifier que le fichier existe et appartient au bon groupe
            FileResponseDTO fileInfo = fileService.findFileById(fileId);
            if (!fileInfo.getGroupId().equals(demande.getPjDa())) {
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage("Ce fichier n'appartient pas à cette demande"));
            }

            // Supprimer le fichier
            ResponseMessage response = fileService.deleteFile(fileId, currentUsername);

            System.out.println("Fichier supprimé avec succès");
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            System.err.println("Fichier non trouvé: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseMessage("Fichier non trouvé"));
        } catch (Exception e) {
            System.err.println("Erreur suppression: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Erreur lors de la suppression du fichier: " + e.getMessage()));
        }
    }

    // ------------------- Lignes DemandeAchat -------------------
    @PostMapping("/{demandeId}/lignes")
    public ResponseEntity<ResponseMessage> addLigneDemande(
            @PathVariable Integer demandeId,
            @RequestBody LigneDemandeAchatRequestDTO ligneDto,
            Authentication authentication) {
        return ResponseEntity.ok(
                demandeAchatService.addLignesDemande(demandeId, ligneDto, authentication.getName())
        );
    }

    @GetMapping("/{demandeId}/lignes")
    public ResponseEntity<List<LigneDemandeAchatResponseDTO>> getLignesDemande(
            @PathVariable Integer demandeId) {
        return ResponseEntity.ok(
                demandeAchatService.findLignesDemandeByDemandeId(demandeId)
        );
    }

    @PutMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> updateLigneDemande(
            @PathVariable Integer ligneId,
            @RequestBody LigneDemandeAchatRequestDTO ligneDto,
            Authentication authentication) {
        return ResponseEntity.ok(
                demandeAchatService.updateLigneDemande(ligneId, ligneDto, authentication.getName())
        );
    }
    // Télécharger un fichier d'une demande d'achat
// Télécharger un fichier d'une demande d'achat
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFileFromDemande(
            @PathVariable Integer fileId,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";

            // Utiliser le FileService pour télécharger
            byte[] fileData = fileService.downloadFile(fileId, currentUsername);
            FileResponseDTO fileInfo = fileService.findFileById(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // Le nom à afficher est NAME.EXTENSION
            String displayName = fileInfo.getName();
            if (fileInfo.getExtension() != null && !fileInfo.getExtension().isEmpty()) {
                displayName += "." + fileInfo.getExtension();
            }
            headers.setContentDispositionFormData("attachment", displayName);
            headers.setContentLength(fileData.length);

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Erreur téléchargement: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> deleteLigneDemande(
            @PathVariable Integer ligneId,
            Authentication authentication) {
        return ResponseEntity.ok(
                demandeAchatService.deleteLigneDemande(ligneId, authentication.getName())
        );
    }
    private String extractOriginalFileName(String generatedName) {
        if (generatedName == null || generatedName.isEmpty()) {
            return "fichier";
        }

        // Le format généré est : DA_{demandeId}_{nomOriginal}
        // Exemple: "DA_15941_13-FL-DAM-03 JAM-SI.xls" -> "13-FL-DAM-03 JAM-SI.xls"

        String pattern = "^DA_\\d+_(.+)$";
        if (generatedName.matches(pattern)) {
            // Extraire tout ce qui suit "DA_{id}_"
            return generatedName.replaceFirst("^DA_\\d+_", "");
        }

        // Si le format ne correspond pas, retourner le nom tel quel
        return generatedName;
    }
}