package com.alibaba.otter.canal.admin.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.otter.canal.admin.model.BaseModel;
import com.alibaba.otter.canal.admin.model.User;
import com.alibaba.otter.canal.admin.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * 用户管理控制层
 *
 * @author rewerma 2019-07-13 下午05:12:16
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/{env}/user")
public class UserController {

    public static final LoadingCache<String, User> loginUsers = Caffeine.newBuilder()
                                                                  .maximumSize(10_000)
                                                                  .expireAfterAccess(30, TimeUnit.MINUTES)
                                                                  .build(key -> null); // 用户登录信息缓存

    public static final Cache<String, User> apiKeyUsers = Caffeine.newBuilder()
            .maximumSize(10_000).build();

    @Autowired
    UserService                                    userService;

    static {
        User user = new User();
        user.setId(0L);
        user.setName("api-key");
        user.setPassword("6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9");
        user.setUsername("api-key");
        user.setRoles("admin");
        user.setCreationDate(new Date());
        user.setOldPassword("6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9");
        apiKeyUsers.put("e2b3b8f7a39d420ba3c099b538cd7fb3", user);
        apiKeyUsers.put("60dc1b7164dd4634b10afc5ab1d81665", user);
        apiKeyUsers.put("9cc1fc4e226a4199acd3a5d620d8dc23", user);
        apiKeyUsers.put("ef324202a2e84a698994a81555f526a1", user);
        apiKeyUsers.put("3f579b11a01d4df68831f6e363e49ec9", user);
    }

    /**
     * 用户登录
     *
     * @param user 账号密码
     * @param env 环境变量
     * @return token
     */
    @PostMapping(value = "/login")
    public BaseModel<Map<String, String>> login(@RequestBody User user, @PathVariable String env) {
        User loginUser = userService.find4Login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            Map<String, String> tokenResp = new HashMap<>();
            String token = UUID.randomUUID().toString();
            loginUsers.put(token, loginUser);
            tokenResp.put("token", token);
            return BaseModel.getInstance(tokenResp);
        } else {
            BaseModel<Map<String, String>> model = BaseModel.getInstance(null);
            model.setCode(40001);
            model.setMessage("Invalid username or password");
            return model;
        }
    }

    /**
     * 获取用户信息
     *
     * @param token token
     * @param env 环境变量
     * @return 用户信息
     */
    @GetMapping(value = "/info")
    public BaseModel<User> info(@RequestParam String token, @PathVariable String env) {
        User user = loginUsers.getIfPresent(token);
        if (user != null) {
            return BaseModel.getInstance(user);
        } else {
            BaseModel<User> model = BaseModel.getInstance(null);
            model.setCode(50014);
            model.setMessage("Invalid token");
            return model;
        }
    }

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @param env 环境变量
     * @param httpServletRequest httpServletRequest
     * @return 是否成功
     */
    @PutMapping(value = "")
    public BaseModel<String> update(@RequestBody User user, @PathVariable String env,
                                    HttpServletRequest httpServletRequest) {
        userService.update(user);
        String token = (String) httpServletRequest.getAttribute("token");
        loginUsers.put(token, user);
        return BaseModel.getInstance("success");
    }

    /**
     * 用户退出
     *
     * @param env 环境变量
     * @return 是否成功
     */
    @PostMapping(value = "/logout")
    public BaseModel<String> logout(@PathVariable String env) {
        return BaseModel.getInstance("success");
    }
}
