package com.community.platform.service;

import com.community.platform.entity.User;

public interface StaffDispatchService {

    User selectBestStaff(Long communityId, Long excludeUserId, boolean allowGlobalFallback);

    User selectBestStaff(Long communityId, Long excludeUserId, boolean allowGlobalFallback, String staffType);
}
