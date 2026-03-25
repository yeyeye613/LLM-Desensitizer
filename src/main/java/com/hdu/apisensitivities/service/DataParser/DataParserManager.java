package com.hdu.apisensitivities.service.DataParser;

import com.hdu.apisensitivities.entity.DesensitizationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 数据解析管理器
 * 负责协调不同类型数据的解析工作，并将解析后的数据封装成适合后续处理的格式
 */
@Slf4j
@Component
public class DataParserManager {

    @Autowired
    private DataParserServiceImpl dataParserService;

    /**
     * 解析数据请求
     * @param request 原始脱敏请求
     * @return 解析后的文本内容
     * @throws IOException 当解析过程中发生IO异常时抛出
     */
    public String parseData(DesensitizationRequest request) throws IOException {
        String content = null;
        
        if (request.getContent() != null) {
            // 已经是文本内容
            content = request.getContent();
        } else if (request.getStructuredData() != null && !request.getStructuredData().isEmpty()) {
            // 结构化数据
            content = dataParserService.parseStructuredData(request.getStructuredData(), request.getDataType());
        } else if (request.getBinaryData() != null && request.getBinaryData().length > 0) {
            // 二进制数据
            content = dataParserService.parseBinaryData(request.getBinaryData(), request.getDataType());
        }
        
        return content;
    }

    /**
     * 解析MultipartFile
     * @param file MultipartFile对象
     * @param dataType 数据类型
     * @return 解析后的文本内容
     * @throws IOException 当解析过程中发生IO异常时抛出
     */
    public String parseMultipartFile(MultipartFile file, String dataType) throws IOException {
        return dataParserService.parseMultipartFile(file, dataType);
    }

    /**
     * 根据文件名推断数据类型
     * @param filename 文件名
     * @return 数据类型
     */
    public String inferDataType(String filename) {
        return dataParserService.inferDataType(filename);
    }
}