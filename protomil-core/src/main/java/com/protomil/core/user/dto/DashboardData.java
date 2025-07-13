// src/main/java/com/protomil/core/user/dto/DashboardData.java
package com.protomil.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardData {

    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String userDepartment;
    private List<String> userRoles;
    private List<NavigationItem> navigationItems;
    private List<DashboardWidget> dashboardWidgets;
}