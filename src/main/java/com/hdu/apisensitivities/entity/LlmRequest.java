package com.hdu.apisensitivities.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    private String prompt;
    private LlmProvider provider;
    private String model;
    private Map<String, Object> parameters;
    private String sessionId;
    private List<String> blacklist;
    private List<String> whitelist;
    private String dataType; // 添加数据类型字段，如"TEXT", "JSON", "XML", "IMAGE", "AUDIO", "PDF", "DOC"等

    // 如果未指定provider，使用默认配置
    public LlmProvider getProvider() {
        return provider != null ? provider : LlmProvider.DEEPSEEK;
    }
    
    // 如果未指定dataType，默认为TEXT
    public String getDataType() {
        return dataType != null ? dataType : "TEXT";
    }

    public static LlmRequest fromApiRequest(ApiRequest apiRequest) {
        return LlmRequest.builder()
                .prompt(apiRequest.getPrompt())
                .provider(LlmProvider.DEEPSEEK) // 默认使用DeepSeek
                .model(apiRequest.getModel())
                .parameters(apiRequest.getParameters())
                .sessionId(apiRequest.getSessionId())
                .dataType("TEXT") // API请求默认为文本类型
                .build();
    }
}
