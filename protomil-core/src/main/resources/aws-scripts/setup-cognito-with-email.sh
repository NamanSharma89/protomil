#!/bin/bash

echo "Setting up Cognito Dev User Pool with Email Verification and Logging..."

# Set AWS profile and region
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

# Colors for output
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

# Function to check if command succeeded and provide appropriate feedback
check_result() {
    local operation="$1"
    local skip_message="$2"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $operation successful${NC}"
        return 0
    else
        if [ -n "$skip_message" ]; then
            echo -e "${YELLOW}⚠️  $operation - $skip_message${NC}"
            return 0
        else
            echo -e "${RED}❌ $operation failed${NC}"
            return 1
        fi
    fi
}

# Function to check if resource exists
resource_exists() {
    local resource_type="$1"
    local resource_name="$2"

    case $resource_type in
        "user-pool")
            aws cognito-idp list-user-pools --max-items 60 --profile $AWS_PROFILE --region $AWS_REGION --query "UserPools[?Name=='$resource_name'].Id" --output text 2>/dev/null | grep -q "."
            ;;
        "s3-bucket")
            aws s3api head-bucket --bucket "$resource_name" --profile $AWS_PROFILE --region $AWS_REGION >/dev/null 2>&1
            ;;
        "cloudtrail")
            aws cloudtrail describe-trails --trail-name-list "$resource_name" --profile $AWS_PROFILE --region $AWS_REGION --query "trailList[0].Name" --output text 2>/dev/null | grep -q "$resource_name"
            ;;
        "log-group")
            aws logs describe-log-groups --log-group-name-prefix "$resource_name" --profile $AWS_PROFILE --region $AWS_REGION --query "logGroups[?logGroupName=='$resource_name'].logGroupName" --output text 2>/dev/null | grep -q "."
            ;;
        "lambda-function")
            aws lambda get-function --function-name "$resource_name" --profile $AWS_PROFILE --region $AWS_REGION >/dev/null 2>&1
            ;;
        "iam-role")
            aws iam get-role --role-name "$resource_name" --profile $AWS_PROFILE >/dev/null 2>&1
            ;;
    esac
}

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Step 1: Creating/Updating Cognito User Pool${NC}"
echo -e "${BLUE}=================================================${NC}"

USER_POOL_NAME="protomil-dev-user-pool"
USER_POOL_ID=""

# Check if user pool already exists
if resource_exists "user-pool" "$USER_POOL_NAME"; then
    USER_POOL_ID=$(aws cognito-idp list-user-pools --max-items 60 --profile $AWS_PROFILE --region $AWS_REGION --query "UserPools[?Name=='$USER_POOL_NAME'].Id" --output text 2>/dev/null)
    echo -e "${YELLOW}⚠️  User Pool '$USER_POOL_NAME' already exists with ID: $USER_POOL_ID${NC}"

    # Update existing user pool settings
    echo "Updating user pool configuration..."
    aws cognito-idp update-user-pool \
      --user-pool-id "$USER_POOL_ID" \
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
      }' \
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    check_result "User Pool update" "User pool might be already properly configured"
else
    echo "Creating new user pool..."
    USER_POOL_RESULT=$(aws cognito-idp create-user-pool \
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
      --mfa-configuration "OFF" \
      --account-recovery-setting '{
        "RecoveryMechanisms": [
          {
            "Priority": 1,
            "Name": "verified_email"
          }
        ]
      }' \
      --admin-create-user-config '{
        "AllowAdminCreateUserOnly": false,
        "InviteMessageTemplate": {
          "EmailSubject": "Welcome to Protomil",
          "EmailMessage": "Your username is {username} and temporary password is {####}"
        }
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
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null)

    if [ $? -eq 0 ]; then
        USER_POOL_ID=$(echo $USER_POOL_RESULT | jq -r '.UserPool.Id')
        echo -e "${GREEN}✅ User Pool created with ID: $USER_POOL_ID${NC}"
    else
        echo -e "${RED}❌ Failed to create user pool${NC}"
        exit 1
    fi
fi

USER_POOL_ARN="arn:aws:cognito-idp:$AWS_REGION:$ACCOUNT_ID:userpool/$USER_POOL_ID"

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Step 2: Creating/Updating User Pool Client${NC}"
echo -e "${BLUE}=================================================${NC}"

CLIENT_NAME="protomil-dev-client"
CLIENT_ID=""

# Check if client already exists
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
    echo "Updating user pool client configuration..."
    aws cognito-idp update-user-pool-client \
      --user-pool-id "$USER_POOL_ID" \
      --client-id "$CLIENT_ID" \
      --client-name "$CLIENT_NAME" \
      --explicit-auth-flows ALLOW_ADMIN_USER_PASSWORD_AUTH ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH ALLOW_USER_SRP_AUTH \
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
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    check_result "User Pool Client update" "Client might be already properly configured"
else
    echo "Creating new user pool client..."
    CLIENT_RESULT=$(aws cognito-idp create-user-pool-client \
      --user-pool-id "$USER_POOL_ID" \
      --client-name "$CLIENT_NAME" \
      --explicit-auth-flows ALLOW_ADMIN_USER_PASSWORD_AUTH ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH ALLOW_USER_SRP_AUTH \
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
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null)

    if [ $? -eq 0 ]; then
        CLIENT_ID=$(echo $CLIENT_RESULT | jq -r '.UserPoolClient.ClientId')
        echo -e "${GREEN}✅ User Pool Client created with ID: $CLIENT_ID${NC}"
    else
        echo -e "${RED}❌ Failed to create user pool client${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Step 3: Setting up CloudWatch Log Groups${NC}"
echo -e "${BLUE}=================================================${NC}"

# Create CloudWatch log group for application logs
APP_LOG_GROUP="/aws/cognito/protomil-dev"
echo "Checking application log group: $APP_LOG_GROUP"

if resource_exists "log-group" "$APP_LOG_GROUP"; then
    echo -e "${YELLOW}⚠️  Log group '$APP_LOG_GROUP' already exists${NC}"
else
    aws logs create-log-group \
      --log-group-name "$APP_LOG_GROUP" \
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Application log group created${NC}"

        # Set retention policy
        aws logs put-retention-policy \
          --log-group-name "$APP_LOG_GROUP" \
          --retention-in-days 14 \
          --profile $AWS_PROFILE \
          --region $AWS_REGION >/dev/null 2>&1

        check_result "Log retention policy set to 14 days"
    else
        echo -e "${RED}❌ Failed to create application log group${NC}"
    fi
fi

# Create CloudWatch log group for Cognito Lambda triggers
COGNITO_LOG_GROUP="/aws/lambda/cognito-triggers"
echo "Checking Cognito triggers log group: $COGNITO_LOG_GROUP"

if resource_exists "log-group" "$COGNITO_LOG_GROUP"; then
    echo -e "${YELLOW}⚠️  Log group '$COGNITO_LOG_GROUP' already exists${NC}"
else
    aws logs create-log-group \
      --log-group-name "$COGNITO_LOG_GROUP" \
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null

    check_result "Cognito triggers log group creation" "Log group might already exist"
fi

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Step 4: Setting up CloudTrail${NC}"
echo -e "${BLUE}=================================================${NC}"

TRAIL_NAME="protomil-dev-trail"
S3_BUCKET="protomil-dev-cloudtrail-$ACCOUNT_ID"

# Check if trail already exists
if resource_exists "cloudtrail" "$TRAIL_NAME"; then
    echo -e "${YELLOW}⚠️  CloudTrail '$TRAIL_NAME' already exists${NC}"

    # Ensure the existing trail is logging
    aws cloudtrail start-logging \
      --name "$TRAIL_NAME" \
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    check_result "CloudTrail logging ensured" "Trail might already be logging"
else
    echo "Creating CloudTrail trail: $TRAIL_NAME"

    # Check if S3 bucket exists
    if resource_exists "s3-bucket" "$S3_BUCKET"; then
        echo -e "${YELLOW}⚠️  S3 bucket '$S3_BUCKET' already exists${NC}"
    else
        echo "Creating S3 bucket: $S3_BUCKET"
        aws s3 mb s3://$S3_BUCKET \
          --profile $AWS_PROFILE \
          --region $AWS_REGION >/dev/null 2>&1

        check_result "S3 bucket creation"
    fi

    # Create/Update bucket policy for CloudTrail
    echo "Setting up S3 bucket policy..."
    BUCKET_POLICY=$(cat <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AWSCloudTrailAclCheck",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudtrail.amazonaws.com"
            },
            "Action": "s3:GetBucketAcl",
            "Resource": "arn:aws:s3:::$S3_BUCKET",
            "Condition": {
                "StringEquals": {
                    "AWS:SourceArn": "arn:aws:cloudtrail:$AWS_REGION:$ACCOUNT_ID:trail/$TRAIL_NAME"
                }
            }
        },
        {
            "Sid": "AWSCloudTrailWrite",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudtrail.amazonaws.com"
            },
            "Action": "s3:PutObject",
            "Resource": "arn:aws:s3:::$S3_BUCKET/AWSLogs/$ACCOUNT_ID/*",
            "Condition": {
                "StringEquals": {
                    "s3:x-amz-acl": "bucket-owner-full-control",
                    "AWS:SourceArn": "arn:aws:cloudtrail:$AWS_REGION:$ACCOUNT_ID:trail/$TRAIL_NAME"
                }
            }
        }
    ]
}
EOF
)

    echo "$BUCKET_POLICY" > /tmp/cloudtrail-bucket-policy.json

    aws s3api put-bucket-policy \
      --bucket $S3_BUCKET \
      --policy file:///tmp/cloudtrail-bucket-policy.json \
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    check_result "S3 bucket policy setup" "Bucket policy might already be configured"

    # Create CloudTrail
    aws cloudtrail create-trail \
      --name "$TRAIL_NAME" \
      --s3-bucket-name "$S3_BUCKET" \
      --include-global-service-events \
      --is-multi-region-trail \
      --enable-log-file-validation \
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ CloudTrail created${NC}"

        # Start logging
        aws cloudtrail start-logging \
          --name "$TRAIL_NAME" \
          --profile $AWS_PROFILE \
          --region $AWS_REGION >/dev/null 2>&1

        check_result "CloudTrail logging started"
    else
        echo -e "${YELLOW}⚠️  CloudTrail creation failed - might already exist${NC}"
    fi

    # Clean up temp files
    rm -f /tmp/cloudtrail-bucket-policy.json
fi

echo ""
echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Step 5: Setting up Lambda Function (Optional)${NC}"
echo -e "${BLUE}=================================================${NC}"

LAMBDA_FUNCTION_NAME="cognito-logger"
LAMBDA_ROLE_NAME="cognito-logger-role"

# Check if Lambda function exists
if resource_exists "lambda-function" "$LAMBDA_FUNCTION_NAME"; then
    echo -e "${YELLOW}⚠️  Lambda function '$LAMBDA_FUNCTION_NAME' already exists${NC}"
else
    echo "Creating Lambda function for Cognito logging..."

    # Check if IAM role exists
    if resource_exists "iam-role" "$LAMBDA_ROLE_NAME"; then
        echo -e "${YELLOW}⚠️  IAM role '$LAMBDA_ROLE_NAME' already exists${NC}"
    else
        # Create IAM role for Lambda
        LAMBDA_TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
)

        echo "$LAMBDA_TRUST_POLICY" > /tmp/lambda-trust-policy.json

        aws iam create-role \
          --role-name "$LAMBDA_ROLE_NAME" \
          --assume-role-policy-document file:///tmp/lambda-trust-policy.json \
          --profile $AWS_PROFILE >/dev/null 2>&1

        check_result "IAM role creation for Lambda" "Role might already exist"

        # Attach basic execution role
        aws iam attach-role-policy \
          --role-name "$LAMBDA_ROLE_NAME" \
          --policy-arn "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole" \
          --profile $AWS_PROFILE >/dev/null 2>&1

        check_result "IAM policy attachment" "Policy might already be attached"
    fi

    # Create Lambda function code
    LAMBDA_CODE=$(cat <<'EOF'
import json
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    logger.info(f"Cognito Event: {json.dumps(event, indent=2)}")

    trigger_source = event.get('triggerSource', 'Unknown')
    user_name = event.get('userName', 'Unknown')
    user_pool_id = event.get('userPoolId', 'Unknown')

    logger.info(f"Trigger: {trigger_source}, User: {user_name}, Pool: {user_pool_id}")

    return event
EOF
)

    echo "$LAMBDA_CODE" > /tmp/lambda_function.py
    cd /tmp && zip lambda-function.zip lambda_function.py >/dev/null 2>&1

    LAMBDA_ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/$LAMBDA_ROLE_NAME"

    # Wait for role to be available
    sleep 5

    aws lambda create-function \
      --function-name "$LAMBDA_FUNCTION_NAME" \
      --runtime "python3.9" \
      --role "$LAMBDA_ROLE_ARN" \
      --handler "lambda_function.lambda_handler" \
      --zip-file "fileb:///tmp/lambda-function.zip" \
      --description "Cognito events logger for Protomil" \
      --timeout 30 \
      --profile $AWS_PROFILE \
      --region $AWS_REGION >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Lambda function created${NC}"

        # Add permission for Cognito to invoke Lambda
        aws lambda add-permission \
          --function-name "$LAMBDA_FUNCTION_NAME" \
          --statement-id "cognito-trigger-permission" \
          --action "lambda:InvokeFunction" \
          --principal "cognito-idp.amazonaws.com" \
          --source-arn "$USER_POOL_ARN" \
          --profile $AWS_PROFILE \
          --region $AWS_REGION >/dev/null 2>&1

        check_result "Lambda permissions configured" "Permissions might already be set"
    else
        echo -e "${YELLOW}⚠️  Lambda function creation failed - might already exist${NC}"
    fi

    # Clean up
    rm -f /tmp/lambda_function.py /tmp/lambda-function.zip /tmp/lambda-trust-policy.json
fi

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
echo -e "${BLUE}📋 Environment Variables:${NC}"
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"$AWS_REGION\""
echo "export AWS_PROFILE=\"$AWS_PROFILE\""
echo ""
echo -e "${BLUE}📊 Resources Created/Updated:${NC}"
echo "• User Pool ID: $USER_POOL_ID"
echo "• Client ID: $CLIENT_ID"
echo "• CloudWatch Log Group: $APP_LOG_GROUP"
echo "• CloudTrail: $TRAIL_NAME"
echo "• S3 Bucket: $S3_BUCKET"
echo "• Lambda Function: $LAMBDA_FUNCTION_NAME"
echo ""
echo -e "${BLUE}🔍 Useful Commands:${NC}"
echo ""
echo "# Test user pool configuration"
echo "aws cognito-idp describe-user-pool --user-pool-id $USER_POOL_ID --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "# Check CloudWatch logs"
echo "aws logs filter-log-events --log-group-name '$APP_LOG_GROUP' --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "# Check CloudTrail events"
echo "aws cloudtrail lookup-events --lookup-attributes AttributeKey=EventSource,AttributeValue=cognito-idp.amazonaws.com --start-time \$(date -v-1H -u +%Y-%m-%dT%H:%M:%S) --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo -e "${BLUE}🎯 Next Steps:${NC}"
echo "1. Update your application.yml with the User Pool ID and Client ID"
echo "2. Test user registration with a real email address"
echo "3. Check spam folder for verification emails"
echo "4. Monitor CloudWatch logs for debugging"
echo ""
echo -e "${GREEN}=================================================${NC}"