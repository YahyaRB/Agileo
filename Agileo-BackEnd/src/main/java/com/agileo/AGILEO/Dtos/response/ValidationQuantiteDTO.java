package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationQuantiteDTO {
    private boolean valide;
    private String message;
    private BigDecimal quantiteMaximale;
    private BigDecimal quantiteActuelle;
}