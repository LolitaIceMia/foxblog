package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.security.ProductionJwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/jwt")
public class JwtKeyReloadController {

    private final ProductionJwtProvider provider;

    @PostMapping("/reload-keys")
    public ResponseEntity<Void> reload() {
        provider.reload();
        return ResponseEntity.ok().build();
    }
}