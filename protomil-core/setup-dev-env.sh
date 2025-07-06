#!/bin/bash
# scripts/setup-dev-env.sh

export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1
export AWS_COGNITO_DEV_USER_POOL_ID=ap-south-1_yyVv7lCsX
export AWS_COGNITO_DEV_CLIENT_ID=livghma6otpqkfpe7oma4v9vu

echo "Development environment variables set for protomil-dev profile"
echo "AWS Profile: $AWS_PROFILE"
echo "AWS Region: $AWS_REGION"
echo "Cognito User Pool: $AWS_COGNITO_DEV_USER_POOL_ID"

# Run the application
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev