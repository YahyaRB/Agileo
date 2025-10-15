package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.ArticleReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleReceptionRepository extends JpaRepository<ArticleReception, String> {
    List<ArticleReception> findArticleReceptionsByCommande(Long commande);
}
