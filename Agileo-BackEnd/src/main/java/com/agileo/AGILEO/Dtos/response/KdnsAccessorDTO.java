package com.agileo.AGILEO.Dtos.response;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KdnsAccessorDTO {
    private Integer accessorId;
    private Integer accessorType;
    private String login;
    private String civility;
    private String firstName;
    private String lastName;
    private String fullName;
    private String initiales;
    private String email;
    private String domain;
    private Integer externalUser;
    private LocalDateTime startValid;
    private LocalDateTime endValid;
    private LocalDateTime firstConnection;
    private LocalDateTime lastConnection;
    private Boolean isActive;
}