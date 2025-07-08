#!/bin/bash

echo "Setting up Cognito Dev User Pool with Email Verification..."

# Set AWS profile
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

echo "Creating user pool with email verification..."
USER_POOL_RESULT=$(aws cognito-idp create-user-pool \
  --pool-name "protomil-dev-user-pool" \
  --policies '{
    "PasswordPolicy": {
      "MinimumLength": 8,
      "RequireUppercase": true,
      "RequireLowercase": true,
      "RequireNumbers": true,
      "RequireSymbols": true,
      "TemporaryPasswordValidityDays": 7
    }
  }' \
  --auto-verified-attributes email \
  --username-attributes email \
  --verification-message-template '{
    "DefaultEmailOption": "CONFIRM_WITH_CODE",
    "EmailSubject": "Verify your Protomil account",
    "EmailMessage": "Welcome to Protomil! Your verification code is {####}. Please enter this code to verify your email address."
  }' \
  --email-configuration '{
    "EmailSendingAccount": "COGNITO_DEFAULT"
  }' \
  --account-recovery-setting '{
    "RecoveryMechanisms": [
      {
        "Priority": 1,
        "Name": "verified_email"
      }
    ]
  }' \
  --user-pool-tags '{
    "Environment": "development",
    "Project": "protomil",
    "CreatedBy": "setup-script"
  }' \
  --schema '[
    {
      "Name": "email",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "given_name",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "family_name",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "phone_number",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    },
    {
      "Name": "employee_id",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true,
      "DeveloperOnlyAttribute": false
    },
    {
      "Name": "department",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true,
      "DeveloperOnlyAttribute": false
    }
  ]' \
  --profile protomil-dev \
  --region ap-south-1)

USER_POOL_ID=$(echo $USER_POOL_RESULT | jq -r '.UserPool.Id')
echo "User Pool created with ID: $USER_POOL_ID"

echo "Creating user pool client..."
CLIENT_RESULT=$(aws cognito-idp create-user-pool-client \
  --user-pool-id "$USER_POOL_ID" \
  --client-name "protomil-dev-client" \
  --explicit-auth-flows ALLOW_ADMIN_USER_PASSWORD_AUTH ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH \
  --supported-identity-providers COGNITO \
  --read-attributes email given_name family_name phone_number custom:employee_id custom:department \
  --write-attributes email given_name family_name phone_number custom:employee_id custom:department \
  --refresh-token-validity 30 \
  --access-token-validity 60 \
  --id-token-validity 60 \
  --token-validity-units '{
    "AccessToken": "minutes",
    "IdToken": "minutes",
    "RefreshToken": "days"
  }' \
  --prevent-user-existence-errors ENABLED \
  --enable-token-revocation \
  --profile protomil-dev \
  --region ap-south-1)

CLIENT_ID=$(echo $CLIENT_RESULT | jq -r '.UserPoolClient.ClientId')
echo "Client created with ID: $CLIENT_ID"

echo ""
echo "================================================="
echo "Cognito Setup Complete!"
echo "================================================="
echo ""
echo "Add these environment variables to your setup:"
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"ap-south-1\""
echo "export AWS_PROFILE=\"protomil-dev\""
echo ""
echo "User Pool ARN: arn:aws:cognito-idp:ap-south-1:$(aws sts get-caller-identity --query Account --output text):userpool/$USER_POOL_ID"