package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.entity.primary.KdnsAccessor;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.repository.primary.KdnsAccessorRepository;
import com.agileo.AGILEO.service.KdnsAccessorService;
import com.agileo.AGILEO.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class KdnsAccessorServiceImpl implements KdnsAccessorService {

    @Autowired
    private KdnsAccessorRepository kdnsAccessorRepository;

    // Note: Injection circulaire potentielle, à gérer avec @Lazy si nécessaire
    @Autowired
    private UserService userService;

    // ============ MÉTHODES DE BASE ============

    @Override
    public List<KdnsAccessor> findAllAccessors() {
        return kdnsAccessorRepository.findAll();
    }

    @Override
    public KdnsAccessor findAccessorById(Integer accessorId) {
        return kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("KdnsAccessor not found with id: " + accessorId));
    }

    @Override
    public KdnsAccessor findAccessorByLogin(String login) {
        return kdnsAccessorRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("KdnsAccessor not found with login: " + login));
    }

    @Override
    public List<KdnsAccessor> searchAccessorsByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllAccessors();
        }
        return kdnsAccessorRepository.searchByName(searchTerm.trim());
    }

    @Override
    public List<KdnsAccessor> findAccessorsByType(Integer accessorType) {
        return kdnsAccessorRepository.findByAccessorType(accessorType);
    }

    @Override
    public List<KdnsAccessor> findExternalAccessors(Integer externalUser) {
        return kdnsAccessorRepository.findByExternalUser(externalUser);
    }

    @Override
    public boolean existsById(Integer accessorId) {
        return kdnsAccessorRepository.existsById(accessorId);
    }

    @Override
    public boolean existsByLogin(String login) {
        return kdnsAccessorRepository.findByLogin(login).isPresent();
    }

    // ============ MÉTHODES POUR LIAISON AVEC USER ============

    @Override
    public Long getUserIdByAccessorId(Integer accessorId) {
        // Vérifier que l'accessor existe
        KdnsAccessor accessor = findAccessorById(accessorId);

        try {
            if (accessor.getLogin() != null && !accessor.getLogin().trim().isEmpty()) {
                return userService.findUserByLogin(accessor.getLogin()).getId();
            }
        } catch (Exception e) {
            // Aucun utilisateur trouvé avec ce login
        }

        return null;
    }

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Méthode helper pour valider un accessor
     */
    private void validateAccessor(KdnsAccessor accessor) {
        if (accessor == null) {
            throw new IllegalArgumentException("KdnsAccessor cannot be null");
        }

        if (accessor.getLogin() == null || accessor.getLogin().trim().isEmpty()) {
            throw new IllegalArgumentException("Accessor login cannot be null or empty");
        }
    }

    /**
     * Méthode pour créer un nouvel accessor (si nécessaire)
     */
    public KdnsAccessor createAccessor(KdnsAccessor accessor) {
        validateAccessor(accessor);

        // Vérifier l'unicité du login
        if (existsByLogin(accessor.getLogin())) {
            throw new IllegalArgumentException("Accessor with login '" + accessor.getLogin() + "' already exists");
        }

        return kdnsAccessorRepository.save(accessor);
    }

    /**
     * Méthode pour mettre à jour un accessor
     */
    public KdnsAccessor updateAccessor(Integer accessorId, KdnsAccessor updatedAccessor) {
        KdnsAccessor existingAccessor = findAccessorById(accessorId);

        // Mise à jour des champs modifiables
        if (updatedAccessor.getFirstName() != null) {
            existingAccessor.setFirstName(updatedAccessor.getFirstName());
        }

        if (updatedAccessor.getLastName() != null) {
            existingAccessor.setLastName(updatedAccessor.getLastName());
        }

        if (updatedAccessor.getFullName() != null) {
            existingAccessor.setFullName(updatedAccessor.getFullName());
        }

        if (updatedAccessor.getAccessorType() != null) {
            existingAccessor.setAccessorType(updatedAccessor.getAccessorType());
        }

        if (updatedAccessor.getExternalUser() != null) {
            existingAccessor.setExternalUser(updatedAccessor.getExternalUser());
        }

        return kdnsAccessorRepository.save(existingAccessor);
    }

    /**
     * Méthode pour supprimer un accessor (soft delete si nécessaire)
     */
    public void deleteAccessor(Integer accessorId) {
        KdnsAccessor accessor = findAccessorById(accessorId);

        // Option 1: Suppression physique
        kdnsAccessorRepository.delete(accessor);

        // Option 2: Suppression logique (si vous avez un champ 'active' ou 'deleted')
        // accessor.setActive(false);
        // kdnsAccessorRepository.save(accessor);
    }
}