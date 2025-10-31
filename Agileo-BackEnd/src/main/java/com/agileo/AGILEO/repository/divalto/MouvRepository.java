package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.MOUV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MouvRepository extends JpaRepository<MOUV, Integer> {
}