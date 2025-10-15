package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.RoleRequestDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;

import java.util.List;
import java.util.Set;

public interface RoleService {
    RoleResponseDTO createRole(RoleRequestDTO roleDto, String currentUsername);
    ResponseMessage deleteRole(Long id);
    List<RoleResponseDTO> findAllRoles();
    RoleResponseDTO findRoleById(Long id);
}