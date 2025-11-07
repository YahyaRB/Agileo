package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.ART;
import com.agileo.AGILEO.entity.divalto.ENT;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtRepository extends JpaRepository<ART, Integer> {
    ART findByRef(String ref);
}
