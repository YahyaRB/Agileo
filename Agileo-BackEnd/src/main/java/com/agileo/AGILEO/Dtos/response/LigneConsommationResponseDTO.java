package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LigneConsommationResponseDTO {
    private Integer id;
    private String referenceArticle;
    private String designationArticle;
    private Double quantite;
    private String unite;
    private String familleStatistique1;
    private String familleStatistique2;
    private LocalDateTime sysCreationDate;
    private LocalDateTime sysModificationDate;
}