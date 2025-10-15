package com.agileo.AGILEO.controller;


import com.agileo.AGILEO.Dtos.response.UserResponseDTO;
import com.agileo.AGILEO.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
//    private final AffaireService affaireService;

    public UserController(UserService userService) {
        this.userService = userService;
//        this.affaireService = affaireService;
    }

    @GetMapping("/current")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        try {
            UserResponseDTO user = userService.findUserByLogin(authentication.getName());

//            if (user != null) {
//                Set<AffaireResponseDTO> affaires = affaireService.getAffairesByUserId(user.getId());
//                user.setAffaires(affaires); // Ajout des affaires assignées
//            }

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

}