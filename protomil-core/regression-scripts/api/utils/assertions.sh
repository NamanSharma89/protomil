# tests/api/utils/assertions.sh
#!/bin/bash

# JSON assertion functions
assert_json_field() {
    local response_file="$1"
    local field_path="$2"
    local expected_value="$3"
    local test_description="$4"

    local actual_value=$(jq -r "$field_path" "$response_file" 2>/dev/null)

    if [ "$actual_value" = "$expected_value" ]; then
        log_success "$test_description - $field_path: $actual_value"
    else
        log_error "$test_description - Expected $field_path: $expected_value, Got: $actual_value"
    fi
}

assert_json_field_exists() {
    local response_file="$1"
    local field_path="$2"
    local test_description="$3"

    local field_value=$(jq -r "$field_path" "$response_file" 2>/dev/null)

    if [ "$field_value" != "null" ] && [ -n "$field_value" ]; then
        log_success "$test_description - Field $field_path exists: $field_value"
    else
        log_error "$test_description - Field $field_path does not exist or is null"
    fi
}

assert_json_field_not_null() {
    local response_file="$1"
    local field_path="$2"
    local test_description="$3"

    local field_value=$(jq -r "$field_path" "$response_file" 2>/dev/null)

    if [ "$field_value" != "null" ] && [ "$field_value" != "" ]; then
        log_success "$test_description - Field $field_path is not null: $field_value"
    else
        log_error "$test_description - Field $field_path is null or empty"
    fi
}

assert_json_array_length() {
    local response_file="$1"
    local array_path="$2"
    local expected_length="$3"
    local test_description="$4"

    local actual_length=$(jq -r "$array_path | length" "$response_file" 2>/dev/null)

    if [ "$actual_length" = "$expected_length" ]; then
        log_success "$test_description - Array length: $actual_length"
    else
        log_error "$test_description - Expected array length: $expected_length, Got: $actual_length"
    fi
}

assert_contains_text() {
    local response_file="$1"
    local expected_text="$2"
    local test_description="$3"

    if grep -q "$expected_text" "$response_file"; then
        log_success "$test_description - Contains text: $expected_text"
    else
        log_error "$test_description - Does not contain text: $expected_text"
    fi
}

# Validation assertions for user registration
validate_user_registration_response() {
    local response_file="$1"

    assert_json_field "$response_file" ".success" "true" "Registration Response Structure"
    assert_json_field_exists "$response_file" ".data.userId" "Registration Response Data"
    assert_json_field_exists "$response_file" ".data.email" "Registration Response Data"
    assert_json_field_exists "$response_file" ".data.status" "Registration Response Data"
    assert_json_field_exists "$response_file" ".data.registeredAt" "Registration Response Data"
    assert_json_field_not_null "$response_file" ".timestamp" "Registration Response Metadata"
}

validate_error_response() {
    local response_file="$1"

    assert_json_field "$response_file" ".success" "false" "Error Response Structure"
    assert_json_field_exists "$response_file" ".error.errorCode" "Error Response Details"
    assert_json_field_exists "$response_file" ".error.message" "Error Response Details"
    assert_json_field_not_null "$response_file" ".timestamp" "Error Response Metadata"
}

validate_validation_error_response() {
    local response_file="$1"

    validate_error_response "$response_file"
    assert_json_field "$response_file" ".error.errorCode" "VALIDATION_ERROR" "Validation Error Code"
    assert_json_field_exists "$response_file" ".error.fieldErrors" "Validation Field Errors"
}