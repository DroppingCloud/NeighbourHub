package com.community.platform.vo.guide;

import lombok.Data;

import java.util.List;

/**
 * 智能导办推荐结果 VO
 */
@Data
public class GuideResultVO {

    /** 推荐事项列表 */
    private List<GuideItemVO> items;

    /** 所需材料清单 */
    private List<String> materials;

    /** 办理步骤 */
    private List<String> steps;

    /** 温馨提示 */
    private String tips;

    /** 是否为降级结果（规则匹配，非 AI） */
    private Boolean isFallback;

    @Data
    public static class GuideItemVO {
        private Long itemId;
        private String itemName;
        private String category;
        private String description;
    }
}
