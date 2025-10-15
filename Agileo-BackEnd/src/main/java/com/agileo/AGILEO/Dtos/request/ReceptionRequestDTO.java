package com.agileo.AGILEO.Dtos.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ReceptionRequestDTO {
    private Object affaireId;
    private Object commandeCode;
    private LocalDateTime dateReception;
    private String referenceBl;
    private LocalDateTime dateBl;
    private String refFournisseur;
    private String nomFournisseur;
    private String idAgelio;
    private String statut;
}
