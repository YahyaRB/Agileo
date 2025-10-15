package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.ArticleEnStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;


@Repository
public interface ArticleEnStockRepository extends JpaRepository<ArticleEnStock, String> {

    // Méthode principale - chercher par dépôt
    List<ArticleEnStock> findByDepo(String depo);
    // NOUVEAU : Requête native optimisée pour stock > 0
    @Query(value = "SELECT * FROM Article_en_stock WHERE DEPO = :depot AND SumStQte > 0 ORDER BY REF", nativeQuery = true)
    List<ArticleEnStock> findByDepotWithStockNative(@Param("depot") String depot);

    // Pour debug - compter les articles
    @Query("SELECT COUNT(a) FROM ArticleEnStock a WHERE a.depo = :depot")
    Long countByDepo(@Param("depot") String depot);

    // Pour debug - lister les dépôts existants
    @Query("SELECT DISTINCT a.depo FROM ArticleEnStock a ORDER BY a.depo")
    List<String> findAllDepots();
}