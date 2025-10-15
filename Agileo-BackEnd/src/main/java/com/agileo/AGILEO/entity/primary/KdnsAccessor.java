package com.agileo.AGILEO.entity.primary;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "KDNS_ACCESSOR", schema = "dbo")
public class KdnsAccessor {

    @Id
    @Column(name = "ACCESSORID")
    private Integer accessorId;

    @Column(name = "ACCESSORTYPE")
    private Integer accessorType;

    // ✅ CORRIGÉ: Spécifier TEXT au lieu de CLOB pour correspondre à la base
    @Lob
    @Column(name = "XMLCONTEXT", columnDefinition = "TEXT")
    private String xmlContext;

    @Column(name = "LOGIN", length = 100)
    private String login;

    @Column(name = "PASSWORD", length = 100)
    private String password;

    @Column(name = "LANGUAGEID")
    private Integer languageId;

    @Column(name = "CIVILITY", length = 50)
    private String civility;

    @Column(name = "FIRSTNAME", length = 50)
    private String firstName;

    @Column(name = "LASTNAME", length = 50)
    private String lastName;

    @Column(name = "FULLNAME", length = 110)
    private String fullName;

    @Column(name = "INITIALES", length = 10)
    private String initiales;

    @Column(name = "EMAIL", length = 255)
    private String email;

    @Column(name = "DOMAIN", length = 255)
    private String domain;

    @Column(name = "OBJECTGUID", length = 255)
    private String objectGuid;

    @Column(name = "DIVALTOKEY", length = 255)
    private String divaltoKey;

    @Column(name = "ADLOGIN", length = 100)
    private String adLogin;

    @Column(name = "EXTERNALUSER")
    private Integer externalUser;

    @Column(name = "STARTVALID")
    private LocalDateTime startValid;

    @Column(name = "ENDVALID")
    private LocalDateTime endValid;

    @Column(name = "FIRSTCONNECTION")
    private LocalDateTime firstConnection;

    @Column(name = "LASTCONNECTION")
    private LocalDateTime lastConnection;

    @Column(name = "MEMBERCRC")
    private Integer memberCrc;
}