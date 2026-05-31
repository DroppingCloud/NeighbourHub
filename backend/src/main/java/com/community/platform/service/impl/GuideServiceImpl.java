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
            return "好的，我来帮您办理「" + itemName + "」。\n\n" +
                   "请确认以下信息：\n" +
                   "1. 您是否已准备好身份证、户口本等材料？\n" +
                   "2. 您是否年满80周岁？（高龄津贴申请条件）\n\n" +
                   "确认后请点击下方「开始办理」按钮，我将引导您完成申请。\n\n" +
                   "👉 点击「开始办理」进入申请页面";
        }
        
        // 检查最近一轮对话是否询问了是否需要帮助
        if (context.hasRecentQuestion()) {
            return "好的，请问您具体想办理什么事项？\n\n" +
                   "例如：\n" +
                   "• 老年补贴申请\n" +
                   "• 居住证办理\n" +
                   "• 低保申请";
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
                           lowerMsg.contains("帮我办") ||
                           lowerMsg.contains("我想办");
        
        if (!hasAction) {
            return null;
        }
        
        // 提取可能的事项名称
        String possibleItem = extractPossibleItemName(message);
        if (StringUtils.hasText(possibleItem)) {
            // 尝试查找匹配的事项
            List<ServiceItem> items = findServiceItemsByKeyword(possibleItem);
            if (!items.isEmpty()) {
                ServiceItem item = items.get(0);
                context.setLastRecommendedItem(item.getItemName());
                context.setPendingConfirmation(true);
                
                return "好的，我来帮您办理「" + item.getItemName() + "」。\n\n" +
                       "📋 所需材料：\n" +
                       "• 身份证原件及复印件\n" +
                       "• 户口本原件及复印件\n" +
                       "• 相关申请表\n\n" +
                       "办理流程：提交申请 → 审核 → 办结\n\n" +
                       "请确认是否准备好以上材料？如需开始办理，请回复「需要」或直接点击下方「开始办理」按钮。";
            }
        }
        
        return null;
    }
    
    /**
     * 从消息中提取可能的事项名称
     */
    private String extractPossibleItemName(String message) {
        // 常见事项关键词映射
        Map<String, String> itemKeywords = Map.of(
            "老年补贴", "高龄津贴",
            "高龄补贴", "高龄津贴",
            "养老金", "养老金",
            "居住证", "居住证办理",
            "低保", "低保申请",
            "居住证明", "居住证明开具",
            "身份证", "身份证办理",
            "社保卡", "社保卡办理"
        );
        
        for (Map.Entry<String, String> entry : itemKeywords.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
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
        // 如果AI回复中提到了具体事项，记录下来
        for (ServiceItem item : findServiceItemsByKeyword(aiResponse)) {
            context.setLastRecommendedItem(item.getItemName());
            context.setPendingConfirmation(true);
            
            // 在回复末尾添加引导
            if (!aiResponse.contains("开始办理") && !aiResponse.contains("点击")) {
                return aiResponse + "\n\n如需开始办理，请回复「需要」或点击下方「开始办理」按钮。";
            }
            break;
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
    
    private String ruleBasedChat(String message, SessionContext context) {
        String lowerMsg = message.toLowerCase();
        
        // 老年补贴相关
        if (lowerMsg.contains("老年") || lowerMsg.contains("高龄") || lowerMsg.contains("养老")) {
            context.setLastRecommendedItem("高龄津贴申请");
            context.setPendingConfirmation(true);
            return "高龄津贴申请需要：\n" +
                   "1. 申请人身份证原件及复印件\n" +
                   "2. 户口本原件及复印件\n" +
                   "3. 近期2寸免冠照片\n" +
                   "4. 银行卡或存折复印件\n\n" +
                   "办理流程：社区申请 → 街道审核 → 区级审批 → 按月发放\n\n" +
                   "如需开始办理，请回复「需要」或点击下方「开始办理」按钮。";
        }
        
        // 居住证相关
        if (lowerMsg.contains("居住证")) {
            context.setLastRecommendedItem("居住证办理");
            context.setPendingConfirmation(true);
            return "居住证办理所需材料：\n" +
                   "1. 本人身份证原件及复印件\n" +
                   "2. 居住地住址证明（房产证、租赁合同或住宿证明）\n" +
                   "3. 近期1寸白底免冠照片2张\n\n" +
                   "办理流程：社区登记 → 提交材料 → 等待审核 → 领取居住证\n\n" +
                   "如需开始办理，请回复「需要」或点击下方「开始办理」按钮。";
        }
        
        // 低保相关
        if (lowerMsg.contains("低保") || lowerMsg.contains("困难")) {
            context.setLastRecommendedItem("低保申请");
            context.setPendingConfirmation(true);
            return "低保申请所需材料：\n" +
                   "1. 书面申请书\n" +
                   "2. 家庭成员身份证、户口本\n" +
                   "3. 家庭收入证明\n" +
                   "4. 家庭财产状况证明\n\n" +
                   "如需开始办理，请回复「需要」或点击下方「开始办理」按钮。";
        }
        
        return "我可以帮您查找办理事项、准备材料、提交申请和查询进度。请描述您要办理的事情，例如：\n" +
               "• \"老年补贴怎么办？\"\n" +
               "• \"居住证需要哪些材料？\"\n" +
               "• \"低保怎么申请？\"";
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