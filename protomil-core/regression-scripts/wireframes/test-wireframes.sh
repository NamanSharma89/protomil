#!/bin/bash
# tests/wireframes/test-wireframes.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../api/utils/test-helpers.sh"
source "$SCRIPT_DIR/../api/utils/assertions.sh"

test_wireframe_pages() {
    log_info "=== Testing Wireframe Pages ==="

    # Test wireframes index
    run_test "Wireframes Index Page" "200" "/wireframes/" "GET" "" "check_html_content"

    # Test registration form
    run_test "Registration Form Page" "200" "/wireframes/register" "GET" "" "check_registration_form"

    # Test email validation endpoint
    run_test "Email Validation Endpoint" "200" "/wireframes/validate-email" "POST" "" "check_validation_response"
}

check_html_content() {
    local response_file="$1"

    if grep -q "<!DOCTYPE html>" "$response_file"; then
        log_success "HTML Content - Valid HTML document"
    else
        log_error "HTML Content - Not a valid HTML document"
    fi

    if grep -q "Protomil" "$response_file"; then
        log_success "HTML Content - Contains Protomil branding"
    else
        log_error "HTML Content - Missing Protomil branding"
    fi
}

check_registration_form() {
    local response_file="$1"

    check_html_content "$response_file"

    local required_fields=("email" "firstName" "lastName" "password" "phoneNumber")

    for field in "${required_fields[@]}"; do
        if grep -q "name=\"$field\"" "$response_file"; then
            log_success "Registration Form - Contains $field field"
        else
            log_error "Registration Form - Missing $field field"
        fi
    done
}

check_validation_response() {
    local response_file="$1"
    # This would need to be tested with actual email parameter
    log_info "Email validation endpoint tested (would need actual email parameter)"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    test_wireframe_pages
fi