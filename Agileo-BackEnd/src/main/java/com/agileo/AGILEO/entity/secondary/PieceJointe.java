package com.agileo.AGILEO.entity.secondary;

import com.agileo.AGILEO.entity.primary.Consommation;
import com.agileo.AGILEO.entity.primary.DemandeAchat;
import com.agileo.AGILEO.entity.primary.Reception;
import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class PieceJointe extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String type;
    private String url;

    private Long taille ;

    private LocalDateTime dateUpload;
    @Column(name = "consommation_id")
    private Integer consommationId;

    @Column(name = "demande_achat_id")
    private Integer demandeAchatId;

    @Column(name = "reception_id")
    private Integer receptionId;



}