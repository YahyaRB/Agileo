package com.agileo.AGILEO.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agileo.AGILEO.Dtos.request.FileUploadRequestDTO;
import com.agileo.AGILEO.Dtos.response.FileResponseDTO;
import com.agileo.AGILEO.Dtos.response.FileGroupResponseDTO;
import com.agileo.AGILEO.Dtos.response.DemandeAchatFileResponseDTO;
import com.agileo.AGILEO.Dtos.response.UserResponseDTO;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileImpService implements FileService {
    private static final Logger log = LoggerFactory.getLogger(FileImpService.class);

    private final KdnFileRepository fileRepository;
    private final KdnFileGroupRepository fileGroupRepository;
    private final DemandeAchatRepository demandeAchatRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final UserServiceImpl userService;
    private final ReceptionRepository receptionRepository;

    @Value("${app.path.globalVariable}")
    private String ROOT_DIR;

    @Value("${app.remote.server.username:}")
    private String remoteUsername;

    @Value("${app.remote.server.password:}")
    private String remotePassword;

    @Value("${app.remote.server.domain:}")
    private String remoteDomain;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private Path globalStorageDir;
    private boolean isRemoteAccessVerified = false;

    public FileImpService(KdnFileRepository fileRepository,
                          KdnFileGroupRepository fileGroupRepository,
                          DemandeAchatRepository demandeAchatRepository,
                          KdnsAccessorRepository kdnsAccessorRepository,
                          UserServiceImpl userService,
                          ReceptionRepository receptionRepository) {
        this.fileRepository = fileRepository;
        this.fileGroupRepository = fileGroupRepository;
        this.demandeAchatRepository = demandeAchatRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.userService = userService;
        this.receptionRepository = receptionRepository;
    }

    @PostConstruct
    public void initializeDirectories() {
        try {
            log.info("=== CONFIGURATION FICHIERS SERVEUR DISTANT ===");
            log.info("Chemin configuré: {}", ROOT_DIR);

            // Tentative d'authentification si credentials fournis
            if (remoteUsername != null && !remoteUsername.isEmpty()) {
                log.info("Credentials détectés, tentative d'authentification...");
                authenticateToRemoteServer();
            } else {
                log.warn("Aucun credential configuré - accès en tant qu'utilisateur Windows courant");
            }

            // Initialiser le chemin
            globalStorageDir = Paths.get(ROOT_DIR).toAbsolutePath().normalize();
            log.info("Répertoire résolu: {}", globalStorageDir.toString());

            // Vérifier l'existence du répertoire
            if (!Files.exists(globalStorageDir)) {
                log.warn("Le répertoire n'existe pas, tentative de création...");
                try {
                    Files.createDirectories(globalStorageDir);
                    log.info("Répertoire créé avec succès");
                } catch (Exception e) {
                    log.error("Impossible de créer le répertoire: {}", e.getMessage());
                    throw new RuntimeException("Impossible de créer le répertoire distant: " + e.getMessage());
                }
            }

            // Test d'accès (non bloquant)
            testRemoteAccessAsync();

            log.info("Initialisation terminée (vérification d'accès en cours...)");
            log.info("=============================================");

        } catch (Exception e) {
            log.error("ERREUR lors de l'initialisation: {}", e.getMessage());
            log.warn("L'application démarre en mode dégradé");
            // Fallback sur un répertoire local
            globalStorageDir = Paths.get(System.getProperty("user.home"), "uploads-temp");
            log.warn("Utilisation du répertoire de secours: {}", globalStorageDir);
        }
    }

    /**
     * Authentification au serveur distant Windows via net use
     */
    private void authenticateToRemoteServer() {
        try {
            log.info("Tentative d'authentification au serveur distant...");

            // Construction de la commande net use
            String server = ROOT_DIR.replace("/", "\\");

            StringBuilder commandBuilder = new StringBuilder("net use \"");
            commandBuilder.append(server).append("\"");

            // Ajouter les credentials si fournis
            if (remoteUsername != null && !remoteUsername.isEmpty()) {
                commandBuilder.append(" /user:");
                if (remoteDomain != null && !remoteDomain.isEmpty()) {
                    commandBuilder.append(remoteDomain).append("\\");
                }
                commandBuilder.append(remoteUsername);

                if (remotePassword != null && !remotePassword.isEmpty()) {
                    commandBuilder.append(" ").append(remotePassword);
                }
            }

            commandBuilder.append(" /persistent:no");

            String command = commandBuilder.toString();
            log.debug("Exécution: net use [chemin] /user:[masqué]...");

            // Exécution de la commande
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Lecture de la sortie
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "Windows-1252"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Net use: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Authentification réussie au serveur distant");
            } else if (exitCode == 2) {
                log.info("Le partage est déjà connecté (code 2)");
            } else {
                log.warn("Code de sortie net use: {}. Output: {}", exitCode, output.toString().trim());
                log.warn("Le partage peut déjà être monté ou accessible sans credentials");
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'authentification: {}", e.getMessage());
            log.warn("Poursuite avec les droits Windows actuels...");
        }
    }

    /**
     * Test d'accès asynchrone (non bloquant)
     */
    private void testRemoteAccessAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Attendre 2 secondes
                ensureRemoteAccess();
                log.info("Test d'accès distant: SUCCÈS");
            } catch (Exception e) {
                log.error("Test d'accès distant: ÉCHEC - {}", e.getMessage());
                log.error("Les uploads vers le serveur distant échoueront");
            }
        }).start();
    }

    /**
     * Vérification lazy de l'accès distant
     */
    private void ensureRemoteAccess() throws IOException {
        if (isRemoteAccessVerified) {
            return;
        }

        try {
            log.info("Vérification de l'accès au serveur distant...");

            // Vérifier que le répertoire existe
            if (!Files.exists(globalStorageDir)) {
                log.info("Le répertoire n'existe pas, tentative de création...");
                Files.createDirectories(globalStorageDir);
            }

            // Test d'écriture
            Path testFile = globalStorageDir.resolve(".test_access_" + System.currentTimeMillis());
            Files.write(testFile, "test".getBytes());
            Files.delete(testFile);

            isRemoteAccessVerified = true;
            log.info("Accès au serveur distant vérifié avec succès");

        } catch (IOException e) {
            log.error("Impossible d'accéder au serveur distant: {}", e.getMessage());
            throw new IOException("Pas d'accès en écriture au serveur distant: " + ROOT_DIR +
                    "\nVérifiez: 1) Que le serveur est accessible, 2) Que vous avez les droits, 3) Que les credentials sont corrects", e);
        }
    }

    // ==================== GESTION DES GROUPES DE FICHIERS ====================

    @Override
    public FileGroupResponseDTO createFileGroupForDemande(Integer demandeAchatId, String currentUsername) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeAchatId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat introuvable: " + demandeAchatId));

        KdnFileGroup group = new KdnFileGroup();
        group.setStorageName("Public");
        group.setGroupState(1);
        group.setSysState(1);
        group.setSysCreationDate(LocalDateTime.now());
        group.setSysModificationDate(LocalDateTime.now());

        KdnFileGroup savedGroup = fileGroupRepository.save(group);
        demande.setPjDa(savedGroup.getGroupId());
        demandeAchatRepository.save(demande);

        return mapFileGroupToResponseDto(savedGroup);
    }

    @Override
    public FileGroupResponseDTO findFileGroupById(Integer groupId) {
        KdnFileGroup group = fileGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe de fichiers introuvable: " + groupId));
        return mapFileGroupToResponseDto(group);
    }

    // ==================== GESTION DES FICHIERS ====================

    @Override
    public FileResponseDTO uploadFile(MultipartFile file, Integer groupId, String description, String currentUsername) {
        try {
            // Vérifier l'accès au serveur distant
            ensureRemoteAccess();

            log.info("=== DÉBUT UPLOAD FICHIER ===");
            log.info("Fichier: {}", file.getOriginalFilename());
            log.info("Taille: {} bytes", file.getSize());
            log.info("Groupe ID: {}", groupId);

            validateFile(file);

            KdnFileGroup group = fileGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Groupe de fichiers introuvable: " + groupId));

            String hash = calculateFileHash(file.getBytes());
            Optional<KdnFile> existingFile = fileRepository.findByHashAndSysState(hash, 1);
            if (existingFile.isPresent()) {
                throw new BadRequestException("Ce fichier existe déjà dans le système");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String name = getFileNameWithoutExtension(originalFilename);

            Integer userId = getUserIdFromUsername(currentUsername);
            KdnFile kdnFile = new KdnFile();
            kdnFile.setGroupId(groupId);
            kdnFile.setName(name != null ? name : "fichier_sans_nom");
            kdnFile.setExtension(extension != null ? extension : "");
            kdnFile.setSize((int) file.getSize());
            kdnFile.setStorageName("Public");
            kdnFile.setHash(hash);
            kdnFile.setHashType("SHA1");
            kdnFile.setSysCreatorId(userId);
            kdnFile.setSysUserId(userId);
            kdnFile.setSysState(1);
            kdnFile.setNbOpen(0);
            kdnFile.setDocumentType(1);

            LocalDateTime now = LocalDateTime.now();
            kdnFile.setSysCreationDate(now);
            kdnFile.setSysModificationDate(now);

            KdnFile savedFile = fileRepository.save(kdnFile);

            String finalFileName = generateFileName(groupId, savedFile.getFileId(), extension);
            String savedFilePath = saveFileToGlobalStorage(file, finalFileName);

            savedFile.setFtpPath(finalFileName);
            savedFile.setTempPath(finalFileName);
            savedFile.setSysModificationDate(LocalDateTime.now());
            savedFile = fileRepository.save(savedFile);

            log.info("Fichier sauvé: ID={}, Nom={}", savedFile.getFileId(), finalFileName);
            log.info("=== FIN UPLOAD FICHIER ===");

            return mapFileToResponseDto(savedFile);

        } catch (IOException e) {
            log.error("Erreur IOException: {}", e.getMessage());
            throw new BadRequestException("Erreur d'accès au serveur distant: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur générale: {}", e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de l'upload du fichier: " + e.getMessage());
        }
    }

    @Override
    public FileResponseDTO findFileById(Integer fileId) {
        KdnFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable: " + fileId));
        return mapFileToResponseDto(file);
    }

    @Override
    public List<FileResponseDTO> findFilesByGroupId(Integer groupId) {
        List<KdnFile> files = fileRepository.findActiveFilesByGroupId(groupId);
        return files.stream()
                .map(this::mapFileToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseMessage deleteFile(Integer fileId, String currentUsername) {
        try {
            KdnFile file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable: " + fileId));

            log.info("Suppression du fichier ID: {}, Nom: {}", fileId, file.getFullFileName());

            file.setSysState(0);
            file.setSysModificationDate(LocalDateTime.now());
            fileRepository.save(file);

            try {
                deletePhysicalFile(file);
            } catch (Exception e) {
                log.warn("Impossible de supprimer le fichier physique: {}", e.getMessage());
            }

            log.info("Fichier supprimé avec succès: {}", fileId);
            return new ResponseMessage("Fichier supprimé avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du fichier {}: {}", fileId, e.getMessage());
            throw new BadRequestException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    private void deletePhysicalFile(KdnFile file) throws IOException {
        if (file.getFtpPath() != null && !file.getFtpPath().trim().isEmpty()) {
            Path filePath = globalStorageDir.resolve(file.getFtpPath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Fichier physique supprimé: {}", filePath);
            }
        }
    }

    @Override
    public byte[] downloadFile(Integer fileId, String currentUsername) {
        KdnFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable: " + fileId));

        try {
            Path filePath = null;

            if (file.getFtpPath() != null && !file.getFtpPath().trim().isEmpty()) {
                filePath = globalStorageDir.resolve(file.getFtpPath());
            } else if (file.getTempPath() != null && !file.getTempPath().trim().isEmpty()) {
                filePath = globalStorageDir.resolve(file.getTempPath());
            }

            if (filePath == null || !Files.exists(filePath)) {
                throw new BadRequestException("Fichier physique introuvable sur le disque: " +
                        (filePath != null ? filePath.toString() : "chemin null"));
            }

            file.setNbOpen(file.getNbOpen() + 1);
            file.setSysModificationDate(LocalDateTime.now());
            fileRepository.save(file);

            log.info("Téléchargement du fichier depuis: {}", filePath.toString());
            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier: {}", e.getMessage());
            throw new BadRequestException("Impossible de lire le fichier: " + e.getMessage());
        }
    }

    @Override
    public List<DemandeAchatFileResponseDTO> findFilesByDemandeAchat(Integer demandeAchatId) {
        try {
            DemandeAchat demande = demandeAchatRepository.findById(demandeAchatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat non trouvée: " + demandeAchatId));

            if (demande.getPjDa() == null) {
                return new ArrayList<>();
            }

            List<KdnFile> files = fileRepository.findActiveFilesByGroupId(demande.getPjDa());
            return files.stream().map(this::mapFileToDemandeAchatFileDto).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fichiers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public int getFileCountForDemande(Integer demandeAchatId) {
        try {
            DemandeAchat demande = demandeAchatRepository.findById(demandeAchatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat introuvable: " + demandeAchatId));

            if (demande.getPjDa() == null) {
                return 0;
            }

            List<KdnFile> files = fileRepository.findActiveFilesByGroupId(demande.getPjDa());
            return files.size();
        } catch (Exception e) {
            log.error("Erreur lors du comptage des fichiers pour la demande {}: {}", demandeAchatId, e.getMessage());
            return 0;
        }
    }

    // ==================== ASSOCIATION AVEC DEMANDES D'ACHAT ====================

    @Override
    @Transactional
    public ResponseMessage attachFilesToDemandeAchat(Integer demandeAchatId, List<MultipartFile> files, String currentUsername) {
        try {
            // Vérifier l'accès distant
            ensureRemoteAccess();

            log.info("=== DÉBUT UPLOAD FICHIERS DEMANDE VERS KDN_FILE ===");
            log.info("Demande ID: {}", demandeAchatId);
            log.info("Nombre de fichiers: {}", files.size());

            DemandeAchat demande = demandeAchatRepository.findById(demandeAchatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat introuvable: " + demandeAchatId));

            int fichiesExistants = 0;
            if (demande.getPjDa() != null) {
                List<KdnFile> existingFiles = fileRepository.findActiveFilesByGroupId(demande.getPjDa());
                fichiesExistants = existingFiles.size();
            }

            final int MAX_FILES_PER_DEMANDE = 3;

            if (fichiesExistants >= MAX_FILES_PER_DEMANDE) {
                throw new BadRequestException(
                        String.format("Limite atteinte: Maximum %d fichiers autorisés par demande d'achat. Cette demande a déjà %d fichier(s).",
                                MAX_FILES_PER_DEMANDE, fichiesExistants)
                );
            }

            if (fichiesExistants + files.size() > MAX_FILES_PER_DEMANDE) {
                int remaining = MAX_FILES_PER_DEMANDE - fichiesExistants;
                throw new BadRequestException(
                        String.format("Limite dépassée: Vous tentez d'ajouter %d fichier(s) mais seulement %d fichier(s) peuvent encore être ajoutés (maximum %d par demande).",
                                files.size(), remaining, MAX_FILES_PER_DEMANDE)
                );
            }

            Integer groupId = demande.getPjDa();
            if (groupId == null) {
                KdnFileGroup group = new KdnFileGroup();
                group.setStorageName("Public");
                group.setGroupState(1);
                group.setSysState(1);
                group.setSysCreationDate(LocalDateTime.now());
                group.setSysModificationDate(LocalDateTime.now());

                KdnFileGroup savedGroup = fileGroupRepository.save(group);
                groupId = savedGroup.getGroupId();

                demande.setPjDa(groupId);
                demandeAchatRepository.save(demande);

                log.info("Groupe KDN_FILEGROUP créé avec ID: {}", groupId);
            }

            List<String> uploadedFiles = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    validateFileForUpload(file);
                    String description = "Pièce jointe pour demande " + demandeAchatId;

                    FileResponseDTO uploadedFile = uploadFile(file, groupId, description, currentUsername);
                    uploadedFiles.add(uploadedFile.getFullFileName());
                    log.info("Fichier uploadé avec succès vers KDN_FILE: {}", uploadedFile.getFullFileName());

                } catch (Exception e) {
                    log.error("Erreur lors de l'upload du fichier {}: {}", file.getOriginalFilename(), e.getMessage());
                    errors.add(String.format("Fichier '%s': %s", file.getOriginalFilename(), e.getMessage()));
                }
            }

            StringBuilder messageBuilder = new StringBuilder();

            if (!uploadedFiles.isEmpty()) {
                messageBuilder.append(String.format("%d fichier(s) uploadé(s) avec succès vers KDN_FILE", uploadedFiles.size()));
            }

            if (!errors.isEmpty()) {
                if (messageBuilder.length() > 0) messageBuilder.append("\n");
                messageBuilder.append(String.format("%d erreur(s): %s", errors.size(), String.join("; ", errors)));
            }

            if (uploadedFiles.isEmpty() && !errors.isEmpty()) {
                throw new BadRequestException("Aucun fichier n'a pu être uploadé vers KDN_FILE. Erreurs: " + String.join("; ", errors));
            }

            int totalFichiers = fichiesExistants + uploadedFiles.size();
            messageBuilder.append(String.format("\nTotal: %d/%d fichiers pour cette demande", totalFichiers, MAX_FILES_PER_DEMANDE));

            log.info("=== FIN UPLOAD FICHIERS DEMANDE VERS KDN_FILE ===");
            return new ResponseMessage(messageBuilder.toString());

        } catch (IOException e) {
            log.error("Erreur d'accès au serveur distant: {}", e.getMessage());
            throw new BadRequestException("Impossible d'accéder au serveur distant. Vérifiez la connexion et les droits d'accès.");
        } catch (Exception e) {
            log.error("Erreur générale lors de l'upload vers KDN_FILE: {}", e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de l'upload des fichiers vers KDN_FILE: " + e.getMessage());
        }
    }

    @Override
    public ResponseMessage removeFileFromDemandeAchat(Integer demandeAchatId, Integer fileId, String currentUsername) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeAchatId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat introuvable: " + demandeAchatId));

        KdnFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable: " + fileId));

        if (!file.getGroupId().equals(demande.getPjDa())) {
            throw new BadRequestException("Ce fichier n'appartient pas à cette demande d'achat");
        }

        fileRepository.markAsDeleted(fileId);
        return new ResponseMessage("Fichier supprimé de la demande d'achat avec succès");
    }

    @Override
    public String saveFile(String typeDir, Integer demandeId, MultipartFile file, String generatedName) throws IOException {
        try {
            log.info("=== SAUVEGARDE FICHIER LEGACY ===");
            log.info("Fichier: {}", file.getOriginalFilename());

            String finalFileName = generatedName != null ? generatedName : file.getOriginalFilename();
            return saveFileToGlobalStorage(file, finalFileName);

        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseMessage attachFilesToReception(Integer receptionId, List<MultipartFile> files, String currentUsername) {
        try {
            log.info("=== DÉBUT attachFilesToReception ===");
            log.info("Reception ID: {}", receptionId);
            log.info("Nombre de fichiers: {}", files != null ? files.size() : 0);
            log.info("Utilisateur: {}", currentUsername);

            if (files == null || files.isEmpty()) {
                throw new BadRequestException("Aucun fichier fourni");
            }

            // Vérifier l'accès distant
            ensureRemoteAccess();

            // Récupérer la réception
            Reception reception = receptionRepository.findById(receptionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Réception non trouvée avec l'ID: " + receptionId));

            log.info("Réception trouvée: {}, pj_bc: {}", reception.getNumero(), reception.getPjBc());

            // Vérifier ou créer le groupe de fichiers
            KdnFileGroup fileGroup;
            if (reception.getPjBc() != null && reception.getPjBc() > 0) {
                log.info("Utilisation du groupe existant: {}", reception.getPjBc());
                fileGroup = fileGroupRepository.findById(reception.getPjBc())
                        .orElseThrow(() -> new ResourceNotFoundException("Groupe de fichiers non trouvé"));
            } else {
                log.info("Création d'un nouveau groupe de fichiers...");
                fileGroup = new KdnFileGroup();
                fileGroup.setStorageName("Public");
                fileGroup.setGroupState(1);
                fileGroup.setSysCreationDate(LocalDateTime.now());
                fileGroup.setSysModificationDate(LocalDateTime.now());
                fileGroup.setSysState(1);

                fileGroup = fileGroupRepository.save(fileGroup);
                log.info("Groupe créé avec ID: {}", fileGroup.getGroupId());

                // Mettre à jour la réception avec le nouveau groupe
                reception.setPjBc(fileGroup.getGroupId());
                reception = receptionRepository.save(reception);
                receptionRepository.flush();

                log.info("Réception mise à jour avec groupe: {}", fileGroup.getGroupId());
            }

            // Vérifier la limite de fichiers
            final int MAX_FILES = 3;
            List<KdnFile> existingFiles = fileRepository.findActiveFilesByGroupId(fileGroup.getGroupId());
            int currentFileCount = existingFiles.size();

            log.info("Fichiers existants: {}/{}", currentFileCount, MAX_FILES);

            if (currentFileCount >= MAX_FILES) {
                throw new BadRequestException(
                        String.format("Limite atteinte: Maximum %d fichiers autorisés. Cette réception a déjà %d fichier(s).",
                                MAX_FILES, currentFileCount)
                );
            }

            if (currentFileCount + files.size() > MAX_FILES) {
                int remaining = MAX_FILES - currentFileCount;
                throw new BadRequestException(
                        String.format("Limite dépassée: Vous tentez d'ajouter %d fichier(s) mais seulement %d peuvent être ajoutés.",
                                files.size(), remaining)
                );
            }

            // Upload des fichiers
            List<String> uploadedFiles = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                try {
                    log.info("Traitement fichier {}/{}: {}", i + 1, files.size(), file.getOriginalFilename());

                    validateFileForUpload(file);

                    // Créer l'entrée KdnFile
                    KdnFile kdnFile = new KdnFile();
                    kdnFile.setGroupId(fileGroup.getGroupId());
                    kdnFile.setName(getFileNameWithoutExtension(file.getOriginalFilename()));
                    kdnFile.setExtension(getFileExtension(file.getOriginalFilename()));
                    kdnFile.setSize((int) file.getSize());
                    kdnFile.setStorageName("Public");
                    kdnFile.setDocumentType(1);
                    kdnFile.setSysCreationDate(LocalDateTime.now());
                    kdnFile.setSysCreatorId(getUserIdFromUsername(currentUsername));
                    kdnFile.setSysModificationDate(LocalDateTime.now());
                    kdnFile.setSysUserId(getUserIdFromUsername(currentUsername));
                    kdnFile.setSysState(1);
                    kdnFile.setNbOpen(0);

                    // Sauvegarder en base pour obtenir l'ID
                    KdnFile savedFile = fileRepository.save(kdnFile);
                    log.info("Fichier enregistré en base avec ID: {}", savedFile.getFileId());

                    // Générer le nom de fichier et sauvegarder physiquement
                    String finalFileName = generateFileName(fileGroup.getGroupId(), savedFile.getFileId(), savedFile.getExtension());
                    log.info("Nom de fichier généré: {}", finalFileName);

                    String savedFilePath = saveFileToGlobalStorage(file, finalFileName);
                    log.info("Fichier sauvegardé physiquement: {}", savedFilePath);

                    // Mettre à jour avec le chemin
                    savedFile.setFtpPath(finalFileName);
                    savedFile.setTempPath(finalFileName);
                    savedFile = fileRepository.save(savedFile);

                    uploadedFiles.add(file.getOriginalFilename());
                    log.info("✅ Fichier {} uploadé avec succès", file.getOriginalFilename());

                } catch (Exception e) {
                    log.error("❌ Erreur lors du traitement du fichier {}: {}",
                            file.getOriginalFilename(), e.getMessage());
                    log.error("Stack trace:", e);
                    errors.add(String.format("Fichier '%s': %s", file.getOriginalFilename(), e.getMessage()));
                }
            }

            // Construire le message de réponse
            StringBuilder messageBuilder = new StringBuilder();

            if (!uploadedFiles.isEmpty()) {
                messageBuilder.append(String.format("%d fichier(s) uploadé(s) avec succès", uploadedFiles.size()));
            }

            if (!errors.isEmpty()) {
                if (messageBuilder.length() > 0) messageBuilder.append("\n");
                messageBuilder.append(String.format("%d erreur(s): %s", errors.size(), String.join("; ", errors)));
            }

            if (uploadedFiles.isEmpty() && !errors.isEmpty()) {
                throw new BadRequestException("Aucun fichier n'a pu être uploadé. Erreurs: " + String.join("; ", errors));
            }

            String finalMessage = messageBuilder.toString();
            log.info("=== FIN attachFilesToReception ===");
            log.info("Résultat: {}", finalMessage);

            return new ResponseMessage(finalMessage);

        } catch (BadRequestException | ResourceNotFoundException e) {
            log.error("Erreur métier: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("Erreur d'accès au serveur distant: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new RuntimeException("Impossible d'accéder au serveur distant: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("ERREUR GÉNÉRALE dans attachFilesToReception: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new RuntimeException("Erreur lors de l'upload des fichiers: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DemandeAchatFileResponseDTO> findFilesByReception(Integer receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Réception introuvable: " + receptionId));

        if (reception.getPjBc() == null) {
            return new ArrayList<>();
        }

        List<KdnFile> files = fileRepository.findActiveFilesByGroupId(reception.getPjBc());
        return files.stream()
                .map(this::mapFileToReceptionFileDto)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseMessage removeFileFromReception(Integer receptionId, Integer fileId, String currentUsername) {
        Reception reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Réception introuvable: " + receptionId));

        KdnFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable: " + fileId));

        if (!file.getGroupId().equals(reception.getPjBc())) {
            throw new BadRequestException("Ce fichier n'appartient pas à cette réception");
        }

        fileRepository.markAsDeleted(fileId);
        return new ResponseMessage("Fichier supprimé de la réception avec succès");
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private String generateFileName(Integer groupId, Integer fileId, String extension) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(groupId).append("_").append(fileId);

        if (extension != null && !extension.trim().isEmpty()) {
            fileName.append(".").append(extension);
        }

        return fileName.toString();
    }

    private String saveFileToGlobalStorage(MultipartFile file, String fileName) throws IOException {
        Path fullPath = globalStorageDir.resolve(fileName);

        int counter = 1;
        while (Files.exists(fullPath)) {
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String nameWithoutExt = fileName.substring(0, lastDotIndex);
                String ext = fileName.substring(lastDotIndex);
                fullPath = globalStorageDir.resolve(nameWithoutExt + "_" + counter + ext);
            } else {
                fullPath = globalStorageDir.resolve(fileName + "_" + counter);
            }
            counter++;
        }

        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Fichier sauvé dans le répertoire global: {}", fullPath);
        return fullPath.getFileName().toString();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Le fichier est trop volumineux (max: 50MB)");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BadRequestException("Le nom du fichier est invalide");
        }
    }

    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme SHA-1 non disponible", e);
        }
    }

    private String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private Integer getUserIdFromUsername(String username) {
        try {
            UserResponseDTO user = userService.findUserByLogin(username);
            if (user != null && user.getIdAgelio() != null) {
                return Integer.parseInt(user.getIdAgelio());
            }
            return 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private void validateFileForUpload(MultipartFile file) {
        if (file == null) {
            throw new BadRequestException("Fichier null détecté");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("Fichier vide: " + file.getOriginalFilename());
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("Fichier trop volumineux: %s (%d bytes > %d max)",
                            file.getOriginalFilename(), file.getSize(), MAX_FILE_SIZE)
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BadRequestException("Nom de fichier manquant ou invalide");
        }

        String extension = getFileExtension(filename);
        List<String> extensionsAutorisees = Arrays.asList("pdf", "xls", "xlsx", "png", "jpg", "jpeg", "doc", "docx");
        if (!extensionsAutorisees.contains(extension.toLowerCase())) {
            throw new BadRequestException("Extension non autorisée: " + extension + " (fichier: " + filename + ")");
        }

        log.info("Fichier validé: {} ({} bytes, .{})", filename, file.getSize(), extension);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ==================== MÉTHODES DE MAPPING ====================

    private FileResponseDTO mapFileToResponseDto(KdnFile file) {
        FileResponseDTO dto = new FileResponseDTO();
        dto.setFileId(file.getFileId());
        dto.setGroupId(file.getGroupId());
        dto.setName(file.getName());
        dto.setExtension(file.getExtension());
        dto.setFullFileName(file.getFullFileName());
        dto.setAlt(file.getAlt());
        dto.setSize(file.getSize());
        dto.setSizeFormatted(file.getSizeFormatted());
        dto.setNbOpen(file.getNbOpen());
        dto.setStorageName(file.getStorageName());
        dto.setHash(file.getHash());
        dto.setHashType(file.getHashType());
        dto.setTempPath(file.getTempPath());
        dto.setSysCreationDate(file.getSysCreationDate());
        dto.setSysCreatorId(file.getSysCreatorId());
        dto.setSysModificationDate(file.getSysModificationDate());
        dto.setSysUserId(file.getSysUserId());
        dto.setSysState(file.getSysState());
        dto.setDownloadUrl("/api/files/" + file.getFileId() + "/download");
        dto.setCanDelete(file.getSysState() == 1);
        dto.setCanDownload(file.getSysState() == 1);

        if (file.getSysCreatorId() != null) {
            Optional<KdnsAccessor> creator = kdnsAccessorRepository.findById(file.getSysCreatorId());
            if (creator.isPresent()) {
                dto.setCreateurNom(formatUserName(creator.get()));
                dto.setCreateurLogin(creator.get().getLogin());
            }
        }

        return dto;
    }

    private FileGroupResponseDTO mapFileGroupToResponseDto(KdnFileGroup group) {
        FileGroupResponseDTO dto = new FileGroupResponseDTO();
        dto.setGroupId(group.getGroupId());
        dto.setStorageName(group.getStorageName());
        dto.setGroupState(group.getGroupState());
        dto.setSysState(group.getSysState());
        dto.setSysCreationDate(group.getSysCreationDate());
        dto.setSysModificationDate(group.getSysModificationDate());
        dto.setActive(group.isActive());

        Long fileCount = fileRepository.countActiveFilesByGroupId(group.getGroupId());
        Long totalSize = fileRepository.getTotalSizeByGroupId(group.getGroupId());
        dto.setFileCount(fileCount.intValue());
        dto.setTotalSize(totalSize);
        dto.setTotalSizeFormatted(formatFileSize(totalSize));

        return dto;
    }

    private DemandeAchatFileResponseDTO mapFileToDemandeAchatFileDto(KdnFile file) {
        DemandeAchatFileResponseDTO dto = new DemandeAchatFileResponseDTO();
        dto.setFileId(file.getFileId());
        dto.setName(file.getName());
        dto.setExtension(file.getExtension());
        dto.setFullFileName(file.getFullFileName());
        dto.setSize(file.getSize());
        dto.setSizeFormatted(file.getSizeFormatted());
        dto.setAlt(file.getAlt());
        dto.setUploadDate(file.getSysCreationDate());
        dto.setNbOpen(file.getNbOpen());
        dto.setDownloadUrl("/api/files/" + file.getFileId() + "/download");
        dto.setCanDelete(file.getSysState() == 1);
        dto.setCanDownload(file.getSysState() == 1);
        dto.setCategory(file.getAlt());
        dto.setDocumentType("Pièce jointe");

        if (file.getSysCreatorId() != null) {
            Optional<KdnsAccessor> creator = kdnsAccessorRepository.findById(file.getSysCreatorId());
            if (creator.isPresent()) {
                dto.setUploadedByNom(formatUserName(creator.get()));
                dto.setUploadedBy(creator.get().getLogin());
            }
        }

        return dto;
    }

    private DemandeAchatFileResponseDTO mapFileToReceptionFileDto(KdnFile file) {
        DemandeAchatFileResponseDTO dto = new DemandeAchatFileResponseDTO();
        dto.setFileId(file.getFileId());
        dto.setName(file.getName());
        dto.setExtension(file.getExtension());
        dto.setFullFileName(file.getFullFileName());
        dto.setSize(file.getSize());
        dto.setSizeFormatted(file.getSizeFormatted());
        dto.setAlt(file.getAlt());
        dto.setUploadDate(file.getSysCreationDate());
        dto.setNbOpen(file.getNbOpen());
        dto.setDownloadUrl("/api/receptions/files/" + file.getFileId() + "/download");
        dto.setCanDelete(file.getSysState() == 1);
        dto.setCanDownload(file.getSysState() == 1);
        dto.setCategory(file.getAlt());
        dto.setDocumentType("Pièce jointe réception");

        if (file.getSysCreatorId() != null) {
            Optional<KdnsAccessor> creator = kdnsAccessorRepository.findById(file.getSysCreatorId());
            if (creator.isPresent()) {
                dto.setUploadedByNom(formatUserName(creator.get()));
                dto.setUploadedBy(creator.get().getLogin());
            }
        }

        return dto;
    }

    private String formatUserName(KdnsAccessor accessor) {
        if (accessor.getFullName() != null && !accessor.getFullName().trim().isEmpty()) {
            return accessor.getFullName();
        }
        String firstName = accessor.getFirstName() != null ? accessor.getFirstName() : "";
        String lastName = accessor.getLastName() != null ? accessor.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    // ==================== MÉTHODES PUBLIQUES UTILITAIRES ====================

    public String getRootDirectory() {
        return ROOT_DIR;
    }

    public Map<String, String> getDirectoryPaths() {
        Map<String, String> paths = new HashMap<>();
        paths.put("global", globalStorageDir != null ? globalStorageDir.toString() : "non initialisé");
        paths.put("verified", String.valueOf(isRemoteAccessVerified));
        return paths;
    }

    /**
     * Méthode de diagnostic pour vérifier l'état de la connexion
     */
    public Map<String, Object> checkRemoteStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            status.put("configured_path", ROOT_DIR);
            status.put("resolved_path", globalStorageDir != null ? globalStorageDir.toString() : "null");
            status.put("exists", globalStorageDir != null && Files.exists(globalStorageDir));
            status.put("readable", globalStorageDir != null && Files.isReadable(globalStorageDir));
            status.put("writable", globalStorageDir != null && Files.isWritable(globalStorageDir));
            status.put("verified", isRemoteAccessVerified);

            if (globalStorageDir != null && Files.exists(globalStorageDir)) {
                Path testFile = globalStorageDir.resolve(".test_" + System.currentTimeMillis());
                try {
                    Files.write(testFile, "test".getBytes());
                    Files.delete(testFile);
                    status.put("write_test", "SUCCESS");
                } catch (Exception e) {
                    status.put("write_test", "FAILED: " + e.getMessage());
                }
            } else {
                status.put("write_test", "SKIPPED - Directory not accessible");
            }

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        return status;
    }
}