package com.agileo.AGILEO.Dtos.response;

public class ConsommationStatsDTO {
    private Long totalConsommations;
    private Long totalLignes;

    public Long getTotalConsommations() {
        return totalConsommations;
    }

    public void setTotalConsommations(Long totalConsommations) {
        this.totalConsommations = totalConsommations;
    }

    public Long getTotalLignes() {
        return totalLignes;
    }

    public void setTotalLignes(Long totalLignes) {
        this.totalLignes = totalLignes;
    }
}
