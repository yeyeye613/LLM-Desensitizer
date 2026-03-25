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
public class ApiRequest {
    private String prompt;
    private Map<String, Object> parameters;
    private String model;
    private String sessionId;
    private List<String> blacklist;
    private List<String> whitelist;

    // 转换为新版LlmRequest
    public LlmRequest toLlmRequest() {
        return LlmRequest.builder()
                .prompt(this.prompt)
                .provider(LlmProvider.DEEPSEEK) // 默认使用DeepSeek
                .model(this.model)
                .parameters(this.parameters)
                .sessionId(this.sessionId)
                .blacklist(this.blacklist)
                .whitelist(this.whitelist)
                .build();
    }

}

