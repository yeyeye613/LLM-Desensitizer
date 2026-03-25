package com.hdu.apisensitivities.service.ScenarioPerception;

import com.hdu.apisensitivities.entity.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 情景分析结果类
 * 包含分析出的情景类型和相关参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioAnalysisResult {

    /**
     * 情景类型枚举
     */
    public enum ScenarioType {
        GENERAL_CHAT,        // 普通聊天
        CUSTOMER_SERVICE,    // 客户服务
        MEDICAL_CONSULTATION,// 医疗咨询
        FINANCIAL_ADVICE,    // 财务建议
        LEGAL_ADVICE,        // 法律咨询
        HR_RECRUITMENT,      // 招聘
        EDUCATION,           // 教育
        TECHNICAL_SUPPORT,   // 技术支持
        CODE_DEVELOPMENT,    // 代码开发
        GOVERNMENT_SERVICE,  // 政务服务
        UNKNOWN              // 未知
    }

    private ScenarioType scenarioType;        // 分析出的情景类型
    private double confidence;               // 情景识别的置信度
    private Set<String> sensitiveTypesToInclude;  // 需要包含的敏感信息类型
    private Set<String> sensitiveTypesToExclude;  // 需要排除的敏感信息类型
    private Map<String, Object> parameters;   // 情景相关参数
    
    private Map<SensitiveType, Double> typeConfidenceThresholds; // 针对不同敏感类型的置信度阈值
    private Map<SensitiveType, Boolean> typeStrictModes;         // 针对不同敏感类型的严格模式开关

    // 初始化参数的方法
    public Map<String, Object> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        return parameters;
    }

    // 便捷方法，用于添加参数
    public void addParameter(String key, Object value) {
        getParameters().put(key, value);
    }

    // 检查是否应该包含某种敏感信息类型
    public boolean shouldIncludeType(String type) {
        if (sensitiveTypesToInclude != null && !sensitiveTypesToInclude.isEmpty()) {
            return sensitiveTypesToInclude.contains(type);
        }
        if (sensitiveTypesToExclude != null && !sensitiveTypesToExclude.isEmpty()) {
            return !sensitiveTypesToExclude.contains(type);
        }
        return true;
    }

    // 获取特定类型的置信度阈值
    public double getThresholdForType(SensitiveType type, double defaultThreshold) {
        if (typeConfidenceThresholds != null && typeConfidenceThresholds.containsKey(type)) {
            return typeConfidenceThresholds.get(type);
        }
        return defaultThreshold;
    }

    // 获取特定类型的严格模式设置
    public boolean isStrictModeForType(SensitiveType type) {
        if (typeStrictModes != null && typeStrictModes.containsKey(type)) {
            return typeStrictModes.get(type);
        }
        return false;
    }
}