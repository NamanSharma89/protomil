#!/bin/bash

# test-helpers.sh - Enhanced with health check configuration
BASE_URL="${API_BASE_URL:-http://localhost:8080}"
CONTENT_TYPE="Content-Type:application/json"
TEST_RESULTS_DIR="test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Health check configuration
ENABLE_HEALTH_CHECK="${ENABLE_HEALTH_CHECK:-true}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

mkdir -p $TEST_RESULTS_DIR

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$TEST_RESULTS_DIR/test_log_$TIMESTAMP.log"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1" | tee -a "$TEST_RESULTS_DIR/test_log_$TIMESTAMP.log"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1" | tee -a "$TEST_RESULTS_DIR/test_log_$TIMESTAMP.log"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$TEST_RESULTS_DIR/test_log_$TIMESTAMP.log"
}

# Enhanced run_test function with health check awareness
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local endpoint="$3"
    local method="$4"
    local data_file="$5"
    local additional_checks="$6"

    ((TOTAL_TESTS++))
    log_info "Running test: $test_name"

    local cmd="http $method $BASE_URL$endpoint $CONTENT_TYPE"
    if [ -n "$data_file" ] && [ -f "$data_file" ]; then
        cmd="$cmd < $data_file"
    fi

    local response_file="$TEST_RESULTS_DIR/${test_name// /_}_response_$TIMESTAMP.json"
    local status_code

    if [ -n "$data_file" ] && [ -f "$data_file" ]; then
        status_code=$(http --print=HhBb --ignore-stdin $method "$BASE_URL$endpoint" $CONTENT_TYPE < "$data_file" 2>/dev/null | tee "$response_file" | head -1 | cut -d' ' -f2)
    else
        status_code=$(http --print=HhBb $method "$BASE_URL$endpoint" 2>/dev/null | tee "$response_file" | head -1 | cut -d' ' -f2)
    fi

    if [ "$status_code" = "$expected_status" ]; then
        log_success "$test_name - Status code: $status_code"
        if [ -n "$additional_checks" ]; then
            eval "$additional_checks '$response_file'"
        fi
    else
        log_error "$test_name - Expected: $expected_status, Got: $status_code"
        echo "Response saved to: $response_file"
    fi
}

# Health check function that respects the ENABLE_HEALTH_CHECK setting
check_api_health() {
    if [[ "$ENABLE_HEALTH_CHECK" != "true" ]]; then
        log_info "Health check disabled, skipping API health verification"
        return 0
    fi

    log_info "Checking API health..."
    local health_response=$(http GET "$BASE_URL/actuator/health" 2>/dev/null)
    local health_status=$(echo "$health_response" | jq -r '.status' 2>/dev/null)

    if [ "$health_status" = "UP" ]; then
        log_success "API is healthy"
        return 0
    else
        log_error "API health check failed"
        echo "Health response: $health_response"
        return 1
    fi
}

# Enhanced wait_for_api function
wait_for_api() {
    local max_attempts=${TEST_TIMEOUT:-30}
    local attempt=1

    log_info "Waiting for API to be ready (Health check: $ENABLE_HEALTH_CHECK)..."

    while [ $attempt -le $max_attempts ]; do
        if [[ "$ENABLE_HEALTH_CHECK" == "true" ]]; then
            # Use health endpoint
            if http GET "$BASE_URL/actuator/health" &>/dev/null; then
                log_success "API is ready (health check passed)"
                return 0
            fi
        else
            # Basic connectivity check
            if http GET "$BASE_URL/actuator/info" &>/dev/null || \
               http GET "$BASE_URL/" &>/dev/null || \
               curl -f -s --max-time 3 "$BASE_URL" >/dev/null 2>&1; then
                log_success "API is ready (basic connectivity confirmed)"
                return 0
            fi
        fi

        log_info "Attempt $attempt/$max_attempts - API not ready, waiting..."
        sleep 2
        ((attempt++))
    done

    log_error "API failed to become ready after $max_attempts attempts"
    return 1
}

generate_unique_email() {
    local timestamp=$(date +%s)
    echo "testuser+$timestamp@protomil.com"
}

print_test_summary() {
    echo ""
    echo "=================================="
    echo "           TEST SUMMARY           "
    echo "=================================="
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
    if [ $TOTAL_TESTS -gt 0 ]; then
        echo "Success Rate: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
    fi
    echo "Health Check: $ENABLE_HEALTH_CHECK"
    echo "=================================="

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}All tests passed!${NC}"
        return 0
    else
        echo -e "${RED}Some tests failed!${NC}"
        return 1
    fi
}