package com.agileo.AGILEO.Dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointeRequestDTO {
    private String nom;
    private String type;
    private String description;
}