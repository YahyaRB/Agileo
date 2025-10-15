package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class UserResponseDTO {
    private Long id;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime dernierConnexion;
    private String matricule;
    private String login;
    private String nom;
    private String prenom;
    private String email;
    private Boolean statut;
    private String idAgelio;
    private Set<RoleResponseDTO> roles;
    private List<AccessResponseDTO> acces;


}