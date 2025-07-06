#!/bin/bash
# tests/api/test-user-registration.sh

# Load dependencies
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/utils/test-helpers.sh"
source "$SCRIPT_DIR/utils/assertions.sh"

# Test data directory
DATA_DIR="$SCRIPT_DIR/data"

# Default configuration
ENABLE_HEALTH_CHECK=true

# Display usage information
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    --skip-health-check     Skip API health check before running tests
    --help                  Show this help message

Examples:
    $0                      # Run with health check (default)
    $0 --skip-health-check  # Run without health check

EOF
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-health-check)
                ENABLE_HEALTH_CHECK=false
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

main() {
    # Parse command line arguments
    parse_arguments "$@"

    log_info "Starting User Registration API Test Suite"
    log_info "Base URL: $BASE_URL"
    log_info "Health Check Enabled: $ENABLE_HEALTH_CHECK"

    # Wait for API to be ready
    if ! wait_for_api; then
        log_error "API is not ready. Exiting."
        exit 1
    fi

    # Health check (only if enabled)
    if [[ "$ENABLE_HEALTH_CHECK" == "true" ]]; then
        if ! check_api_health; then
            log_error "API health check failed. Exiting."
            exit 1
        fi
    else
        log_info "Skipping API health check"
    fi

    # Run test scenarios
    test_valid_user_registration
    test_duplicate_user_registration
    test_invalid_email_formats
    test_weak_passwords
    test_missing_required_fields
    test_invalid_phone_numbers
    test_long_field_values
    test_special_characters
    test_wireframe_endpoints

    # Print summary and exit
    print_test_summary
    exit $?
}

test_valid_user_registration() {
    log_info "=== Testing Valid User Registration ==="

    # Create dynamic test data with unique email
    local unique_email=$(generate_unique_email)
    local temp_file="$TEST_RESULTS_DIR/temp_valid_user_$TIMESTAMP.json"

    cat > "$temp_file" << EOF
{
  "email": "$unique_email",
  "firstName": "John",
  "lastName": "Doe",
  "password": "SecurePass123!",
  "phoneNumber": "+919876543210",
  "employeeId": "EMP$(date +%s)",
  "department": "Manufacturing"
}
EOF

    run_test "Valid User Registration" "201" "/api/v1/users/register" "POST" "$temp_file" "validate_user_registration_response"

    # Clean up
    rm -f "$temp_file"
}

test_duplicate_user_registration() {
    log_info "=== Testing Duplicate User Registration ==="

    local duplicate_email="duplicate@protomil.com"
    local temp_file="$TEST_RESULTS_DIR/temp_duplicate_user_$TIMESTAMP.json"

    # First registration
    cat > "$temp_file" << EOF
{
  "email": "$duplicate_email",
  "firstName": "First",
  "lastName": "User",
  "password": "SecurePass123!",
  "phoneNumber": "+919876543211"
}
EOF

    run_test "First Registration" "201" "/api/v1/users/register" "POST" "$temp_file" "validate_user_registration_response"

    # Duplicate registration
    run_test "Duplicate Email Registration" "400" "/api/v1/users/register" "POST" "$temp_file" "validate_error_response"

    rm -f "$temp_file"
}

test_invalid_email_formats() {
    log_info "=== Testing Invalid Email Formats ==="

    local invalid_emails=("invalid-email" "test@" "@domain.com" "test.domain.com" "test..test@domain.com")

    for email in "${invalid_emails[@]}"; do
        local temp_file="$TEST_RESULTS_DIR/temp_invalid_email_$TIMESTAMP.json"

        cat > "$temp_file" << EOF
{
  "email": "$email",
  "firstName": "Test",
  "lastName": "User",
  "password": "SecurePass123!",
  "phoneNumber": "+919876543212"
}
EOF

        run_test "Invalid Email: $email" "400" "/api/v1/users/register" "POST" "$temp_file" "validate_validation_error_response"
        rm -f "$temp_file"
    done
}

test_weak_passwords() {
    log_info "=== Testing Weak Passwords ==="

    local weak_passwords=("weak" "12345678" "password" "PASSWORD" "Password" "Pass123")

    for password in "${weak_passwords[@]}"; do
        local temp_file="$TEST_RESULTS_DIR/temp_weak_password_$TIMESTAMP.json"

        cat > "$temp_file" << EOF
{
  "email": "weakpass$(date +%s)@protomil.com",
  "firstName": "Test",
  "lastName": "User",
  "password": "$password",
  "phoneNumber": "+919876543213"
}
EOF

        run_test "Weak Password: $password" "400" "/api/v1/users/register" "POST" "$temp_file" "validate_validation_error_response"
        rm -f "$temp_file"
    done
}

test_missing_required_fields() {
    log_info "=== Testing Missing Required Fields ==="

    # Missing email
    run_test "Missing Email" "400" "/api/v1/users/register" "POST" "$DATA_DIR/invalid-users/missing-email.json" "validate_validation_error_response"

    # Missing required fields
    run_test "Missing Required Fields" "400" "/api/v1/users/register" "POST" "$DATA_DIR/invalid-users/missing-required-fields.json" "validate_validation_error_response"
}

test_invalid_phone_numbers() {
    log_info "=== Testing Invalid Phone Numbers ==="

    local invalid_phones=("123" "invalid-phone" "1234567890123456789" "+91abcd" "")

    for phone in "${invalid_phones[@]}"; do
        local temp_file="$TEST_RESULTS_DIR/temp_invalid_phone_$TIMESTAMP.json"

        cat > "$temp_file" << EOF
{
  "email": "phone$(date +%s)@protomil.com",
  "firstName": "Test",
  "lastName": "User",
  "password": "SecurePass123!",
  "phoneNumber": "$phone"
}
EOF

        run_test "Invalid Phone: $phone" "400" "/api/v1/users/register" "POST" "$temp_file" "validate_validation_error_response"
        rm -f "$temp_file"
    done
}

test_long_field_values() {
    log_info "=== Testing Long Field Values ==="

    local long_string=$(printf 'a%.0s' {1..256})
    local temp_file="$TEST_RESULTS_DIR/temp_long_fields_$TIMESTAMP.json"

    cat > "$temp_file" << EOF
{
  "email": "long$(date +%s)@protomil.com",
  "firstName": "$long_string",
  "lastName": "$long_string",
  "password": "SecurePass123!",
  "phoneNumber": "+919876543214"
}
EOF

    run_test "Long Field Values" "400" "/api/v1/users/register" "POST" "$temp_file" "validate_validation_error_response"
    rm -f "$temp_file"
}

test_special_characters() {
    log_info "=== Testing Special Characters ==="

    local temp_file="$TEST_RESULTS_DIR/temp_special_chars_$TIMESTAMP.json"

    cat > "$temp_file" << EOF
{
  "email": "special$(date +%s)@protomil.com",
  "firstName": "João",
  "lastName": "José",
  "password": "SecurePass123!",
  "phoneNumber": "+919876543215",
  "employeeId": "EMP-001",
  "department": "R&D"
}
EOF

    run_test "Special Characters" "201" "/api/v1/users/register" "POST" "$temp_file" "validate_user_registration_response"
    rm -f "$temp_file"
}

test_wireframe_endpoints() {
    log_info "=== Testing Wireframe Endpoints ==="

    run_test "Wireframes Index" "200" "/wireframes/" "GET"
    run_test "Registration Form" "200" "/wireframes/register" "GET"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi