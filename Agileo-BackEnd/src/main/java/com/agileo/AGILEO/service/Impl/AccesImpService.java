package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.AccessRequestDTO;
import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.entity.secondary.Acces;
import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.exception.*;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.secondary.AccesRepository;
import com.agileo.AGILEO.repository.secondary.UserRepository;
import com.agileo.AGILEO.service.AccesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccesImpService implements AccesService {

    private final AccesRepository accesRepository;
    private final UserRepository userRepository;

    public AccesImpService(AccesRepository accesRepository, UserRepository userRepository) {
        this.accesRepository = accesRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AccessResponseDTO createAccess(AccessRequestDTO accessDto, String currentUsername) {
        validateAccessCreation(accessDto);

        Acces acces = new Acces();
        acces.setCode(accessDto.getCode());
        setAuditFields(acces, currentUsername);

        Acces savedAccess = accesRepository.save(acces);
        return mapEntityToDto(savedAccess);
    }

    @Override
    public ResponseMessage updateAccess(Long id, AccessRequestDTO accessDto, String currentUsername) {
        Acces acces = getAccessById(id);

        if (!acces.getCode().equals(accessDto.getCode()) &&
                accesRepository.existsByCode(accessDto.getCode())) {
            throw new ConflictException("Access code already exists");
        }

        acces.setCode(accessDto.getCode());
        acces.setLastModifiedBy(currentUsername);
        acces.setLastModifiedDate(LocalDateTime.now());

        accesRepository.save(acces);
        return new ResponseMessage("Access updated successfully");
    }

    @Override
    public ResponseMessage deleteAccess(Long id) {
        Acces acces = getAccessById(id);

        // Check if any users have this access before deleting
        if (!acces.getUsers().isEmpty()) {
            throw new ConflictException("Cannot delete access that is assigned to users");
        }

        accesRepository.delete(acces);
        return new ResponseMessage("Access deleted successfully");
    }

    @Override
    public List<AccessResponseDTO> findAllAccesses() {
        return accesRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public AccessResponseDTO findAccessById(Long id) {
        return mapEntityToDto(getAccessById(id));
    }

    @Override
    public Set<String> getUserAccessCodes(Long userId) {
        User user = getUserById(userId);
        return user.getAcces().stream()
                .map(Acces::getCode)
                .collect(Collectors.toSet());
    }

    @Override
    public ResponseMessage addAccessToUser(Long userId, Long accessId, String currentUsername) {
        User user = getUserById(userId);
        Acces acces = getAccessById(accessId);

        if (user.getAcces().contains(acces)) {
            throw new ConflictException("User already has this access");
        }

        user.getAcces().add(acces);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());

        userRepository.save(user);
        return new ResponseMessage("Access added to user successfully");
    }

    @Override
    public ResponseMessage removeAccessFromUser(Long userId, Long accessId, String currentUsername) {
        User user = getUserById(userId);
        user.getAcces().removeIf(a -> a.getId().equals(accessId));
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());

        userRepository.save(user);
        return new ResponseMessage("Access removed from user successfully");
    }

    @Override
    public boolean hasAccess(Long userId, String accessCode) {
        User user = getUserById(userId);
        return user.getAcces().stream()
                .anyMatch(a -> a.getCode().equals(accessCode));
    }

    // Helper methods
    private Acces getAccessById(Long id) {
        return accesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Access not found with id: " + id));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void validateAccessCreation(AccessRequestDTO dto) {
        if (accesRepository.existsByCode(dto.getCode())) {
            throw new ConflictException("Access code already exists");
        }
    }

    private AccessResponseDTO mapEntityToDto(Acces entity) {
        AccessResponseDTO dto = new AccessResponseDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }

    private void setAuditFields(Acces acces, String username) {
        acces.setCreatedBy(username);
        acces.setCreatedDate(LocalDateTime.now());
        acces.setLastModifiedBy(username);
        acces.setLastModifiedDate(LocalDateTime.now());
    }
}