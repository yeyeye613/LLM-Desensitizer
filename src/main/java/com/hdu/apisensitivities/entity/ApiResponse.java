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
public class ApiResponse {
    private String originalResponse;
    private String desensitizedResponse;
    private List<SensitiveEntity> inputSensitiveEntities;
    private List<SensitiveEntity> outputSensitiveEntities;

    // 从新版LlmResponse创建
    public static ApiResponse fromLlmResponse(LlmResponse llmResponse) {
        return ApiResponse.builder()
                .originalResponse(llmResponse.getOriginalResponse())
                .desensitizedResponse(llmResponse.getDesensitizedResponse())
                .inputSensitiveEntities(llmResponse.getInputSensitiveEntities())
                .outputSensitiveEntities(llmResponse.getOutputSensitiveEntities())
                .build();
    }

}
