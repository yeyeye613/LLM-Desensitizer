package com.hdu.apisensitivities.service.DataParser;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 数据解析服务接口
 * 负责解析各种类型的数据文件，包括PDF、Word、Excel、JSON、图像等
 */
public interface DataParserService {
    
    /**
     * 解析二进制数据
     * @param binaryData 二进制数据
     * @param dataType 数据类型（PDF、IMAGE、AUDIO、DOC、DOCX、EXCEL等）
     * @return 解析后的文本内容
     * @throws IOException 当解析过程中发生IO异常时抛出
     */
    String parseBinaryData(byte[] binaryData, String dataType) throws IOException;
    
    /**
     * 解析MultipartFile
     * @param file MultipartFile对象
     * @param dataType 数据类型
     * @return 解析后的文本内容
     * @throws IOException 当解析过程中发生IO异常时抛出
     */
    String parseMultipartFile(MultipartFile file, String dataType) throws IOException;
    
    /**
     * 解析结构化数据
     * @param structuredData 结构化数据（如JSON、XML等）
     * @param dataType 数据类型（JSON、XML等）
     * @return 解析后的字符串表示
     */
    String parseStructuredData(Map<String, Object> structuredData, String dataType);
    
    /**
     * 推断文件类型
     * @param filename 文件名
     * @return 数据类型
     */
    String inferDataType(String filename);
}