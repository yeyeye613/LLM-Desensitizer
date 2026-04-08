package com.hdu.apisensitivities.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdu.apisensitivities.entity.DesensitizationRequest;
import com.hdu.apisensitivities.entity.DesensitizationResponse;
import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.service.DesensitizationManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class DesensitizationBenchmarkTest {

    @Autowired
    private DesensitizationManager desensitizationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<String, String> TYPE_MAP = Map.of(
            "person", "CHINESE_NAME",
            "phone", "MOBILE_PHONE",
            "address", "ADDRESS",
            "email", "EMAIL"
    );

    @Test
    public void runBenchmark() throws Exception {
        // 1. 创建本次测试的日志文件
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String logFileName = String.format("benchmark_result_%s.log", timestamp);
        Path logPath = Paths.get("./logs", logFileName);

        // 确保 logs 目录存在
        Files.createDirectories(logPath.getParent());

        // 2. 准备输出流（同时输出到控制台和文件）
        List<String> allLogs = new ArrayList<>();

        // 3. 读取测试数据
        InputStream is = getClass().getClassLoader().getResourceAsStream("my_pii_test_set.json");
        List<TestDataDTO> testCases = objectMapper.readValue(is, new TypeReference<>() {});

        // 4. 统计变量
        int totalExpected = 0;
        int totalFound = 0;
        int failedCases = 0;
        List<TestCaseResult> caseResults = new ArrayList<>();

        for (TestDataDTO testCase : testCases) {
            TestCaseResult result = new TestCaseResult();
            result.setCaseId(testCase.getId());
            result.setExpectedCount(testCase.getExpected_entities().size());

            // 执行识别
            DesensitizationRequest request = DesensitizationRequest.builder()
                    .content(testCase.getContent())
                    .dataType("TEXT")
                    .language(testCase.getLanguage())
                    .autoScenarioDetection(true)
                    .build();

            DesensitizationResponse response = desensitizationManager.process(request);
            List<SensitiveEntity> actualEntities = response.getDetectedEntities();

            if (actualEntities == null) {
                actualEntities = Collections.emptyList();
            }

            // 匹配实体
            List<String> unmatchedEntities = new ArrayList<>();
            int matchedCount = 0;

            for (Map<String, Object> exp : testCase.getExpected_entities()) {
                String expText = (String) exp.get("text");
                String expType = (String) exp.get("type");

                boolean isFound = actualEntities.stream()
                        .anyMatch(a -> a.getOriginalText().equals(expText));

                if (isFound) {
                    matchedCount++;
                    totalFound++;
                } else {
                    unmatchedEntities.add(expText + "(" + expType + ")");
                }
            }

            totalExpected += testCase.getExpected_entities().size();
            result.setMatchedCount(matchedCount);
            result.setUnmatchedEntities(unmatchedEntities);

            double recall = testCase.getExpected_entities().size() == 0 ? 0 :
                    (double) matchedCount / testCase.getExpected_entities().size() * 100;
            result.setRecall(recall);
            result.setSuccess(matchedCount == testCase.getExpected_entities().size());

            if (!result.isSuccess()) {
                failedCases++;
            }

            caseResults.add(result);
        }

        // 5. 生成详细报告
        StringBuilder report = new StringBuilder();
        report.append("\n").append(repeat("=", 80)).append("\n");
        report.append("【详细测试结果】\n");
        report.append(repeat("=", 80)).append("\n\n");

        for (TestCaseResult result : caseResults) {
            String status = result.isSuccess() ? "✓ 通过" : "✗ 失败";
            String recallStr = String.format("%.2f%%", result.getRecall());

            report.append(String.format("%-20s 预期:%-3d 匹配:%-3d 召回率:%-8s %s\n",
                    result.getCaseId(),
                    result.getExpectedCount(),
                    result.getMatchedCount(),
                    recallStr,
                    status));

            if (!result.getUnmatchedEntities().isEmpty()) {
                report.append(String.format("  └─ 未识别实体: %s\n",
                        String.join(", ", result.getUnmatchedEntities())));
            }
        }

        // 6. 生成统计汇总
        double overallRecall = totalExpected == 0 ? 0 : (double) totalFound / totalExpected * 100;
        double failureRate = testCases.isEmpty() ? 0 : (double) failedCases / testCases.size() * 100;

        report.append("\n").append(repeat("=", 80)).append("\n");
        report.append("【测试统计】\n");
        report.append(repeat("=", 80)).append("\n");
        report.append(String.format("总测试用例数: %d\n", testCases.size()));
        report.append(String.format("失败用例数: %d (%.2f%%)\n", failedCases, failureRate));
        report.append(String.format("总预期实体数: %d\n", totalExpected));
        report.append(String.format("总匹配实体数: %d\n", totalFound));
        report.append(String.format("总体召回率: %.2f%%\n", overallRecall));
        report.append(repeat("=", 80)).append("\n");
        report.append(String.format("日志文件: %s\n", logFileName));
        report.append(repeat("=", 80)).append("\n");

        // 7. 输出到控制台
        System.out.println(report.toString());
        log.info(report.toString());

        // 8. 写入文件
        Files.write(logPath, report.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 9. 同时保存一份 JSON 格式的详细结果（便于后续分析）
        String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(caseResults);
        Path jsonPath = Paths.get("./logs", String.format("benchmark_detail_%s.json", timestamp));
        Files.write(jsonPath, jsonResult.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("\n✅ 测试完成！");
        System.out.println("📄 详细报告: " + logPath.toAbsolutePath());
        System.out.println("📊 JSON数据: " + jsonPath.toAbsolutePath());
    }

    // 辅助方法：重复字符串
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    // 内部类：测试用例结果
    private static class TestCaseResult {
        private String caseId;
        private int expectedCount;
        private int matchedCount;
        private List<String> unmatchedEntities;
        private double recall;
        private boolean success;

        // Getters and Setters
        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }

        public int getExpectedCount() { return expectedCount; }
        public void setExpectedCount(int expectedCount) { this.expectedCount = expectedCount; }

        public int getMatchedCount() { return matchedCount; }
        public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }

        public List<String> getUnmatchedEntities() { return unmatchedEntities; }
        public void setUnmatchedEntities(List<String> unmatchedEntities) { this.unmatchedEntities = unmatchedEntities; }

        public double getRecall() { return recall; }
        public void setRecall(double recall) { this.recall = recall; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}