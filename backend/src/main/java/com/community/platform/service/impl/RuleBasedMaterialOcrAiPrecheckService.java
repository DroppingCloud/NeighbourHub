package com.community.platform.service.impl;

import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.service.MaterialOcrAiPrecheckService;
import com.community.platform.vo.application.MaterialAiPrecheckResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class RuleBasedMaterialOcrAiPrecheckService implements MaterialOcrAiPrecheckService {

    private static final long MIN_USEFUL_FILE_SIZE = 1024;
    private static final long MIN_DOCUMENT_FILE_SIZE = 512;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MIN_GENERAL_IMAGE_WIDTH = 800;
    private static final int MIN_GENERAL_IMAGE_HEIGHT = 600;
    private static final int MIN_FATAL_IMAGE_WIDTH = 260;
    private static final int MIN_FATAL_IMAGE_HEIGHT = 260;
    private static final int MIN_PHOTO_WIDTH = 295;
    private static final int MIN_PHOTO_HEIGHT = 413;
    private static final int OCR_TARGET_LONG_SIDE = 1600;
    private static final int MIN_OCR_TEXT_LENGTH = 10;
    private static final int MIN_DOCUMENT_TEXT_LENGTH = 8;
    private static final int MAX_PDF_OCR_PAGES = 3;
    private static final double MIN_LUMINANCE_VARIANCE = 80.0;
    private static final double MIN_EDGE_DELTA = 4.0;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${ai.dashscope.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String dashscopeUrl;

    @Value("${ai.dashscope.ocr-model:qwen-vl-ocr-latest}")
    private String dashscopeOcrModel;

    @Value("${ai.dashscope.enabled:true}")
    private Boolean dashscopeEnabled;

    @Override
    public MaterialAiPrecheckResultVO precheck(ApplicationMaterial material, Path filePath) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> ocrLines = new ArrayList<>();

        ocrLines.add("OCR/AI预审模式：本地强规则 + 阿里百炼 DashScope");
        ocrLines.add("材料名称：" + safe(material.getMaterialName()));
        ocrLines.add("文件名称：" + safe(material.getFileName()));
        ocrLines.add("文件类型：" + safe(material.getFileType()));

        if (filePath == null || !Files.exists(filePath)) {
            issues.add("材料文件不存在，无法进行OCR/AI预审");
            return buildResult(issues, suggestions, ocrLines);
        }

        String extension = normalizeExtension(material.getFileType(), material.getFileName());
        try {
            long fileSize = Files.size(filePath);
            ocrLines.add("文件大小：" + fileSize + " bytes");
            long minUsefulFileSize = getMinUsefulFileSize(extension);
            if (fileSize <= 0) {
                issues.add("材料文件为空");
            } else if (fileSize < minUsefulFileSize) {
                issues.add(getSmallFileIssue(extension));
            }
            if (fileSize > MAX_FILE_SIZE) {
                issues.add("材料文件超过20MB限制");
            }

            validateFileSignature(filePath, extension, issues, ocrLines);
            inspectContentByType(material, filePath, extension, issues, suggestions, ocrLines);
            runMaterialSemanticRules(material, extension, issues, suggestions, ocrLines);
            callDashScopeOcrIfPossible(material, filePath, extension, issues, suggestions, ocrLines);
        } catch (IOException e) {
            issues.add("材料文件读取失败，可能已损坏或无访问权限");
        }

        return buildResult(issues, suggestions, ocrLines);
    }

    private void callDashScopeOcrIfPossible(ApplicationMaterial material,
                                            Path filePath,
                                            String extension,
                                            List<String> issues,
                                            List<String> suggestions,
                                            List<String> ocrLines) {
        if (!Boolean.TRUE.equals(dashscopeEnabled)) {
            ocrLines.add("阿里百炼OCR：未启用，已使用本地规则预审。");
            return;
        }
        if (!StringUtils.hasText(dashscopeApiKey)) {
            ocrLines.add("阿里百炼OCR：未配置DASHSCOPE_API_KEY，已使用本地规则预审。");
            return;
        }
        if (!isImage(extension)) {
            ocrLines.add("阿里百炼OCR：当前仅对JPG/JPEG/PNG图片材料调用；PDF/DOC/DOCX使用本地规则预审。");
            return;
        }

        try {
            String dataUrl = buildImageDataUrl(filePath, extension, ocrLines);
            String prompt = buildDashScopePrompt(material);
            String content = callDashScope(prompt, dataUrl);
            if (!StringUtils.hasText(content)) {
                issues.add("阿里百炼OCR未返回有效识别文本，请重新上传清晰材料");
                ocrLines.add("阿里百炼OCR返回为空。");
                return;
            }
            String trimmed = content.trim();
            ocrLines.add("阿里百炼OCR识别结果：");
            ocrLines.add(trimmed);
            analyzeDashScopeContent(material, trimmed, issues, suggestions);
        } catch (Exception e) {
            log.warn("DashScope OCR failed, fallback to local precheck: {}", e.getMessage());
            issues.add("阿里百炼OCR调用失败，无法完成自动预审，请稍后重试或重新上传材料");
            ocrLines.add("阿里百炼OCR调用失败：" + e.getMessage());
        }
    }

    private String callDashScope(String prompt, String dataUrl) {
        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", dataUrl);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", imageUrl);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textContent, imageContent));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashscopeOcrModel);
        requestBody.put("messages", List.of(message));
        requestBody.put("temperature", 0);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(dashscopeApiKey);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    dashscopeUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    new ParameterizedTypeReference<>() {}
            );
            return parseDashScopeContent(response.getBody());
        } catch (RestClientException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private String parseDashScopeContent(Map<String, Object> body) {
        if (body == null || !(body.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return "";
        }
        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return "";
        }
        Object contentObj = message.get("content");
        return contentObj == null ? "" : String.valueOf(contentObj);
    }

    private String buildDashScopePrompt(ApplicationMaterial material) {
        if (isPhotoMaterial(material)) {
            return """
                    请对这张社区政务申请证件照/本人相片进行AI预审。
                    材料名称：%s

                    请严格输出：
                    1. 是否为单人正面人像照片；
                    2. 人脸是否清晰、无遮挡、无明显模糊；
                    3. 背景是否简洁，是否疑似证件照；
                    4. 是否与材料名称匹配；
                    5. 如存在问题，请明确写出“不是照片”“不是证件照”“无人像”“多人”“不清晰”“模糊”“遮挡”之一。
                    注意：本人相片不要求识别文字，不要因为没有文字而判定无法识别。
                    """.formatted(safe(material.getMaterialName()));
        }
        return """
                请对这份社区政务申请材料进行OCR识别和预审。
                材料名称：%s

                请严格输出：
                1. 识别到的主要文字；
                2. 图片是否清晰完整；
                3. 是否疑似与材料名称匹配；如果不匹配，请写明具体原因；
                4. 是否存在缺页、遮挡、模糊、非证件/非材料截图、关键字段缺失；
                5. 需要人工重点核验的字段。

                说明：户口簿、户籍信息登记表、家庭成员登记表均可视为户籍/户口类材料；不要仅因标题不是“户口簿”就判定不匹配。
                如果看不清、无法识别、材料类型不匹配或关键字段缺失，请明确写出“不清晰”“无法识别”“不匹配”或“缺失”。
                """.formatted(safe(material.getMaterialName()));
    }

    private void analyzeDashScopeContent(ApplicationMaterial material, String content, List<String> issues, List<String> suggestions) {
        String normalized = content.replaceAll("\\s+", "");
        if (isPhotoMaterial(material)) {
            analyzePhotoDashScopeContent(normalized, issues, suggestions);
            return;
        }
        List<String> severeFailSignals = List.of(
                "无法识别",
                "不能识别",
                "看不清",
                "严重遮挡"
        );
        List<String> completenessFailSignals = List.of(
                "关键字段缺失",
                "字段缺失",
                "缺少关键",
                "材料不完整"
        );
        List<String> mismatchFailSignals = List.of(
                "不匹配",
                "不是该材料",
                "并非该材料"
        );
        List<String> qualityFailSignals = List.of(
                "不清晰",
                "模糊",
                "无法判断"
        );
        boolean hasCompatibleContent = isOcrContentCompatibleWithMaterial(material, normalized);
        for (String signal : severeFailSignals) {
            if (containsFailSignal(normalized, signal)) {
                issues.add("阿里百炼OCR提示材料存在问题：" + signal);
                return;
            }
        }
        for (String signal : completenessFailSignals) {
            if (containsFailSignal(normalized, signal)) {
                if (hasCompatibleContent) {
                    suggestions.add("阿里百炼OCR提示可能存在字段缺失，但已识别到当前材料核心字段，建议工作人员人工复核");
                    continue;
                }
                issues.add("阿里百炼OCR提示材料存在问题：" + signal);
                return;
            }
        }
        for (String signal : mismatchFailSignals) {
            if (containsFailSignal(normalized, signal)) {
                if (hasCompatibleContent) {
                    suggestions.add("阿里百炼OCR返回了材料不匹配提示，但识别内容包含当前材料核心字段，已转为人工复核建议");
                    continue;
                }
                issues.add("阿里百炼OCR提示材料存在问题：" + signal);
                return;
            }
        }
        boolean hasPositiveQualityConclusion = hasPositiveQualityConclusion(normalized);
        boolean hasPositiveMatchConclusion = hasPositiveMatchConclusion(normalized);
        for (String signal : qualityFailSignals) {
            if (containsFailSignal(normalized, signal)) {
                if (hasCompatibleContent) {
                    String reason = hasPositiveQualityConclusion && hasPositiveMatchConclusion
                            ? "阿里百炼OCR返回了互相矛盾的清晰度提示"
                            : "阿里百炼OCR提示材料可能不够清晰";
                    suggestions.add(reason + "，但已识别到当前材料核心字段，建议工作人员人工复核");
                    continue;
                }
                issues.add("阿里百炼OCR提示材料存在问题：" + signal);
                return;
            }
        }
        if (normalized.length() < MIN_OCR_TEXT_LENGTH) {
            issues.add("OCR识别文本过少，材料可能不清晰或内容不完整");
            return;
        }
        if (requiresOcrSemanticMatch(material) && !hasCompatibleContent) {
            issues.add("OCR未识别到与“" + safe(material.getMaterialName()) + "”匹配的核心字段，请上传正确材料");
            return;
        }
        if (normalized.contains("建议人工复核") || normalized.contains("需人工核验")) {
            suggestions.add("阿里百炼OCR建议人工复核关键字段");
        }
    }

    private void analyzePhotoDashScopeContent(String normalized, List<String> issues, List<String> suggestions) {
        List<String> photoFailSignals = List.of(
                "无人像",
                "无人脸",
                "没有人像",
                "没有人脸",
                "多人",
                "多个人",
                "严重遮挡"
        );
        List<String> photoQualitySignals = List.of(
                "不是照片",
                "非照片",
                "不是证件照",
                "非证件照",
                "遮挡",
                "不清晰",
                "模糊",
                "看不清"
        );
        boolean positivePhoto = normalized.contains("单人")
                || normalized.contains("正面")
                || normalized.contains("证件照")
                || normalized.contains("本人相片")
                || normalized.contains("免冠")
                || normalized.contains("照片")
                || normalized.contains("人像")
                || normalized.contains("头像")
                || normalized.contains("人像照片")
                || normalized.contains("正面人像")
                || normalized.contains("背景简洁")
                || normalized.contains("白色背景")
                || normalized.contains("白底")
                || normalized.contains("蓝底")
                || normalized.contains("符合要求")
                || normalized.contains("合格")
                || normalized.contains("人脸清晰")
                || normalized.contains("人像清晰")
                || normalized.contains("照片清晰")
                || normalized.contains("清晰完整");
        for (String signal : photoFailSignals) {
            if (containsFailSignal(normalized, signal)) {
                if (("不清晰".equals(signal) || "模糊".equals(signal)) && positivePhoto && hasPositiveQualityConclusion(normalized)) {
                    suggestions.add("阿里百炼对照片清晰度返回了矛盾提示，但已判断为单人证件照，建议工作人员人工复核");
                    continue;
                }
                issues.add("阿里百炼AI提示本人相片存在问题：" + signal);
                return;
            }
        }
        for (String signal : photoQualitySignals) {
            if (containsFailSignal(normalized, signal)) {
                if (positivePhoto) {
                    suggestions.add("阿里百炼对本人相片返回了需复核提示，但未发现多人、无人像或非照片等硬性问题，建议工作人员人工复核");
                    continue;
                }
                if ("看不清".equals(signal)) {
                    issues.add("阿里百炼AI提示本人相片存在问题：" + signal);
                    return;
                }
                suggestions.add("阿里百炼AI提示本人相片可能存在问题：" + signal + "，建议工作人员人工复核");
            }
        }
        if (!positivePhoto && normalized.length() >= MIN_OCR_TEXT_LENGTH) {
            suggestions.add("阿里百炼AI未明确确认该材料为合格本人相片，建议工作人员人工复核");
        }
        if (normalized.contains("建议人工复核") || normalized.contains("需人工核验")) {
            suggestions.add("阿里百炼AI建议人工复核本人相片");
        }
    }

    private boolean hasPositiveQualityConclusion(String normalizedText) {
        return normalizedText.contains("图片清晰完整")
                || normalizedText.contains("清晰完整")
                || normalizedText.contains("文字清晰")
                || normalizedText.contains("图像清晰")
                || normalizedText.contains("照片清晰")
                || normalizedText.contains("清楚完整");
    }

    private boolean hasPositiveMatchConclusion(String normalizedText) {
        return normalizedText.contains("匹配")
                && !containsFailSignal(normalizedText, "不匹配")
                && !containsFailSignal(normalizedText, "不是该材料")
                && !containsFailSignal(normalizedText, "并非该材料");
    }

    private boolean containsFailSignal(String normalizedText, String signal) {
        int index = normalizedText.indexOf(signal);
        while (index >= 0) {
            String prefix = normalizedText.substring(Math.max(0, index - 8), index);
            if (!isNegatedSignalPrefix(prefix)) {
                return true;
            }
            index = normalizedText.indexOf(signal, index + signal.length());
        }
        return false;
    }

    private boolean isNegatedSignalPrefix(String prefix) {
        return prefix.endsWith("无")
                || prefix.endsWith("未")
                || prefix.endsWith("没有")
                || prefix.endsWith("无明显")
                || prefix.endsWith("未发现")
                || prefix.endsWith("未见")
                || prefix.endsWith("不存在")
                || prefix.endsWith("没有发现")
                || prefix.endsWith("未检测到")
                || prefix.endsWith("没有检测到")
                || prefix.endsWith("未识别到")
                || prefix.endsWith("不含")
                || prefix.endsWith("不涉及");
    }

    private boolean requiresOcrSemanticMatch(ApplicationMaterial material) {
        String materialName = safe(material.getMaterialName());
        return materialName.contains("身份证")
                || materialName.contains("身份")
                || materialName.contains("户口")
                || materialName.contains("户籍")
                || materialName.contains("合同")
                || materialName.contains("产权")
                || materialName.contains("不动产")
                || materialName.contains("申请表")
                || materialName.contains("证明")
                || materialName.contains("银行卡")
                || materialName.contains("学生证")
                || materialName.contains("残疾证")
                || materialName.contains("营业执照");
    }

    private boolean isOcrContentCompatibleWithMaterial(ApplicationMaterial material, String normalizedOcrText) {
        String materialName = safe(material.getMaterialName());
        if (materialName.contains("身份证") || materialName.contains("身份")) {
            boolean hasIdNumber = normalizedOcrText.contains("公民身份号码")
                    || normalizedOcrText.matches(".*\\d{17}[0-9Xx].*");
            boolean hasFrontSideField = normalizedOcrText.contains("姓名")
                    || normalizedOcrText.contains("性别")
                    || normalizedOcrText.contains("出生")
                    || normalizedOcrText.contains("住址")
                    || normalizedOcrText.contains("居民身份证");
            return hasIdNumber && hasFrontSideField;
        }
        if (materialName.contains("户口") || materialName.contains("户籍")) {
            return normalizedOcrText.contains("户口簿")
                    || normalizedOcrText.contains("户籍信息")
                    || normalizedOcrText.contains("户籍所在地")
                    || normalizedOcrText.contains("户籍登记")
                    || normalizedOcrText.contains("家庭成员登记")
                    || normalizedOcrText.contains("与户主关系")
                    || normalizedOcrText.contains("户号")
                    || normalizedOcrText.contains("户主")
                    || normalizedOcrText.contains("常住人口");
        }
        if (materialName.contains("照片") || materialName.contains("相片") || materialName.contains("证件照")) {
            return !normalizedOcrText.contains("非照片") && !normalizedOcrText.contains("不是照片");
        }
        if (materialName.contains("租赁合同") || materialName.contains("房屋租赁合同")) {
            return normalizedOcrText.contains("租赁")
                    || normalizedOcrText.contains("出租")
                    || normalizedOcrText.contains("承租")
                    || normalizedOcrText.contains("合同");
        }
        if (materialName.contains("购房合同")) {
            return normalizedOcrText.contains("购房")
                    || normalizedOcrText.contains("买受人")
                    || normalizedOcrText.contains("出卖人")
                    || normalizedOcrText.contains("合同");
        }
        if (materialName.contains("产权") || materialName.contains("不动产")) {
            return normalizedOcrText.contains("不动产")
                    || normalizedOcrText.contains("产权")
                    || normalizedOcrText.contains("权利人");
        }
        if (materialName.contains("申请表")) {
            return normalizedOcrText.contains("申请")
                    || normalizedOcrText.contains("申请人")
                    || normalizedOcrText.contains("联系电话");
        }
        if (materialName.contains("证明")) {
            return normalizedOcrText.contains("证明")
                    || normalizedOcrText.contains("兹证明")
                    || normalizedOcrText.contains("情况属实");
        }
        if (materialName.contains("银行卡")) {
            return normalizedOcrText.contains("银行")
                    || normalizedOcrText.contains("账号")
                    || normalizedOcrText.contains("开户")
                    || normalizedOcrText.matches(".*\\d{12,19}.*");
        }
        if (materialName.contains("学生证")) {
            return normalizedOcrText.contains("学生证")
                    || normalizedOcrText.contains("学校")
                    || normalizedOcrText.contains("学号");
        }
        if (materialName.contains("残疾证")) {
            return normalizedOcrText.contains("残疾")
                    || normalizedOcrText.contains("残疾人证");
        }
        if (materialName.contains("营业执照")) {
            return normalizedOcrText.contains("营业执照")
                    || normalizedOcrText.contains("统一社会信用代码")
                    || normalizedOcrText.contains("法定代表人");
        }
        return false;
    }

    private String buildImageDataUrl(Path filePath, String extension, List<String> ocrLines) throws IOException {
        BufferedImage image = ImageIO.read(filePath.toFile());
        if (image != null) {
            BufferedImage processed = upscaleForOcrIfNeeded(image, ocrLines);
            return buildImageDataUrl(processed, ocrLines);
        }

        String mime = "png".equals(extension) ? "image/png" : "image/jpeg";
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(filePath));
        return "data:" + mime + ";base64," + base64;
    }

    private String buildImageDataUrl(BufferedImage image, List<String> ocrLines) throws IOException {
        BufferedImage processed = upscaleForOcrIfNeeded(image, ocrLines);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(processed, "png", outputStream);
        String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + base64;
    }

    private BufferedImage upscaleForOcrIfNeeded(BufferedImage source, List<String> ocrLines) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longSide = Math.max(width, height);
        if (longSide >= OCR_TARGET_LONG_SIDE) {
            ocrLines.add("OCR图片增强：原图尺寸已满足识别要求，未放大。");
            return source;
        }

        double scale = Math.min(3.0, (double) OCR_TARGET_LONG_SIDE / longSide);
        if (scale <= 1.05) {
            return source;
        }
        int targetWidth = Math.max(width, (int) Math.round(width * scale));
        int targetHeight = Math.max(height, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        ocrLines.add("OCR图片增强：已将低分辨率图片从 " + width + "x" + height + " 放大到 " + targetWidth + "x" + targetHeight + " 后送审。");
        return resized;
    }

    private void validateFileSignature(Path filePath, String extension, List<String> issues, List<String> ocrLines) throws IOException {
        byte[] header = readHeader(filePath, 8);
        String signature = toHex(header);
        ocrLines.add("文件头特征：" + signature);

        if ("pdf".equals(extension) && !startsWith(header, "%PDF".getBytes(StandardCharsets.UTF_8))) {
            issues.add("文件扩展名为PDF，但文件头不符合PDF格式");
        }
        if ("png".equals(extension) && !startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
            issues.add("文件扩展名为PNG，但文件头不符合PNG格式");
        }
        if (("jpg".equals(extension) || "jpeg".equals(extension))
                && !(header.length >= 3 && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF)) {
            issues.add("文件扩展名为JPG/JPEG，但文件头不符合图片格式");
        }
        if ("docx".equals(extension) && !startsWith(header, new byte[]{0x50, 0x4B})) {
            issues.add("文件扩展名为DOCX，但文件头不符合Office文档格式");
        }
        if ("doc".equals(extension)
                && !startsWith(header, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0})
                && !looksLikeHtmlDocument(filePath)) {
            issues.add("文件扩展名为DOC，但文件头不符合旧版Word文档格式");
        }
    }

    private long getMinUsefulFileSize(String extension) {
        if ("doc".equals(extension) || "docx".equals(extension)) {
            return MIN_DOCUMENT_FILE_SIZE;
        }
        return MIN_USEFUL_FILE_SIZE;
    }

    private String getSmallFileIssue(String extension) {
        if ("doc".equals(extension) || "docx".equals(extension)) {
            return "Word文件内容过少，可能未填写或保存异常";
        }
        if ("pdf".equals(extension)) {
            return "PDF文件过小，可能为空白文件、损坏文件或页数不完整";
        }
        if (isImage(extension)) {
            return "图片文件过小，可能为空白图片或保存异常";
        }
        return "材料文件过小，可能为空白文件或损坏文件";
    }

    private boolean looksLikeHtmlDocument(Path filePath) throws IOException {
        String text = readTextWithDetectedCharset(filePath, 512).trim().toLowerCase(Locale.ROOT);
        return text.startsWith("<!doctype html")
                || text.startsWith("<html")
                || text.contains("<html")
                || text.contains("<meta charset");
    }

    private void inspectContentByType(ApplicationMaterial material,
                                      Path filePath,
                                      String extension,
                                      List<String> issues,
                                      List<String> suggestions,
                                      List<String> ocrLines) {
        if (isImage(extension)) {
            inspectImage(filePath, material, issues, suggestions, ocrLines);
            return;
        }
        if ("pdf".equals(extension)) {
            inspectPdfDocument(material, filePath, issues, suggestions, ocrLines);
            return;
        }
        if ("doc".equals(extension) || "docx".equals(extension)) {
            inspectWordDocument(material, filePath, extension, issues, suggestions, ocrLines);
        }
    }

    private void inspectPdfDocument(ApplicationMaterial material,
                                    Path filePath,
                                    List<String> issues,
                                    List<String> suggestions,
                                    List<String> ocrLines) {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            int pages = document.getNumberOfPages();
            ocrLines.add("PDF识别摘要：页数 " + pages);
            if (pages <= 0) {
                issues.add("PDF文件没有有效页面");
                return;
            }

            String text = normalizeExtractedText(new PDFTextStripper().getText(document));
            if (StringUtils.hasText(text)) {
                ocrLines.add("PDF文本抽取结果：" + abbreviate(text, 500));
                validateExtractedDocumentText(material, "PDF文本", text, issues, suggestions);
                return;
            }

            ocrLines.add("PDF文本抽取结果为空，尝试将PDF页面渲染为图片后调用阿里百炼OCR。");
            if (!Boolean.TRUE.equals(dashscopeEnabled)) {
                issues.add("PDF无法抽取文本，且阿里百炼OCR未启用，无法完成自动预审");
                return;
            }
            if (!StringUtils.hasText(dashscopeApiKey)) {
                issues.add("PDF无法抽取文本，且未配置DASHSCOPE_API_KEY，无法完成自动预审");
                return;
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder combined = new StringBuilder();
            int limit = Math.min(pages, MAX_PDF_OCR_PAGES);
            for (int pageIndex = 0; pageIndex < limit; pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 180, ImageType.RGB);
                String dataUrl = buildImageDataUrl(pageImage, ocrLines);
                String content = callDashScope(buildDashScopePrompt(material), dataUrl);
                if (StringUtils.hasText(content)) {
                    combined.append(content).append('\n');
                }
            }
            String ocrText = normalizeExtractedText(combined.toString());
            if (!StringUtils.hasText(ocrText)) {
                issues.add("PDF页面OCR未返回有效识别文本，请上传文字清晰的PDF或图片材料");
                return;
            }
            ocrLines.add("PDF页面OCR识别结果：" + abbreviate(ocrText, 700));
            analyzeDashScopeContent(material, ocrText, issues, suggestions);
        } catch (IOException e) {
            issues.add("PDF文件解析失败，文件可能损坏或不是有效PDF");
        } catch (Exception e) {
            issues.add("PDF OCR/AI预审失败：" + e.getMessage());
        }
    }

    private void inspectWordDocument(ApplicationMaterial material,
                                     Path filePath,
                                     String extension,
                                     List<String> issues,
                                     List<String> suggestions,
                                     List<String> ocrLines) {
        try {
            String text;
            if ("docx".equals(extension)) {
                text = extractDocxText(filePath);
            } else if (looksLikeHtmlDocument(filePath)) {
                text = extractHtmlDocText(filePath);
            } else {
                text = extractDocText(filePath);
            }
            String normalizedText = normalizeExtractedText(text);
            if (!StringUtils.hasText(normalizedText) || normalizedText.length() < MIN_DOCUMENT_TEXT_LENGTH) {
                issues.add("Word文档未抽取到有效正文，可能为空白、损坏或内容过少");
                return;
            }
            ocrLines.add("Word文本抽取结果：" + abbreviate(normalizedText, 700));
            validateExtractedDocumentText(material, "Word文本", normalizedText, issues, suggestions);
        } catch (IOException e) {
            issues.add("Word文档解析失败，文件可能损坏或不是有效Word文档");
        } catch (RuntimeException e) {
            issues.add("Word文档内容读取失败：" + e.getMessage());
        }
    }

    private String extractDocxText(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocText(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractHtmlDocText(Path filePath) throws IOException {
        String html = readTextWithDetectedCharset(filePath, (int) Math.min(Files.size(filePath), 2L * 1024 * 1024));
        return html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private String readTextWithDetectedCharset(Path filePath, int maxBytes) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        int length = Math.min(bytes.length, maxBytes);
        if (length >= 2) {
            if ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)) {
                return new String(bytes, 2, length - 2, StandardCharsets.UTF_16LE);
            }
            if ((bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)) {
                return new String(bytes, 2, length - 2, StandardCharsets.UTF_16BE);
            }
        }
        if (length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, length - 3, StandardCharsets.UTF_8);
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private void validateExtractedDocumentText(ApplicationMaterial material,
                                               String sourceName,
                                               String text,
                                               List<String> issues,
                                               List<String> suggestions) {
        String normalized = text.replaceAll("\\s+", "");
        if (containsAny(normalized, List.of("样例", "示例", "模板", "填写说明", "请填写", "________", "（填写", "(填写"))) {
            issues.add(sourceName + "仍包含模板、示例或未填写占位内容，请填写完整后重新上传");
            return;
        }
        if (requiresOcrSemanticMatch(material) && !isOcrContentCompatibleWithMaterial(material, normalized)) {
            issues.add(sourceName + "未识别到与“" + safe(material.getMaterialName()) + "”匹配的核心字段，请上传正确材料");
            return;
        }
        if (normalized.length() < MIN_DOCUMENT_TEXT_LENGTH) {
            issues.add(sourceName + "内容过少，材料可能未填写完整");
        }
    }

    private void inspectImage(Path filePath,
                              ApplicationMaterial material,
                              List<String> issues,
                              List<String> suggestions,
                              List<String> ocrLines) {
        try {
            BufferedImage image = ImageIO.read(filePath.toFile());
            if (image == null) {
                issues.add("图片无法读取，文件可能损坏或并非真实图片");
                return;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            ocrLines.add("图片尺寸：" + width + "x" + height);

            if (width < MIN_FATAL_IMAGE_WIDTH || height < MIN_FATAL_IMAGE_HEIGHT) {
                issues.add("图片尺寸过小，无法进行可靠识别");
            } else if (isPhotoMaterial(material)) {
                if (width < MIN_PHOTO_WIDTH || height < MIN_PHOTO_HEIGHT) {
                    suggestions.add("证件照尺寸偏小，系统会尝试增强识别；如识别失败请重新上传更清晰照片");
                }
            } else if (width < MIN_GENERAL_IMAGE_WIDTH || height < MIN_GENERAL_IMAGE_HEIGHT) {
                suggestions.add("图片分辨率偏低，系统会尝试增强识别；如识别失败请重新上传高清扫描件或照片");
            }

            ImageQuality quality = inspectImageQuality(image);
            ocrLines.add("图片亮度方差：" + Math.round(quality.luminanceVariance()));
            ocrLines.add("图片边缘变化值：" + Math.round(quality.edgeDelta()));
            if (quality.luminanceVariance() < MIN_LUMINANCE_VARIANCE) {
                issues.add("图片内容过于单一，疑似空白页、纯色图或严重曝光异常");
            }
            if (quality.edgeDelta() < MIN_EDGE_DELTA) {
                issues.add("图片边缘细节过少，疑似模糊、失焦或不是有效材料照片");
            }

            if (width < 1200 || height < 800) {
                suggestions.add("建议上传更高清的扫描件或照片，以便工作人员核验");
            }
            ocrLines.add("图片本地识别摘要：已完成可读性、尺寸、空白/模糊和格式检测。");
        } catch (IOException e) {
            issues.add("图片读取失败，文件可能损坏");
        }
    }

    private ImageQuality inspectImageQuality(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int stepX = Math.max(1, width / 60);
        int stepY = Math.max(1, height / 60);
        long count = 0;
        double sum = 0;
        double squareSum = 0;
        double edgeSum = 0;
        long edgeCount = 0;

        for (int y = 0; y < height; y += stepY) {
            int previous = -1;
            for (int x = 0; x < width; x += stepX) {
                int luminance = luminance(image.getRGB(x, y));
                sum += luminance;
                squareSum += (double) luminance * luminance;
                count++;
                if (previous >= 0) {
                    edgeSum += Math.abs(luminance - previous);
                    edgeCount++;
                }
                previous = luminance;
            }
        }

        double mean = count == 0 ? 0 : sum / count;
        double variance = count == 0 ? 0 : squareSum / count - mean * mean;
        double edgeDelta = edgeCount == 0 ? 0 : edgeSum / edgeCount;
        return new ImageQuality(Math.max(0, variance), edgeDelta);
    }

    private int luminance(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return (int) Math.round(0.299 * red + 0.587 * green + 0.114 * blue);
    }

    private void runMaterialSemanticRules(ApplicationMaterial material,
                                          String extension,
                                          List<String> issues,
                                          List<String> suggestions,
                                          List<String> ocrLines) {
        String source = (safe(material.getMaterialName()) + " " + safe(material.getFileName())).toLowerCase(Locale.ROOT);
        if (isPhotoMaterial(material) && !isImage(extension)) {
            issues.add("照片类材料必须上传JPG、JPEG或PNG图片");
        }
        if (source.contains("身份证") || source.contains("身份")) {
            suggestions.add("AI建议：请确认身份证姓名、证件号码和有效期区域清晰可见");
            ocrLines.add("AI字段提示：应核验姓名、身份证号、有效期。");
        }
        if (source.contains("户口") || source.contains("关系")) {
            suggestions.add("AI建议：请确认首页、本人页或关系页完整上传");
            ocrLines.add("AI字段提示：应核验户籍地址、成员关系和本人页。");
        }
        if (source.contains("合同") || source.contains("证明") || source.contains("申请表") || source.contains("声明书")) {
            suggestions.add("AI建议：请确认材料中签字、日期、盖章等关键项完整");
            ocrLines.add("AI字段提示：应核验签字、日期、盖章和申请人信息。");
        }
    }

    private MaterialAiPrecheckResultVO buildResult(List<String> issues, List<String> suggestions, List<String> ocrLines) {
        MaterialAiPrecheckResultVO result = new MaterialAiPrecheckResultVO();
        boolean passed = issues.isEmpty();
        result.setPrecheckStatus(passed ? "passed" : "failed");
        if (passed) {
            String extra = suggestions.isEmpty() ? "" : "；" + String.join("；", suggestions);
            result.setPrecheckRemark("OCR/AI预审通过：格式、文件质量和材料类型规则检查通过" + extra);
        } else {
            result.setPrecheckRemark("OCR/AI预审未通过：" + String.join("；", issues));
        }
        result.setOcrText(String.join("\n", ocrLines));
        return result;
    }

    private byte[] readHeader(Path path, int size) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[size];
            int count = inputStream.read(buffer);
            if (count <= 0) {
                return new byte[0];
            }
            byte[] result = new byte[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        }
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", value));
        }
        return builder.toString();
    }

    private String normalizeExtension(String fileType, String fileName) {
        if (StringUtils.hasText(fileType)) {
            return fileType.toLowerCase(Locale.ROOT).replace(".", "");
        }
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isImage(String extension) {
        return "jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension);
    }

    private boolean isPhotoMaterial(ApplicationMaterial material) {
        String source = safe(material.getMaterialName()).toLowerCase(Locale.ROOT);
        return source.contains("照片") || source.contains("相片") || source.contains("证件照") || source.contains("photo");
    }

    private String normalizeExtractedText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String abbreviate(String value, int maxLength) {
        String normalized = normalizeExtractedText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private boolean containsAny(String source, List<String> keywords) {
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ImageQuality(double luminanceVariance, double edgeDelta) {
    }
}
