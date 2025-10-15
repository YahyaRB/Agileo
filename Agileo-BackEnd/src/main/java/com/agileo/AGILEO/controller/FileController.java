// ==================== FileController.java ====================
package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.FileUploadRequestDTO;
import com.agileo.AGILEO.Dtos.response.FileResponseDTO;
import com.agileo.AGILEO.Dtos.response.FileGroupResponseDTO;
import com.agileo.AGILEO.Dtos.response.DemandeAchatFileResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.service.FileService;
import com.agileo.AGILEO.service.Impl.FileImpService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // ==================== Endpoints pour les groupes de fichiers ====================

    @PostMapping("/groups/demande/{demandeAchatId}")
    public ResponseEntity<FileGroupResponseDTO> createFileGroupForDemande(
            @PathVariable Integer demandeAchatId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        FileGroupResponseDTO group = fileService.createFileGroupForDemande(demandeAchatId, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<FileGroupResponseDTO> getFileGroup(@PathVariable Integer groupId) {
        FileGroupResponseDTO group = fileService.findFileGroupById(groupId);
        return ResponseEntity.ok(group);
    }

    // ==================== Endpoints pour les fichiers individuels ====================

    @PostMapping("/upload")
    public ResponseEntity<FileResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("groupId") Integer groupId,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        FileResponseDTO uploadedFile = fileService.uploadFile(file, groupId, description, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponseDTO> getFile(@PathVariable Integer fileId) {
        FileResponseDTO file = fileService.findFileById(fileId);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<FileResponseDTO>> getFilesByGroup(@PathVariable Integer groupId) {
        List<FileResponseDTO> files = fileService.findFilesByGroupId(groupId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Integer fileId,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            byte[] fileData = fileService.downloadFile(fileId, currentUsername);

            // Récupérer les informations du fichier pour le nom
            FileResponseDTO fileInfo = fileService.findFileById(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileInfo.getFullFileName());
            headers.setContentLength(fileData.length);

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ResponseMessage> deleteFile(
            @PathVariable Integer fileId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        ResponseMessage response = fileService.deleteFile(fileId, currentUsername);
        return ResponseEntity.ok(response);
    }

    // ==================== Endpoints spécifiques aux demandes d'achat ====================

    @PostMapping("/demandes-achat/{demandeAchatId}/upload")
    public ResponseEntity<ResponseMessage> uploadFilesForDemandeAchat(
            @PathVariable Integer demandeAchatId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        ResponseMessage response = fileService.attachFilesToDemandeAchat(demandeAchatId, files, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/demandes-achat/{demandeAchatId}")
    public ResponseEntity<List<DemandeAchatFileResponseDTO>> getFilesByDemandeAchat(
            @PathVariable Integer demandeAchatId) {
        List<DemandeAchatFileResponseDTO> files = fileService.findFilesByDemandeAchat(demandeAchatId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/demandes-achat/{demandeAchatId}/files/{fileId}")
    public ResponseEntity<ResponseMessage> removeFileFromDemandeAchat(
            @PathVariable Integer demandeAchatId,
            @PathVariable Integer fileId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "system";
        ResponseMessage response = fileService.removeFileFromDemandeAchat(demandeAchatId, fileId, currentUsername);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/remote-status")
    public ResponseEntity<Map<String, Object>> checkRemoteStatus() {
        try {
            FileImpService fileImpService = (FileImpService) fileService;
            Map<String, Object> status = fileImpService.checkRemoteStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}