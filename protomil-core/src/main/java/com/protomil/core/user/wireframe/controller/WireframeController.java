package com.protomil.core.user.wireframe.controller;

import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.service.UserRegistrationService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
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

import java.time.LocalDateTime;
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
        // Initialize all required model attributes
        model.addAttribute("userRegistration", new UserRegistrationRequest());
        model.addAttribute("pageTitle", "User Registration - Protomil");

        // Initialize boolean flags to prevent null pointer issues
        model.addAttribute("registrationSuccess", false);
        model.addAttribute("hasErrors", false);
        model.addAttribute("errorMessage", null);
        model.addAttribute("fieldErrors", new HashMap<String, String>());

        return "wireframes/register";
    }

    @PostMapping("/register")
    @HxRequest
    public String processRegistration(
            @Valid @ModelAttribute("userRegistration") UserRegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            HtmxResponse htmxResponse) {

        // Always initialize base attributes
        model.addAttribute("registrationSuccess", false);
        model.addAttribute("hasErrors", false);
        model.addAttribute("errorMessage", null);
        model.addAttribute("fieldErrors", new HashMap<String, String>());

        if (bindingResult.hasErrors()) {
            // Add field-specific errors to model
            Map<String, String> fieldErrors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                fieldErrors.put(error.getField(), error.getDefaultMessage());
            }
            model.addAttribute("fieldErrors", fieldErrors);
            model.addAttribute("hasErrors", true);

            log.debug("Validation errors: {}", fieldErrors);

            // Return just the form fragment to avoid full page reload
            return "wireframes/register :: registration-form";
        }

        try {
            UserRegistrationResponse response = userRegistrationService.registerUser(request);

            model.addAttribute("registrationSuccess", true);
            model.addAttribute("userEmail", response.getEmail());
            model.addAttribute("userId", response.getUserId());
            model.addAttribute("emailVerificationRequired", response.getEmailVerificationRequired());
            model.addAttribute("adminApprovalRequired", response.getAdminApprovalRequired());

            // Trigger a custom event for potential further processing
            htmxResponse.addTrigger("user-registered");

            log.info("User registration successful for email: {}", response.getEmail());
            return "wireframes/register :: success-message";

        } catch (BusinessException e) {
            log.error("Business error during registration: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("hasErrors", true);
            return "wireframes/register :: error-message";

        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("hasErrors", true);
            return "wireframes/register :: error-message";
        }
    }

    @PostMapping("/validate-email")
    @HxRequest
    @ResponseBody
    public String validateEmail(@RequestParam("email") String email) {
        try {
            // Simple email validation
            if (email == null || email.trim().isEmpty()) {
                return "<span class='error-text'>Email is required</span>";
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return "<span class='error-text'>Please enter a valid email address</span>";
            }

            return "<span class='success-text'>✓ Email looks good</span>";

        } catch (Exception e) {
            log.error("Error validating email: {}", email, e);
            return "<span class='error-text'>Error validating email</span>";
        }
    }

    @PostMapping("/validate-phone")
    @HxRequest
    @ResponseBody
    public String validatePhone(@RequestParam("phoneNumber") String phoneNumber) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return "<span class='error-text'>Phone number is required</span>";
            }

            if (!phoneNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
                return "<span class='error-text'>Please enter a valid phone number</span>";
            }

            return "<span class='success-text'>✓ Phone number looks good</span>";

        } catch (Exception e) {
            log.error("Error validating phone: {}", phoneNumber, e);
            return "<span class='error-text'>Error validating phone number</span>";
        }
    }

    @PostMapping("/check-password-strength")
    @HxRequest
    @ResponseBody
    public String checkPasswordStrength(@RequestParam("password") String password) {
        if (password == null || password.isEmpty()) {
            return "<div class='password-strength weak'>Password is required</div>";
        }

        int score = 0;
        String feedback = "";

        if (password.length() >= 8) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[@$!%*?&].*")) score++;

        switch (score) {
            case 0, 1, 2 -> {
                feedback = "Weak - " + getPasswordRequirements(password);
                return "<div class='password-strength weak'>" + feedback + "</div>";
            }
            case 3, 4 -> {
                feedback = "Fair - " + getPasswordRequirements(password);
                return "<div class='password-strength fair'>" + feedback + "</div>";
            }
            case 5 -> {
                return "<div class='password-strength strong'>✓ Strong password</div>";
            }
        }

        return "<div class='password-strength weak'>Please enter a password</div>";
    }

    private String getPasswordRequirements(String password) {
        StringBuilder requirements = new StringBuilder("Needs: ");
        if (password.length() < 8) requirements.append("8+ chars, ");
        if (!password.matches(".*[a-z].*")) requirements.append("lowercase, ");
        if (!password.matches(".*[A-Z].*")) requirements.append("uppercase, ");
        if (!password.matches(".*\\d.*")) requirements.append("number, ");
        if (!password.matches(".*[@$!%*?&].*")) requirements.append("special char, ");

        String result = requirements.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}