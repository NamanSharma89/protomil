#!/bin/bash

# Enhanced Cognito setup script with inline custom attributes creation
# File: src/main/resources/aws-scripts/setup-cognito-with-inline-attributes.sh

echo "Setting up Cognito User Pool with Inline Custom Attributes..."

export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

# Color codes for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Utility functions
check_result() {
    local operation="$1"
    local result_code=$2
    if [ $result_code -eq 0 ]; then
        echo -e "${GREEN}✅ $operation successful${NC}"
        return 0
    else
        echo -e "${RED}❌ $operation failed${NC}"
        return 1
    fi
}

# Verify AWS credentials
echo -e "${BLUE}Checking AWS credentials...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --profile $AWS_PROFILE --region $AWS_REGION 2>/dev/null)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to get AWS Account ID. Check your AWS credentials and profile.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ AWS Account ID: $ACCOUNT_ID${NC}"

# Check for existing user pool and clean up if necessary
USER_POOL_NAME="protomil-dev-user-pool"
echo -e "${BLUE}Checking for existing user pool...${NC}"
EXISTING_USER_POOL_ID=$(aws cognito-idp list-user-pools \
    --max-items 60 \
    --profile $AWS_PROFILE \
    --region $AWS_REGION \
    --query "UserPools[?Name=='$USER_POOL_NAME'].Id" \
    --output text 2>/dev/null)

if [ -n "$EXISTING_USER_POOL_ID" ] && [ "$EXISTING_USER_POOL_ID" != "None" ] && [ "$EXISTING_USER_POOL_ID" != "" ]; then
    echo -e "${YELLOW}⚠️  Found existing user pool: $EXISTING_USER_POOL_ID${NC}"
    echo -e "${YELLOW}🗑️  Deleting existing user pool to create fresh one...${NC}"
    aws cognito-idp delete-user-pool \
        --user-pool-id "$EXISTING_USER_POOL_ID" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Existing user pool deleted${NC}"
    else
        echo -e "${YELLOW}⚠️  Could not delete existing user pool (might not exist)${NC}"
    fi

    echo -e "${BLUE}⏳ Waiting 15 seconds for AWS cleanup...${NC}"
    sleep 15
else
    echo -e "${GREEN}✅ No existing user pool found${NC}"
fi

# Create user pool with custom attributes in schema
echo -e "${BLUE}Creating new user pool with custom attributes...${NC}"

USER_POOL_CREATION_OUTPUT=$(aws cognito-idp create-user-pool \
    --pool-name "$USER_POOL_NAME" \
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
        "SmsMessage": "Your verification code is {####}",
        "EmailMessage": "Welcome to Protomil! Your verification code is {####}. Please enter this code to verify your email address.",
        "EmailSubject": "Verify your Protomil account",
        "DefaultEmailOption": "CONFIRM_WITH_CODE"
    }' \
    --user-pool-add-ons '{
        "AdvancedSecurityMode": "OFF"
    }' \
    --admin-create-user-config '{
        "AllowAdminCreateUserOnly": false,
        "InviteMessageTemplate": {
            "EmailMessage": "Welcome to Protomil! Your username is {username} and temporary password is {####}",
            "EmailSubject": "Welcome to Protomil"
        }
    }' \
    --email-configuration '{
        "EmailSendingAccount": "COGNITO_DEFAULT"
    }' \
    --user-pool-tags '{
        "Environment": "development",
        "Project": "protomil",
        "CreatedBy": "setup-script",
        "Purpose": "user-authentication"
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
            "DeveloperOnlyAttribute": false,
            "StringAttributeConstraints": {
                "MinLength": "1",
                "MaxLength": "50"
            }
        },
        {
            "Name": "department",
            "AttributeDataType": "String",
            "Required": false,
            "Mutable": true,
            "DeveloperOnlyAttribute": false,
            "StringAttributeConstraints": {
                "MinLength": "1",
                "MaxLength": "100"
            }
        },
        {
            "Name": "approval_status",
            "AttributeDataType": "String",
            "Required": false,
            "Mutable": true,
            "DeveloperOnlyAttribute": false,
            "StringAttributeConstraints": {
                "MinLength": "1",
                "MaxLength": "50"
            }
        },
        {
            "Name": "local_user_id",
            "AttributeDataType": "String",
            "Required": false,
            "Mutable": true,
            "DeveloperOnlyAttribute": false,
            "StringAttributeConstraints": {
                "MinLength": "1",
                "MaxLength": "36"
            }
        },
        {
            "Name": "user_roles",
            "AttributeDataType": "String",
            "Required": false,
            "Mutable": true,
            "DeveloperOnlyAttribute": false,
            "StringAttributeConstraints": {
                "MinLength": "0",
                "MaxLength": "500"
            }
        }
    ]' \
    --profile $AWS_PROFILE \
    --region $AWS_REGION 2>&1)

CREATE_RESULT=$?

if [ $CREATE_RESULT -eq 0 ]; then
    USER_POOL_ID=$(echo "$USER_POOL_CREATION_OUTPUT" | jq -r '.UserPool.Id' 2>/dev/null)
    if [ -n "$USER_POOL_ID" ] && [ "$USER_POOL_ID" != "null" ]; then
        echo -e "${GREEN}✅ User Pool created successfully with ID: $USER_POOL_ID${NC}"
        echo -e "${GREEN}✅ Custom attributes included in schema during creation${NC}"
    else
        echo -e "${RED}❌ Could not extract User Pool ID from response${NC}"
        echo "Response: $USER_POOL_CREATION_OUTPUT"
        exit 1
    fi
else
    echo -e "${RED}❌ Failed to create user pool${NC}"
    echo "Error output:"
    echo "$USER_POOL_CREATION_OUTPUT"
    exit 1
fi

# Create user pool client
echo -e "${BLUE}Creating user pool client...${NC}"
CLIENT_NAME="protomil-dev-client"

CLIENT_CREATION_OUTPUT=$(aws cognito-idp create-user-pool-client \
    --user-pool-id "$USER_POOL_ID" \
    --client-name "$CLIENT_NAME" \
    --refresh-token-validity 120 \
    --access-token-validity 30 \
    --id-token-validity 30 \
    --token-validity-units '{
        "AccessToken": "minutes",
        "IdToken": "minutes",
        "RefreshToken": "minutes"
    }' \
    --explicit-auth-flows '[
        "ALLOW_USER_PASSWORD_AUTH",
        "ALLOW_REFRESH_TOKEN_AUTH",
        "ALLOW_USER_SRP_AUTH"
    ]' \
    --supported-identity-providers '["COGNITO"]' \
    --write-attributes '[
        "email",
        "given_name",
        "family_name",
        "phone_number",
        "custom:employee_id",
        "custom:department",
        "custom:approval_status",
        "custom:local_user_id",
        "custom:user_roles"
    ]' \
    --read-attributes '[
        "email",
        "given_name",
        "family_name",
        "phone_number",
        "custom:employee_id",
        "custom:department",
        "custom:approval_status",
        "custom:local_user_id",
        "custom:user_roles"
    ]' \
    --profile $AWS_PROFILE \
    --region $AWS_REGION 2>&1)

CLIENT_CREATE_RESULT=$?

if [ $CLIENT_CREATE_RESULT -eq 0 ]; then
    CLIENT_ID=$(echo "$CLIENT_CREATION_OUTPUT" | jq -r '.UserPoolClient.ClientId' 2>/dev/null)
    if [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "null" ]; then
        echo -e "${GREEN}✅ User Pool Client created successfully with ID: $CLIENT_ID${NC}"
    else
        echo -e "${RED}❌ Could not extract Client ID from response${NC}"
        echo "Response: $CLIENT_CREATION_OUTPUT"

        echo -e "${YELLOW}🧹 Cleaning up user pool due to client creation failure...${NC}"
        aws cognito-idp delete-user-pool \
            --user-pool-id "$USER_POOL_ID" \
            --profile $AWS_PROFILE \
            --region $AWS_REGION >/dev/null 2>&1
        exit 1
    fi
else
    echo -e "${RED}❌ Failed to create user pool client${NC}"
    echo "Error output:"
    echo "$CLIENT_CREATION_OUTPUT"

    echo -e "${YELLOW}🧹 Cleaning up user pool due to client creation failure...${NC}"
    aws cognito-idp delete-user-pool \
        --user-pool-id "$USER_POOL_ID" \
        --profile $AWS_PROFILE \
        --region $AWS_REGION >/dev/null 2>&1
    exit 1
fi

# Verify user pool configuration
echo -e "${BLUE}Verifying user pool configuration...${NC}"
VERIFICATION_OUTPUT=$(aws cognito-idp describe-user-pool \
    --user-pool-id "$USER_POOL_ID" \
    --profile $AWS_PROFILE \
    --region $AWS_REGION 2>/dev/null)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ User pool verification successful${NC}"

    # Check for custom attributes
    CUSTOM_ATTRS=$(echo "$VERIFICATION_OUTPUT" | jq -r '.UserPool.Schema[] | select(.Name | startswith("custom:")) | .Name' 2>/dev/null)
    if [ -n "$CUSTOM_ATTRS" ]; then
        echo -e "${GREEN}✅ Custom attributes found:${NC}"
        echo "$CUSTOM_ATTRS" | sed 's/^/  • /'
    else
        echo -e "${YELLOW}⚠️  No custom attributes detected in verification${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Could not verify user pool configuration (but creation was successful)${NC}"
fi

# Output results
echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}🎉 Cognito Setup Completed Successfully!${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
echo -e "${BLUE}📋 Environment Variables (copy and run these):${NC}"
echo ""
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"$AWS_REGION\""
echo "export AWS_PROFILE=\"$AWS_PROFILE\""
echo ""

echo -e "${BLUE}🏷️  Custom Attributes Created in Schema:${NC}"
echo "• custom:employee_id - Employee identification"
echo "• custom:department - User department"
echo "• custom:approval_status - Sync with local DB user status"
echo "• custom:local_user_id - Reference to local database user ID"
echo "• custom:user_roles - Comma-separated list of user roles"
echo ""

echo -e "${BLUE}⏰ Token Configuration:${NC}"
echo "• Access Token: 30 minutes"
echo "• ID Token: 30 minutes"
echo "• Refresh Token: 120 minutes (2 hours)"
echo ""

echo -e "${BLUE}🔐 Authentication Flows Enabled:${NC}"
echo "• ALLOW_USER_PASSWORD_AUTH (username/password login)"
echo "• ALLOW_REFRESH_TOKEN_AUTH (token refresh)"
echo "• ALLOW_USER_SRP_AUTH (secure remote password)"
echo ""

echo -e "${BLUE}🔧 Next Steps:${NC}"
echo "1. Copy and run the environment variables above in your terminal"
echo "2. Add them to your shell profile (.bashrc/.zshrc) for persistence"
echo "3. Update your application-dev.yml with the User Pool ID and Client ID"
echo "4. Start your Spring Boot application: ./setup-dev-env.sh"
echo "5. Test user registration: http://localhost:8080/wireframes/register"
echo "6. Check AWS Cognito Console to verify users are created"
echo ""

echo -e "${BLUE}🧪 Testing Commands:${NC}"
echo "# Test user pool exists"
echo "aws cognito-idp describe-user-pool --user-pool-id $USER_POOL_ID --profile $AWS_PROFILE"
echo ""
echo "# Test client exists"
echo "aws cognito-idp describe-user-pool-client --user-pool-id $USER_POOL_ID --client-id $CLIENT_ID --profile $AWS_PROFILE"
echo ""
echo "# List users (after some registrations)"
echo "aws cognito-idp list-users --user-pool-id $USER_POOL_ID --profile $AWS_PROFILE"
echo ""

echo -e "${GREEN}Setup completed successfully! 🚀${NC}"