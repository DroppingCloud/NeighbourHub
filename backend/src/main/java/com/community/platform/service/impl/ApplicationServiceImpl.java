package com.community.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.service.ApplicationService;
import com.community.platform.vo.application.ApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    @Override
    @Transactional
    public Long submit(Long userId, ApplicationSubmitDTO dto) {
        // TODO: 校验事项是否存在、校验申请条件、保存申请单、创建工单、发送通知
        throw new UnsupportedOperationException("TODO: 实现申请提交");
    }

    @Override
    public Page<ApplicationVO> getList(Long userId, ApplicationQueryDTO query) {
        // TODO: 分页查询申请列表（仅本人或授权代理的申请）
        throw new UnsupportedOperationException("TODO: 实现申请列表查询");
    }

    @Override
    public ApplicationVO getDetail(Long userId, Long applicationId) {
        // TODO: 查询申请详情，校验数据权限
        throw new UnsupportedOperationException("TODO: 实现申请详情查询");
    }

    @Override
    @Transactional
    public void resubmit(Long userId, Long applicationId, ApplicationSubmitDTO dto) {
        // TODO: 校验申请状态为 supplement_required、更新申请单、重置工单状态
        throw new UnsupportedOperationException("TODO: 实现补件重新提交");
    }
}
