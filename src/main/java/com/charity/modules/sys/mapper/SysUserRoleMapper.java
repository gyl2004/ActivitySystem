package com.charity.modules.sys.mapper;

import com.charity.modules.sys.entity.SysUserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {

    @Select("<script>" +
            "SELECT user_id AS userId, role_id AS roleId FROM sys_user_role " +
            "WHERE user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<SysUserRole> listByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> listRoleIdsByUserId(Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
