package org.example.tf25.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alias del endpoint de login para compatibilidad con la consigna de alumnos.
 *
 * Expone: POST /api/authenticate
 * Devuelve el mismo payload que /api/v1/authenticate ({ "id_token": "..." }).
 */
@RestController
@RequestMapping("/api")
public class AuthAliasController {

    private final AuthController authController;

    public AuthAliasController(AuthController authController) {
        this.authController = authController;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthController.LoginRequest req) {
        return authController.authenticate(req);
    }
}
