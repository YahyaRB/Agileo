package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AffaireReceptionResponseDTO {
    private String articleId;

    private String designation;

    private Long commande;

    private String unite;

    private String affaireCode;

    private String fournisseurRef;

    private BigDecimal qteCommandee;

    private BigDecimal qteLivree;

    private BigDecimal qteRest;
}
