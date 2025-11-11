package com.agileo.AGILEO.Dtos.response;

import java.util.List;

public class UserActivityStatsDTO {
    private List<MonthlyCountDTO> demandesAchat;
    private List<MonthlyCountDTO> receptions;
    private List<MonthlyCountDTO> consommations;

    public List<MonthlyCountDTO> getDemandesAchat() {
        return demandesAchat;
    }

    public void setDemandesAchat(List<MonthlyCountDTO> demandesAchat) {
        this.demandesAchat = demandesAchat;
    }

    public List<MonthlyCountDTO> getReceptions() {
        return receptions;
    }

    public void setReceptions(List<MonthlyCountDTO> receptions) {
        this.receptions = receptions;
    }

    public List<MonthlyCountDTO> getConsommations() {
        return consommations;
    }

    public void setConsommations(List<MonthlyCountDTO> consommations) {
        this.consommations = consommations;
    }
}

