package com.foxsoftware.foxblog.service.media;

import com.foxsoftware.foxblog.dto.media.MediaUploadResponse;
import com.foxsoftware.foxblog.entity.Media;
import com.foxsoftware.foxblog.repository.MediaRepository;
import com.foxsoftware.foxblog.storage.MediaStorage;
import com.foxsoftware.foxblog.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaStorage mediaStorage;

    @Override
    @Transactional
    public MediaUploadResponse upload(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String hash = HashUtils.sha256Hex(bytes);

            Optional<Media> existing = mediaRepository.findFirstBySha256Hash(hash);
            if (existing.isPresent()) {
                Media m = existing.get();
                return MediaUploadResponse.builder()
                        .id(m.getId().toString())
                        .mimeType(m.getMimeType())
                        .sizeBytes(m.getSizeBytes())
                        .sha256(m.getSha256Hash())
                        .storagePath(m.getStoragePath())
                        .reused(true)
                        .build();
            }

            String subdir = LocalDate.now().toString();
            String storedPath = mediaStorage.store(subdir, file.getOriginalFilename(), bytes);

            Media media = Media.builder()
                    .storagePath(storedPath)
                    .mimeType(file.getContentType())
                    .sizeBytes((long) bytes.length)
                    .sha256Hash(hash)
                    .build();
            Media saved = mediaRepository.save(media);

            return MediaUploadResponse.builder()
                    .id(saved.getId().toString())
                    .mimeType(saved.getMimeType())
                    .sizeBytes(saved.getSizeBytes())
                    .sha256(saved.getSha256Hash())
                    .storagePath(saved.getStoragePath())
                    .reused(false)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("媒体上传失败: " + e.getMessage(), e);
        }
    }
}