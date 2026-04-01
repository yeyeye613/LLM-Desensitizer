package com.hdu.apisensitivities.service.LlmClient;

import com.hdu.apisensitivities.config.LlmConfig;
import com.hdu.apisensitivities.entity.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class DoubaoClient implements LlmClient {

    private final RestTemplate restTemplate;

    public DoubaoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String sendRequest(String prompt, LlmConfig config, Map<String, Object> parameters) {
        try {
            HttpHeaders headers = createHeaders(config);
            Map<String, Object> requestBody = createRequestBody(prompt, config, parameters);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getApiUrl(), HttpMethod.POST, entity, Map.class);

            return extractResponse(response.getBody());

        } catch (Exception e) {
            log.error("豆包 API调用失败", e);
            throw new RuntimeException("豆包 API调用失败: " + e.getMessage());
        }
    }

    @Override
    public LlmProvider getSupportedProvider() {
        return LlmProvider.DOUBAO;
    }

    @Override
    public boolean validateConfig(LlmConfig config) {
        return config.getApiKey() != null && !config.getApiKey().trim().isEmpty();
    }
    
    @Override
    public String sendStructuredRequest(Map<String, Object> structuredData, LlmConfig config, Map<String, Object> parameters) {
        log.info("豆包 结构化数据请求准备中，模型: {}, 参数: {}", config.getModel(), parameters);
        
        try {
            // 将结构化数据转换为JSON字符串
            ObjectMapper mapper = new ObjectMapper();
            String structuredDataJson = mapper.writeValueAsString(structuredData);
            
            // 构建提示词，说明这是结构化数据
            String prompt = "请分析以下结构化数据:\n" + structuredDataJson;
            
            // 复用现有的sendRequest方法发送请求
            return sendRequest(prompt, config, parameters);
        } catch (Exception e) {
            log.error("豆包 结构化数据请求失败", e);
            throw new RuntimeException("豆包 结构化数据请求失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String sendBinaryRequest(byte[] binaryData, String dataType, LlmConfig config, Map<String, Object> parameters) {
        log.info("豆包 二进制数据请求准备中，数据类型: {}, 模型: {}", dataType, config.getModel());
        
        try {
            // 将二进制数据转换为Base64编码字符串
            String base64Data = Base64.getEncoder().encodeToString(binaryData);
            
            // 构建提示词，说明这是二进制数据的Base64编码
            String prompt = String.format("这是一个Base64编码的%s数据，请根据需要进行分析:\n%s", dataType, base64Data);
            
            // 复用现有的sendRequest方法发送请求
            return sendRequest(prompt, config, parameters);
        } catch (Exception e) {
            log.error("豆包 二进制数据请求失败", e);
            throw new RuntimeException("豆包 二进制数据请求失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supportsDataType(String dataType) {
        // 支持的结构化数据类型
        Set<String> supportedTypes = Set.of("TEXT", "JSON", "XML");
        return supportedTypes.contains(dataType.toUpperCase());
    }

    public HttpHeaders createHeaders(LlmConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());
        return headers;
    }

    private Map<String, Object> createRequestBody(String prompt, LlmConfig config, Map<String, Object> parameters) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", createMessages(prompt));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        // 豆包API特定参数
        requestBody.put("stream", false);

        if (parameters != null) {
            requestBody.putAll(parameters);
        }

        return requestBody;
    }

    private Object createMessages(String prompt) {
        return new Object[] {
                Map.of("role", "user", "content", prompt)
        };
    }

    private String extractResponse(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("响应体为空");
        }

        if (responseBody.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
            throw new RuntimeException("豆包 API错误: " + error.get("message"));
        }

        if (responseBody.containsKey("choices")) {
            java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                return (String) message.get("content");
            }
        }

        // 豆包API可能使用不同的响应格式
        if (responseBody.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data.containsKey("choices")) {
                java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) data.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
        }

        throw new RuntimeException("无法解析豆包响应");
    }
}
