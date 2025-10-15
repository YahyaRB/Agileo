package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.KdnsAccessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KdnsAccessorRepository extends JpaRepository<KdnsAccessor, Integer> {

    Optional<KdnsAccessor> findByLogin(String login);

    List<KdnsAccessor> findByAccessorType(Integer accessorType);

    @Query("SELECT k FROM KdnsAccessor k WHERE k.fullName LIKE %:name% OR k.firstName LIKE %:name% OR k.lastName LIKE %:name%")
    List<KdnsAccessor> searchByName(@Param("name") String name);

    List<KdnsAccessor> findByExternalUser(Integer externalUser);

    /**
     * Trouver le plus grand ID d'accessor (pour génération d'ID)
     */
    @Query("SELECT MAX(k.accessorId) FROM KdnsAccessor k")
    Integer findMaxAccessorId();


    @Query("SELECT k FROM KdnsAccessor k WHERE k.firstName = :firstName AND k.lastName = :lastName")
    List<KdnsAccessor> findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);


}