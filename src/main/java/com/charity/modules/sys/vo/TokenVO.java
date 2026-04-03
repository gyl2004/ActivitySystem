package com.charity.modules.sys.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 令牌响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {
    private String token;
    private String prefix;
    private List<String> roles;
}
