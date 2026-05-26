package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.ServiceItem;

public interface ServiceItemService {
    Page<ServiceItem> getList(String category, String status, Integer pageNum, Integer pageSize);
    ServiceItem getDetail(Long itemId);
    Long create(ServiceItem serviceItem);
    void update(ServiceItem serviceItem);
    void delete(Long itemId);
}
