package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.ArticleErp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleErpRepository extends JpaRepository<ArticleErp, Long> {

    // ==================== RECHERCHE COMBINÉE (UNIFIÉE) ====================

    /**
     * ✅ MÉTHODE PRINCIPALE : Recherche avec filtres optionnels
     * - Si search est null/vide ET familleCode est null/vide → tous les articles
     * - Si search fourni → filtre par recherche (+ famille si fournie)
     * - Si familleCode fourni → filtre par famille (+ recherche si fournie)
     */
    @Query("SELECT a FROM ArticleErp a WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(a.ref) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND " +
            "(:familleCode IS NULL OR :familleCode = '' OR " +
            " a.fam0001 = :familleCode)")
    Page<ArticleErp> searchArticlesWithFilters(
            @Param("search") String search,
            @Param("familleCode") String familleCode,
            Pageable pageable
    );

    // ==================== MÉTHODES LEGACY (compatibilité) ====================

    /**
     * Recherche simple par terme
     */
    @Query("SELECT a FROM ArticleErp a WHERE " +
            "LOWER(a.ref) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ArticleErp> searchArticles(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Filtre par famille statistique 1
     */
    Page<ArticleErp> findByCodeFam1(String codeFam, Pageable pageable);

    /**
     * Récupérer les codes de familles distincts
     */
    @Query("SELECT DISTINCT a.fam0001 FROM ArticleErp a WHERE a.fam0001 IS NOT NULL ORDER BY a.fam0001")
    List<String> findDistinctCodeFam1();



    /**
     * Compter les articles par famille
     */
    @Query("SELECT a.fam0001, COUNT(a) FROM ArticleErp a " +
            "WHERE a.fam0001 IS NOT NULL " +
            "GROUP BY a.fam0001 " +
            "ORDER BY a.fam0001")
    List<Object[]> countArticlesByCodeFam1();
}