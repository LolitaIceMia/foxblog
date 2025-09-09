package com.foxsoftware.foxblog.storage;

public interface MediaStorage {
    /**
     * 存储媒体文件
     * @param subdir  子目录（如 2025-09-09）
     * @param originalFilename 原始文件名
     * @param data 文件字节
     * @return 相对或逻辑存储路径
     */
    String store(String subdir, String originalFilename, byte[] data);
}