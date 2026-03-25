package com.hdu.apisensitivities.config;

import com.hdu.apisensitivities.entity.LlmProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfig {
    private LlmProvider provider;
    private String apiUrl;
    private String apiKey;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, Object> additionalParams;
}
