package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.util.List;

@Data
public class AffaireDetailsDTO {
    private String affaire;
    private String libelle;
    private List<UserAssignmentDTO> assignedUsers;
    private Integer totalUsers;

    @Data
    public static class UserAssignmentDTO {
        private Integer accessoirId;
        private String fullName;
        private String login;
        private String email;
        private String commentaire;
        private java.time.LocalDateTime assignmentDate;
        private Integer assignedBy;
        private String assignedByName;
    }
}