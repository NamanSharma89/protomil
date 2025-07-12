package com.protomil.core.user.service;

import com.protomil.core.config.CognitoProperties;
import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.exception.ExternalServiceException;
import com.protomil.core.shared.exception.ResourceNotFoundException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.events.UserEmailVerifiedEvent;
import com.protomil.core.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.Optional;

@Service
@Transactional
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final Optional<CognitoIdentityProviderClient> cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final ApplicationEventPublisher eventPublisher;

    public EmailVerificationService(UserRepository userRepository,
                                    Optional<CognitoIdentityProviderClient> cognitoClient,
                                    CognitoProperties cognitoProperties,
                                    ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.cognitoProperties = cognitoProperties;
        this.eventPublisher = eventPublisher;
    }

    @LogExecutionTime
    public void verifyEmail(String email, String verificationCode) {
        log.info("Starting email verification for email: {}", email);

        // Step 1: Find user in database
        User user = findUserByEmail(email);
        log.info("Found user in database: userId={}, status={}, cognitoSub={}",
                user.getId(), user.getStatus(), user.getCognitoUserSub());

        // Step 2: Validate user status
        validateUserStatusForVerification(user);

        // Step 3: If Cognito is disabled, simulate verification
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - simulating email verification for: {}", email);
            completeEmailVerification(user);
            return;
        }

        // Step 4: Check Cognito user state before verification
        checkCognitoUserState(email);

        // Step 5: Verify in Cognito
        verifyCognitoEmail(email, verificationCode);

        // Step 6: Complete verification in database
        completeEmailVerification(user);
        log.info("Email verification completed successfully for user: {}", email);
    }

    @LogExecutionTime
    public void resendVerificationCode(String email) {
        log.info("Resending verification code for email: {}", email);

        // Step 1: Find user in database
        User user = findUserByEmail(email);
        log.info("Found user for resend: userId={}, status={}", user.getId(), user.getStatus());

        // Step 2: Validate user status
        validateUserStatusForResend(user);

        // Step 3: If Cognito is disabled, simulate resend
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - simulating resend verification code for: {}", email);
            return;
        }

        // Step 4: Check Cognito user state
        checkCognitoUserState(email);

        // Step 5: Resend in Cognito
        resendCognitoVerificationCode(email);
        log.info("Verification code resent successfully to: {}", email);
    }

    private User findUserByEmail(String email) {
        log.debug("Looking up user by email: {}", email);

        // Trim and normalize email
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            // Try with original case
            userOptional = userRepository.findByEmail(email);
        }

        if (userOptional.isEmpty()) {
            log.error("User not found in database with email: {} (normalized: {})", email, normalizedEmail);

            // Check if user exists in Cognito but not in our database
            if (cognitoProperties.isEnabled() && cognitoClient.isPresent()) {
                checkIfUserExistsInCognitoOnly(email);
            }

            throw new ResourceNotFoundException("User not found with email: " + email)
                    .addDetail("email", email)
                    .addDetail("normalizedEmail", normalizedEmail)
                    .addDetail("searchType", "email");
        }

        User user = userOptional.get();
        log.debug("User found in database: userId={}, cognitoSub={}, status={}",
                user.getId(), user.getCognitoUserSub(), user.getStatus());
        return user;
    }

    private void checkIfUserExistsInCognitoOnly(String email) {
        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build();

            AdminGetUserResponse cognitoUser = cognitoClient.get().adminGetUser(getUserRequest);
            log.error("CRITICAL: User exists in Cognito but NOT in database - email: {}, cognitoStatus: {}",
                    email, cognitoUser.userStatus());

            throw new BusinessException("User exists in Cognito but not in our database. Please contact support.")
                    .addDetail("email", email)
                    .addDetail("cognitoStatus", cognitoUser.userStatus().toString())
                    .addDetail("issue", "database_cognito_mismatch");

        } catch (UserNotFoundException e) {
            log.error("User not found in either database or Cognito: {}", email);
            // Let the original ResourceNotFoundException be thrown
        } catch (CognitoIdentityProviderException e) {
            log.error("Error checking Cognito user existence: {}", e.awsErrorDetails().errorMessage());
            // Let the original ResourceNotFoundException be thrown
        }
    }

    private void checkCognitoUserState(String email) {
        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build();

            AdminGetUserResponse response = cognitoClient.get().adminGetUser(getUserRequest);

            log.info("Cognito user state - Email: {}, Status: {}, Enabled: {}",
                    email, response.userStatus(), response.enabled());

            // Log user attributes for debugging
            boolean emailVerified = false;
            String emailValue = null;
            for (AttributeType attr : response.userAttributes()) {
                if ("email".equals(attr.name())) {
                    emailValue = attr.value();
                } else if ("email_verified".equals(attr.name())) {
                    emailVerified = "true".equals(attr.value());
                }
            }

            log.info("Cognito user attributes - Email value: {}, Email verified: {}", emailValue, emailVerified);

            // Validate Cognito user state
            if (!"UNCONFIRMED".equals(response.userStatus().toString())) {
                log.warn("Cognito user not in UNCONFIRMED status - Status: {}", response.userStatus());
                if ("CONFIRMED".equals(response.userStatus().toString())) {
                    throw new BusinessException("Email is already verified in Cognito")
                            .addDetail("cognitoStatus", response.userStatus().toString());
                }
            }

        } catch (UserNotFoundException e) {
            log.error("User not found in Cognito: {}", email);
            throw new BusinessException("User not found in Cognito. Please register first.")
                    .addDetail("email", email)
                    .addDetail("cognitoError", "UserNotFound");

        } catch (CognitoIdentityProviderException e) {
            log.error("Error checking Cognito user state: {}", e.awsErrorDetails().errorMessage());
            throw new ExternalServiceException("Unable to check user status in Cognito", "AWS Cognito", e)
                    .addDetail("email", email)
                    .addDetail("cognitoErrorCode", e.awsErrorDetails().errorCode());
        }
    }

    private void validateUserStatusForVerification(User user) {
        log.debug("Validating user status for verification: {}", user.getStatus());

        switch (user.getStatus()) {
            case PENDING_APPROVAL:
                log.warn("User email already verified but pending approval: {}", user.getEmail());
                throw new BusinessException("Your email is already verified. Your account is pending admin approval.")
                        .addDetail("currentStatus", user.getStatus().toString())
                        .addDetail("userId", user.getId().toString());

            case ACTIVE:
                log.warn("User account already active: {}", user.getEmail());
                throw new BusinessException("Your account is already verified and active.")
                        .addDetail("currentStatus", user.getStatus().toString())
                        .addDetail("userId", user.getId().toString());

            case SUSPENDED:
                log.error("Suspended user attempting verification: {}", user.getEmail());
                throw new BusinessException("Your account is suspended. Please contact support.")
                        .addDetail("currentStatus", user.getStatus().toString())
                        .addDetail("userId", user.getId().toString());

            case DELETED:
                log.error("Deleted user attempting verification: {}", user.getEmail());
                throw new BusinessException("Your account has been deleted. Please contact support.")
                        .addDetail("currentStatus", user.getStatus().toString())
                        .addDetail("userId", user.getId().toString());

            case PENDING_VERIFICATION:
                // This is the expected status - continue
                log.debug("User in correct status for verification: {}", user.getStatus());
                break;

            default:
                log.error("User in unexpected status for verification: email={}, status={}",
                        user.getEmail(), user.getStatus());
                throw new BusinessException("Account is not in verification pending status. Current status: " + user.getStatus())
                        .addDetail("currentStatus", user.getStatus().toString())
                        .addDetail("expectedStatus", UserStatus.PENDING_VERIFICATION.toString())
                        .addDetail("userId", user.getId().toString());
        }
    }

    private void validateUserStatusForResend(User user) {
        log.debug("Validating user status for resend: {}", user.getStatus());

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            log.error("User not in correct status for resend: email={}, status={}",
                    user.getEmail(), user.getStatus());
            throw new BusinessException("Cannot resend verification code. Account status: " + user.getStatus())
                    .addDetail("currentStatus", user.getStatus().toString())
                    .addDetail("expectedStatus", UserStatus.PENDING_VERIFICATION.toString())
                    .addDetail("userId", user.getId().toString());
        }
    }

    private void verifyCognitoEmail(String email, String verificationCode) {
        try {
            log.info("Attempting Cognito email verification for: {}", email);

            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .confirmationCode(verificationCode)
                    .build();

            cognitoClient.get().confirmSignUp(confirmRequest);
            log.info("Email verification successful in Cognito for: {}", email);

        } catch (CognitoIdentityProviderException e) {
            log.error("Email verification failed in Cognito for: {} - Error Code: {}, Error: {}",
                    email, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());

            String errorMessage = mapVerificationError(e.awsErrorDetails().errorCode(), e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e)
                    .addDetail("email", email)
                    .addDetail("cognitoErrorCode", e.awsErrorDetails().errorCode())
                    .addDetail("cognitoErrorMessage", e.awsErrorDetails().errorMessage());
        }
    }

    private void resendCognitoVerificationCode(String email) {
        try {
            log.info("Attempting to resend verification code in Cognito for: {}", email);

            ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .build();

            ResendConfirmationCodeResponse response = cognitoClient.get().resendConfirmationCode(resendRequest);

            if (response.codeDeliveryDetails() != null) {
                log.info("Verification code resent successfully to: {} via: {}, destination: {}",
                        email,
                        response.codeDeliveryDetails().deliveryMedium(),
                        response.codeDeliveryDetails().destination());
            } else {
                log.warn("Verification code sent but no delivery details returned for: {}", email);
            }

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to resend verification code for: {} - Error Code: {}, Error: {}",
                    email, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());

            String errorMessage = mapVerificationError(e.awsErrorDetails().errorCode(), e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e)
                    .addDetail("email", email)
                    .addDetail("cognitoErrorCode", e.awsErrorDetails().errorCode())
                    .addDetail("cognitoErrorMessage", e.awsErrorDetails().errorMessage());
        }
    }

    private void completeEmailVerification(User user) {
        log.info("Completing email verification for user: {}", user.getEmail());

        UserStatus previousStatus = user.getStatus();
        user.setStatus(UserStatus.PENDING_APPROVAL);
        User savedUser = userRepository.save(user);

        log.info("User status updated: email={}, previousStatus={}, newStatus={}",
                user.getEmail(), previousStatus, savedUser.getStatus());

        eventPublisher.publishEvent(new UserEmailVerifiedEvent(savedUser));
        log.info("UserEmailVerifiedEvent published for user: {}", user.getEmail());
    }

    private String mapVerificationError(String errorCode, CognitoIdentityProviderException e) {
        return switch (errorCode) {
            case "CodeMismatchException" -> "Invalid verification code. Please check the code and try again.";
            case "ExpiredCodeException" -> "Verification code has expired. Please request a new code.";
            case "LimitExceededException" -> "Too many attempts. Please wait before trying again.";
            case "UserNotFoundException" -> "User not found in Cognito. Please register first.";
            case "NotAuthorizedException" -> "User is not authorized to perform this action.";
            case "UserNotConfirmedException" -> "User email is not confirmed yet.";
            case "AliasExistsException" -> "Email address is already verified for another account.";
            case "CodeDeliveryFailureException" -> "Failed to send verification code. Please try again later.";
            case "InvalidParameterException" -> "Invalid request parameters. Please check your input.";
            case "TooManyRequestsException" -> "Too many requests. Please wait and try again.";
            case "InvalidLambdaResponseException" -> "Service configuration error. Please contact support.";
            case "UserLambdaValidationException" -> "User validation failed. Please contact support.";
            default -> "Email verification failed: " + e.awsErrorDetails().errorMessage();
        };
    }
}