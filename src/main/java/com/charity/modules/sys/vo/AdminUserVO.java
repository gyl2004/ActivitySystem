package com.charity.modules.sys.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private Integer points;
    private Double volunteerDuration;
    private LocalDateTime createTime;
    private Long roleId;
    private String roleName;
    private String roleKey;
    private List<String> roleKeys;
}
