# src/main/resources/aws-scripts/setup-cognito-with-custom-attributes.sh
#!/bin/bash

echo "Setting up Cognito User Pool with Custom Attributes for Login Flow..."
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get AWS Account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --profile $AWS_PROFILE --region $AWS_REGION 2>/dev/null)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to get AWS Account ID. Check your AWS credentials and profile.${NC}"
    exit 1
fi

echo -e "${BLUE}AWS Account ID: $ACCOUNT_ID${NC}"

# Check if user pool exists
USER_POOL_NAME="protomil-dev-user-pool"
EXISTING_USER_POOL_ID=$(aws cognito-idp list-user-pools --max-items 60 --profile $AWS_PROFILE --region $AWS_REGION --query "UserPools[?Name=='$USER_POOL_NAME'].Id" --output text 2>/dev/null)

if [ -n "$EXISTING_USER_POOL_ID" ] && [ "$EXISTING_USER_POOL_ID" != "None" ]; then
    echo -e "${YELLOW}⚠️  User Pool '$USER_POOL_NAME' already exists with ID: $EXISTING_USER_POOL_ID${NC}"
    echo "Updating user pool with custom attributes..."
    USER_POOL_ID="$EXISTING_USER_POOL_ID"

    # Update existing user pool with custom attributes
    aws cognito-idp update-user-pool \
        --user-pool-id "$USER_POOL_ID" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION \
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
        --user-attribute-update-settings '{
            "AttributesRequireVerificationBeforeUpdate": ["email"]
        }' \
        --verification-message-template '{
            "DefaultEmailOption": "CONFIRM_WITH_CODE",
            "EmailSubject": "Verify your Protomil account",
            "EmailMessage": "Welcome to Protomil! Your verification code is {####}. Please enter this code to verify your email address."
        }' \
        --email-configuration '{
            "EmailSendingAccount": "COGNITO_DEFAULT"
        }' \
        --admin-create-user-config '{
            "AllowAdminCreateUserOnly": false,
            "InviteMessageTemplate": {
                "EmailSubject": "Welcome to Protomil",
                "EmailMessage": "Your username is {username} and temporary password is {####}"
            }
        }' 2>/dev/null

    echo -e "${GREEN}✅ User Pool updated successfully${NC}"
else
    echo "Creating new user pool with custom attributes..."

    USER_POOL_RESULT=$(aws cognito-idp create-user-pool \
        --pool-name "$USER_POOL_NAME" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION \
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
        --verification-message-template '{
            "DefaultEmailOption": "CONFIRM_WITH_CODE",
            "EmailSubject": "Verify your Protomil account",
            "EmailMessage": "Welcome to Protomil! Your verification code is {####}. Please enter this code to verify your email address."
        }' \
        --email-configuration '{
            "EmailSendingAccount": "COGNITO_DEFAULT"
        }' \
        --user-attribute-update-settings '{
            "AttributesRequireVerificationBeforeUpdate": ["email"]
        }' \
        --admin-create-user-config '{
            "AllowAdminCreateUserOnly": false,
            "InviteMessageTemplate": {
                "EmailSubject": "Welcome to Protomil",
                "EmailMessage": "Your username is {username} and temporary password is {####}"
            }
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
            },
            {
                "Name": "approval_status",
                "AttributeDataType": "String",
                "Required": false,
                "Mutable": true,
                "DeveloperOnlyAttribute": false
            },
            {
                "Name": "local_user_id",
                "AttributeDataType": "String",
                "Required": false,
                "Mutable": true,
                "DeveloperOnlyAttribute": false
            },
            {
                "Name": "user_roles",
                "AttributeDataType": "String",
                "Required": false,
                "Mutable": true,
                "DeveloperOnlyAttribute": false
            }
        ]' \
        --tags '{
            "Environment": "development",
            "Project": "protomil",
            "CreatedBy": "setup-script"
        }' 2>/dev/null)

    if [ $? -eq 0 ]; then
        USER_POOL_ID=$(echo $USER_POOL_RESULT | jq -r '.UserPool.Id')
        echo -e "${GREEN}✅ User Pool created with ID: $USER_POOL_ID${NC}"
    else
        echo -e "${RED}❌ Failed to create user pool${NC}"
        exit 1
    fi
fi

# Create or update user pool client
CLIENT_NAME="protomil-dev-client"
EXISTING_CLIENT_ID=$(aws cognito-idp list-user-pool-clients \
    --user-pool-id "$USER_POOL_ID" \
    --profile $AWS_PROFILE \
    --region $AWS_REGION \
    --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId" \
    --output text 2>/dev/null)

if [ -n "$EXISTING_CLIENT_ID" ] && [ "$EXISTING_CLIENT_ID" != "None" ]; then
    CLIENT_ID="$EXISTING_CLIENT_ID"
    echo -e "${YELLOW}⚠️  User Pool Client '$CLIENT_NAME' already exists with ID: $CLIENT_ID${NC}"

    # Update existing client
    aws cognito-idp update-user-pool-client \
        --user-pool-id "$USER_POOL_ID" \
        --client-id "$CLIENT_ID" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION \
        --explicit-auth-flows "ALLOW_USER_PASSWORD_AUTH" "ALLOW_REFRESH_TOKEN_AUTH" "ALLOW_USER_SRP_AUTH" \
        --generate-secret false \
        --prevent-user-existence-errors "ENABLED" \
        --enable-token-revocation true \
        --access-token-validity 30 \
        --id-token-validity 30 \
        --refresh-token-validity 120 \
        --token-validity-units '{
            "AccessToken": "minutes",
            "IdToken": "minutes",
            "RefreshToken": "minutes"
        }' 2>/dev/null

    echo -e "${GREEN}✅ User Pool Client updated successfully${NC}"
else
    echo "Creating new user pool client..."

    CLIENT_RESULT=$(aws cognito-idp create-user-pool-client \
        --user-pool-id "$USER_POOL_ID" \
        --client-name "$CLIENT_NAME" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION \
        --explicit-auth-flows "ALLOW_USER_PASSWORD_AUTH" "ALLOW_REFRESH_TOKEN_AUTH" "ALLOW_USER_SRP_AUTH" \
        --generate-secret false \
        --prevent-user-existence-errors "ENABLED" \
        --enable-token-revocation true \
        --access-token-validity 30 \
        --id-token-validity 30 \
        --refresh-token-validity 120 \
        --token-validity-units '{
            "AccessToken": "minutes",
            "IdToken": "minutes",
            "RefreshToken": "minutes"
        }' 2>/dev/null)

    if [ $? -eq 0 ]; then
        CLIENT_ID=$(echo $CLIENT_RESULT | jq -r '.UserPoolClient.ClientId')
        echo -e "${GREEN}✅ User Pool Client created with ID: $CLIENT_ID${NC}"
    else
        echo -e "${RED}❌ Failed to create user pool client${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}Cognito Setup Complete with Custom Attributes!${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
echo -e "${BLUE}📋 Environment Variables:${NC}"
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"$AWS_REGION\""
echo "export AWS_PROFILE=\"$AWS_PROFILE\""
echo ""
echo -e "${BLUE}🎯 Custom Attributes Added:${NC}"
echo "• custom:approval_status - Sync with local DB user status"
echo "• custom:local_user_id - Reference to local database user ID"
echo "• custom:user_roles - Comma-separated list of user roles"
echo "• custom:employee_id - Employee identification"
echo "• custom:department - User department"
echo ""
echo -e "${BLUE}⏰ Token Configuration:${NC}"
echo "• Access Token: 30 minutes"
echo "• ID Token: 30 minutes"
echo "• Refresh Token: 120 minutes (2 hours)"
echo ""
echo -e "${BLUE}🔐 Auth Flows Enabled:${NC}"
echo "• ALLOW_USER_PASSWORD_AUTH (for login)"
echo "• ALLOW_REFRESH_TOKEN_AUTH (for token refresh)"
echo "• ALLOW_USER_SRP_AUTH (for enhanced security)"