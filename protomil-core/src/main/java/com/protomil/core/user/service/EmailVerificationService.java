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
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Optional;

@Service
@Transactional
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final Optional<CognitoIdentityProviderClient> cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final ApplicationEventPublisher eventPublisher;

    public EmailVerificationService(
            UserRepository userRepository,
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("User email is already verified or account is not in verification pending status");
        }

        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - simulating email verification for: {}", email);
            completeEmailVerification(user);
            return;
        }

        try {
            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .confirmationCode(verificationCode)
                    .build();

            cognitoClient.get().confirmSignUp(confirmRequest);
            log.info("Email verification successful in Cognito for: {}", email);

            completeEmailVerification(user);

        } catch (CognitoIdentityProviderException e) {
            log.error("Email verification failed in Cognito for: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);

            String errorMessage = mapVerificationError(e.awsErrorDetails().errorCode(), e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e);
        }
    }

    @LogExecutionTime
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("User email is already verified or account is not in verification pending status");
        }

        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - simulating resend verification code for: {}", email);
            return;
        }

        try {
            ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .build();

            ResendConfirmationCodeResponse response = cognitoClient.get().resendConfirmationCode(resendRequest);
            log.info("Verification code resent successfully to: {} via: {}",
                    email, response.codeDeliveryDetails().deliveryMedium());

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to resend verification code for: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);

            String errorMessage = mapVerificationError(e.awsErrorDetails().errorCode(), e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e);
        }
    }

    private void completeEmailVerification(User user) {
        user.setStatus(UserStatus.PENDING_APPROVAL);
        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserEmailVerifiedEvent(savedUser));
        log.info("Email verification completed for user: {}", user.getEmail());
    }

    private String mapVerificationError(String errorCode, CognitoIdentityProviderException e) {
        return switch (errorCode) {
            case "CodeMismatchException" ->
                    "Invalid verification code. Please check the code and try again.";
            case "ExpiredCodeException" ->
                    "Verification code has expired. Please request a new code.";
            case "LimitExceededException" ->
                    "Too many attempts. Please wait before trying again.";
            case "UserNotFoundException" ->
                    "User not found. Please register first.";
            case "NotAuthorizedException" ->
                    "User is not authorized to perform this action.";
            case "UserNotConfirmedException" ->
                    "User email is not confirmed yet.";
            case "AliasExistsException" ->
                    "Email address is already verified for another account.";
            case "CodeDeliveryFailureException" ->
                    "Failed to send verification code. Please try again later.";
            default ->
                    "Email verification failed: " + e.awsErrorDetails().errorMessage();
        };
    }
}