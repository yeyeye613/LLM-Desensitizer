package com.hdu.apisensitivities.benchmark;

import lombok.Data;
import java.util.List;
import java.util.Map;
import com.hdu.apisensitivities.entity.Message; 
@Data
public class TestDataDTO {
    private String id;
    private String content;
    private String language;
    private List<Message> history;
    private List<Map<String, Object>> expected_entities; // 对应 JSON 中的 entities 数组
}