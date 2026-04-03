package com.charity.modules.sys.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^(1[3-9][0-9]{9})?$", message = "手机号格式不正确")
    private String phone;
    
    private Integer gender; // 0-未知, 1-男, 2-女
}
