package com.protomil.core.jobcard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "app.jobcard")
public class JobCardConfig {

    private int maxFileUploadSize = 100 * 1024 * 1024; // 100MB
    private int maxAttachmentsPerJob = 10;
    private boolean enableNotifications = true;
    private int assignmentTimeout = 24; // hours

    public int getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

    public void setMaxFileUploadSize(int maxFileUploadSize) {
        this.maxFileUploadSize = maxFileUploadSize;
    }

    public int getMaxAttachmentsPerJob() {
        return maxAttachmentsPerJob;
    }

    public void setMaxAttachmentsPerJob(int maxAttachmentsPerJob) {
        this.maxAttachmentsPerJob = maxAttachmentsPerJob;
    }

    public boolean isEnableNotifications() {
        return enableNotifications;
    }

    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    public int getAssignmentTimeout() {
        return assignmentTimeout;
    }

    public void setAssignmentTimeout(int assignmentTimeout) {
        this.assignmentTimeout = assignmentTimeout;
    }
}