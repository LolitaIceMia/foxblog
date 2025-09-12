package com.foxsoftware.foxblog.dto.tag;

import lombok.Data;

/**
 * 更新标签请求
 */
@Data
public class TagUpdateRequest {
    private String name; // 必填
}