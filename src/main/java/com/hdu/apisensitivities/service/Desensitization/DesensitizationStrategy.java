package com.hdu.apisensitivities.service.Desensitization;

import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.entity.SensitiveType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DesensitizationStrategy {
    /**
     * 对文本进行脱敏处理
     * @param text 原始文本
     * @param sensitiveEntities 检测到的敏感实体列表
     * @return 脱敏后的文本
     */
    String desensitize(String text, List<SensitiveEntity> sensitiveEntities);

    /**
     * 对结构化数据进行脱敏处理
     * @param structuredData 原始结构化数据
     * @param sensitiveEntities 检测到的敏感实体列表
     * @return 脱敏后的结构化数据
     */
    Map<String, Object> desensitizeStructuredData(Map<String, Object> structuredData, List<SensitiveEntity> sensitiveEntities);

    /**
     * 对二进制数据进行脱敏处理
     * @param binaryData 原始二进制数据
     * @param dataType 数据类型
     * @param sensitiveEntities 检测到的敏感实体列表
     * @return 脱敏后的二进制数据
     */
    byte[] desensitizeBinaryData(byte[] binaryData, String dataType, List<SensitiveEntity> sensitiveEntities);

    /**
     * 获取策略支持的敏感类型
     * @return 支持的敏感类型集合
     */
    Set<SensitiveType> supportedTypes();

    /**
     * 获取策略支持的数据类型
     * @return 支持的数据类型集合
     */
    Set<String> supportedDataTypes();

    /**
     * 检查是否支持指定的数据类型
     * @param dataType 数据类型
     * @return 是否支持
     */
    boolean supportsDataType(String dataType);

    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getName();
}
