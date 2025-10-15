package com.agileo.AGILEO.entity.secondary;

import lombok.Data;

import jakarta.persistence.*;


import java.util.List;

@Entity
@Data
public class Acces extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @ManyToMany(mappedBy = "acces")
    private List<User> users;


}
