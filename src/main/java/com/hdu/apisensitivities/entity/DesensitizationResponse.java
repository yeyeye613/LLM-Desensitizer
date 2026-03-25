package com.hdu.apisensitivities.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesensitizationResponse {
    private String originalContent;
    private String desensitizedContent;
    private List<SensitiveEntity> detectedEntities;
    private boolean success;
    private String message;
}
