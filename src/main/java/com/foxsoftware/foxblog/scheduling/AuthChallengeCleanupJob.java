package com.foxsoftware.foxblog.scheduling;

import com.foxsoftware.foxblog.service.AdminAuthService;
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
        try {
            int removed = authService.sweepExpired();
            if (removed > 0) {
                log.info("[TOTP] cleaned {} expired challenges", removed);
            } else {
                log.debug("[TOTP] no expired challenges to clean");
            }
        } catch (Exception e) {
            log.error("[TOTP] challenge cleanup failed", e);
        }
    }
}
