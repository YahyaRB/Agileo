package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.AffaireLieUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffaireLieUserRepository extends JpaRepository<AffaireLieUser, Integer> {

    List<AffaireLieUser> findByAffaire(String affaire);
    List<AffaireLieUser> findByAccessoirId(Integer accessoirId);
    @Query("SELECT a FROM AffaireLieUser a WHERE a.libelle LIKE %:keyword% OR a.affaire LIKE %:keyword%")
    List<AffaireLieUser> searchByKeyword(@Param("keyword") String keyword);
}