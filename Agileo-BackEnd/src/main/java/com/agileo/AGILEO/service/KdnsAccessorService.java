package com.agileo.AGILEO.service;

import com.agileo.AGILEO.entity.primary.KdnsAccessor;

import java.util.List;

public interface KdnsAccessorService {

    List<KdnsAccessor> findAllAccessors();

    KdnsAccessor findAccessorById(Integer accessorId);

    KdnsAccessor findAccessorByLogin(String login);

    List<KdnsAccessor> searchAccessorsByName(String searchTerm);

    List<KdnsAccessor> findAccessorsByType(Integer accessorType);

    List<KdnsAccessor> findExternalAccessors(Integer externalUser);

    Long getUserIdByAccessorId(Integer accessorId);

    boolean existsById(Integer accessorId);

    boolean existsByLogin(String login);
}