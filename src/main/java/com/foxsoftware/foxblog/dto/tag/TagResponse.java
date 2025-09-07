package com.foxsoftware.foxblog.dto.tag;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TagResponse {
    Long id;
    String name;
}