package com.foxsoftware.foxblog.dto.tag;

import lombok.Data;

/**
 * 创建标签请求
 */
@Data
public class TagCreateRequest {
    private String name; // 必填
}