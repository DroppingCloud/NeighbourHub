package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.ServiceMaterialTemplate;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.ServiceMaterialTemplateMapper;
import com.community.platform.service.GuideService;
import com.community.platform.vo.guide.GuideResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceMaterialTemplateMapper materialTemplateMapper;

    @Value("${ai.fallback-enabled:true}")
    private Boolean fallbackEnabled;

    @Override
    public GuideResultVO recommend(GuideRequestDTO dto) {
        String keyword = StringUtils.hasText(dto.getNeedType()) ? dto.getNeedType() : dto.getDescription();
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, "online")
                .and(StringUtils.hasText(keyword), w -> w
                        .like(ServiceItem::getItemName, keyword)
                        .or()
                        .like(ServiceItem::getCategory, keyword)
                        .or()
                        .like(ServiceItem::getDescription, keyword))
                .orderByDesc(ServiceItem::getCreateTime)
                .last("LIMIT 5");
        List<ServiceItem> items = serviceItemMapper.selectList(wrapper);

        GuideResultVO vo = new GuideResultVO();
        vo.setItems(items.stream().map(this::toGuideItem).toList());
        vo.setMaterials(resolveMaterials(items));
        vo.setSteps(List.of("确认办理事项", "准备并上传材料", "提交申请", "等待审核", "查看办理进度"));
        vo.setTips(resolveTips(dto));
        vo.setIsFallback(Boolean.TRUE.equals(fallbackEnabled));
        return vo;
    }

    @Override
    public String chat(ChatMessageDTO dto) {
        String message = dto.getMessage() == null ? "" : dto.getMessage();
        if (message.contains("材料")) {
            return "请先在导办推荐中选择事项，再按材料清单上传身份证明、申请表和事项要求的附件。";
        }
        if (message.contains("进度") || message.contains("审核")) {
            return "您可以在“进度查询/我的申请”中查看审核状态，退回补件时系统会发送通知。";
        }
        if (message.contains("预约") || message.contains("服务")) {
            return "社区服务可在服务预约页面选择助餐、陪诊或上门服务，并填写期望时间和地址。";
        }
        return "我可以帮您查找办理事项、准备材料、提交申请和查询进度。请描述您要办理的事情。";
    }

    private GuideResultVO.GuideItemVO toGuideItem(ServiceItem item) {
        GuideResultVO.GuideItemVO vo = new GuideResultVO.GuideItemVO();
        vo.setItemId(item.getItemId());
        vo.setItemName(item.getItemName());
        vo.setCategory(item.getCategory());
        vo.setDescription(item.getDescription());
        return vo;
    }

    private List<String> resolveMaterials(List<ServiceItem> items) {
        if (items.isEmpty()) {
            return List.of("身份证明材料", "申请表", "其他事项要求材料");
        }
        List<String> materials = materialTemplateMapper.selectList(new LambdaQueryWrapper<ServiceMaterialTemplate>()
                        .eq(ServiceMaterialTemplate::getItemId, items.get(0).getItemId())
                        .orderByAsc(ServiceMaterialTemplate::getSortOrder)
                        .orderByAsc(ServiceMaterialTemplate::getTemplateId))
                .stream()
                .map(ServiceMaterialTemplate::getMaterialName)
                .toList();
        return materials.isEmpty() ? List.of("身份证明材料", "申请表", "其他事项要求材料") : materials;
    }

    private String resolveTips(GuideRequestDTO dto) {
        if (dto.getAge() != null && dto.getAge() >= 60) {
            return "已优先匹配老年人常办事项，请注意材料完整性和联系方式准确性。";
        }
        if ("non_local".equals(dto.getResidentType())) {
            return "非本地户籍事项可能需要居住证明或社保缴纳证明，请以事项材料清单为准。";
        }
        return "请按推荐事项准备材料并提交申请，审核结果会通过站内通知提醒。";
    }
}
