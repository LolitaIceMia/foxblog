   -- 插入新媒体记录示例
   INSERT INTO media (id, storage_path, mime_type, size_bytes)
   VALUES (UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000'), '/path/to/file.jpg', 'image/jpeg', 102400);

   -- 查询媒体记录示例
   SELECT BIN_TO_UUID(id) as id, storage_path, mime_type, created_at FROM media ORDER BY created_at DESC;
   -- 查询包含媒体的帖子
   SELECT p.id, m.storage_path
   FROM posts p
   JOIN post_media pm ON p.id = pm.post_id
   JOIN media m ON pm.media_id = m.id;

