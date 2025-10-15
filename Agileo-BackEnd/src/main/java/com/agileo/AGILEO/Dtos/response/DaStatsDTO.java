package com.agileo.AGILEO.Dtos.response;

public class DaStatsDTO {
    private Long totalDemandes;
    private Long enAttente;
    private Long approuvees;
    private Long rejetee;

    public Long getTotalDemandes() {
        return totalDemandes;
    }

    public void setTotalDemandes(Long totalDemandes) {
        this.totalDemandes = totalDemandes;
    }

    public Long getEnAttente() {
        return enAttente;
    }

    public void setEnAttente(Long enAttente) {
        this.enAttente = enAttente;
    }

    public Long getApprouvees() {
        return approuvees;
    }

    public void setApprouvees(Long approuvees) {
        this.approuvees = approuvees;
    }

    public Long getRejetee() {
        return rejetee;
    }

    public void setRejetee(Long rejetee) {
        this.rejetee = rejetee;
    }
}
