package com.agileo.AGILEO.Dtos.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public class UserRequestDTO {
    @NotBlank
    private String matricule;

    @NotBlank
    @Size(min = 3, max = 20)
    private String login;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    @Email
    private String email;


    private Boolean statut;
    private String idAgelio;

    private Set<Long> roleIds;
    private Set<Long> accesIds;
    private Set<Long> affaireIds;

    // Getters and Setters
    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getStatut() {
        return statut;
    }

    public void setStatut(Boolean statut) {
        this.statut = statut;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public Set<Long> getAccesIds() {
        return accesIds;
    }

    public void setAccesIds(Set<Long> accesIds) {
        this.accesIds = accesIds;
    }

    public Set<Long> getAffaireIds() {
        return affaireIds;
    }

    public void setAffaireIds(Set<Long> affaireIds) {
        this.affaireIds = affaireIds;
    }
}