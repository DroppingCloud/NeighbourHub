package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.vo.application.ApplicationVO;

public interface ApplicationService {
    Long submit(Long userId, ApplicationSubmitDTO dto);
    Page<ApplicationVO> getList(Long userId, ApplicationQueryDTO query);
    ApplicationVO getDetail(Long userId, Long applicationId);
    void resubmit(Long userId, Long applicationId, ApplicationSubmitDTO dto);
    void withdraw(Long userId, Long applicationId);
}
