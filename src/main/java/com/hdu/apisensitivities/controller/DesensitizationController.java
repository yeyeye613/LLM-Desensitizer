package com.hdu.apisensitivities.controller;

import com.hdu.apisensitivities.service.DataParser.DataParserManager;
import com.hdu.apisensitivities.entity.DesensitizationRequest;
import com.hdu.apisensitivities.entity.DesensitizationResponse;
import com.hdu.apisensitivities.service.DesensitizationManager;
import com.hdu.apisensitivities.service.SensitiveDetection.SensitiveDetectionService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// 【入口1】接收HTTP请求
@Slf4j
@RestController
@RequestMapping("/desensitize")
public class DesensitizationController {

    private final DesensitizationManager desensitizationManager;
    private final DataParserManager dataParserManager;

    @Autowired
    public DesensitizationController(DesensitizationManager desensitizationManager,
                                     SensitiveDetectionService detectionService,
                                     DataParserManager dataParserManager) {
        this.desensitizationManager = desensitizationManager;
        this.dataParserManager = dataParserManager;
    }

    @PostMapping("/text")
    public ResponseEntity<DesensitizationResponse> desensitizeText(
            @RequestBody DesensitizationRequest request) {
        // 确保数据类型设置正确
        if (request.getDataType() == null) {
            request.setDataType("TEXT");
        }
        DesensitizationResponse response = desensitizationManager.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 处理结构化数据脱敏请求
     * 支持JSON、XML等结构化数据格式
     */
    @PostMapping("/structured")
    public ResponseEntity<DesensitizationResponse> desensitizeStructuredData(
            @RequestBody DesensitizationRequest request) {
        // 验证请求是否包含结构化数据
        if (!request.isStructuredData()) {
        DesensitizationResponse response = DesensitizationResponse.builder()
                .originalContent(null)
                .desensitizedContent(null)
                .detectedEntities(Collections.emptyList()) // 注意字段名是 detectedEntities
                .success(false)
                .message("无效的结构化数据请求")
                .scenarioType("ERROR") // 新增字段，建议赋值
                .build(); // 结尾加 build()
                
        return ResponseEntity.badRequest().body(response);}
        DesensitizationResponse response = desensitizationManager.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 处理二进制数据脱敏请求
     * 支持图片、音频、PDF等媒体文件
     */
    @PostMapping("/binary")
    public ResponseEntity<DesensitizationResponse> desensitizeBinaryData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dataType", required = false) String dataType,
            @RequestParam(value = "language", required = false) String language) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                
                DesensitizationResponse errorResponse = new DesensitizationResponse(
                        null,                           // originalContent
                        null,                           // desensitizedContent
                        Collections.emptyList(),        // detectedEntities (新字段名)
                        false,                          // success
                        "上传的文件为空",
                        "UNKNOWN"                       // scenarioType (新增的第6个参数)
                );
            }

            // 构建脱敏请求
            DesensitizationRequest request = DesensitizationRequest.builder()
                    .binaryData(file.getBytes())
                    .dataType(dataType != null ? dataType : dataParserManager.inferDataType(file.getOriginalFilename()))
                    .language(language != null ? language : "zh")
                    .build();

            DesensitizationResponse response = desensitizationManager.process(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("脱敏处理发生异常", e);
                DesensitizationResponse errorResponse = new DesensitizationResponse(
                    null,                           // originalContent
                    null,                           // desensitizedContent
                    Collections.emptyList(),        // detectedEntities (新字段名)
                    false,                          // success
                    "系统内部错误: " + e.getMessage(), // message
                    "UNKNOWN"                       // scenarioType (新增的第6个参数)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, DesensitizationResponse>> batchDesensitize(
            @RequestBody List<DesensitizationRequest> requests) {
        Map<String, DesensitizationResponse> responses = requests.parallelStream()
                .collect(java.util.stream.Collectors.toMap(
                        DesensitizationRequest::getContent,
                        desensitizationManager::process
                ));
        return ResponseEntity.ok(responses);
    }


    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is healthy");
    }
}