package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.vo.workorder.WorkOrderVO;

public interface WorkOrderService {
    Page<WorkOrderVO> getList(WorkOrderQueryDTO query);
    WorkOrderVO getDetail(Long orderId);
    void audit(Long staffUserId, WorkOrderAuditDTO dto);
}
