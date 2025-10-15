package com.agileo.AGILEO.Dtos.response;

public class FamilleStatistiqueDTO {
    private String code;
    private String designation;

    public FamilleStatistiqueDTO() {
    }

    public FamilleStatistiqueDTO(String code, String designation) {
        this.code = code;
        this.designation = designation;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @Override
    public String toString() {
        return "FamilleStatistiqueDTO{" +
                "code='" + code + '\'' +
                ", designation='" + designation + '\'' +
                '}';
    }
}