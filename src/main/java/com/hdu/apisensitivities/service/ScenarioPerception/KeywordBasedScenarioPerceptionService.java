package com.hdu.apisensitivities.service.ScenarioPerception;

import com.hdu.apisensitivities.entity.DesensitizationRequest;
import com.hdu.apisensitivities.entity.SensitiveType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeywordBasedScenarioPerceptionService implements ScenarioPerceptionService {

    // 定义不同情景的关键词及其权重
    private final Map<ScenarioAnalysisResult.ScenarioType, Map<String, Integer>> scenarioKeywords;

    // 定义不同情景下需要特别处理的敏感信息类型
    private final Map<ScenarioAnalysisResult.ScenarioType, Set<SensitiveType>> scenarioSensitiveTypeMap;

    public KeywordBasedScenarioPerceptionService() {
        // 初始化情景关键词
        scenarioKeywords = new EnumMap<>(ScenarioAnalysisResult.ScenarioType.class);
        initScenarioKeywords();

        // 初始化情景与敏感信息类型的映射
        scenarioSensitiveTypeMap = new EnumMap<>(ScenarioAnalysisResult.ScenarioType.class);
        initScenarioSensitiveTypes();
    }

    private void initScenarioKeywords() {
        // 普通聊天情景
        Map<String, Integer> generalChat = new HashMap<>();
        generalChat.put("你好", 1); generalChat.put("谢谢", 1); generalChat.put("再见", 1);
        generalChat.put("天气", 1); generalChat.put("新闻", 1); generalChat.put("电影", 1);
        generalChat.put("音乐", 1); generalChat.put("旅游", 1); generalChat.put("美食", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.GENERAL_CHAT, generalChat);

        // 客户服务情景
        Map<String, Integer> customerService = new HashMap<>();
        customerService.put("订单", 2); customerService.put("退货", 2); customerService.put("退款", 2);
        customerService.put("物流", 1); customerService.put("客服", 2); customerService.put("投诉", 2);
        customerService.put("售后", 2); customerService.put("商品", 1); customerService.put("购买", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.CUSTOMER_SERVICE, customerService);

        // 医疗咨询情景
        Map<String, Integer> medical = new HashMap<>();
        medical.put("医生", 2); medical.put("医院", 2); medical.put("病情", 2);
        medical.put("症状", 2); medical.put("药品", 2); medical.put("治疗", 2);
        medical.put("检查", 1); medical.put("病历", 2); medical.put("健康", 1); medical.put("疾病", 2);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.MEDICAL_CONSULTATION, medical);

        // 财务建议情景
        Map<String, Integer> financial = new HashMap<>();
        financial.put("股票", 2); financial.put("基金", 2); financial.put("理财", 2);
        financial.put("投资", 2); financial.put("贷款", 2); financial.put("保险", 2);
        financial.put("银行", 1); financial.put("账户", 1); financial.put("余额", 1); financial.put("交易", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.FINANCIAL_ADVICE, financial);

        // 法律咨询情景
        Map<String, Integer> legal = new HashMap<>();
        legal.put("律师", 2); legal.put("法律", 2); legal.put("合同", 2);
        legal.put("诉讼", 2); legal.put("权益", 1); legal.put("纠纷", 2);
        legal.put("法规", 1); legal.put("条款", 1); legal.put("证据", 2);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.LEGAL_ADVICE, legal);

        // 招聘情景
        Map<String, Integer> hr = new HashMap<>();
        hr.put("简历", 2); hr.put("面试", 2); hr.put("招聘", 2);
        hr.put("职位", 1); hr.put("薪资", 2); hr.put("工作", 1);
        hr.put("经验", 1); hr.put("学历", 1); hr.put("背景", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.HR_RECRUITMENT, hr);

        // 教育情景
        Map<String, Integer> education = new HashMap<>();
        education.put("学校", 2); education.put("老师", 2); education.put("学生", 2);
        education.put("课程", 1); education.put("学习", 1); education.put("考试", 2);
        education.put("成绩", 2); education.put("作业", 1); education.put("教育", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.EDUCATION, education);

        // 技术支持情景
        Map<String, Integer> tech = new HashMap<>();
        tech.put("问题", 1); tech.put("bug", 2); tech.put("系统", 1);
        tech.put("程序", 1); tech.put("测试", 1); tech.put("部署", 2);
        tech.put("配置", 1); tech.put("错误", 1); tech.put("故障", 2);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.TECHNICAL_SUPPORT, tech);

        // 代码开发情景 (新增)
        Map<String, Integer> code = new HashMap<>();
        code.put("public", 2); code.put("class", 2); code.put("void", 2);
        code.put("import", 2); code.put("return", 1); code.put("if", 1);
        code.put("else", 1); code.put("for", 1); code.put("while", 1);
        code.put("function", 2); code.put("var", 1); code.put("const", 1);
        code.put("let", 1); code.put("api_key", 3); code.put("token", 2);
        code.put("password", 2); code.put("secret", 2);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.CODE_DEVELOPMENT, code);

        // 政务服务情景
        Map<String, Integer> gov = new HashMap<>();
        gov.put("政府", 2); gov.put("政策", 1); gov.put("证件", 2);
        gov.put("手续", 1); gov.put("证明", 2); gov.put("申请", 1);
        gov.put("审批", 2); gov.put("公章", 2); gov.put("机构", 1);
        scenarioKeywords.put(ScenarioAnalysisResult.ScenarioType.GOVERNMENT_SERVICE, gov);
    }

    private void initScenarioSensitiveTypes() {
        // 医疗咨询情景
        scenarioSensitiveTypeMap.put(ScenarioAnalysisResult.ScenarioType.MEDICAL_CONSULTATION,
                Set.of(SensitiveType.ID_CARD, SensitiveType.PHONE_NUMBER, SensitiveType.ADDRESS, SensitiveType.NAME));

        // 财务建议情景
        scenarioSensitiveTypeMap.put(ScenarioAnalysisResult.ScenarioType.FINANCIAL_ADVICE,
                Set.of(SensitiveType.BANK_CARD, SensitiveType.CREDIT_CARD, SensitiveType.ID_CARD, SensitiveType.PHONE_NUMBER));

        // 招聘情景
        scenarioSensitiveTypeMap.put(ScenarioAnalysisResult.ScenarioType.HR_RECRUITMENT,
                Set.of(SensitiveType.ID_CARD, SensitiveType.PHONE_NUMBER, SensitiveType.EMAIL, SensitiveType.ADDRESS, SensitiveType.NAME));

        // 政务服务情景
        scenarioSensitiveTypeMap.put(ScenarioAnalysisResult.ScenarioType.GOVERNMENT_SERVICE,
                Set.of(SensitiveType.ID_CARD, SensitiveType.PHONE_NUMBER, SensitiveType.ADDRESS, SensitiveType.PASSPORT, SensitiveType.SOCIAL_SECURITY));
        
        // 代码开发情景
        scenarioSensitiveTypeMap.put(ScenarioAnalysisResult.ScenarioType.CODE_DEVELOPMENT,
                Set.of(SensitiveType.API_KEY, SensitiveType.IP_ADDRESS, SensitiveType.EMAIL, SensitiveType.PHONE_NUMBER));
    }

    @Override
    public ScenarioAnalysisResult analyzeScenario(DesensitizationRequest request) {
        if (request == null) {
            return getDefaultScenario();
        }

        String content = request.getMainContent();
        if (content == null || content.trim().isEmpty()) {
            return getDefaultScenario();
        }

        // 统计各个情景的加权得分
        Map<ScenarioAnalysisResult.ScenarioType, Integer> scores = new EnumMap<>(ScenarioAnalysisResult.ScenarioType.class);

        for (Map.Entry<ScenarioAnalysisResult.ScenarioType, Map<String, Integer>> entry : scenarioKeywords.entrySet()) {
            int score = 0;
            for (Map.Entry<String, Integer> keywordEntry : entry.getValue().entrySet()) {
                if (content.contains(keywordEntry.getKey())) {
                    score += keywordEntry.getValue();
                }
            }
            scores.put(entry.getKey(), score);
        }

        // 找出得分最高的情景
        Optional<Map.Entry<ScenarioAnalysisResult.ScenarioType, Integer>> bestMatch =
                scores.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .max(Map.Entry.comparingByValue());

        if (bestMatch.isPresent()) {
            ScenarioAnalysisResult.ScenarioType scenarioType = bestMatch.get().getKey();
            int score = bestMatch.get().getValue();

            // 计算置信度 (基于得分，稍微平滑一下)
            // 假设得分10分以上就是非常确信
            double confidence = Math.min(1.0, score / 10.0);

            // 构建情景分析结果
            ScenarioAnalysisResult result = ScenarioAnalysisResult.builder()
                    .scenarioType(scenarioType)
                    .confidence(confidence)
                    .build();

            // 设置敏感类型
            if (scenarioSensitiveTypeMap.containsKey(scenarioType)) {
                result.setSensitiveTypesToInclude(
                        scenarioSensitiveTypeMap.get(scenarioType).stream()
                                .map(Enum::name)
                                .collect(Collectors.toSet())
                );
            }
            
            // 设置动态阈值和严格模式
            configureDynamicRules(result);

            log.info("识别到情景: {}, 得分: {}, 置信度: {:.2f}", scenarioType, score, confidence);
            return result;
        }

        // 如果没有匹配到任何情景，返回默认情景
        return getDefaultScenario();
    }
    
    // 配置动态规则（阈值和严格模式）
    private void configureDynamicRules(ScenarioAnalysisResult result) {
        Map<SensitiveType, Double> thresholds = new HashMap<>();
        Map<SensitiveType, Boolean> strictModes = new HashMap<>();
        
        switch (result.getScenarioType()) {
            case FINANCIAL_ADVICE:
                // 财务场景，对银行卡等极其敏感，降低阈值以提高召回率
                thresholds.put(SensitiveType.BANK_CARD, 0.6);
                thresholds.put(SensitiveType.CREDIT_CARD, 0.6);
                strictModes.put(SensitiveType.BANK_CARD, true); // 开启严格模式进行校验
                break;
                
            case MEDICAL_CONSULTATION:
                // 医疗场景，关注身份证和姓名
                thresholds.put(SensitiveType.ID_CARD, 0.7);
                thresholds.put(SensitiveType.NAME, 0.6);
                break;
                
            case CODE_DEVELOPMENT:
                // 代码场景，关注API Key和IP
                thresholds.put(SensitiveType.API_KEY, 0.5); // API Key模式比较固定，可以放宽
                thresholds.put(SensitiveType.IP_ADDRESS, 0.8);
                break;
                
            case GENERAL_CHAT:
                // 普通聊天，提高阈值，减少误报
                thresholds.put(SensitiveType.BANK_CARD, 0.85);
                thresholds.put(SensitiveType.ID_CARD, 0.65);
                break;
                
            default:
                break;
        }
        
        result.setTypeConfidenceThresholds(thresholds);
        result.setTypeStrictModes(strictModes);
    }

    @Override
    public void adjustDetectionScope(DesensitizationRequest request, ScenarioAnalysisResult analysisResult) {
        if (request == null || analysisResult == null) {
            return;
        }

        ScenarioAnalysisResult.ScenarioType scenarioType = analysisResult.getScenarioType();

        // 兼容旧逻辑：设置全局严格模式
        switch (scenarioType) {
            case MEDICAL_CONSULTATION:
            case FINANCIAL_ADVICE:
            case LEGAL_ADVICE:
            case CODE_DEVELOPMENT: // 新增
                request.setStrictMode(true);
                break;
            case GENERAL_CHAT:
                request.setStrictMode(false);
                break;
            default:
                break;
        }

        // 兼容旧逻辑：设置全局置信度阈值
        if (scenarioType == ScenarioAnalysisResult.ScenarioType.MEDICAL_CONSULTATION ||
            scenarioType == ScenarioAnalysisResult.ScenarioType.FINANCIAL_ADVICE) {
            request.setConfidenceThreshold(0.8);
        }
        
        // 设置需要检测的敏感类型集合
        request.setIncludeTypes(analysisResult.getSensitiveTypesToInclude());

        // 将情景分析结果存储到请求的元数据中，以便后续处理
        if (request.getMetadata() == null) {
            request.setMetadata(new HashMap<>());
        }
        request.getMetadata().put("scenarioAnalysisResult", analysisResult);
    }

    @Override
    public ScenarioAnalysisResult getDefaultScenario() {
        ScenarioAnalysisResult result = ScenarioAnalysisResult.builder()
                .scenarioType(ScenarioAnalysisResult.ScenarioType.GENERAL_CHAT)
                .confidence(0.5)
                .build();
        configureDynamicRules(result);
        return result;
    }
}
