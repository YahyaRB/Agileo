
package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.FileUploadRequestDTO;
import com.agileo.AGILEO.Dtos.response.FileResponseDTO;
import com.agileo.AGILEO.Dtos.response.FileGroupResponseDTO;
import com.agileo.AGILEO.Dtos.response.DemandeAchatFileResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {

    // ==================== Gestion des groupes de fichiers ====================
    FileGroupResponseDTO createFileGroupForDemande(Integer demandeAchatId, String currentUsername);
    FileGroupResponseDTO findFileGroupById(Integer groupId);

    // ==================== Gestion des fichiers ====================
    FileResponseDTO uploadFile(MultipartFile file, Integer groupId, String description, String currentUsername);
    FileResponseDTO findFileById(Integer fileId);
    List<FileResponseDTO> findFilesByGroupId(Integer groupId);
    ResponseMessage deleteFile(Integer fileId, String currentUsername);
    byte[] downloadFile(Integer fileId, String currentUsername);

    // ==================== Association avec demandes d'achat ====================

    List<DemandeAchatFileResponseDTO> findFilesByDemandeAchat(Integer demandeAchatId);
    ResponseMessage removeFileFromDemandeAchat(Integer demandeAchatId, Integer fileId, String currentUsername);
    public List<DemandeAchatFileResponseDTO> findFilesByReception(Integer receptionId);
    public ResponseMessage attachFilesToReception(Integer receptionId, List<MultipartFile> files, String currentUsername);
    @Transactional
    public ResponseMessage attachFilesToDemandeAchat(Integer demandeAchatId, List<MultipartFile> files, String currentUsername) ;
    public ResponseMessage removeFileFromReception(Integer receptionId, Integer fileId, String currentUsername);
    String saveFile(String typeDir,Integer demandeId, MultipartFile file, String generatedName) throws IOException;
}