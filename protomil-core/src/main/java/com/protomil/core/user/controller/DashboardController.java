// src/main/java/com/protomil/core/user/controller/DashboardController.java
package com.protomil.core.user.controller;

import com.protomil.core.shared.security.UserTokenClaims;
import com.protomil.core.user.service.DashboardService;
import com.protomil.core.user.dto.DashboardData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/wireframes")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpServletRequest request, Model model) {
        UserTokenClaims userClaims = (UserTokenClaims) request.getAttribute("authenticatedUser");

        if (userClaims == null) {
            log.warn("No authenticated user found in request, redirecting to login");
            return "redirect:/wireframes/login";
        }

        log.debug("Loading dashboard for user: {} with roles: {}",
                userClaims.getEmail(), userClaims.getRoles());

        try {
            // Load dashboard data based on user roles
            DashboardData dashboardData = dashboardService.getDashboardData(userClaims);

            // Add data to model
            model.addAttribute("user", userClaims);
            model.addAttribute("dashboardData", dashboardData);
            model.addAttribute("pageTitle", "Dashboard - Protomil");

            log.debug("Dashboard loaded successfully for user: {}", userClaims.getEmail());

            return "wireframes/dashboard";

        } catch (Exception e) {
            log.error("Error loading dashboard for user: {}", userClaims.getEmail(), e);
            model.addAttribute("error", "Unable to load dashboard. Please try again.");
            return "wireframes/dashboard";
        }
    }

    @GetMapping("/profile")
    public String showProfile(HttpServletRequest request, Model model) {
        UserTokenClaims userClaims = (UserTokenClaims) request.getAttribute("authenticatedUser");

        if (userClaims == null) {
            log.warn("No authenticated user found in request, redirecting to login");
            return "redirect:/wireframes/login";
        }

        log.debug("Loading profile for user: {}", userClaims.getEmail());

        model.addAttribute("user", userClaims);
        model.addAttribute("pageTitle", "User Profile - Protomil");

        return "wireframes/profile";
    }
}