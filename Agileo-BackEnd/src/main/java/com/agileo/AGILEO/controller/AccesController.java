package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.AccessRequestDTO;
import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.service.AccesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/access")
@PreAuthorize("hasRole('ADMIN')")
public class AccesController {

    private final AccesService accesService;

    public AccesController(AccesService accesService) {
        this.accesService = accesService;
    }

    @PostMapping
    public ResponseEntity<AccessResponseDTO> createAccess(
            @RequestBody AccessRequestDTO accessDto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accesService.createAccess(accessDto, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<AccessResponseDTO>> getAllAccesses() {
        return ResponseEntity.ok(accesService.findAllAccesses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessResponseDTO> getAccessById(@PathVariable Long id) {
        return ResponseEntity.ok(accesService.findAccessById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateAccess(
            @PathVariable Long id,
            @RequestBody AccessRequestDTO accessDto,
            Authentication authentication) {
        return ResponseEntity.ok(accesService.updateAccess(id, accessDto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteAccess(@PathVariable Long id) {
        return ResponseEntity.ok(accesService.deleteAccess(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<String>> getUserAccesses(@PathVariable Long userId) {
        return ResponseEntity.ok(accesService.getUserAccessCodes(userId));
    }


}