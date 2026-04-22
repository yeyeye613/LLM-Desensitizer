package com.hdu.apisensitivities.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.apisensitivities.config.LlmConfig;
import com.hdu.apisensitivities.entity.*;
import com.hdu.apisensitivities.service.LlmClient.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LLM 代理服务，负责处理大语言模型请求的全流程。
 * <p>
 * 主要功能包括：
 * <ul>
 *     <li>接收 {@link LlmRequest} 或 {@link ApiRequest} 请求</li>
 *     <li>根据数据类型（文本/JSON/二进制等）进行敏感信息脱敏</li>
 *     <li>调用对应的 {@link LlmClient} 实现与真实 LLM API 交互</li>
 *     <li>对返回内容再次脱敏并封装为 {@link LlmResponse} 或 {@link ApiResponse}</li>
 *     <li>支持同步、异步、批量处理及提供商健康测试</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class LlmProxyService {

    private final DesensitizationManager desensitizationManager;
    private final LlmConfigService configService;
    private final Map<LlmProvider, LlmClient> llmClients;

    /**
     * 构造 LLM 代理服务实例。
     *
     * @param desensitizationManager 敏感信息脱敏管理器，用于输入/输出的内容脱敏
     * @param configService          LLM 提供商配置服务，获取各提供商的 API 密钥、端点等配置
     * @param clients                所有已注册的 LLM 客户端实现，将按支持的提供商自动映射
     */
    @Autowired
    public LlmProxyService(DesensitizationManager desensitizationManager,
                           LlmConfigService configService,
                           List<LlmClient> clients) {
        this.desensitizationManager = desensitizationManager;
        this.configService = configService;
        this.llmClients = clients.stream()
                .collect(Collectors.toMap(LlmClient::getSupportedProvider, client -> client));
    }

    /**
     * 同步处理单个 LLM 请求（新版请求模型）。
     * <p>
     * 处理流程：输入脱敏 → 调用 LLM API → 输出脱敏 → 封装响应。
     * 支持的数据类型包括 TEXT、JSON、XML、IMAGE、AUDIO、PDF、DOC 等。
     * </p>
     *
     * @param request LLM 请求对象，必须包含 prompt 或结构化数据/二进制数据
     * @return 处理结果，包含原始响应、脱敏响应、检测到的敏感实体、耗时等信息
     * @throws RuntimeException 当提供商未启用、配置无效或 API 调用失败时抛出
     */
    // TODO: 日志和计时都可以拆分成注解的
    public LlmResponse processLlmRequest(LlmRequest request) {
        Instant start = Instant.now();

        try {
            LlmProvider provider = request.getProvider();
            LlmConfig config = configService.getConfigOrDefault(provider);

            log.info("开始处理LLM请求，提供商: {}, 会话ID: {}, 数据类型: {}",
                    provider, request.getSessionId(), request.getDataType());

            // 验证提供商是否启用
            if (!configService.isProviderEnabled(provider)) {
                throw new RuntimeException("LLM提供商未启用或配置不完整: " + provider);
            }

            // 根据数据类型执行不同的脱敏逻辑
            DesensitizationResult result = processWithDataSensitiveProtection(request, config);

            long processingTime = Duration.between(start, Instant.now()).toMillis();

            // 构建响应
            LlmResponse.LlmResponseBuilder responseBuilder = LlmResponse.builder()
                    .originalResponse(result.getOriginalResponse())
                    .desensitizedResponse(result.getDesensitizedResponse())
                    .inputSensitiveEntities(result.getInputEntities())
                    .outputSensitiveEntities(result.getOutputEntities())
                    .provider(provider)
                    .model(config.getModel())
                    .processingTimeMs(processingTime)
                    .success(true);

            // 设置响应数据类型
            responseBuilder.dataType(request.getDataType());

            // 对于JSON和XML类型的响应，尝试解析为结构化数据
            if ("JSON".equals(request.getDataType()) || "XML".equals(request.getDataType())) {
                try {
                    // 尝试将脱敏后的响应解析为结构化数据
                    Map<String, Object> structuredData = parseJson(result.getDesensitizedResponse());
                    if (structuredData != null && !structuredData.isEmpty()) {
                        responseBuilder.structuredResponse(structuredData);
                    }
                } catch (Exception e) {
                    log.warn("无法将响应解析为结构化数据: {}", e.getMessage());
                    // 解析失败不影响返回，仍然返回文本形式的响应
                }
            }

            return responseBuilder.build();

        } catch (Exception e) {
            log.error("处理LLM请求失败", e);
            long processingTime = Duration.between(start, Instant.now()).toMillis();

            return LlmResponse.builder()
                    .originalResponse(null)
                    .desensitizedResponse(null)
                    .inputSensitiveEntities(List.of())
                    .outputSensitiveEntities(List.of())
                    .provider(request.getProvider())
                    .processingTimeMs(processingTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    
    //处理LLM请求（兼容旧版ApiRequest）
    public ApiResponse processLlmRequest(ApiRequest request) {
        LlmRequest llmRequest = request.toLlmRequest();
        LlmResponse llmResponse = processLlmRequest(llmRequest);
        return llmResponse.toApiResponse();
    }

    /**
     * 异步处理 LLM 请求（新版）。
     *
     * @param request LLM 请求对象
     * @return 包含 {@link LlmResponse} 的 CompletableFuture
     */
    @Async
    public CompletableFuture<LlmResponse> processLlmRequestAsync(LlmRequest request) {
        return CompletableFuture.completedFuture(processLlmRequest(request));
    }

    @Async
    public CompletableFuture<ApiResponse> processLlmRequestAsync(ApiRequest request) {
        return CompletableFuture.completedFuture(processLlmRequest(request));
    }

    //批量处理LLM请求
    public Map<String, LlmResponse> batchProcessLlmRequests(List<LlmRequest> requests) {
        return requests.parallelStream()
                .collect(Collectors.toMap(
                        LlmRequest::getSessionId,
                        this::processLlmRequest
                ));
    }

    public Map<String, ApiResponse> batchProcessLlmRequestsLegacy(List<ApiRequest> requests) {
        return requests.parallelStream()
                .collect(Collectors.toMap(
                        ApiRequest::getSessionId,
                        this::processLlmRequest
                ));
    }

    /**
     * 测试所有已注册 LLM 提供商的配置是否有效。
     * <p>
     * 依次调用每个客户端的 {@link LlmClient#validateConfig(LlmConfig)} 方法。
     * </p>
     *
     * @return 每个提供商对应的测试结果（true=配置有效，false=无效或异常）
     */
    public Map<LlmProvider, Boolean> testAllProviders() {
        return llmClients.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                LlmConfig config = configService.getConfigOrDefault(entry.getKey());
                                return configService.isProviderEnabled(entry.getKey()) &&
                                        entry.getValue().validateConfig(config);
                            } catch (Exception e) {
                                log.warn("提供商测试失败: {}", entry.getKey(), e);
                                return false;
                            }
                        }
                ));
    }

    /**
     * 根据请求的数据类型执行输入脱敏、API 调用和输出脱敏的核心逻辑。
     *
     * @param request 原始 LLM 请求
     * @param config  对应提供商的配置
     * @return 封装了原始响应、脱敏响应及输入/输出敏感实体的结果对象
     */
    private DesensitizationResult processWithDataSensitiveProtection(LlmRequest request, LlmConfig config) {
        // 1. 输入预处理与脱敏
        DesensitizationRequest inputRequest = buildDesensitizationRequestForLlm(request);
        DesensitizationResponse inputDesensitized = desensitizationManager.process(inputRequest);
        
        log.info("输入脱敏完成，数据类型: {}, 检测到 {} 个敏感实体", 
                inputRequest.getDataType(), inputDesensitized.getDetectedEntities().size());
        
        // 2. 根据数据类型选择合适的API调用方式
        String llmResponse = callLlmApiWithDataType(inputRequest, inputDesensitized, config, request.getParameters(), request.getProvider());
        
        // 3. 输出脱敏 - 保持与输入相同的数据类型进行处理
        DesensitizationRequest outputRequest = buildDesensitizationResponseForOutput(llmResponse, request);
        DesensitizationResponse outputDesensitized = desensitizationManager.process(outputRequest);
        
        log.info("输出脱敏完成，数据类型: {}, 检测到 {} 个敏感实体", 
                outputRequest.getDataType(), outputDesensitized.getDetectedEntities().size());
        
        return new DesensitizationResult(
                llmResponse,
                outputDesensitized.getDesensitizedContent(),
                inputDesensitized.getDetectedEntities(),
                outputDesensitized.getDetectedEntities()
        );
    }
    
    // 创建基本的脱敏请求对象
    private DesensitizationRequest createBaseDesensitizationRequest(LlmRequest request, String dataType) {
        DesensitizationRequest desensitizationRequest = new DesensitizationRequest();
        desensitizationRequest.setLanguage("mixed");
        desensitizationRequest.setStrictMode(true);
        desensitizationRequest.setBlacklist(request.getBlacklist());
        desensitizationRequest.setWhitelist(request.getWhitelist());
        desensitizationRequest.setPreserveStructure(true);
        desensitizationRequest.setDataType(dataType);
        return desensitizationRequest;
    }
    
    // 为LLM请求构建脱敏请求
    private DesensitizationRequest buildDesensitizationRequestForLlm(LlmRequest request) {
        String dataType = request.getDataType() != null ? request.getDataType() : "TEXT";
        
        DesensitizationRequest desensitizationRequest = createBaseDesensitizationRequest(request, dataType);
        
        // 根据数据类型设置不同的内容
        switch (dataType) {
            case "JSON", "XML":
                // 处理结构化数据
                if (request.getParameters() != null && request.getParameters().containsKey("structuredData")) {
                    Object structuredDataObj = request.getParameters().get("structuredData");
                    if (structuredDataObj instanceof Map) {
                        Map<String, Object> structuredData = (Map<String, Object>) structuredDataObj;
                        desensitizationRequest.setStructuredData(structuredData);
                        log.debug("使用参数中的结构化数据，字段数量: {}", structuredData != null ? structuredData.size() : 0);
                    } else {
                        log.warn("structuredData参数不是Map类型，实际类型: {}", structuredDataObj != null ? structuredDataObj.getClass().getName() : "null");
                    }
                } else if (request.getPrompt() != null) {
                    // 尝试将prompt解析为JSON
                    try {
                        Map<String, Object> parsedData = parseJson(request.getPrompt());
                        if (parsedData != null && !parsedData.isEmpty()) {
                            desensitizationRequest.setStructuredData(parsedData);
                            log.debug("成功将prompt解析为结构化数据，字段数量: {}", parsedData.size());
                        } else {
                            // 解析成功但数据为空，使用原始prompt
                            desensitizationRequest.setContent(request.getPrompt());
                            log.debug("prompt解析为结构化数据但为空，使用原始文本");
                        }
                    } catch (Exception e) {
                        // 解析失败，使用原始prompt
                        desensitizationRequest.setContent(request.getPrompt());
                        log.debug("无法将prompt解析为结构化数据: {}, 使用原始文本", e.getMessage());
                    }
                }
                break;
            case "IMAGE", "AUDIO", "PDF", "DOC":
                // 处理二进制数据
                if (request.getParameters() != null && request.getParameters().containsKey("binaryData")) {
                    Object binaryDataObj = request.getParameters().get("binaryData");
                    if (binaryDataObj instanceof byte[]) {
                        byte[] binaryData = (byte[]) binaryDataObj;
                        desensitizationRequest.setBinaryData(binaryData);
                        log.debug("使用二进制数据，大小: {} 字节", binaryData != null ? binaryData.length : 0);
                    } else {
                        log.warn("binaryData参数不是byte[]类型，实际类型: {}", binaryDataObj != null ? binaryDataObj.getClass().getName() : "null");
                    }
                }
                // 同时也处理文本描述
                if (request.getPrompt() != null) {
                    desensitizationRequest.setContent(request.getPrompt());
                    log.debug("使用文本描述: {}", request.getPrompt().length() > 100 ? 
                            request.getPrompt().substring(0, 100) + "..." : request.getPrompt());
                }
                break;
            default:
                // 默认处理文本
                desensitizationRequest.setContent(request.getPrompt());
                log.debug("使用文本数据: {}", request.getPrompt() != null && request.getPrompt().length() > 100 ? 
                        request.getPrompt().substring(0, 100) + "..." : request.getPrompt());
                break;
        }
        
        return desensitizationRequest;
    }
    
    // 为输出构建脱敏请求
    private DesensitizationRequest buildDesensitizationResponseForOutput(String response, LlmRequest request) {
        String inputDataType = request.getDataType() != null ? request.getDataType() : "TEXT";
        
        DesensitizationRequest desensitizationRequest = createBaseDesensitizationRequest(request, inputDataType); // 默认保持与输入相同的数据类型
        
        // 根据输入数据类型和响应内容决定如何处理输出
        switch (inputDataType) {
            case "JSON":
                // 对于JSON输入，尝试将响应解析为JSON
                if (response != null) {
                    try {
                        Map<String, Object> parsedData = parseJson(response);
                        if (parsedData != null && !parsedData.isEmpty()) {
                            desensitizationRequest.setStructuredData(parsedData);
                            log.debug("输出响应成功解析为JSON，字段数量: {}", parsedData.size());
                        } else {
                            // 解析成功但数据为空，作为文本处理
                            desensitizationRequest.setContent(response);
                            desensitizationRequest.setDataType("TEXT"); // 更新为文本类型
                            log.debug("输出响应解析为JSON但为空，作为文本处理");
                        }
                    } catch (Exception e) {
                        // 解析失败，作为文本处理
                        desensitizationRequest.setContent(response);
                        desensitizationRequest.setDataType("TEXT"); // 更新为文本类型
                        log.debug("无法将输出响应解析为JSON: {}, 作为文本处理", e.getMessage());
                    }
                }
                break;
            case "XML":
                // 对于XML输入，尝试将响应解析为结构化数据
                if (response != null) {
                    try {
                        // 这里可以添加XML解析逻辑
                        // 暂时作为文本处理
                        desensitizationRequest.setContent(response);
                        desensitizationRequest.setDataType("TEXT"); // 更新为文本类型
                    } catch (Exception e) {
                        desensitizationRequest.setContent(response);
                        desensitizationRequest.setDataType("TEXT"); // 更新为文本类型
                    }
                }
                break;
            case "IMAGE", "AUDIO", "PDF", "DOC":
                // 对于二进制输入，响应通常是文本描述
                desensitizationRequest.setContent(response);
                desensitizationRequest.setDataType("TEXT"); // 更新为文本类型
                break;
            default:
                // 默认作为文本处理
                desensitizationRequest.setContent(response);
                break;
        }
        
        return desensitizationRequest;
    }
    
    
    /**
     * 为不同数据类型准备调用 LLM API 时的参数。
     * <p>
     * 会添加元数据（如数据类型、是否包含敏感信息），并移除原始参数中的二进制或结构化数据字段。
     * </p>
     *
     * @param inputRequest        脱敏请求对象
     * @param inputDesensitized   输入脱敏结果
     * @param originalParams      原始扩展参数
     * @return 处理后的参数字典
     */
    private String callLlmApiWithDataType(DesensitizationRequest inputRequest, 
                                         DesensitizationResponse inputDesensitized,
                                         LlmConfig config, 
                                         Map<String, Object> parameters, 
                                         LlmProvider provider) {
        log.info("调用真实LLM API，提供商: {}, 数据类型: {}, 敏感实体数: {}", 
                provider, inputRequest.getDataType(), inputDesensitized.getDetectedEntities().size());
        
        LlmClient client = llmClients.get(provider);
        if (client == null) {
            throw new RuntimeException("不支持的LLM提供商: " + provider);
        }
        
        // 检查客户端配置是否有效
        if (!client.validateConfig(config)) {
            throw new RuntimeException("LLM客户端配置无效: " + provider);
        }
        
        // 获取数据类型并标准化
        String dataType = inputRequest.getDataType() != null ? inputRequest.getDataType().toUpperCase() : "TEXT";
        
        // 检查客户端是否支持指定的数据类型
        boolean isDataTypeSupported = client.supportsDataType(dataType);
        if (!isDataTypeSupported) {
            log.warn("客户端 {} 不直接支持数据类型 {}，将使用兼容模式处理", 
                    provider, dataType);
        }
        
        // 根据数据类型准备参数
        Map<String, Object> processedParams = prepareParamsForDataType(inputRequest, inputDesensitized, parameters);
        
        // 根据数据类型调用相应的方法
        try {
            if (inputRequest.isBinaryData()) {
                if (isDataTypeSupported) {
                    // 二进制数据处理 - 使用专门的方法
                    log.debug("使用二进制数据处理方法");
                    return client.sendBinaryRequest(
                            inputRequest.getBinaryData(), 
                            dataType, 
                            config, 
                            processedParams
                    );
                } else {
                    // 二进制数据兼容模式处理
                    log.debug("使用二进制数据兼容模式处理");
                    String prompt = generatePromptForBinaryData(inputRequest, inputDesensitized);
                    return client.sendRequest(prompt, config, processedParams);
                }
            } else if (inputRequest.isStructuredData()) {
                if (isDataTypeSupported) {
                    // 结构化数据处理 - 使用专门的方法
                    log.debug("使用结构化数据处理方法");
                    return client.sendStructuredRequest(
                            inputRequest.getStructuredData(), 
                            config, 
                            processedParams
                    );
                } else {
                    // 结构化数据兼容模式处理
                    log.debug("使用结构化数据兼容模式处理");
                    String prompt = generatePromptForStructuredData(inputRequest, inputDesensitized);
                    return client.sendRequest(prompt, config, processedParams);
                }
            } else {
                // 文本数据处理（默认情况）
                log.debug("使用文本数据处理方法");
                String content = inputDesensitized.getDesensitizedContent();
                if (content == null || content.isEmpty()) {
                    content = inputRequest.getContent() != null ? inputRequest.getContent() : "";
                    log.warn("脱敏后内容为空，使用原始内容");
                }
                return client.sendRequest(content, config, processedParams);
            }
        } catch (Exception e) {
            log.error("调用LLM API失败", e);
            throw new RuntimeException("LLM API调用失败: " + e.getMessage(), e);
        }
    }
    
    // 为不同数据类型准备参数
    private Map<String, Object> prepareParamsForDataType(DesensitizationRequest inputRequest, 
                                                       DesensitizationResponse inputDesensitized,
                                                       Map<String, Object> originalParams) {
        Map<String, Object> processedParams = new HashMap<>();
        if (originalParams != null) {
            processedParams.putAll(originalParams);
        }
        
        // 添加元数据信息
        processedParams.put("_data_type", inputRequest.getDataType());
        processedParams.put("_has_sensitive_info", !inputDesensitized.getDetectedEntities().isEmpty());
        processedParams.put("_sensitive_count", inputDesensitized.getDetectedEntities().size());
        
        // 根据数据类型添加特定参数
        if (inputRequest.isStructuredData()) {
            // 对于结构化数据，添加结构化标志
            processedParams.put("structured_data", true);
            // 添加结构化数据深度信息
            if (inputRequest.getStructuredData() != null) {
                processedParams.put("structured_depth", calculateObjectDepth(inputRequest.getStructuredData()));
            }
        } else if (inputRequest.isBinaryData()) {
            // 对于二进制数据，添加二进制标志
            processedParams.put("binary_data", true);
            if (inputRequest.getBinaryData() != null) {
                processedParams.put("binary_size", inputRequest.getBinaryData().length);
            }
        }
        
        // 移除敏感数据参数
        processedParams.remove("binaryData");
        processedParams.remove("structuredData");
        
        return processedParams;
    }
    
    // 计算对象深度（用于评估结构化数据的复杂度）
    private int calculateObjectDepth(Object obj) {
        if (obj == null) {
            return 0;
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) {
                return 1;
            }
            return 1 + map.values().stream()
                    .mapToInt(this::calculateObjectDepth)
                    .max()
                    .orElse(0);
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) {
                return 1;
            }
            return 1 + list.stream()
                    .mapToInt(this::calculateObjectDepth)
                    .max()
                    .orElse(0);
        }
        
        return 1; // 基本类型
    }
    
    // 为二进制数据生成提示
    private String generatePromptForBinaryData(DesensitizationRequest inputRequest, DesensitizationResponse inputDesensitized) {
        StringBuilder prompt = new StringBuilder();
        
        // 根据不同的数据类型生成不同的提示
        String dataType = inputRequest.getDataType() != null ? inputRequest.getDataType() : "二进制";
        
        prompt.append("# " + dataType + "数据分析任务\n\n");
        prompt.append("## 数据描述\n");
        prompt.append("- 数据类型: " + dataType + "\n");
        
        if (inputRequest.getBinaryData() != null) {
            prompt.append("- 数据大小: " + inputRequest.getBinaryData().length + " 字节\n");
        }
        
        prompt.append("\n## 数据内容\n");
        if (inputDesensitized.getDesensitizedContent() != null) {
            prompt.append(inputDesensitized.getDesensitizedContent());
        } else {
            prompt.append("[无法提取文本内容]");
        }
        
        // 添加用户原始提示（如果有）
        if (inputRequest.getContent() != null) {
            prompt.append("\n\n## 用户问题\n").append(inputRequest.getContent());
        }
        
        // 添加指令以确保回答的质量
        prompt.append("\n\n## 回答要求\n");
        prompt.append("1. 请基于提供的数据内容进行分析\n");
        prompt.append("2. 如果内容中包含敏感信息，请确保在回答中不直接引用\n");
        prompt.append("3. 请提供清晰、结构化的回答\n");
        
        return prompt.toString();
    }
    
    // 为结构化数据生成提示
    private String generatePromptForStructuredData(DesensitizationRequest inputRequest, DesensitizationResponse inputDesensitized) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# 结构化数据分析任务\n\n");
        prompt.append("## 数据描述\n");
        prompt.append("- 数据类型: " + inputRequest.getDataType() + "\n");
        
        if (inputRequest.getStructuredData() != null) {
            prompt.append("- 字段数量: " + inputRequest.getStructuredData().size() + "\n");
        }
        
        prompt.append("\n## 数据内容\n");
        if (inputDesensitized.getDesensitizedContent() != null) {
            prompt.append("```json\n" + inputDesensitized.getDesensitizedContent() + "\n```\n");
        } else {
            prompt.append("[无法提取结构化内容]");
        }
        
        // 添加用户原始提示（如果有）
        if (inputRequest.getContent() != null) {
            prompt.append("\n\n## 用户问题\n").append(inputRequest.getContent());
        }
        
        // 添加指令以确保回答的质量
        prompt.append("\n\n## 回答要求\n");
        prompt.append("1. 请分析提供的结构化数据\n");
        prompt.append("2. 按照数据的结构和层级进行有条理的分析\n");
        prompt.append("3. 如需返回结构化结果，请保持与输入相似的数据结构\n");
        prompt.append("4. 确保回答中不包含任何敏感信息\n");
        
        return prompt.toString();
    }

    // JSON解析（使用Jackson）
    private Map<String, Object> parseJson(String jsonString) {
        try {
            if (jsonString == null || jsonString.trim().isEmpty() || 
                "null".equals(jsonString.trim()) || "undefined".equals(jsonString.trim())) {
                return null;
            }
            
            // 使用Jackson解析JSON
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("JSON解析失败: {}", e.getMessage());
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
    
    // 内部类，用于封装脱敏结果
    private static class DesensitizationResult {
        private final String originalResponse;
        private final String desensitizedResponse;
        private final List<SensitiveEntity> inputEntities;
        private final List<SensitiveEntity> outputEntities;
        
        public DesensitizationResult(String originalResponse, String desensitizedResponse,
                                    List<SensitiveEntity> inputEntities, List<SensitiveEntity> outputEntities) {
            this.originalResponse = originalResponse;
            this.desensitizedResponse = desensitizedResponse;
            this.inputEntities = inputEntities;
            this.outputEntities = outputEntities;
        }
        
        public String getOriginalResponse() {
            return originalResponse;
        }
        
        public String getDesensitizedResponse() {
            return desensitizedResponse;
        }
        
        public List<SensitiveEntity> getInputEntities() {
            return inputEntities;
        }
        
        public List<SensitiveEntity> getOutputEntities() {
            return outputEntities;
        }
    }

}

