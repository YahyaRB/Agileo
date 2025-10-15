package com.agileo.AGILEO.Dtos.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsommationRequestDTO {
    private String affaireId;
    private LocalDateTime dateConsommation;
    private String commentaire;
    private String refInterne;
}