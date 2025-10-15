package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointeResponseDTO  {
    private Long id;
    private String nom;
    private String type;
    private String url;
    private String downloadUrl;
    private Long taille;

}