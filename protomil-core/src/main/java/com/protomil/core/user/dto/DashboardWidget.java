// src/main/java/com/protomil/core/user/dto/DashboardWidget.java
package com.protomil.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget {

    private String id;
    private String title;
    private String type; // primary, secondary, success, danger, warning, info
    private String content;
    private String icon;
    private int order;
    private String size = "col-12"; // Bootstrap column classes
    private boolean enabled = true;
    private String url; // Optional click-through URL
}