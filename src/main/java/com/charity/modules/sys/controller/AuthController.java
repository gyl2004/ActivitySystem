package com.charity.modules.sys.controller;

import com.charity.common.Result;
import com.charity.modules.sys.dto.LoginDTO;
import com.charity.modules.sys.dto.RegisterDTO;
import com.charity.security.LoginUser;
import com.charity.modules.sys.service.SysUserService;
import com.charity.modules.sys.vo.TokenVO;
import com.charity.modules.sys.mapper.SysPermissionMapper;
import com.charity.modules.sys.mapper.SysRoleMapper;
import com.charity.modules.sys.vo.UserInfoVO;
import com.charity.util.JwtUtils;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SysUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Value("${jwt.prefix}")
    private String prefix;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtils.generateToken(loginUser);
        
        List<String> roles = roleMapper.findRoleKeysByUserId(loginUser.getUserId());
        return Result.success(new TokenVO(token, prefix, roles));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUser(userService.getById(userId));
        userInfo.setRoles(roleMapper.findRoleKeysByUserId(userId));
        userInfo.setPermissions(permissionMapper.findPermissionKeysByUserId(userId));
        
        return Result.success(userInfo);
    }
}
