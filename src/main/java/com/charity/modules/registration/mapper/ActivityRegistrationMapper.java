package com.charity.modules.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.dto.RegistrationQueryDTO;
import com.charity.modules.registration.vo.RegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {
    IPage<RegistrationVO> selectVOPage(Page<RegistrationVO> page, @Param("query") RegistrationQueryDTO query);

    List<RegistrationVO> selectVOList(@Param("query") RegistrationQueryDTO query);
}
