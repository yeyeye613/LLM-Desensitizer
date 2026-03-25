package com.hdu.apisensitivities.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SensitiveRule {
    private Long id;
    private String patternName;
    private String regex;
    private Boolean isEnabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
