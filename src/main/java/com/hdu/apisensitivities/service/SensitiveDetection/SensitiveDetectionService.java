package com.hdu.apisensitivities.service.SensitiveDetection;

import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.service.ScenarioPerception.ScenarioAnalysisResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface SensitiveDetectionService {
    /**
     * 检测文本中的敏感信息
     * @param text 待检测文本
     * @param language 语言类型
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfo(String text, String language);

    /**
     * 检测文本中的敏感信息（按指定类型范围）
     * @param text 待检测文本
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfo(String text, String language, Set<String> includeTypes);

    /**
     * 检测文本中的敏感信息（基于情景分析结果）
     * @param text 待检测文本
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @param context 情景分析结果，包含动态阈值和规则
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfo(String text, String language, Set<String> includeTypes, ScenarioAnalysisResult context);

    /**
     * 检测结构化数据中的敏感信息
     * @param structuredData 待检测的结构化数据（JSON/XML等）
     * @param language 语言类型
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfoInStructuredData(Map<String, Object> structuredData, String language);

    /**
     * 检测结构化数据中的敏感信息（按指定类型范围）
     * @param structuredData 待检测的结构化数据（JSON/XML等）
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfoInStructuredData(Map<String, Object> structuredData, String language, Set<String> includeTypes);

    /**
     * 检测二进制数据中的敏感信息
     * @param binaryData 待检测的二进制数据
     * @param dataType 数据类型（如PDF、IMAGE、AUDIO等）
     * @param language 语言类型
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfoInBinary(byte[] binaryData, String dataType, String language);

    /**
     * 检测二进制数据中的敏感信息（按指定类型范围）
     * @param binaryData 待检测的二进制数据
     * @param dataType 数据类型（如PDF、IMAGE、AUDIO等）
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectSensitiveInfoInBinary(byte[] binaryData, String dataType, String language, Set<String> includeTypes);

    /**
     * 添加自定义检测模式
     * @param patternName 模式名称
     * @param regex 正则表达式
     */
    void addCustomPattern(String patternName, String regex);

    /**
     * 移除自定义检测模式
     * @param patternName 模式名称
     */
    void removeCustomPattern(String patternName);

    /**
     * 获取所有自定义检测模式
     * @return 模式名称和Pattern对象的映射
     */
    Map<String, Pattern> getCustomPatterns();

    /**
     * 根据数据类型检测敏感信息
     * @param data 待检测数据（可以是String、Map、byte[]等）
     * @param dataType 数据类型（TEXT、JSON、XML、IMAGE、AUDIO等）
     * @param language 语言类型
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectByDataType(Object data, String dataType, String language);

    /**
     * 根据数据类型检测敏感信息（按指定类型范围）
     * @param data 待检测数据（可以是String、Map、byte[]等）
     * @param dataType 数据类型（TEXT、JSON、XML、IMAGE、AUDIO等）
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @return 敏感信息实体列表
     */
    List<SensitiveEntity> detectByDataType(Object data, String dataType, String language, Set<String> includeTypes);

    /**
     * 批量检测文本中的敏感信息
     * @param texts 待检测文本列表
     * @param language 语言类型
     * @return 文本和对应敏感信息实体列表的映射
     */
    Map<String, List<SensitiveEntity>> batchDetect(List<String> texts, String language);

    /**
     * 批量检测文本中的敏感信息（按指定类型范围）
     * @param texts 待检测文本列表
     * @param language 语言类型
     * @param includeTypes 需要检测的敏感类型集合
     * @return 文本和对应敏感信息实体列表的映射
     */
    Map<String, List<SensitiveEntity>> batchDetect(List<String> texts, String language, Set<String> includeTypes);

    /**
     * 批量检测结构化数据中的敏感信息
     * @param dataMap 待检测的结构化数据映射
     * @param language 语言类型
     * @return 键和对应敏感信息实体列表的映射
     */
    Map<String, List<SensitiveEntity>> batchDetectStructuredData(Map<String, Map<String, Object>> dataMap, String language);

    /**
     * 验证检测准确率
     * @param testDataPath 测试数据路径
     * @return 准确率是否通过
     */
    boolean validateAccuracy(String testDataPath);

}
