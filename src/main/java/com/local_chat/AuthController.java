package com.local_chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.constraints.NotBlank;

@Controller 
public class AuthController {
    private final Database db;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(Database db) {
        this.db = db;
    }

    @GetMapping("/login") 
    public String getAuthPage() {
        return "login";
    }

    private boolean isValidPassword(String password) {
        return (password.matches("[A-Za-z0-9]+") && 
             (password.length() <= 50 && password.length() >= 4));
    }
    private boolean isValidUsername(String username) {
        return (username.matches("[A-Za-z0-9]+") &&
             (username.length() <= 40 && username.length() > 0));
    }

    public record Credentials(
        @NotBlank String username, 
        @NotBlank String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody Credentials creds) {

        if (db.userExists(creds.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("USERNAME_IN_USE");
            } if (!this.isValidUsername(creds.username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("BAD_USERNAME");
        } if (!this.isValidPassword(creds.password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("BAD_PASSWORD");
        }
        
        String password_hash = this.encoder.encode(creds.password());
        int res = this.db.insertUser(creds.username, password_hash);
        if (res == -1) ResponseEntity.status(500);

        return ResponseEntity.ok().build();
    }
}

