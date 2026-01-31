package com.example.demo.service;

import com.example.demo.model.app.AppOnlineUser;
import com.example.demo.utils.IpUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.erupt.upms.model.EruptUser;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AppMonitorService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private IpUtils ipUtils;

    // 更新心跳/在线状态
    @Transactional
    public void updateOnlineStatus(EruptUser user, String token, String ip, Map<String, Object> deviceInfo) {
        // 查找当前用户是否已在线 (使用 Token 区分会话，或者仅使用 UserId 区分用户)
        // 这里假设一个用户在同一 Token 下唯一，如果多设备登录会产生多条记录

        List<AppOnlineUser> onlineUsers = entityManager.createQuery(
                "from AppOnlineUser where token = :token", AppOnlineUser.class)
                .setParameter("token", token)
                .setMaxResults(1)
                .getResultList();

        AppOnlineUser onlineUser;
        if (onlineUsers.isEmpty()) {
            // 新上线
            onlineUser = new AppOnlineUser();
            onlineUser.setUserId(user.getId());
            onlineUser.setUsername(user.getAccount()); // 或 getName
            onlineUser.setToken(token);
            onlineUser.setLoginTime(new Date());

            // 填充设备信息
            if (deviceInfo != null) {
                onlineUser.setPlatform((String) deviceInfo.get("platform"));
                onlineUser.setAppVersion((String) deviceInfo.get("versionName"));
                onlineUser.setDeviceModel((String) deviceInfo.get("model"));
                onlineUser.setOsVersion((String) deviceInfo.get("osVersion"));
            }
        } else {
            onlineUser = onlineUsers.get(0);
        }

        // 更新动态信息
        onlineUser.setIp(ip);
        onlineUser.setRegion(ipUtils.getRegion(ip));
        onlineUser.setLastActiveTime(new Date());

        entityManager.persist(onlineUser);
    }

    // 定时清理离线用户 (每分钟执行)
    // 假设超过 5 分钟未活跃即为离线
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void cleanupOfflineUsers() {
        Date threshold = new Date(System.currentTimeMillis() - 5 * 60 * 1000); // 5 minutes ago

        int deletedCount = entityManager.createQuery(
                "delete from AppOnlineUser where lastActiveTime < :threshold")
                .setParameter("threshold", threshold)
                .executeUpdate();

        // System.out.println("Cleaned up " + deletedCount + " offline users.");
    }
}
