package com.hdu.apisensitivities.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveEntity {
    private SensitiveType type;
    private String originalText;
    private int start;
    private int end;
    private double confidence;
    private Map<String, Object> metadata; // 添加元数据字段，用于存储结构化数据的额外信息
    private String content;
    
    // 补上这个构造函数
    public SensitiveEntity(SensitiveType type, String content, int start, int end) {
        this.type = type;
        this.content = content;
        this.start = start;
        this.end = end;
    }

    // 初始化metadata的方法
    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }
    
    // 便捷方法，用于添加字段路径信息
    public void addFieldPath(String fieldPath) {
        getMetadata().put("fieldPath", fieldPath);
    }
    
    // 便捷方法，用于添加其他元数据信息
    public void addMetadata(String key, Object value) {
        getMetadata().put(key, value);
    }
}