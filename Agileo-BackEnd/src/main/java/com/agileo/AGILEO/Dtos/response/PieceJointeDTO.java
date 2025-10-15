package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieceJointeDTO {
    private Long id;
    private String nom;
    private String url;
    private Long taille;
    private String type;
    private LocalDateTime dateUpload;

    public PieceJointeDTO(Long id, String nom, String type) {
        this.id = id;
        this.nom = nom;
        this.type = type;

    }

    public PieceJointeDTO(Long id, String nom, String type, String url) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.url = url;

    }
}
