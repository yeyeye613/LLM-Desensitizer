package com.hdu.apisensitivities.service;

import com.hdu.apisensitivities.config.LlmConfig;
import com.hdu.apisensitivities.entity.LlmProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class LlmConfigService {

    private final Map<LlmProvider, LlmConfig> configMap = new HashMap<>();

    @Value("${llm.providers.ollama.url:http://localhost:11434/v1/chat/completions}")
    private String ollamaUrl;

    @Value("${llm.providers.ollama.key:ollama}")
    private String ollamaKey;

    @Value("${llm.providers.ollama.model:deepseek-r1:7b}")
    private String ollamaModel;

    @Value("${llm.providers.openai.url:https://api.openai.com/v1/chat/completions}")
    private String openaiUrl;

    @Value("${llm.providers.openai.key}")
    private String openaiKey;

    @Value("${llm.providers.openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${llm.providers.deepseek.url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekUrl;

    @Value("${llm.providers.deepseek.key}")
    private String deepseekKey;

    @Value("${llm.providers.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${llm.providers.doubao.url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
    private String doubaoUrl;

    @Value("${llm.providers.doubao.key}")
    private String doubaoKey;

    @Value("${llm.providers.doubao.model:doubao-pro}")
    private String doubaoModel;

    @Value("${llm.providers.qwen.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String qwenUrl;

    @Value("${llm.providers.qwen.key}")
    private String qwenKey;

    @Value("${llm.providers.qwen.model:qwen-plus}")
    private String qwenModel;
    
    @Value("${llm.providers.kimi.url:https://api.moonshot.cn/v1/chat/completions}")
    private String kimiUrl;

    @Value("${llm.providers.kimi.key}")
    private String kimiKey;

    @Value("${llm.providers.kimi.model:moonshot-v1-8k}")
    private String kimiModel;

    @Value("${llm.providers.hunyuan.url:https://hunyuan.cloud.tencent.com/v1/chat/completions}")
    private String hunyuanUrl;

    @Value("${llm.providers.hunyuan.key}")
    private String hunyuanKey;

    @Value("${llm.providers.hunyuan.model:hunyuan-pro}")
    private String hunyuanModel;

    @Value("${llm.default.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${llm.default.maxTokens:1000}")
    private Integer defaultMaxTokens;

    @PostConstruct
    public void init() {
        // 新增：初始化 Ollama 配置
        configMap.put(LlmProvider.OLLAMA, LlmConfig.builder()
                .provider(LlmProvider.OLLAMA)
                .apiUrl(ollamaUrl)
                .apiKey(ollamaKey)
                .model(ollamaModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());

        log.info("LLM配置初始化完成，已包含本地 Ollama 提供商");

        // 初始化OpenAI配置
        configMap.put(LlmProvider.OPENAI, LlmConfig.builder()
                .provider(LlmProvider.OPENAI)
                .apiUrl(openaiUrl)
                .apiKey(openaiKey)
                .model(openaiModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());

        // 初始化DeepSeek配置
        configMap.put(LlmProvider.DEEPSEEK, LlmConfig.builder()
                .provider(LlmProvider.DEEPSEEK)
                .apiUrl(deepseekUrl)
                .apiKey(deepseekKey)
                .model(deepseekModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());

        // 初始化豆包配置
        configMap.put(LlmProvider.DOUBAO, LlmConfig.builder()
                .provider(LlmProvider.DOUBAO)
                .apiUrl(doubaoUrl)
                .apiKey(doubaoKey)
                .model(doubaoModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());
                
        // 初始化通义千问配置
        configMap.put(LlmProvider.QWEN, LlmConfig.builder()
                .provider(LlmProvider.QWEN)
                .apiUrl(qwenUrl)
                .apiKey(qwenKey)
                .model(qwenModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());
        
        // 初始化Kimi配置
        configMap.put(LlmProvider.KIMI, LlmConfig.builder()
                .provider(LlmProvider.KIMI)
                .apiUrl(kimiUrl)
                .apiKey(kimiKey)
                .model(kimiModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());
        
        // 初始化腾讯混元配置
        configMap.put(LlmProvider.HUNYUAN, LlmConfig.builder()
                .provider(LlmProvider.HUNYUAN)
                .apiUrl(hunyuanUrl)
                .apiKey(hunyuanKey)
                .model(hunyuanModel)
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .build());

        log.info("LLM配置初始化完成，已配置 {} 个提供商", configMap.size());
    }

    // 修改此方法：如果请求没指定提供商，可以默认走本地
    public LlmConfig getConfigOrDefault(LlmProvider provider) {
        return configMap.getOrDefault(provider, configMap.get(LlmProvider.OLLAMA));
    }

    public Map<LlmProvider, LlmConfig> getAllConfigs() {
        return new HashMap<>(configMap);
    }

    public boolean isProviderEnabled(LlmProvider provider) {
        LlmConfig config = configMap.get(provider);
        // 开发环境下简化检查，允许空API密钥进行测试
        // 生产环境中应该严格检查API密钥
        return config != null;
    }
}
