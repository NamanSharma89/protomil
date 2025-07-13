// src/main/java/com/protomil/core/user/controller/LoginController.java (Updated)
package com.protomil.core.user.controller;

import com.protomil.core.shared.exception.AuthenticationException;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.security.CookieManager;
import com.protomil.core.shared.security.SessionManager;
import com.protomil.core.shared.security.TokenPair;
import com.protomil.core.shared.security.UserTokenClaims;
import com.protomil.core.user.dto.LoginRequest;
import com.protomil.core.user.dto.LoginResponse;
import com.protomil.core.user.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/wireframes")
@Slf4j
public class LoginController {

    private final AuthenticationService authenticationService;

    @Value("${protomil.ui.login.company-logo:}")
    private String companyLogo;

    @Value("${protomil.ui.login.company-name:Protomil}")
    private String companyName;

    @Value("${protomil.ui.login.company-tagline:Manufacturing Execution System}")
    private String companyTagline;

    private final CookieManager cookieManager;
    private final SessionManager sessionManager;

    public LoginController(AuthenticationService authenticationService,
                           CookieManager cookieManager,
                           SessionManager sessionManager) {
        this.authenticationService = authenticationService;
        this.cookieManager = cookieManager;
        this.sessionManager = sessionManager;
    }

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String redirect,
            HttpServletRequest request,
            Model model) {

        log.debug("Displaying login page with message: {}, error: {}", message, error);

        // Check if user is already authenticated
        if (isUserAuthenticated(request)) {
            log.debug("User already authenticated, redirecting to dashboard");
            return "redirect:/wireframes/dashboard";
        }

        // Initialize form model
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("pageTitle", "Login - " + companyName);

        // Add messages if present
        if (StringUtils.hasText(message)) {
            model.addAttribute("message", message);
        }

        if (StringUtils.hasText(error)) {
            model.addAttribute("error", error);
        }

        // Store redirect URL for post-login redirect (validate it's safe)
        if (StringUtils.hasText(redirect) && isSafeRedirectUrl(redirect)) {
            model.addAttribute("redirectUrl", redirect);
        } else {
            model.addAttribute("redirectUrl", "/wireframes/dashboard");
        }

        // Add company customization
        addCompanyBranding(model);

        return "wireframes/login";
    }

    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Processing login request for email: {}", loginRequest.getEmail());

        // Initialize model for potential re-display
        model.addAttribute("pageTitle", "Login - " + companyName);
        addCompanyBranding(model);

        // Validate form input
        if (bindingResult.hasErrors()) {
            log.debug("Login form validation errors: {}", bindingResult.getAllErrors());
            model.addAttribute("error", "Please correct the highlighted fields");
            return "wireframes/login";
        }

        try {
            // Authenticate user
            LoginResponse loginResponse = authenticationService.authenticateUser(loginRequest);

            // Create user token claims for cookie
            UserTokenClaims userClaims = UserTokenClaims.builder()
                    .cognitoSub(loginResponse.getEmail()) // Using email as cognito sub for now
                    .userId(loginResponse.getUserId())
                    .email(loginResponse.getEmail())
                    .firstName(loginResponse.getFirstName())
                    .lastName(loginResponse.getLastName())
                    .department(loginResponse.getDepartment())
                    .roles(loginResponse.getRoles())
                    .tokenType("access")
                    .build();

            // Create token pair for cookie
            TokenPair tokenPair = TokenPair.builder()
                    .accessToken(loginResponse.getAccessToken())
                    .refreshToken(loginResponse.getRefreshToken())
                    .accessTokenExpiresIn(loginResponse.getExpiresIn())
                    .refreshTokenExpiresIn(7200) // 2 hours
                    .tokenType("Bearer")
                    .issuedAt(loginResponse.getLoginTime())
                    .build();

            // Set authentication cookies
            cookieManager.setAuthenticationCookies(response, tokenPair, userClaims,
                    loginRequest.getRememberMe());

            // Determine redirect URL
            String targetUrl = determineRedirectUrl(redirectUrl);

            log.info("Login successful for user: {} with roles: {}, redirecting to: {}",
                    loginRequest.getEmail(), loginResponse.getRoles(), targetUrl);

            // Add success message for redirect
            redirectAttributes.addFlashAttribute("message",
                    "Welcome back, " + loginResponse.getFirstName() + "!");

            return "redirect:" + targetUrl;

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {} - {}", loginRequest.getEmail(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "wireframes/login";

        } catch (BusinessException e) {
            log.warn("Business rule violation during login for user: {} - {}", loginRequest.getEmail(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "wireframes/login";

        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", loginRequest.getEmail(), e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "wireframes/login";
        }
    }


    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        log.info("Processing logout request");

        try {
            // Get user from request if available
            UserTokenClaims userClaims = (UserTokenClaims) request.getAttribute("authenticatedUser");
            if (userClaims != null) {
                authenticationService.logout(userClaims.getUserId());
            }

        } catch (Exception e) {
            log.warn("Error during logout process: {}", e.getMessage());
            // Continue with logout even if there are errors
        }

        // Clear authentication cookies
        cookieManager.clearAuthenticationCookies(response);

        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully.");

        log.info("Logout completed successfully");
        return "redirect:/wireframes/login";
    }


    private void setAuthenticationCookies(HttpServletResponse response, LoginResponse loginResponse) {
        // Access token cookie (HttpOnly for security)
        Cookie accessTokenCookie = new Cookie("ACCESS_TOKEN", loginResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // TODO: Set to true in production with HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(loginResponse.getExpiresIn()); // Token expiration time
        response.addCookie(accessTokenCookie);

        // Refresh token cookie (HttpOnly for security)
        if (loginResponse.getRefreshToken() != null) {
            Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", loginResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(false); // TODO: Set to true in production with HTTPS
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(7200); // 2 hours (refresh token lifetime)
            response.addCookie(refreshTokenCookie);
        }

        // User info cookie (not HttpOnly - needed by JavaScript)
        String userInfo = String.format("{\"userId\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"roles\":%s}",
                loginResponse.getUserId(),
                loginResponse.getEmail(),
                loginResponse.getFirstName(),
                loginResponse.getRoles().toString());

        Cookie userInfoCookie = new Cookie("USER_INFO", userInfo);
        userInfoCookie.setHttpOnly(false); // Accessible to JavaScript
        userInfoCookie.setSecure(false); // TODO: Set to true in production
        userInfoCookie.setPath("/");
        userInfoCookie.setMaxAge(loginResponse.getRememberMe() ? 7200 : -1); // Session or remember me
        response.addCookie(userInfoCookie);

        log.debug("Authentication cookies set for user: {}", loginResponse.getEmail());
    }

    private void clearAuthenticationCookies(HttpServletResponse response) {
        String[] cookieNames = {"ACCESS_TOKEN", "REFRESH_TOKEN", "USER_INFO"};

        for (String cookieName : cookieNames) {
            Cookie cookie = new Cookie(cookieName, "");
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // TODO: Set to true in production
            cookie.setPath("/");
            cookie.setMaxAge(0); // Delete cookie
            response.addCookie(cookie);
        }

        log.debug("Authentication cookies cleared");
    }

    private boolean isUserAuthenticated(HttpServletRequest request) {
        return cookieManager.hasValidAuthCookies(request);
    }

    private boolean isSafeRedirectUrl(String redirectUrl) {
        // Validate redirect URL to prevent open redirect attacks
        if (!StringUtils.hasText(redirectUrl)) {
            return false;
        }

        // Only allow internal URLs
        return redirectUrl.startsWith("/wireframes/") ||
                redirectUrl.startsWith("/api/") ||
                redirectUrl.equals("/") ||
                redirectUrl.startsWith("/dashboard");
    }

    private String determineRedirectUrl(String redirectUrl) {
        if (StringUtils.hasText(redirectUrl) && isSafeRedirectUrl(redirectUrl)) {
            return redirectUrl;
        }
        return "/wireframes/dashboard"; // Default redirect
    }

    private void addCompanyBranding(Model model) {
        if (StringUtils.hasText(companyLogo)) {
            model.addAttribute("companyLogo", companyLogo);
        }
        model.addAttribute("companyName", companyName);
        model.addAttribute("companyTagline", companyTagline);

        log.debug("Company branding added: name={}, tagline={}, logo={}",
                companyName, companyTagline, companyLogo);
    }
}