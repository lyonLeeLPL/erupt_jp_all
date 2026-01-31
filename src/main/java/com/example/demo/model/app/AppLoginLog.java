package com.example.demo.model.app;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Date;

@Table(name = "app_login_log")
@Entity
@Erupt(name = "App登录日志", orderBy = "loginTime desc")
public class AppLoginLog extends BaseModel {

    @EruptField(views = @View(title = "账号", sortable = true), edit = @Edit(title = "账号", search = @Search))
    private String username;

    @EruptField(views = @View(title = "登录时间", sortable = true), edit = @Edit(title = "登录时间", search = @Search(vague = true)))
    private Date loginTime;

    @EruptField(views = @View(title = "平台"), edit = @Edit(title = "平台", search = @Search, type = EditType.CHOICE))
    private String platform;

    @EruptField(views = @View(title = "App版本"), edit = @Edit(title = "App版本", search = @Search))
    private String appVersion; // versionName (versionCode)

    @EruptField(views = @View(title = "设备型号"), edit = @Edit(title = "设备型号", search = @Search(vague = true)))
    private String deviceModel;

    @EruptField(views = @View(title = "系统版本"), edit = @Edit(title = "系统版本"))
    private String osVersion;

    @EruptField(views = @View(title = "设备ID"), edit = @Edit(title = "设备ID"))
    private String deviceId;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
