package com.protomil.core.user.service;

import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.shared.security.UserTokenClaims;
import com.protomil.core.user.dto.DashboardData;
import com.protomil.core.user.dto.NavigationItem;
import com.protomil.core.user.dto.DashboardWidget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DashboardService {

    private final RoleService roleService;

    public DashboardService(RoleService roleService) {
        this.roleService = roleService;
    }

    @LogExecutionTime
    public DashboardData getDashboardData(UserTokenClaims userClaims) {
        log.debug("Building dashboard data for user: {} with roles: {}",
                userClaims.getUserId(), userClaims.getRoles());

        List<NavigationItem> navigation = buildNavigationMenu(userClaims);
        List<DashboardWidget> widgets = buildDashboardWidgets(userClaims);

        return DashboardData.builder()
                .userId(userClaims.getUserId())
                .userEmail(userClaims.getEmail())
                .userFullName(userClaims.getFirstName() + " " + userClaims.getLastName())
                .userDepartment(userClaims.getDepartment())
                .userRoles(userClaims.getRoles())
                .navigationItems(navigation)
                .dashboardWidgets(widgets)
                .build();
    }

    private List<NavigationItem> buildNavigationMenu(UserTokenClaims userClaims) {
        List<NavigationItem> navigation = new ArrayList<>();
        List<String> roles = userClaims.getRoles();

        navigation.add(NavigationItem.builder()
                .id("dashboard")
                .label("Dashboard")
                .icon("bi-speedometer2")
                .url("/wireframes/dashboard")
                .active(true)
                .enabled(true)
                .order(1)
                .build());

        navigation.add(NavigationItem.builder()
                .id("profile")
                .label("My Profile")
                .icon("bi-person-circle")
                .url("/wireframes/profile")
                .enabled(true)
                .order(2)
                .build());

        if (hasAnyRole(roles, "TECHNICIAN", "SUPERVISOR", "ADMIN")) {
            navigation.add(NavigationItem.builder()
                    .id("jobcards")
                    .label("Job Cards")
                    .icon("bi-clipboard-check")
                    .url("/wireframes/jobcards")
                    .order(3)
                    .enabled(false)
                    .tooltip("Available in Phase 2")
                    .build());
        }

        if (hasAnyRole(roles, "SUPERVISOR", "ADMIN")) {
            navigation.add(NavigationItem.builder()
                    .id("equipment")
                    .label("Equipment")
                    .icon("bi-gear-fill")
                    .url("/wireframes/equipment")
                    .order(4)
                    .enabled(false)
                    .tooltip("Available in Phase 4")
                    .build());
        }

        if (hasAnyRole(roles, "ADMIN")) {
            navigation.add(NavigationItem.builder()
                    .id("users")
                    .label("User Management")
                    .icon("bi-people-fill")
                    .url("/wireframes/users")
                    .order(5)
                    .enabled(false)
                    .tooltip("Available in Phase 3")
                    .build());
        }

        if (hasAnyRole(roles, "SUPERVISOR", "ADMIN")) {
            navigation.add(NavigationItem.builder()
                    .id("reports")
                    .label("Reports")
                    .icon("bi-graph-up")
                    .url("/wireframes/reports")
                    .order(6)
                    .enabled(false)
                    .tooltip("Available in Phase 5")
                    .build());
        }

        return navigation;
    }

    private List<DashboardWidget> buildDashboardWidgets(UserTokenClaims userClaims) {
        List<DashboardWidget> widgets = new ArrayList<>();
        List<String> roles = userClaims.getRoles();

        // Welcome widget - always shown, no URL needed
        widgets.add(DashboardWidget.builder()
                .id("welcome")
                .title("Welcome, " + userClaims.getFirstName())
                .type("info")
                .content("Welcome to Protomil Manufacturing Execution System")
                .icon("bi-house-heart")
                .order(1)
                .size("col-12")
                .enabled(true)
                .url("") // Explicitly set empty URL for widgets without actions
                .build());

        if (hasAnyRole(roles, "PENDING_USER")) {
            widgets.add(DashboardWidget.builder()
                    .id("pending_approval")
                    .title("Account Pending Approval")
                    .type("warning")
                    .content("Your account is pending administrator approval. You will receive an email notification once approved.")
                    .icon("bi-clock-history")
                    .order(2)
                    .size("col-12")
                    .enabled(true)
                    .url("") // No action URL for this widget
                    .build());
        }

        if (hasAnyRole(roles, "TECHNICIAN")) {
            widgets.add(DashboardWidget.builder()
                    .id("my_jobcards")
                    .title("My Job Cards")
                    .type("primary")
                    .content("0 Active Jobs • 0 Completed Today")
                    .icon("bi-clipboard-check")
                    .order(3)
                    .size("col-md-6")
                    .enabled(false)
                    .url("/wireframes/jobcards") // Set URL for future use
                    .build());
        }

        if (hasAnyRole(roles, "SUPERVISOR")) {
            widgets.add(DashboardWidget.builder()
                    .id("team_overview")
                    .title("Team Overview")
                    .type("success")
                    .content("0 Active Team Members • 0 Jobs in Progress")
                    .icon("bi-people")
                    .order(4)
                    .size("col-md-6")
                    .enabled(false)
                    .url("/wireframes/team") // Set URL for future use
                    .build());
        }

        if (hasAnyRole(roles, "ADMIN")) {
            widgets.add(DashboardWidget.builder()
                    .id("system_overview")
                    .title("System Overview")
                    .type("info")
                    .content("0 Pending Approvals • 0 Active Users")
                    .icon("bi-speedometer2")
                    .order(5)
                    .size("col-md-6")
                    .enabled(false)
                    .url("/wireframes/admin") // Set URL for future use
                    .build());
        }

        return widgets;
    }

    private boolean hasAnyRole(List<String> userRoles, String... requiredRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }

        for (String required : requiredRoles) {
            if (userRoles.contains(required)) {
                return true;
            }
        }
        return false;
    }
}