package com.example.demo.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.service.AppMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.erupt.upms.model.EruptUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/app/monitor")
public class AppMonitorController {

    @Autowired
    private AppMonitorService appMonitorService;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody Map<String, Object> info, HttpServletRequest request) {
        // 校验登录
        try {
            StpUtil.checkLogin();
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "Not logged in");
            return result;
        }

        long userId = StpUtil.getLoginIdAsLong();
        String token = StpUtil.getTokenValue();

        // 获取用户实体（可选，只需ID一般足够，但为了Username显示，Service层会handle）
        EruptUser user = entityManager.find(EruptUser.class, userId);

        // 获取 IP
        String ip = com.example.demo.utils.IpUtils.getClientIp(request);

        // 异步或同步更新状态
        appMonitorService.updateOnlineStatus(user, token, ip, info);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Heartbeat received");
        return result;
    }
}
