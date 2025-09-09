package com.foxsoftware.foxblog.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileSystemStorage implements MediaStorage {

    @Value("${app.media.root:media}")
    private String rootDir;

    @Override
    public String store(String subdir, String originalFilename, byte[] data) {
        try {
            String safeName = UUID.randomUUID() + "_" +
                    (originalFilename == null ? "file" : originalFilename.replaceAll("\\s+", "_"));
            Path base = Paths.get(rootDir).toAbsolutePath();
            Files.createDirectories(base.resolve(subdir));
            Path target = base.resolve(subdir).resolve(safeName);
            Files.write(target, data, StandardOpenOption.CREATE_NEW);
            log.info("[MEDIA] stored {}", target);
            return subdir + "/" + safeName;
        } catch (IOException e) {
            throw new RuntimeException("存储媒体失败", e);
        }
    }
}