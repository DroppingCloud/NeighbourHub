package com.community.platform.enums;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum WorkOrderStatus {
    PENDING("pending", "待审核"),
    PROCESSING("processing", "处理中"),
    APPROVED("approved", "审核通过"),
    REJECTED("rejected", "已驳回"),
    SUPPLEMENT_REQUIRED("supplement_required", "退回补件"),
    COMPLETED("completed", "已办结"),
    CANCELLED("cancelled", "已撤回");

    private final String code;
    private final String label;
    private static final Map<String, WorkOrderStatus> CODE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        PENDING.code, Set.of(PROCESSING.code, CANCELLED.code),
        PROCESSING.code, Set.of(APPROVED.code, REJECTED.code, SUPPLEMENT_REQUIRED.code, CANCELLED.code),
        SUPPLEMENT_REQUIRED.code, Set.of(PENDING.code, CANCELLED.code),
        APPROVED.code, Set.of(COMPLETED.code, CANCELLED.code),
        REJECTED.code, Set.of(),
        COMPLETED.code, Set.of(),
        CANCELLED.code, Set.of()
    );

    static {
        for (WorkOrderStatus status : values()) {
            CODE_MAP.put(status.code, status);
        }
    }

    WorkOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static WorkOrderStatus fromCode(String code) {
        return CODE_MAP.get(code);
    }

    public static boolean isValidTransition(String from, String to) {
        Set<String> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}