package com.community.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.service.WorkOrderService;
import com.community.platform.vo.workorder.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    @Override
    public Page<WorkOrderVO> getList(WorkOrderQueryDTO query) {
        // TODO: 分页查询工单列表（工作人员只看自己负责的）
        throw new UnsupportedOperationException("TODO: 实现工单列表查询");
    }

    @Override
    public WorkOrderVO getDetail(Long orderId) {
        // TODO: 查询工单详情，关联申请信息、材料信息
        throw new UnsupportedOperationException("TODO: 实现工单详情查询");
    }

    @Override
    @Transactional
    public void audit(Long staffUserId, WorkOrderAuditDTO dto) {
        // TODO: 状态流转集中处理：
        //       approved → 申请状态更新为 approved
        //       rejected → 申请状态更新为 rejected
        //       supplement_required → 申请状态更新为 supplement_required
        //       completed → 申请状态更新为 completed
        //       记录工单日志、发送通知给居民
        throw new UnsupportedOperationException("TODO: 实现工单审核");
    }
}
