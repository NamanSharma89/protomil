package com.protomil.core.user.wireframe.controller;

import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wireframes")
@Slf4j
public class WireframeController {

    private final UserRegistrationService userRegistrationService;

    public WireframeController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Protomil - User Registration");
        return "wireframes/index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        initializeFormModel(model, new UserRegistrationRequest());
        model.addAttribute("pageTitle", "User Registration - Protomil");
        log.debug("Displaying registration form");
        return "wireframes/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("userRegistration") UserRegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest httpRequest) {

        log.info("Processing registration request for email: {}", request.getEmail());

        // Initialize model with form data
        initializeFormModel(model, request);

        // Handle validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = extractFieldErrors(bindingResult);
            model.addAttribute("fieldErrors", fieldErrors);
            model.addAttribute("hasErrors", true);

            log.debug("Validation errors: {}", fieldErrors);
            return getViewName(httpRequest, "wireframes/register");
        }

        try {
            UserRegistrationResponse response = userRegistrationService.registerUser(request);

            // Success case
            model.addAttribute("registrationSuccess", true);
            model.addAttribute("userEmail", response.getEmail());
            model.addAttribute("userId", response.getUserId());
            model.addAttribute("emailVerificationRequired", response.getEmailVerificationRequired());
            model.addAttribute("adminApprovalRequired", response.getAdminApprovalRequired());

            log.info("User registration successful for email: {}", response.getEmail());
            return getViewName(httpRequest, "wireframes/register");

        } catch (BusinessException e) {
            log.error("Business error during registration: {}", e.getMessage());

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", e.getMessage());

            // Add suggestions based on error type
            if (e.getMessage().contains("already exists")) {
                model.addAttribute("suggestions", List.of(
                        "Try logging in if you already have an account",
                        "Use a different email address",
                        "Contact support if you believe this is an error"
                ));
            }

            return getViewName(httpRequest, "wireframes/register");

        } catch (Exception e) {
            log.error("Unexpected error during registration", e);

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("errorDetails", "If the problem persists, please contact support.");

            return getViewName(httpRequest, "wireframes/register");
        }
    }

    @PostMapping("/validate-email")
    @ResponseBody
    public String validateEmail(@RequestParam("email") String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return "<small class='text-danger'><i class='bi bi-x-circle me-1'></i>Email is required</small>";
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return "<small class='text-danger'><i class='bi bi-x-circle me-1'></i>Please enter a valid email address</small>";
            }

            return "<small class='text-success'><i class='bi bi-check-circle me-1'></i>Email looks good</small>";

        } catch (Exception e) {
            log.error("Error validating email: {}", email, e);
            return "<small class='text-warning'><i class='bi bi-exclamation-triangle me-1'></i>Unable to validate email</small>";
        }
    }

    @PostMapping("/validate-phone")
    @ResponseBody
    public String validatePhone(@RequestParam("phoneNumber") String phoneNumber) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return "<small class='text-danger'><i class='bi bi-x-circle me-1'></i>Phone number is required</small>";
            }

            if (!phoneNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
                return "<small class='text-danger'><i class='bi bi-x-circle me-1'></i>Please enter a valid phone number</small>";
            }

            return "<small class='text-success'><i class='bi bi-check-circle me-1'></i>Phone number looks good</small>";

        } catch (Exception e) {
            log.error("Error validating phone: {}", phoneNumber, e);
            return "<small class='text-warning'><i class='bi bi-exclamation-triangle me-1'></i>Unable to validate phone</small>";
        }
    }

    @PostMapping("/check-password-strength")
    @ResponseBody
    public String checkPasswordStrength(@RequestParam("password") String password) {
        if (password == null || password.isEmpty()) {
            return "<small class='text-muted'>Enter a password to see strength</small>";
        }

        int score = calculatePasswordScore(password);
        String feedback = getPasswordFeedback(password, score);
        String colorClass = getPasswordColorClass(score);
        String icon = getPasswordIcon(score);

        return String.format("<small class='%s'><i class='%s me-1'></i>%s</small>",
                colorClass, icon, feedback);
    }

    // Helper methods
    private void initializeFormModel(Model model, UserRegistrationRequest request) {
        model.addAttribute("userRegistration", request);
        model.addAttribute("registrationSuccess", false);
        model.addAttribute("hasErrors", false);
        model.addAttribute("errorMessage", null);
        model.addAttribute("fieldErrors", new HashMap<String, String>());
    }

    private Map<String, String> extractFieldErrors(BindingResult bindingResult) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return fieldErrors;
    }

    private String getViewName(HttpServletRequest request, String defaultView) {
        return isHtmxRequest(request) ? "wireframes/register :: main-content" : defaultView;
    }

    private boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }

    private int calculatePasswordScore(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[@$!%*?&].*")) score++;
        return score;
    }

    private String getPasswordFeedback(String password, int score) {
        return switch (score) {
            case 0, 1, 2 -> "Weak - " + getPasswordRequirements(password);
            case 3, 4 -> "Good - " + getPasswordRequirements(password);
            case 5 -> "Strong password";
            default -> "Enter a password";
        };
    }

    private String getPasswordColorClass(int score) {
        return switch (score) {
            case 0, 1, 2 -> "text-danger";
            case 3, 4 -> "text-warning";
            case 5 -> "text-success";
            default -> "text-muted";
        };
    }

    private String getPasswordIcon(int score) {
        return switch (score) {
            case 0, 1, 2 -> "bi bi-shield-exclamation";
            case 3, 4 -> "bi bi-shield-check";
            case 5 -> "bi bi-shield-fill-check";
            default -> "bi bi-shield";
        };
    }

    private String getPasswordRequirements(String password) {
        StringBuilder requirements = new StringBuilder("needs: ");

        if (password.length() < 8) requirements.append("8+ chars, ");
        if (!password.matches(".*[a-z].*")) requirements.append("lowercase, ");
        if (!password.matches(".*[A-Z].*")) requirements.append("uppercase, ");
        if (!password.matches(".*\\d.*")) requirements.append("number, ");
        if (!password.matches(".*[@$!%*?&].*")) requirements.append("special char, ");

        String result = requirements.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}