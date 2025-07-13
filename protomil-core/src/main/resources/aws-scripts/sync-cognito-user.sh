#!/bin/bash

# Cognito User Sync Script
echo "=== Syncing User with Cognito ==="

# Configuration
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1
export AWS_COGNITO_DEV_USER_POOL_ID="ap-south-1_vFaRNroNg"
export AWS_COGNITO_DEV_CLIENT_ID="2bsn4p4i1s6bcm8m1r5vvcpb55"
USER_EMAIL="dev.namansharma89@gmail.com"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if environment variables are set
if [ -z "$AWS_COGNITO_DEV_USER_POOL_ID" ] || [ -z "$AWS_COGNITO_DEV_CLIENT_ID" ]; then
    echo -e "${RED}❌ Cognito environment variables not set${NC}"
    echo "Please set:"
    echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"your-user-pool-id\""
    echo "export AWS_COGNITO_DEV_CLIENT_ID=\"your-client-id\""
    exit 1
fi

echo -e "${BLUE}User Pool ID: $AWS_COGNITO_DEV_USER_POOL_ID${NC}"
echo -e "${BLUE}Client ID: $AWS_COGNITO_DEV_CLIENT_ID${NC}"
echo -e "${BLUE}User Email: $USER_EMAIL${NC}"
echo ""

# Function to execute AWS command with error handling
execute_aws_command() {
    local description="$1"
    shift
    local command=("$@")

    echo -e "${BLUE}$description...${NC}"

    if "${command[@]}" 2>/dev/null; then
        echo -e "${GREEN}✅ $description completed successfully${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  $description failed or not needed${NC}"
        return 1
    fi
}

# Step 1: Check current user status
echo -e "${BLUE}=== Step 1: Checking current user status in Cognito ===${NC}"
aws cognito-idp admin-get-user \
    --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
    --username="$USER_EMAIL" \
    --profile="$AWS_PROFILE" \
    --region="$AWS_REGION" 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ User not found in Cognito${NC}"
    echo -e "${YELLOW}The user might not be registered in Cognito yet${NC}"
    exit 1
fi

echo ""

# Step 2: Get local user ID from database
echo -e "${BLUE}=== Step 2: Getting local user ID ===${NC}"
LOCAL_USER_ID=$(docker exec -i postgres psql -U protomil_user -d protomil_db -t -c "SELECT id FROM users WHERE email = '$USER_EMAIL';" | tr -d ' ' | head -1)

if [ -n "$LOCAL_USER_ID" ]; then
    echo -e "${GREEN}✅ Local User ID: $LOCAL_USER_ID${NC}"
else
    echo -e "${RED}❌ Could not get local user ID${NC}"
    LOCAL_USER_ID=""
fi

echo ""

# Step 3: Update user attributes
echo -e "${BLUE}=== Step 3: Updating user attributes in Cognito ===${NC}"
ATTRIBUTES=(
    "Name=custom:approval_status,Value=ACTIVE"
    "Name=custom:user_roles,Value=SUPER_ADMIN"
)

if [ -n "$LOCAL_USER_ID" ]; then
    ATTRIBUTES+=("Name=custom:local_user_id,Value=$LOCAL_USER_ID")
fi

execute_aws_command "Update user attributes" \
    aws cognito-idp admin-update-user-attributes \
    --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
    --username="$USER_EMAIL" \
    --user-attributes "${ATTRIBUTES[@]}" \
    --profile="$AWS_PROFILE" \
    --region="$AWS_REGION"

echo ""

# Step 4: Enable user
echo -e "${BLUE}=== Step 4: Enabling user in Cognito ===${NC}"
execute_aws_command "Enable user" \
    aws cognito-idp admin-enable-user \
    --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
    --username="$USER_EMAIL" \
    --profile="$AWS_PROFILE" \
    --region="$AWS_REGION"

echo ""

# Step 5: Confirm user email
echo -e "${BLUE}=== Step 5: Confirming user email ===${NC}"
execute_aws_command "Confirm user signup" \
    aws cognito-idp admin-confirm-sign-up \
    --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
    --username="$USER_EMAIL" \
    --profile="$AWS_PROFILE" \
    --region="$AWS_REGION"

echo ""

# Step 6: Verify final status
echo -e "${BLUE}=== Step 6: Verifying final status ===${NC}"
echo -e "${BLUE}Final user status in Cognito:${NC}"
aws cognito-idp admin-get-user \
    --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
    --username="$USER_EMAIL" \
    --profile="$AWS_PROFILE" \
    --region="$AWS_REGION" \
    --output table

echo ""
echo -e "${GREEN}🎉 Cognito sync completed!${NC}"
echo -e "${BLUE}Next steps:${NC}"
echo "1. Try logging in to your application"
echo "2. Check the application logs for any authentication issues"
echo "3. Verify the user has SUPER_ADMIN access to all features"