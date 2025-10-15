package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

@Data
public class AffaireStatsDTO {
    private Long totalAffaires;
    private Long totalAssignments;
    private Long totalActiveUsers;
    private Long totalInactiveAssignments;
    private AffaireMostUsed affaireMostUsed;
    private UserMostAssigned userMostAssigned;

    @Data
    public static class AffaireMostUsed {
        private String affaire;
        private String libelle;
        private Long assignmentCount;
    }

    @Data
    public static class UserMostAssigned {
        private Integer accessoirId;
        private String fullName;
        private Long assignmentCount;
    }
}