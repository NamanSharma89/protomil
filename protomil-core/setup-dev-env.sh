#!/bin/bash
# scripts/setup-dev-env.sh

export AWS_PROFILE=nash-cli-1
export AWS_REGION=ap-south-1
export AWS_COGNITO_DEV_USER_POOL_ID=ap-south-1_CoqZmMS22
export AWS_COGNITO_DEV_CLIENT_ID=livghma6otpqkfpe7oma4v9vu

echo "Development environment variables set for nash-cli profile"
echo "AWS Profile: $AWS_PROFILE"
echo "AWS Region: $AWS_REGION"
echo "Cognito User Pool: $AWS_COGNITO_DEV_USER_POOL_ID"

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev