package com.protomil.core.user.wireframe.controller;

import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.service.EmailVerificationService;
import com.protomil.core.user.service.UserRegistrationService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wireframes")
@Slf4j
public class WireframeController {

    private final UserRegistrationService userRegistrationService;
    private final EmailVerificationService emailVerificationService;

    public WireframeController(
            UserRegistrationService userRegistrationService,
            EmailVerificationService emailVerificationService) {
        this.userRegistrationService = userRegistrationService;
        this.emailVerificationService = emailVerificationService;
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
            RedirectAttributes redirectAttributes) {

        log.info("Processing registration request for email: {}", request.getEmail());

        // Initialize model with form data
        initializeFormModel(model, request);
        model.addAttribute("pageTitle", "User Registration - Protomil");

        // Handle validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = extractFieldErrors(bindingResult);
            model.addAttribute("fieldErrors", fieldErrors);
            model.addAttribute("hasErrors", true);

            log.debug("Validation errors: {}", fieldErrors);
            return "wireframes/register";
        }

        try {
            UserRegistrationResponse response = userRegistrationService.registerUser(request);

            // Success case
            if (response.getEmailVerificationRequired()) {
                // Redirect to email verification page
                redirectAttributes.addFlashAttribute("email", response.getEmail());
                redirectAttributes.addFlashAttribute("message",
                        "Registration successful! Please check your email for verification code.");
                return "redirect:/wireframes/verify-email";
            } else {
                // Direct success without email verification
                model.addAttribute("registrationSuccess", true);
                model.addAttribute("userEmail", response.getEmail());
                model.addAttribute("userId", response.getUserId());
                model.addAttribute("emailVerificationRequired", false);
                model.addAttribute("adminApprovalRequired", response.getAdminApprovalRequired());
            }

            log.info("User registration successful for email: {}", response.getEmail());
            return "wireframes/register";

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

            return "wireframes/register";

        } catch (Exception e) {
            log.error("Unexpected error during registration", e);

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("errorDetails", "If the problem persists, please contact support.");

            return "wireframes/register";
        }
    }

    @GetMapping("/verify-email")
    public String showEmailVerificationForm(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("pageTitle", "Email Verification - Protomil");
        return "wireframes/verify-email";
    }

    @PostMapping("/verify-email")
    public String processEmailVerification(
            @RequestParam String email,
            @RequestParam String verificationCode,
            Model model) {

        log.info("Processing email verification for: {}", email);

        try {
            emailVerificationService.verifyEmail(email, verificationCode);

            model.addAttribute("verificationSuccess", true);
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Email Verified - Protomil");

            log.info("Email verification successful for: {}", email);
            return "wireframes/verify-email";

        } catch (BusinessException e) {
            log.error("Email verification failed for: {} - {}", email, e.getMessage());

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Email Verification - Protomil");

            return "wireframes/verify-email";

        } catch (Exception e) {
            log.error("Unexpected error during email verification for: {}", email, e);

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Email Verification - Protomil");

            return "wireframes/verify-email";
        }
    }

    @PostMapping("/resend-verification")
    public String resendVerificationCode(@RequestParam String email, Model model) {
        log.info("Resending verification code for: {}", email);

        try {
            emailVerificationService.resendVerificationCode(email);

            model.addAttribute("email", email);
            model.addAttribute("message", "Verification code sent successfully! Please check your email.");
            model.addAttribute("pageTitle", "Email Verification - Protomil");

            return "wireframes/verify-email";

        } catch (BusinessException e) {
            log.error("Failed to resend verification code for: {} - {}", email, e.getMessage());

            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Email Verification - Protomil");

            return "wireframes/verify-email";
        }
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
}