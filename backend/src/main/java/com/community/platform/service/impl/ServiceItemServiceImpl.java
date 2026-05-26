package com.community.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.ServiceItem;
import com.community.platform.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    @Override
    public Page<ServiceItem> getList(String category, String status, Integer pageNum, Integer pageSize) {
        // TODO: 分页查询事项列表
        throw new UnsupportedOperationException("TODO: 实现事项列表查询");
    }

    @Override
    public ServiceItem getDetail(Long itemId) {
        // TODO: 查询事项详情
        throw new UnsupportedOperationException("TODO: 实现事项详情查询");
    }

    @Override
    public Long create(ServiceItem serviceItem) {
        // TODO: 创建事项
        throw new UnsupportedOperationException("TODO: 实现事项创建");
    }

    @Override
    public void update(ServiceItem serviceItem) {
        // TODO: 更新事项信息
        throw new UnsupportedOperationException("TODO: 实现事项更新");
    }

    @Override
    public void delete(Long itemId) {
        // TODO: 逻辑删除事项
        throw new UnsupportedOperationException("TODO: 实现事项删除");
    }
}
