package com.example.demo.model.app;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import xyz.erupt.annotation.sub_field.sub_edit.VL;

@Table(name = "app_version")
@Entity
@Erupt(name = "App版本管理", orderBy = "versionCode desc")
public class AppVersion extends BaseModel {

    @EruptField(views = @View(title = "平台"), edit = @Edit(title = "平台", type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = "Android", label = "Android"),
            @VL(value = "iOS", label = "iOS")
    }), notNull = true, search = @Search))
    private String platform;

    @EruptField(views = @View(title = "版本名称"), edit = @Edit(title = "版本名称", notNull = true, search = @Search))
    private String versionName;

    @EruptField(views = @View(title = "版本号", sortable = true), edit = @Edit(title = "版本号(整数)", notNull = true))
    private Integer versionCode;

    @EruptField(views = @View(title = "最低可用版本号"), edit = @Edit(title = "最低可用版本号(小于此版本强制更新)", notNull = true))
    private Integer minVersionCode;

    @EruptField(views = @View(title = "强制更新"), edit = @Edit(title = "是否强制更新"))
    private Boolean isForceUpdate;

    @Lob
    @EruptField(views = @View(title = "更新内容"), edit = @Edit(title = "更新内容", type = EditType.TEXTAREA))
    private String updateContent;

    @EruptField(views = @View(title = "下载地址"), edit = @Edit(title = "下载地址"))
    private String downloadUrl;

    @EruptField(views = @View(title = "启用状态"), edit = @Edit(title = "是否启用", notNull = true))
    private Boolean isActive = true;

    // Getters and Setters
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public Integer getMinVersionCode() {
        return minVersionCode;
    }

    public void setMinVersionCode(Integer minVersionCode) {
        this.minVersionCode = minVersionCode;
    }

    public Boolean getIsForceUpdate() {
        return isForceUpdate;
    }

    public void setIsForceUpdate(Boolean isForceUpdate) {
        this.isForceUpdate = isForceUpdate;
    }

    public String getUpdateContent() {
        return updateContent;
    }

    public void setUpdateContent(String updateContent) {
        this.updateContent = updateContent;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
