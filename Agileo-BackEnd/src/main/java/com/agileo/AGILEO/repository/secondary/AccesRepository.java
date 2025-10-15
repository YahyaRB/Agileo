package com.agileo.AGILEO.repository.secondary;

import com.agileo.AGILEO.entity.secondary.Acces;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccesRepository extends JpaRepository<Acces, Long> {
    Optional<Acces> findByCode(String code);
    Acces findAccesById(Long accesId);
    boolean existsByCode(String code);

}