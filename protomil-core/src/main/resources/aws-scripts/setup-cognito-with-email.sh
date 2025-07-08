#!/bin/bash

echo "Setting up Cognito Dev User Pool with Email Verification and Logging..."

# Set AWS profile and region
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

# Get AWS Account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --profile $AWS_PROFILE --region $AWS_REGION)
echo "AWS Account ID: $ACCOUNT_ID"

# Function to check if command succeeded
check_result() {
    if [ $? -eq 0 ]; then
        echo "✅ $1 successful"
    else
        echo "❌ $1 failed"
        exit 1
    fi
}

echo ""
echo "================================================="
echo "Step 1: Creating Cognito User Pool"
echo "================================================="

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
  --profile $AWS_PROFILE \
  --region $AWS_REGION)

check_result "User Pool creation"

USER_POOL_ID=$(echo $USER_POOL_RESULT | jq -r '.UserPool.Id')
USER_POOL_ARN="arn:aws:cognito-idp:$AWS_REGION:$ACCOUNT_ID:userpool/$USER_POOL_ID"
echo "User Pool created with ID: $USER_POOL_ID"

echo ""
echo "================================================="
echo "Step 2: Creating User Pool Client"
echo "================================================="

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
  --profile $AWS_PROFILE \
  --region $AWS_REGION)

check_result "User Pool Client creation"

CLIENT_ID=$(echo $CLIENT_RESULT | jq -r '.UserPoolClient.ClientId')
echo "Client created with ID: $CLIENT_ID"

echo ""
echo "================================================="
echo "Step 3: Setting up CloudWatch Log Groups"
echo "================================================="

# Create CloudWatch log group for application logs
APP_LOG_GROUP="/aws/cognito/protomil-dev"
echo "Creating application log group: $APP_LOG_GROUP"

aws logs create-log-group \
  --log-group-name "$APP_LOG_GROUP" \
  --profile $AWS_PROFILE \
  --region $AWS_REGION 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Application log group created"

    # Set retention policy
    aws logs put-retention-policy \
      --log-group-name "$APP_LOG_GROUP" \
      --retention-in-days 14 \
      --profile $AWS_PROFILE \
      --region $AWS_REGION

    echo "✅ Log retention set to 14 days"
else
    echo "⚠️  Application log group may already exist"
fi

# Create CloudWatch log group for Cognito Lambda triggers (if any)
COGNITO_LOG_GROUP="/aws/lambda/cognito-triggers"
echo "Creating Cognito triggers log group: $COGNITO_LOG_GROUP"

aws logs create-log-group \
  --log-group-name "$COGNITO_LOG_GROUP" \
  --profile $AWS_PROFILE \
  --region $AWS_REGION 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Cognito triggers log group created"
else
    echo "⚠️  Cognito triggers log group may already exist"
fi

echo ""
echo "================================================="
echo "Step 4: Setting up CloudTrail (Simplified)"
echo "================================================="

TRAIL_NAME="protomil-dev-trail"
S3_BUCKET="protomil-dev-cloudtrail-$ACCOUNT_ID"

# Check if trail already exists
EXISTING_TRAIL=$(aws cloudtrail describe-trails \
  --trail-name-list "$TRAIL_NAME" \
  --profile $AWS_PROFILE \
  --region $AWS_REGION 2>/dev/null | jq -r '.trailList[0].Name // empty')

if [ -z "$EXISTING_TRAIL" ]; then
    echo "Creating CloudTrail trail: $TRAIL_NAME"

    # Create S3 bucket for CloudTrail logs
    echo "Creating S3 bucket: $S3_BUCKET"

    aws s3 mb s3://$S3_BUCKET \
      --profile $AWS_PROFILE \
      --region $AWS_REGION

    check_result "S3 bucket creation"

    # Create bucket policy for CloudTrail
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
      --region $AWS_REGION

    check_result "S3 bucket policy setup"

    # Create CloudTrail with management events (includes all Cognito operations)
    aws cloudtrail create-trail \
      --name "$TRAIL_NAME" \
      --s3-bucket-name "$S3_BUCKET" \
      --include-global-service-events \
      --is-multi-region-trail \
      --enable-log-file-validation \
      --profile $AWS_PROFILE \
      --region $AWS_REGION

    check_result "CloudTrail creation"

    # Start logging
    aws cloudtrail start-logging \
      --name "$TRAIL_NAME" \
      --profile $AWS_PROFILE \
      --region $AWS_REGION

    check_result "CloudTrail logging start"

    # Clean up temp files
    rm -f /tmp/cloudtrail-bucket-policy.json

    echo "✅ CloudTrail setup complete"
    echo "ℹ️  All Cognito operations will be logged as management events"

else
    echo "⚠️  CloudTrail '$EXISTING_TRAIL' already exists"

    # Ensure the existing trail is logging
    aws cloudtrail start-logging \
      --name "$EXISTING_TRAIL" \
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null

    echo "✅ Ensured existing CloudTrail is logging"
fi

echo ""
echo "================================================="
echo "Step 5: Setting up CloudWatch Metrics and Alarms"
echo "================================================="

# Create CloudWatch alarm for failed sign-ups
aws cloudwatch put-metric-alarm \
  --alarm-name "Protomil-SignUp-Failures" \
  --alarm-description "Alert when Cognito sign-up failures exceed threshold" \
  --metric-name "SignUpThrottles" \
  --namespace "AWS/Cognito" \
  --statistic "Sum" \
  --period 300 \
  --threshold 5 \
  --comparison-operator "GreaterThanThreshold" \
  --dimensions "Name=UserPool,Value=$USER_POOL_ID" \
  --evaluation-periods 1 \
  --alarm-actions "arn:aws:sns:$AWS_REGION:$ACCOUNT_ID:protomil-alerts" \
  --treat-missing-data "notBreaching" \
  --profile $AWS_PROFILE \
  --region $AWS_REGION 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ CloudWatch alarm created"
else
    echo "⚠️  CloudWatch alarm creation failed (SNS topic may not exist)"
fi

echo ""
echo "================================================="
echo "Step 6: Creating Lambda Function for Enhanced Logging"
echo "================================================="

# Create Lambda function for Cognito triggers (optional)
LAMBDA_FUNCTION_NAME="cognito-logger"
LAMBDA_ROLE_NAME="cognito-logger-role"

# Check if Lambda function exists
EXISTING_LAMBDA=$(aws lambda get-function \
  --function-name "$LAMBDA_FUNCTION_NAME" \
  --profile $AWS_PROFILE \
  --region $AWS_REGION 2>/dev/null | jq -r '.Configuration.FunctionName // empty')

if [ -z "$EXISTING_LAMBDA" ]; then
    echo "Creating Lambda function for Cognito logging..."

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
      --profile $AWS_PROFILE 2>/dev/null

    # Attach basic execution role
    aws iam attach-role-policy \
      --role-name "$LAMBDA_ROLE_NAME" \
      --policy-arn "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole" \
      --profile $AWS_PROFILE 2>/dev/null

    # Create Lambda function code
    LAMBDA_CODE=$(cat <<'EOF'
import json
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    logger.info(f"Cognito Event: {json.dumps(event, indent=2)}")

    # Log specific event details
    trigger_source = event.get('triggerSource', 'Unknown')
    user_name = event.get('userName', 'Unknown')
    user_pool_id = event.get('userPoolId', 'Unknown')

    logger.info(f"Trigger: {trigger_source}, User: {user_name}, Pool: {user_pool_id}")

    return event
EOF
)

    echo "$LAMBDA_CODE" > /tmp/lambda_function.py
    cd /tmp && zip lambda-function.zip lambda_function.py

    LAMBDA_ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/$LAMBDA_ROLE_NAME"

    # Wait for role to be available
    sleep 10

    aws lambda create-function \
      --function-name "$LAMBDA_FUNCTION_NAME" \
      --runtime "python3.9" \
      --role "$LAMBDA_ROLE_ARN" \
      --handler "lambda_function.lambda_handler" \
      --zip-file "fileb:///tmp/lambda-function.zip" \
      --description "Cognito events logger for Protomil" \
      --timeout 30 \
      --profile $AWS_PROFILE \
      --region $AWS_REGION 2>/dev/null

    if [ $? -eq 0 ]; then
        echo "✅ Lambda function created"

        # Add permission for Cognito to invoke Lambda
        aws lambda add-permission \
          --function-name "$LAMBDA_FUNCTION_NAME" \
          --statement-id "cognito-trigger-permission" \
          --action "lambda:InvokeFunction" \
          --principal "cognito-idp.amazonaws.com" \
          --source-arn "$USER_POOL_ARN" \
          --profile $AWS_PROFILE \
          --region $AWS_REGION 2>/dev/null

        echo "✅ Lambda permissions configured"
    else
        echo "⚠️  Lambda function creation failed"
    fi

    # Clean up
    rm -f /tmp/lambda_function.py /tmp/lambda-function.zip /tmp/lambda-trust-policy.json /tmp/cloudtrail-bucket-policy.json
else
    echo "⚠️  Lambda function '$EXISTING_LAMBDA' already exists"
fi

echo ""
echo "================================================="
echo "Setup Complete!"
echo "================================================="
echo ""
echo "📋 Environment Variables:"
echo "export AWS_COGNITO_DEV_USER_POOL_ID=\"$USER_POOL_ID\""
echo "export AWS_COGNITO_DEV_CLIENT_ID=\"$CLIENT_ID\""
echo "export AWS_REGION=\"$AWS_REGION\""
echo "export AWS_PROFILE=\"$AWS_PROFILE\""
echo ""
echo "📊 Monitoring Resources:"
echo "• User Pool ID: $USER_POOL_ID"
echo "• CloudWatch Log Group: $APP_LOG_GROUP"
echo "• CloudTrail: $TRAIL_NAME"
echo "• S3 Bucket: $S3_BUCKET"
echo "• Lambda Function: $LAMBDA_FUNCTION_NAME"
echo ""
echo "🔍 Useful Commands:"
echo ""
echo "# Check CloudWatch logs"
echo "aws logs filter-log-events --log-group-name '$APP_LOG_GROUP' --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "# Check CloudTrail events"
echo "aws cloudtrail lookup-events --lookup-attributes AttributeKey=EventSource,AttributeValue=cognito-idp.amazonaws.com --start-time \$(date -v-1H -u +%Y-%m-%dT%H:%M:%S) --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "# Check Cognito metrics"
echo "aws cloudwatch get-metric-statistics --namespace AWS/Cognito --metric-name SignUpSuccesses --dimensions Name=UserPool,Value=$USER_POOL_ID --start-time \$(date -v-1H -u +%Y-%m-%dT%H:%M:%S) --end-time \$(date -u +%Y-%m-%dT%H:%M:%S) --period 300 --statistics Sum --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "# Monitor real-time logs"
echo "aws logs tail '$APP_LOG_GROUP' --follow --profile $AWS_PROFILE --region $AWS_REGION"
echo ""
echo "🎯 Next Steps:"
echo "1. Update your application.yml with the new User Pool ID and Client ID"
echo "2. Test user registration and check logs"
echo "3. Monitor CloudWatch metrics for email delivery"
echo "4. Check spam folder for verification emails"
echo ""
echo "================================================="