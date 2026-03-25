package com.hdu.apisensitivities.service.LlmClient;

import com.hdu.apisensitivities.config.LlmConfig;
import com.hdu.apisensitivities.entity.LlmProvider;

import java.util.Map;

public interface LlmClient {

    //发送文本请求到LLM API（向后兼容）
    String sendRequest(String prompt, LlmConfig config, Map<String, Object> parameters);

    //发送结构化数据请求到LLM API
    String sendStructuredRequest(Map<String, Object> structuredData, LlmConfig config, Map<String, Object> parameters);

    //发送二进制数据请求到LLM API
    String sendBinaryRequest(byte[] binaryData, String dataType, LlmConfig config, Map<String, Object> parameters);

    //支持的提供商
    LlmProvider getSupportedProvider();

    //验证配置是否有效
    boolean validateConfig(LlmConfig config);
    
    //检查客户端是否支持指定的数据类型
    boolean supportsDataType(String dataType);
}
