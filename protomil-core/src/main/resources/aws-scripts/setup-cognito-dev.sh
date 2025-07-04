#!/bin/bash

# setup-cognito-dev.sh
echo "Setting up Cognito Dev User Pool with nash-cli profile..."

# Create user pool
echo "Creating user pool..."
USER_POOL_RESULT=$(aws cognito-idp create-user-pool \
    --pool-name "protomil-dev-users" \
    --policies "PasswordPolicy={MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true,RequireSymbols=true}" \
    --auto-verified-attributes email \
    --username-attributes email \
    --region ap-south-1 \
    --profile nash-cli-1)

USER_POOL_ID=$(echo $USER_POOL_RESULT | jq -r '.UserPool.Id')
echo "User Pool created with ID: $USER_POOL_ID"

# Create user pool client
echo "Creating user pool client..."
CLIENT_RESULT=$(aws cognito-idp create-user-pool-client \
    --user-pool-id "$USER_POOL_ID" \
    --client-name "protomil-dev-client" \
    --no-generate-secret \
    --region ap-south-1 \
    --profile nash-cli-1)

CLIENT_ID=$(echo $CLIENT_RESULT | jq -r '.UserPoolClient.ClientId')
echo "Client created with ID: $CLIENT_ID"

# Output environment variables
echo ""
echo "Add these environment variables to your setup:"
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"ap-south-1\""
echo "export AWS_PROFILE=\"nash-cli-1\""