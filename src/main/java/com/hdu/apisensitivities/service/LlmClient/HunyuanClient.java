package com.hdu.apisensitivities.service.LlmClient;

import com.hdu.apisensitivities.config.LlmConfig;
import com.hdu.apisensitivities.entity.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 腾讯混元大模型客户端实现
 */
@Slf4j
@Component
public class HunyuanClient implements LlmClient {

    private final RestTemplate restTemplate;

    public HunyuanClient() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String sendRequest(String prompt, LlmConfig config, Map<String, Object> parameters) {
        log.info("发送请求到腾讯混元大模型，模型: {}, 参数: {}", config.getModel(), parameters);

        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(prompt, config, parameters);
            
            // 创建请求头
            HttpHeaders headers = createHeaders(config);
            
            // 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            // 解析响应
            return parseResponse(responseEntity.getBody());
        } catch (Exception e) {
            log.error("调用腾讯混元大模型API失败", e);
            throw new RuntimeException("调用腾讯混元大模型API失败: " + e.getMessage(), e);
        }
    }

    @Override
    public LlmProvider getSupportedProvider() {
        return LlmProvider.HUNYUAN;
    }

    @Override
    public boolean validateConfig(LlmConfig config) {
        return config.getApiKey() != null && !config.getApiKey().trim().isEmpty();
    }
    
    @Override
    public String sendStructuredRequest(Map<String, Object> structuredData, LlmConfig config, Map<String, Object> parameters) {
        log.info("腾讯混元 结构化数据请求准备中，模型: {}, 参数: {}", config.getModel(), parameters);
        
        try {
            // 将结构化数据转换为JSON字符串
            ObjectMapper mapper = new ObjectMapper();
            String structuredDataJson = mapper.writeValueAsString(structuredData);
            
            // 构建提示词，说明这是结构化数据
            String prompt = "请分析以下结构化数据:\n" + structuredDataJson;
            
            // 复用现有的sendRequest方法发送请求
            return sendRequest(prompt, config, parameters);
        } catch (Exception e) {
            log.error("腾讯混元 结构化数据请求失败", e);
            throw new RuntimeException("腾讯混元 结构化数据请求失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String sendBinaryRequest(byte[] binaryData, String dataType, LlmConfig config, Map<String, Object> parameters) {
        log.info("腾讯混元 二进制数据请求准备中，数据类型: {}, 模型: {}", dataType, config.getModel());
        
        try {
            // 将二进制数据转换为Base64编码字符串
            String base64Data = Base64.getEncoder().encodeToString(binaryData);
            
            // 构建提示词，说明这是二进制数据的Base64编码
            String prompt = String.format("这是一个Base64编码的%s数据，请根据需要进行分析:\n%s", dataType, base64Data);
            
            // 复用现有的sendRequest方法发送请求
            return sendRequest(prompt, config, parameters);
        } catch (Exception e) {
            log.error("腾讯混元 二进制数据请求失败", e);
            throw new RuntimeException("腾讯混元 二进制数据请求失败: " + e.getMessage(), e);
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

    private Map<String, Object> buildRequestBody(String prompt, LlmConfig config, Map<String, Object> parameters) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 设置模型
        requestBody.put("model", config.getModel());
        
        // 设置消息
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);
        
        // 设置温度
        requestBody.put("temperature", parameters.getOrDefault("temperature", config.getTemperature()));
        
        // 设置最大令牌数
        requestBody.put("max_tokens", parameters.getOrDefault("maxTokens", config.getMaxTokens()));
        
        // 设置停止词
        if (parameters.containsKey("stop")) {
            requestBody.put("stop", parameters.get("stop"));
        }
        
        // 设置频率惩罚
        if (parameters.containsKey("frequencyPenalty")) {
            requestBody.put("frequency_penalty", parameters.get("frequencyPenalty"));
        }
        
        // 设置存在惩罚
        if (parameters.containsKey("presencePenalty")) {
            requestBody.put("presence_penalty", parameters.get("presencePenalty"));
        }
        
        // 设置top_p
        if (parameters.containsKey("topP")) {
            requestBody.put("top_p", parameters.get("topP"));
        }
        
        return requestBody;
    }

    private String parseResponse(Map<String, Object> responseBody) {
        if (responseBody == null || !responseBody.containsKey("choices")) {
            throw new RuntimeException("无效的腾讯混元大模型响应: 缺少choices字段");
        }
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("无效的腾讯混元大模型响应: choices列表为空");
        }
        
        Map<String, Object> firstChoice = choices.get(0);
        if (!firstChoice.containsKey("message")) {
            throw new RuntimeException("无效的腾讯混元大模型响应: 缺少message字段");
        }
        
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        if (!message.containsKey("content")) {
            throw new RuntimeException("无效的腾讯混元大模型响应: 缺少content字段");
        }
        
        return (String) message.get("content");
    }
}