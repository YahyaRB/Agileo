package com.agileo.AGILEO.Dtos.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValidateQuantiteRequestDTO {
    private String referenceArticle;
    private BigDecimal quantiteDemandee;
    private Integer ligneReceptionId;
}
