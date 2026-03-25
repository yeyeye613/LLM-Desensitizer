package com.hdu.apisensitivities.controller;

import com.hdu.apisensitivities.entity.*;
import com.hdu.apisensitivities.service.LlmConfigService;
import com.hdu.apisensitivities.service.LlmProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/llm")
public class LlmProxyController {

    private final LlmProxyService llmProxyService;
    private final LlmConfigService configService;

    @Autowired
    public LlmProxyController(LlmProxyService llmProxyService, LlmConfigService configService) {
        this.llmProxyService = llmProxyService;
        this.configService = configService;
    }


    //新版LLM请求（多供应商）- 支持多种数据类型
    @PostMapping("/proxy")
    public ResponseEntity<LlmResponse> processLlmRequest(@RequestBody LlmRequest request) {
        // 确保数据类型设置正确，默认为TEXT
        if (request.getDataType() == null) {
            request.setDataType("TEXT");
        }
        LlmResponse response = llmProxyService.processLlmRequest(request);
        return ResponseEntity.ok(response);
    }
    
    //处理结构化数据的LLM请求，直接接收结构化数据，自动转换为合适的格式发送给LLM服务
    @PostMapping("/proxy/structured")
    public ResponseEntity<LlmResponse> processStructuredLlmRequest(@RequestBody Map<String, Object> structuredData, 
                                                                 @RequestParam(defaultValue = "DEEPSEEK") String provider,
                                                                 @RequestParam(required = false) String model) {
        // 构建LLM请求
        LlmRequest request = LlmRequest.builder()
                .provider(LlmProvider.valueOf(provider.toUpperCase()))
                .model(model)
                .dataType("JSON") // 指定为JSON数据类型
                .parameters(Map.of("structured_data", structuredData))
                .build();
                
        LlmResponse response = llmProxyService.processLlmRequest(request);
        return ResponseEntity.ok(response);
    }

    //兼容旧版LLM请求
    @PostMapping("/proxy/legacy")
    public ResponseEntity<ApiResponse> processLlmRequestLegacy(@RequestBody ApiRequest request) {
        ApiResponse response = llmProxyService.processLlmRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/proxy/async")
    public CompletableFuture<ResponseEntity<LlmResponse>> processLlmRequestAsync(@RequestBody LlmRequest request) {
        // 确保数据类型设置正确，默认为TEXT
        if (request.getDataType() == null) {
            request.setDataType("TEXT");
        }
        return llmProxyService.processLlmRequestAsync(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/proxy/async/legacy")
    public CompletableFuture<ResponseEntity<ApiResponse>> processLlmRequestAsyncLegacy(@RequestBody ApiRequest request) {
        return llmProxyService.processLlmRequestAsync(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/proxy/batch")
    public ResponseEntity<Map<String, LlmResponse>> batchProcessLlmRequests(@RequestBody List<LlmRequest> requests) {
        // 确保每个请求都有数据类型设置
        requests.forEach(request -> {
            if (request.getDataType() == null) {
                request.setDataType("TEXT");
            }
        });
        Map<String, LlmResponse> responses = llmProxyService.batchProcessLlmRequests(requests);
        return ResponseEntity.ok(responses);
    }
    
    //异步处理结构化数据的LLM请求
    @PostMapping("/proxy/async/structured")
    public CompletableFuture<ResponseEntity<LlmResponse>> processStructuredLlmRequestAsync(
            @RequestBody Map<String, Object> structuredData,
            @RequestParam(defaultValue = "DEEPSEEK") String provider,
            @RequestParam(required = false) String model) {
        // 构建LLM请求
        LlmRequest request = LlmRequest.builder()
                .provider(LlmProvider.valueOf(provider.toUpperCase()))
                .model(model)
                .dataType("JSON")
                .parameters(Map.of("structured_data", structuredData))
                .build();
                
        return llmProxyService.processLlmRequestAsync(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/proxy/batch/legacy")
    public ResponseEntity<Map<String, ApiResponse>> batchProcessLlmRequestsLegacy(@RequestBody List<ApiRequest> requests) {
        Map<String, ApiResponse> responses = llmProxyService.batchProcessLlmRequestsLegacy(requests);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<LlmProvider, Boolean>> getProviders() {
        Map<LlmProvider, Boolean> providerStatus = llmProxyService.testAllProviders();
        return ResponseEntity.ok(providerStatus);
    }

    @GetMapping("/configs")
    public ResponseEntity<Map<LlmProvider, Object>> getConfigs() {
        Map<LlmProvider, Object> configs = configService.getAllConfigs().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            var config = entry.getValue();
                            return Map.of(
                                    "enabled", configService.isProviderEnabled(entry.getKey()),
                                    "model", config.getModel(),
                                    "url", config.getApiUrl()
                            );
                        }
                ));
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/providers/{provider}/test")
    public ResponseEntity<String> testProvider(@PathVariable LlmProvider provider) {
        try {
            boolean isValid = configService.isProviderEnabled(provider);
            return ResponseEntity.ok("提供商 " + provider + " 测试结果: " + (isValid ? "正常" : "配置异常"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("提供商测试失败: " + e.getMessage());
        }
    }
}
