package com.foxsoftware.foxblog.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChallengeCleanupJob {

    private final AdminAuthService authService;
    @Scheduled(fixedDelay = 60000)
    public void sweep() {
        int removed = authService.sweepExpired();
        if (removed > 0) {
            log.info("[TOTP] cleaned {} expired challenges", removed);
        }
    }
}