package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.Reception;
import com.agileo.AGILEO.entity.primary.VentilationArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface VentilationArticleRepository extends JpaRepository<VentilationArticle, String> {
    @Query("SELECT v FROM VentilationArticle v WHERE v.depot = :depot AND v.ref = :ref")
    VentilationArticle findByReferenceAndDepot(@Param("ref") String ref, @Param("depot") String depot);
}
