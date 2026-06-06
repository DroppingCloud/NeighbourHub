package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.entity.ServiceItem;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.service.AiService;
import com.community.platform.service.GuideService;
import com.community.platform.vo.guide.GuideResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final ServiceItemMapper serviceItemMapper;
    private final AiService aiService;
    
    // 存储会话历史，包含上下文状态
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    @Value("${ai.fallback-enabled:true}")
    private Boolean fallbackEnabled;

    @Override
    public GuideResultVO recommend(GuideRequestDTO dto) {
        if (Boolean.TRUE.equals(fallbackEnabled)) {
            try {
                String aiRecommendation = aiService.ask(buildRecommendPrompt(dto));
                if (StringUtils.hasText(aiRecommendation)) {
                    return parseAiRecommendation(aiRecommendation, dto);
                }
            } catch (Exception e) {
                log.warn("AI recommendation failed: {}", e.getMessage());
            }
        }
        return ruleBasedRecommend(dto);
    }

    @Override
    public String chat(ChatMessageDTO dto) {
        String message = dto.getMessage();
        if (!StringUtils.hasText(message)) {
            return "请问您想咨询什么问题？";
        }
        
        String sessionId = dto.getSessionId();
        SessionContext context = getOrCreateContext(sessionId);
        
        // 保存用户消息到历史
        context.addHistory("user", message);
        
        String response;
        
        // 首先检查是否是确认/肯定性回复（需要上下文）
        response = handleAffirmativeResponse(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            return response;
        }
        
        // 检查是否是拒绝/否定性回复
        response = handleNegativeResponse(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            return response;
        }
        
        // 检查是否是意图明确的办理请求
        response = handleApplicationRequest(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            return response;
        }
        
        // 尝试AI对话
        if (Boolean.TRUE.equals(fallbackEnabled)) {
            try {
                String aiResponse = aiService.chat(message, context.getHistoryForAi());
                if (StringUtils.hasText(aiResponse)) {
                    // 解析AI回复，判断是否需要推荐事项
                    String enhancedResponse = enhanceAiResponse(aiResponse, context);
                    context.addHistory("assistant", enhancedResponse);
                    // 更新上下文中的最后推荐事项
                    updateContextFromResponse(enhancedResponse, context);
                    return enhancedResponse;
                }
            } catch (Exception e) {
                log.warn("AI chat failed: {}", e.getMessage());
            }
        }
        
        // 降级：规则匹配
        response = ruleBasedChat(message, context);
        context.addHistory("assistant", response);
        return response;
    }

    @Override
    public void chatStream(ChatMessageDTO dto, java.util.function.Consumer<String> onToken) {
        String message = dto.getMessage();
        if (!StringUtils.hasText(message)) {
            onToken.accept("请问您想咨询什么问题？");
            onToken.accept("[DONE]");
            return;
        }

        String sessionId = dto.getSessionId();
        SessionContext context = getOrCreateContext(sessionId);
        context.addHistory("user", message);

        // 优先处理本地规则（肯定/否定/办理请求）以快速响应
        String response = handleAffirmativeResponse(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            onToken.accept(response);
            onToken.accept("[DONE]");
            return;
        }

        response = handleNegativeResponse(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            onToken.accept(response);
            onToken.accept("[DONE]");
            return;
        }

        response = handleApplicationRequest(message, context);
        if (response != null) {
            context.addHistory("assistant", response);
            onToken.accept(response);
            onToken.accept("[DONE]");
            return;
        }

        // 使用 AI 流式回复
        if (Boolean.TRUE.equals(fallbackEnabled)) {
            StringBuilder acc = new StringBuilder();
            aiService.chatStream(message, context.getHistoryForAi(), token -> {
                if ("[DONE]".equals(token)) {
                    String finalResp = acc.toString();
                    if (StringUtils.hasText(finalResp)) {
                        String enhanced = enhanceAiResponse(finalResp, context);
                        context.addHistory("assistant", enhanced);
                        updateContextFromResponse(enhanced, context);
                        onToken.accept("[DONE]");
                    } else {
                        // fallback to rule-based
                        String rb = ruleBasedChat(message, context);
                        context.addHistory("assistant", rb);
                        onToken.accept(rb);
                        onToken.accept("[DONE]");
                    }
                } else {
                    acc.append(token);
                    onToken.accept(token);
                }
            });
            return;
        }

        // 非 AI 模式，返回规则化回复
        response = ruleBasedChat(message, context);
        context.addHistory("assistant", response);
        onToken.accept(response);
        onToken.accept("[DONE]");
    }
    
    /**
     * 处理肯定性回复（如"需要"、"好的"、"可以"、"是的"）
     */
    private String handleAffirmativeResponse(String message, SessionContext context) {
        String lowerMsg = message.toLowerCase().trim();
        
        // 常见的肯定表达
        boolean isAffirmative = lowerMsg.equals("需要") || 
                                lowerMsg.equals("好的") ||
                                lowerMsg.equals("好") ||
                                lowerMsg.equals("可以") ||
                                lowerMsg.equals("是的") ||
                                lowerMsg.equals("对") ||
                                lowerMsg.equals("嗯") ||
                                lowerMsg.equals("ok") ||
                                lowerMsg.equals("yes") ||
                                lowerMsg.contains("开始办理") ||
                                lowerMsg.contains("帮我办");
        
        if (!isAffirmative) {
            return null;
        }
        
        // 检查上下文中是否有等待确认的事项
        if (context.getLastRecommendedItem() != null) {
            String itemName = context.getLastRecommendedItem();
            ProjectTarget target = resolveProjectTarget(itemName);
            if (target != null && "booking".equals(target.actionType)) {
                return buildBookingGuide(target);
            }
            if (target != null) {
                return buildApplicationGuide(target);
            }
        }
        
        // 检查最近一轮对话是否询问了是否需要帮助
        if (context.hasRecentQuestion()) {
            return "好的，请问您具体想办理什么事项？\n\n" +
                   "例如：\n" +
                   "• 老年补贴申请\n" +
                   "• 居住证办理\n" +
                   "• 居住证明开具\n" +
                   "• 便民证明\n" +
                   "• 助餐服务预约\n" +
                   "• 陪诊服务预约\n" +
                   "• 上门服务预约";
        }
        
        return null;
    }
    
    /**
     * 处理否定性回复
     */
    private String handleNegativeResponse(String message, SessionContext context) {
        String lowerMsg = message.toLowerCase().trim();
        
        boolean isNegative = lowerMsg.equals("不需要") || 
                             lowerMsg.equals("不用") ||
                             lowerMsg.equals("不") ||
                             lowerMsg.equals("算了") ||
                             lowerMsg.equals("取消");
        
        if (!isNegative) {
            return null;
        }
        
        return "好的，如果您改变了主意，随时可以再来找我。请问还有其他可以帮您的吗？";
    }
    
    /**
     * 处理明确的办理请求
     */
    private String handleApplicationRequest(String message, SessionContext context) {
        String lowerMsg = message.toLowerCase();
        
        // 检测是否包含办理动作关键词
        boolean hasAction = lowerMsg.contains("办理") || 
                           lowerMsg.contains("申请") ||
                           lowerMsg.contains("预约") ||
                           lowerMsg.contains("帮我办") ||
                           lowerMsg.contains("我想办");
        
        if (!hasAction) {
            return null;
        }
        
        ProjectTarget target = resolveProjectTarget(message);
        if (target != null) {
            context.setLastRecommendedItem(target.name);
            context.setPendingConfirmation(true);
            return "booking".equals(target.actionType) ? buildBookingGuide(target) : buildApplicationGuide(target);
        }
        
        return null;
    }
    
    /**
     * 从消息中提取可能的事项名称
     */
    private String extractPossibleItemName(String message) {
        ProjectTarget target = resolveProjectTarget(message);
        return target == null ? null : target.name;
    }
    
    /**
     * 根据关键词查找服务事项
     */
    private List<ServiceItem> findServiceItemsByKeyword(String keyword) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, "online")
                .and(w -> w
                    .like(ServiceItem::getItemName, keyword)
                    .or()
                    .like(ServiceItem::getCategory, keyword)
                    .or()
                    .like(ServiceItem::getDescription, keyword))
                .last("LIMIT 3");
        return serviceItemMapper.selectList(wrapper);
    }
    
    /**
     * 增强AI回复，添加操作引导
     */
    private String enhanceAiResponse(String aiResponse, SessionContext context) {
        ProjectTarget target = resolveProjectTarget(aiResponse);
        if (target != null) {
            context.setLastRecommendedItem(target.name);
            context.setPendingConfirmation(true);

            String actionText = "booking".equals(target.actionType) ? "立即预约" : "开始办理";
            if (!aiResponse.contains(actionText) && !aiResponse.contains("点击")) {
                return aiResponse + "\n\n如需继续，请点击下方「" + actionText + "」按钮。";
            }
        }
        return aiResponse;
    }
    
    /**
     * 从回复中更新上下文
     */
    private void updateContextFromResponse(String response, SessionContext context) {
        if (response.contains("开始办理") || response.contains("点击")) {
            context.setPendingConfirmation(true);
        }
    }

    private ProjectTarget resolveProjectTarget(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        // 证明类必须先于“居住证”判断，避免把“居住证明开具”误判为“居住证办理”。
        if (containsAny(text, "居住证明开具", "居住证明", "开居住证明")) {
            return ProjectTarget.application("居住证明开具", 3L, "证明");
        }
        if (containsAny(text, "居住证办理", "办理居住证", "办居住证", "居住证")) {
            return ProjectTarget.application("居住证办理", 1L, "证件");
        }
        if (containsAny(text, "老年补贴申请", "老年补贴", "高龄津贴", "高龄补贴")) {
            return ProjectTarget.application("老年补贴申请", 2L, "补贴");
        }
        if (containsAny(text, "便民证明", "便民证明开具", "无犯罪记录证明", "低收入证明", "困难证明", "同一人身份证明")) {
            return ProjectTarget.application("便民证明", 4L, "证明");
        }

        if (containsAny(text, "助餐服务", "助餐", "送餐", "配餐")) {
            return ProjectTarget.booking("助餐服务", "dining");
        }
        if (containsAny(text, "陪诊服务", "陪诊", "陪同就医", "取药")) {
            return ProjectTarget.booking("陪诊服务", "accompany");
        }
        if (containsAny(text, "上门服务", "上门", "家政清洁", "维修探访", "维修", "探访")) {
            return ProjectTarget.booking("上门服务", "home_visit");
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildApplicationGuide(ProjectTarget target) {
        String detail;
        switch (target.name) {
            case "居住证办理" -> detail =
                    "办理对象：非本地户籍居民。\n" +
                    "申请条件：可按合法稳定住所、合法稳定就业、连续就读三类情形之一申请。\n" +
                    "核心材料：居民身份证、本人相片，以及当前申请条件对应的一项证明材料。\n" +
                    "特别说明：这是“居住证办理”，用于申领居住证，不等同于“居住证明开具”。";
            case "居住证明开具" -> detail =
                    "办理对象：需要开具本辖区居住情况证明的居民。\n" +
                    "申请条件：居民身份与居住地址可核验；住在近亲属家中时需补充亲属关系证明。\n" +
                    "核心材料：身份证、居住地址证明；按实际情况补充亲属关系或房屋相关证明。\n" +
                    "特别说明：这是“居住证明开具”，用于开证明，不是办理居住证。";
            case "老年补贴申请" -> detail =
                    "办理对象：符合社区高龄津贴条件的老年居民。\n" +
                    "核心材料：身份证、户口簿、近期免冠两寸照片、社保卡或银行卡、高龄津贴申请表。\n" +
                    "办理结果：审核通过后按规则发放补贴。";
            case "便民证明" -> detail =
                    "办理对象：需要开具常见社区证明的居民。\n" +
                    "证明类型：无犯罪记录证明、低收入/困难证明、同一人身份证明等。\n" +
                    "核心材料：身份证明、申请事由说明，并按证明类型补充对应材料。";
            default -> detail = "请按事项页面提示填写信息并上传材料。";
        }

        return "已为您匹配到事项办理：「" + target.name + "」。\n\n" +
                detail + "\n\n" +
                "办理流程：填写信息 → 上传材料 → 提交申请 → 等待审核。\n\n" +
                "如需继续，请点击下方「开始办理」按钮。";
    }

    private String buildBookingGuide(ProjectTarget target) {
        String detail;
        switch (target.name) {
            case "助餐服务" -> detail = "适用于社区食堂配送到家等用餐协助需求。";
            case "陪诊服务" -> detail = "适用于陪同就医、取药等医疗陪伴需求。";
            case "上门服务" -> detail = "适用于家政清洁、维修探访等到家服务需求。";
            default -> detail = "请按预约页面填写时间、地址和特殊需求。";
        }
        return "已为您匹配到服务预约：「" + target.name + "」。\n\n" +
                detail + "\n" +
                "预约时需要填写：预约时间、服务地址、特殊需求。\n\n" +
                "如需继续，请点击下方「立即预约」按钮。";
    }
    
    private String ruleBasedChat(String message, SessionContext context) {
        ProjectTarget target = resolveProjectTarget(message);
        if (target != null) {
            context.setLastRecommendedItem(target.name);
            context.setPendingConfirmation(true);
            return "booking".equals(target.actionType) ? buildBookingGuide(target) : buildApplicationGuide(target);
        }

        String generalAnswer = answerGeneralQuestion(message);
        if (generalAnswer != null) {
            context.setLastRecommendedItem(null);
            context.setPendingConfirmation(false);
            return generalAnswer;
        }
        
        return "我可以根据本平台已开放的服务帮您判断去哪里办理。\n\n" +
               "事项办理：居住证办理、老年补贴申请、居住证明开具、便民证明。\n" +
               "服务预约：助餐服务、陪诊服务、上门服务。\n\n" +
               "请尽量说清楚您要办的是“证件/证明/补贴申请”，还是“预约社区服务”。例如：\n" +
               "1. 居住证办理需要哪些材料？\n" +
               "2. 开居住证明怎么办？\n" +
               "3. 老年补贴怎么申请？\n" +
               "4. 我想预约陪诊服务。";
    }

    private String answerGeneralQuestion(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }

        if (containsAny(message, "多久", "多长时间", "办理时间", "处理时间", "几天", "什么时候", "时限")) {
            return "办理时长会根据业务类型和材料完整度不同而变化。\n\n" +
                    "一般规则：\n" +
                    "1. 事项办理提交后，工作人员会先进行材料审核。\n" +
                    "2. 材料齐全、信息准确时，通常会更快进入办理环节。\n" +
                    "3. 如果材料不清晰或缺少证明，会收到补充材料通知，补充后继续审核。\n" +
                    "4. 服务预约提交后，工作人员接取并确认后，会按预约时间提供服务。\n\n" +
                    "您可以在“进度查询”或“我的申请/我的预约”里查看最新状态；如时间较急，建议联系社区服务中心或拨打 12345。";
        }

        if (containsAny(message, "进度", "状态", "查询", "查到哪", "到哪了")) {
            return "您可以通过平台查看办理或预约进度。\n\n" +
                    "查询方式：\n" +
                    "1. 事项办理：进入“我的申请”或“进度查询”。\n" +
                    "2. 服务预约：进入“我的预约”或“进度查询”。\n" +
                    "3. 有补充材料、审核结果、预约接取等变化时，也会在“消息通知”中提醒。\n\n" +
                    "如果您是家属代办，请先确认已完成居民绑定，并切换到对应代办对象。";
        }

        if (containsAny(message, "材料", "资料", "要带什么", "需要什么", "准备什么")) {
            return "不同业务需要的材料不同，平台会在具体事项页面展示材料清单。\n\n" +
                    "通用建议：\n" +
                    "1. 身份相关材料请保证照片或扫描件清晰完整。\n" +
                    "2. 地址、就业、就读、补贴等证明材料需与申请条件一致。\n" +
                    "3. 如果系统提示补充材料，请按通知说明重新上传。\n\n" +
                    "您可以告诉我具体业务名称，例如“居住证办理需要什么材料”或“便民证明要什么材料”，我可以按具体业务说明。";
        }

        if (containsAny(message, "家属", "代办", "帮老人", "替", "绑定")) {
            return "家属可以在平台中进行代办，但需要先完成绑定关系。\n\n" +
                    "操作方式：\n" +
                    "1. 进入“家属代办”。\n" +
                    "2. 提交绑定申请。\n" +
                    "3. 等待居民确认后，即可代居民提交事项申请、服务预约并查看相关通知。\n\n" +
                    "如果绑定未完成，系统会限制代办提交。";
        }

        if (containsAny(message, "通知", "消息", "提醒", "铃铛")) {
            return "平台会通过“消息通知”和顶部铃铛提醒您业务变化。\n\n" +
                    "常见通知包括：\n" +
                    "1. 事项审核结果。\n" +
                    "2. 补充材料提醒。\n" +
                    "3. 服务预约接取、开始、完成等状态变化。\n\n" +
                    "点击通知后，系统会尽量跳转到对应业务所在位置。";
        }

        if (containsAny(message, "怎么用", "不会用", "流程", "步骤")) {
            return "平台主要分为事项办理和服务预约两类流程。\n\n" +
                    "事项办理流程：选择事项 → 查看条件和材料 → 填写申请信息 → 上传材料 → 提交审核 → 查看进度。\n" +
                    "服务预约流程：选择服务类型 → 填写预约时间和地址 → 提交预约 → 等待工作人员接取 → 按预约时间服务。\n\n" +
                    "如果您告诉我具体要办什么，我可以进一步帮您判断应该去“事项办理”还是“服务预约”。";
        }

        return null;
    }
    
    private SessionContext getOrCreateContext(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionContexts.computeIfAbsent(sessionId, k -> new SessionContext());
        }
        return new SessionContext();
    }
    
    // 其他辅助方法...
    private String buildRecommendPrompt(GuideRequestDTO dto) { /* 同上 */ return ""; }
    private GuideResultVO parseAiRecommendation(String aiResponse, GuideRequestDTO dto) { return new GuideResultVO(); }
    private GuideResultVO ruleBasedRecommend(GuideRequestDTO dto) { return new GuideResultVO(); }

    private static class ProjectTarget {
        private final String name;
        private final String actionType;
        private final Long itemId;
        private final String category;
        private final String bookingServiceType;

        private ProjectTarget(String name, String actionType, Long itemId, String category, String bookingServiceType) {
            this.name = name;
            this.actionType = actionType;
            this.itemId = itemId;
            this.category = category;
            this.bookingServiceType = bookingServiceType;
        }

        private static ProjectTarget application(String name, Long itemId, String category) {
            return new ProjectTarget(name, "application", itemId, category, null);
        }

        private static ProjectTarget booking(String name, String bookingServiceType) {
            return new ProjectTarget(name, "booking", null, null, bookingServiceType);
        }
    }
    
    /**
     * 会话上下文 - 存储对话状态
     */
    private static class SessionContext {
        private final List<AiService.ChatHistory> history = new ArrayList<>();
        private String lastRecommendedItem;
        private boolean pendingConfirmation;
        private long lastQuestionTime;
        
        public void addHistory(String role, String content) {
            history.add(new AiService.ChatHistory(role, content));
            // 只保留最近20条
            while (history.size() > 20) {
                history.remove(0);
            }
            if ("user".equals(role)) {
                lastQuestionTime = System.currentTimeMillis();
            }
        }
        
        public List<AiService.ChatHistory> getHistoryForAi() {
            return new ArrayList<>(history);
        }
        
        public String getLastRecommendedItem() {
            return lastRecommendedItem;
        }
        
        public void setLastRecommendedItem(String item) {
            this.lastRecommendedItem = item;
        }
        

        
        public void setPendingConfirmation(boolean pending) {
            this.pendingConfirmation = pending;
        }
        
        public boolean hasRecentQuestion() {
            // 检查最近30秒内是否有问题
            return System.currentTimeMillis() - lastQuestionTime < 30000;
        }
    }
}
