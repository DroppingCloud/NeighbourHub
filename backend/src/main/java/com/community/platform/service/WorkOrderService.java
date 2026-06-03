package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.vo.workorder.WorkOrderVO;

import java.util.List;
import java.util.Map;

public interface WorkOrderService {
    Page<WorkOrderVO> getList(WorkOrderQueryDTO query);
    WorkOrderVO getDetail(Long orderId);
    void audit(Long staffUserId, WorkOrderAuditDTO dto);
    List<WorkOrderLog> getLogs(Long orderId);
    void assign(Long orderId);
    void autoReassign(Long orderId);
    void batchAudit(Long staffUserId, List<WorkOrderAuditDTO> audits);
    void updateStatusByApplicationId(Long applicationId, String status);
    Map<String, Long> getStats();
}
