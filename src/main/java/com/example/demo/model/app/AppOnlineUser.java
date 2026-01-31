package com.example.demo.model.app;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Date;

@Table(name = "app_online_user")
@Entity
@Erupt(name = "在线用户监控", orderBy = "lastActiveTime desc")
public class AppOnlineUser extends BaseModel {

    @EruptField(views = @View(title = "账号", sortable = true), edit = @Edit(title = "账号", search = @Search))
    private String username;

    @EruptField(views = @View(title = "用户ID"), edit = @Edit(title = "用户ID", search = @Search))
    private Long userId;

    @EruptField(views = @View(title = "Token"), edit = @Edit(title = "Token"))
    private String token; // 用于区分多会话

    @EruptField(views = @View(title = "IP地址"), edit = @Edit(title = "IP地址", search = @Search))
    private String ip;

    @EruptField(views = @View(title = "地理位置"), edit = @Edit(title = "地理位置", search = @Search))
    private String region; // 国家|区域|省份|城市|ISP

    @EruptField(views = @View(title = "平台"), edit = @Edit(title = "平台", type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = "Android", label = "Android"),
            @VL(value = "iOS", label = "iOS")
    })))
    private String platform;

    @EruptField(views = @View(title = "App版本"), edit = @Edit(title = "App版本", search = @Search))
    private String appVersion;

    @EruptField(views = @View(title = "设备型号"), edit = @Edit(title = "设备型号", search = @Search(vague = true)))
    private String deviceModel;

    @EruptField(views = @View(title = "系统版本"), edit = @Edit(title = "系统版本"))
    private String osVersion;

    @EruptField(views = @View(title = "登录时间", type = ViewType.DATE_TIME), edit = @Edit(title = "登录时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private Date loginTime;

    @EruptField(views = @View(title = "最后活跃时间", sortable = true, type = ViewType.DATE_TIME), edit = @Edit(title = "最后活跃时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private Date lastActiveTime;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public Date getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}
