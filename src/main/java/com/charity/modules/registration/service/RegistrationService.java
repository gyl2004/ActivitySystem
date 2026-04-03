package com.charity.modules.registration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.dto.RegistrationDTO;
import com.charity.modules.registration.dto.AuditDTO;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.modules.registration.vo.RegistrationVO;

import java.util.List;
import java.util.Map;

/**
 * 报名服务接口
 */
public interface RegistrationService extends IService<ActivityRegistration> {
    
    /**
     * 获取报名状态统计
     */
    Map<Integer, Long> getStatusStats();

    /**
     * 分页查询所有报名记录 (管理端)
     */
    IPage<RegistrationVO> findPageWithDetails(Page<ActivityRegistration> page, Integer status, Long activityId);

    /**
     * 提交报名
     */
    void register(RegistrationDTO registrationDTO, Long userId);
    
    /**
     * 取消报名
     */
    void cancel(Long activityId, Long userId);
    
    /**
     * 审核报名
     */
    void audit(Long id, AuditDTO auditDTO);

    /**
     * 批量审核报名
     */
    void batchAudit(List<Long> ids, AuditDTO auditDTO);

    /**
     * 报名退款处理
     */
    void refund(Long id);
}

    /**
     * 获取当前用户的报名记录
     */
    List<RegistrationVO> findMyRegistrations(Long userId);
}
