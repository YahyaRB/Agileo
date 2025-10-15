package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.AccessRequestDTO;
import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;

import java.util.List;
import java.util.Set;

public interface AccesService {
    AccessResponseDTO createAccess(AccessRequestDTO accessDto, String currentUsername);
    ResponseMessage updateAccess(Long id, AccessRequestDTO accessDto, String currentUsername);
    ResponseMessage deleteAccess(Long id);
    List<AccessResponseDTO> findAllAccesses();
    AccessResponseDTO findAccessById(Long id);

    Set<String> getUserAccessCodes(Long userId);
    ResponseMessage addAccessToUser(Long userId, Long accessId, String currentUsername);
    ResponseMessage removeAccessFromUser(Long userId, Long accessId, String currentUsername);
    boolean hasAccess(Long userId, String accessCode);
}