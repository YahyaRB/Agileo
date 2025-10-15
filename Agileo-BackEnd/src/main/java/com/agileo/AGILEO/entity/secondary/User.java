package com.agileo.AGILEO.entity.secondary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Data
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "login"),
                @UniqueConstraint(columnNames = "email")
        })
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String matricule;
    private String login;
    private String nom;
    private String prenom;
    private String email;
    private LocalDateTime dernierConnex;
    private Boolean statut;
    private String idAgelio;


    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(name = "keycloak_enabled")
    private Boolean keycloakEnabled = true;



    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_acces",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "acces_id"))
    private List<Acces> acces = new LinkedList<>();





    public User() {
    }

    public User(String matricule, String login, String nom, String prenom,
                String email, String password, LocalDateTime dernierConnex, Boolean statut , String idAgelio) {
        this.matricule = matricule;
        this.login = login;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.dernierConnex = dernierConnex;
        this.statut = statut;
        this.idAgelio = idAgelio;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        // NEVER include collections in hashCode - causes infinite recursion
        return Objects.hash(id, login);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", matricule='" + matricule + '\'' +
                ", login='" + login + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                '}';
    }


}