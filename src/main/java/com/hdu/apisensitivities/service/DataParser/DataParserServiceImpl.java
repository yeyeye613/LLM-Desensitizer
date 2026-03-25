package com.hdu.apisensitivities.service.DataParser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 数据解析服务实现
 * 使用Apache PDFBox、Apache POI等库解析各种格式的数据
 */
@Slf4j
@Service
public class DataParserServiceImpl implements DataParserService {

    @Override
    public String parseBinaryData(byte[] binaryData, String dataType) throws IOException {
        try {
            // 根据不同的数据类型采用不同的提取策略
            switch (dataType.toUpperCase()) {
                case "PDF":
                    return extractTextFromPDF(binaryData);
                case "IMAGE":
                    return extractTextFromImage(binaryData);
                case "AUDIO":
                    return extractTextFromAudio(binaryData);
                case "DOC":
                    return extractTextFromDOC(binaryData);
                case "DOCX":
                    return extractTextFromDOCX(binaryData);
                case "EXCEL":
                    return extractTextFromExcel(binaryData);
                case "JSON":
                    return extractTextFromJson(binaryData);
                case "XML":
                    return extractTextFromXml(binaryData);
                default:
                    // 对于其他类型，尝试将二进制数据转换为UTF-8字符串
                    return new String(binaryData, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("二进制数据文本提取失败: {}", e.getMessage(), e);
            // 出错时尝试基本的字符串转换作为后备方案
            try {
                return new String(binaryData, StandardCharsets.UTF_8);
            } catch (Exception fallbackException) {
                log.error("二进制数据文本提取后备方案也失败了: {}", fallbackException.getMessage(), fallbackException);
                throw new IOException("数据解析失败", e);
            }
        }
    }

    @Override
    public String parseMultipartFile(MultipartFile file, String dataType) throws IOException {
        return parseBinaryData(file.getBytes(), dataType);
    }

    @Override
    public String parseStructuredData(Map<String, Object> structuredData, String dataType) {
        // 结构化数据可以直接转换为字符串表示
        if (structuredData == null) {
            return "";
        }
        return structuredData.toString();
    }

    @Override
    public String inferDataType(String filename) {
        if (filename == null) return "BINARY";

        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) return "BINARY";

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();

        switch (extension) {
            case "pdf":
                return "PDF";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
                return "IMAGE";
            case "mp3":
            case "wav":
            case "flac":
                return "AUDIO";
            case "doc":
                return "DOC";
            case "docx":
                return "DOCX";
            case "xls":
            case "xlsx":
                return "EXCEL";
            case "json":
                return "JSON";
            case "xml":
                return "XML";
            default:
                return "BINARY";
        }
    }

    // 从PDF中提取文本
    private String extractTextFromPDF(byte[] pdfData) throws IOException {
        try (PDDocument document = PDDocument.load(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("成功从PDF中提取了 {} 个字符", text.length());
            return text;
        } catch (Exception e) {
            log.error("PDF文本提取失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 从图像中提取文本（简化版，实际应用中应该使用OCR技术）
    private String extractTextFromImage(byte[] imageData) {
        // 在实际应用中这里应该使用OCR库，比如Tesseract
        log.info("图像文本提取需要OCR库支持，当前仅返回基础字符串转换结果");
        return new String(imageData, StandardCharsets.UTF_8);
    }

    // 从音频中提取文本（简化版，实际应用中应该使用语音识别技术）
    private String extractTextFromAudio(byte[] audioData) {
        // 音频数据文本提取需要专门的语音识别库
        log.info("音频文本提取需要语音识别库支持");
        return "[音频数据]";
    }

    // 从DOC中提取文本
    private String extractTextFromDOC(byte[] docData) throws IOException {
        // 注意：对于旧版本的DOC文件，需要使用HWPF库
        // 这里简单处理，实际应该使用正确的API
        log.warn("DOC文件处理需要Apache POI HWPF支持");
        return "[DOC文档内容]";
    }

    // 从DOCX中提取文本
    private String extractTextFromDOCX(byte[] docxData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(docxData);
             XWPFDocument document = new XWPFDocument(bis)) {
            
            StringBuilder textBuilder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                textBuilder.append(paragraph.getText()).append("\n");
            }
            
            String text = textBuilder.toString();
            log.info("成功从DOCX中提取了 {} 个字符", text.length());
            return text;
        } catch (Exception e) {
            log.error("DOCX文本提取失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 从Excel中提取文本
    private String extractTextFromExcel(byte[] excelData) throws IOException {
        StringBuilder textBuilder = new StringBuilder();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData))) {
            int numberOfSheets = workbook.getNumberOfSheets();
            log.info("Excel文档包含 {} 个工作表", numberOfSheets);

            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                textBuilder.append("工作表: ").append(sheetName).append("\n");

                // 遍历行
                for (Row row : sheet) {
                    // 遍历单元格
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            textBuilder.append(cellValue).append("\t");
                        }
                    }
                    textBuilder.append("\n");
                }
                textBuilder.append("\n");
            }

            String text = textBuilder.toString();
            log.info("成功从Excel中提取了 {} 个字符", text.length());
            return text;
        } catch (Exception e) {
            log.error("Excel文本提取失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 从JSON中提取文本
    private String extractTextFromJson(byte[] jsonData) {
        return new String(jsonData, StandardCharsets.UTF_8);
    }

    // 从XML中提取文本
    private String extractTextFromXml(byte[] xmlData) {
        return new String(xmlData, StandardCharsets.UTF_8);
    }

    // 获取单元格的值作为字符串
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}