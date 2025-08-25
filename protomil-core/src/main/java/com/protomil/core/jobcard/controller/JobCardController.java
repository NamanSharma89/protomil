package com.protomil.core.jobcard.controller;

import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.dto.JobAssignmentRequest;
import com.protomil.core.jobcard.dto.JobCardCreateRequest;
import com.protomil.core.jobcard.dto.JobCardResponse;
import com.protomil.core.jobcard.dto.JobCardSummary;
import com.protomil.core.jobcard.dto.JobCardUpdateRequest;
import com.protomil.core.jobcard.service.JobAssignmentService;
import com.protomil.core.jobcard.service.JobCardService;
import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.shared.logging.LogExecutionTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Card Management", description = "APIs for managing job cards")
public class JobCardController {

    private final JobCardService jobCardService;
    private final JobAssignmentService jobAssignmentService;

    @PostMapping
    @Operation(
            summary = "Create a new job card",
            description = "Creates a new job card from a template with dynamic fields"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Job card created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data - validation errors or template issues",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Template not found with provided ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> createJobCard(
            @Valid @RequestBody
            @Parameter(description = "Job card creation request")
            JobCardCreateRequest request) {

        log.info("Creating job card with template ID: {}", request.getTemplateId());

        JobCardResponse response = jobCardService.createJobCard(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{jobCardId}")
    @Operation(
            summary = "Get job card by ID",
            description = "Retrieves detailed job card information including work instructions and progress"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found with provided ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCard(
            @PathVariable
            @Parameter(description = "Job card ID", example = "1")
            Long jobCardId) {

        log.debug("Retrieving job card with ID: {}", jobCardId);

        JobCardResponse response = jobCardService.getJobCardById(jobCardId);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/job-number/{jobNumber}")
    @Operation(
            summary = "Get job card by job number",
            description = "Retrieves job card using the unique job number identifier"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found with provided job number",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCardByNumber(
            @PathVariable
            @Parameter(description = "Unique job number", example = "JC-2025-001")
            String jobNumber) {

        log.debug("Retrieving job card with job number: {}", jobNumber);

        JobCardResponse response = jobCardService.getJobCardByJobNumber(jobNumber);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    @Operation(
            summary = "Get job cards with filters",
            description = "Retrieves paginated list of job cards with optional filtering by status, assignment, etc."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job cards retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Page<JobCardSummary>>> getJobCards(
            @RequestParam(required = false)
            @Parameter(description = "Filter by job status")
            JobStatus status,

            @RequestParam(required = false)
            @Parameter(description = "Filter by assigned user ID")
            UUID assignedTo,

            @RequestParam(required = false)
            @Parameter(description = "Filter by creator user ID")
            UUID createdBy,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Number of items per page", example = "20")
            int size,

            @RequestParam(defaultValue = "createdAt")
            @Parameter(description = "Field to sort by", example = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            @Parameter(description = "Sort direction", example = "DESC")
            Sort.Direction sortDir) {

        log.debug("Retrieving job cards - status: {}, assignedTo: {}, page: {}",
                status, assignedTo, page);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<JobCardSummary> response = jobCardService.getJobCards(
                status, assignedTo, createdBy, pageable
        );

        return ResponseEntity.ok(
                ApiResponse.<Page<JobCardSummary>>builder()
                        .success(true)
                        .message("Job cards retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search job cards",
            description = "Searches job cards by title, description, job number, or other text fields"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Page<JobCardSummary>>> searchJobCards(
            @RequestParam
            @Parameter(description = "Search term to find in job cards", example = "Motor Assembly")
            String q,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Number of items per page", example = "20")
            int size) {

        log.debug("Searching job cards with term: {}", q);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobCardSummary> response = jobCardService.searchJobCards(q, pageable);

        return ResponseEntity.ok(
                ApiResponse.<Page<JobCardSummary>>builder()
                        .success(true)
                        .message("Job cards search completed successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{jobCardId}")
    @Operation(
            summary = "Update job card",
            description = "Updates job card details. Only allowed for job cards in DRAFT status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or job card not in DRAFT status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> updateJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to update", example = "1")
            Long jobCardId,

            @Valid @RequestBody
            @Parameter(description = "Job card update request")
            JobCardUpdateRequest request) {

        log.info("Updating job card with ID: {}", jobCardId);

        JobCardResponse response = jobCardService.updateJobCard(jobCardId, request);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card updated successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/assign")
    @Operation(
            summary = "Assign job card to personnel",
            description = "Assigns job card to a specific user with optional machine assignment"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card assigned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid assignment - user unavailable, machine occupied, etc.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card, user, or machine not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> assignJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to assign", example = "1")
            Long jobCardId,

            @Valid @RequestBody
            @Parameter(description = "Assignment details")
            JobAssignmentRequest request) {

        log.info("Assigning job card {} to user {}", jobCardId, request.getAssignedTo());

        JobCardResponse response = jobAssignmentService.assignJobCard(jobCardId, request);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card assigned successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/unassign")
    @Operation(
            summary = "Unassign job card",
            description = "Removes current personnel assignment from job card"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card unassigned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> unassignJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to unassign", example = "1")
            Long jobCardId,

            @RequestParam(required = false, defaultValue = "Manual unassignment")
            @Parameter(description = "Reason for unassignment")
            String reason) {

        log.info("Unassigning job card {} - Reason: {}", jobCardId, reason);

        JobCardResponse response = jobAssignmentService.unassignJobCard(jobCardId, reason);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card unassigned successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/start")
    @Operation(
            summary = "Start job card execution",
            description = "Starts the execution of an assigned job card, changing status to IN_PROGRESS"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card started successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Job card cannot be started - wrong status or not assigned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> startJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to start", example = "1")
            Long jobCardId) {

        log.info("Starting job card with ID: {}", jobCardId);

        JobCardResponse response = jobCardService.startJobCard(jobCardId);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card started successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/complete")
    @Operation(
            summary = "Complete job card",
            description = "Marks job card as completed after all work instructions are finished"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Job card cannot be completed - wrong status or incomplete work",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> completeJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to complete", example = "1")
            Long jobCardId) {

        log.info("Completing job card with ID: {}", jobCardId);

        JobCardResponse response = jobCardService.completeJobCard(jobCardId);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card completed successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/cancel")
    @Operation(
            summary = "Cancel job card",
            description = "Cancels job card execution with a reason. Cannot cancel already completed jobs."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card cancelled successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Job card cannot be cancelled - already in final status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> cancelJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to cancel", example = "1")
            Long jobCardId,

            @RequestParam(required = false, defaultValue = "Manual cancellation")
            @Parameter(description = "Reason for cancellation")
            String reason) {

        log.info("Cancelling job card {} - Reason: {}", jobCardId, reason);

        JobCardResponse response = jobCardService.cancelJobCard(jobCardId, reason);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card cancelled successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{jobCardId}/status")
    @Operation(
            summary = "Change job card status",
            description = "Manually changes job card status with validation of allowed transitions"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job card status changed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid status transition",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardResponse>> changeJobCardStatus(
            @PathVariable
            @Parameter(description = "Job card ID", example = "1")
            Long jobCardId,

            @RequestParam
            @Parameter(description = "New status to set", example = "IN_PROGRESS")
            JobStatus status,

            @RequestParam(required = false)
            @Parameter(description = "Reason for status change")
            String reason) {

        log.info("Changing job card {} status to {} - Reason: {}", jobCardId, status, reason);

        JobCardResponse response = jobCardService.changeJobCardStatus(jobCardId, status, reason);

        return ResponseEntity.ok(
                ApiResponse.<JobCardResponse>builder()
                        .success(true)
                        .message("Job card status changed successfully")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{jobCardId}")
    @Operation(
            summary = "Delete job card",
            description = "Permanently deletes a job card. Only allowed for job cards in DRAFT status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Job card deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Cannot delete job card - not in DRAFT status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Job card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Void>> deleteJobCard(
            @PathVariable
            @Parameter(description = "Job card ID to delete", example = "1")
            Long jobCardId) {

        log.info("Deleting job card with ID: {}", jobCardId);

        jobCardService.deleteJobCard(jobCardId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Job card deleted successfully")
                        .data(null)
                        .build());
    }

    @GetMapping("/statistics/status-counts")
    @Operation(
            summary = "Get job card status counts",
            description = "Retrieves count of job cards grouped by their current status"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status counts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Map<JobStatus, Long>>> getStatusCounts() {

        log.debug("Retrieving job card status counts");

        Map<JobStatus, Long> response = jobCardService.getJobCardStatusCounts();

        return ResponseEntity.ok(
                ApiResponse.<Map<JobStatus, Long>>builder()
                        .success(true)
                        .message("Status counts retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/user/{userId}/active")
    @Operation(
            summary = "Get active job cards by user",
            description = "Retrieves all active job cards assigned to a specific user"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Active job cards retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<JobCardSummary>>> getActiveJobCardsByUser(
            @PathVariable
            @Parameter(description = "User ID to get active jobs for", example = "123")
            UUID userId) {

        log.debug("Retrieving active job cards for user: {}", userId);

        List<JobCardSummary> response = jobCardService.getActiveJobCardsByUser(userId);

        return ResponseEntity.ok(
                ApiResponse.<List<JobCardSummary>>builder()
                        .success(true)
                        .message("Active job cards retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/overdue")
    @Operation(
            summary = "Get overdue job cards",
            description = "Retrieves all job cards that have passed their target completion date"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Overdue job cards retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<JobCardSummary>>> getOverdueJobCards() {

        log.debug("Retrieving overdue job cards");

        List<JobCardSummary> response = jobCardService.getOverdueJobCards();

        return ResponseEntity.ok(
                ApiResponse.<List<JobCardSummary>>builder()
                        .success(true)
                        .message("Overdue job cards retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/template/{templateId}")
    @Operation(
            summary = "Get job cards by template",
            description = "Retrieves all job cards created from a specific template"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job cards retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<JobCardSummary>>> getJobCardsByTemplate(
            @PathVariable
            @Parameter(description = "Template ID", example = "1")
            Long templateId) {

        log.debug("Retrieving job cards for template: {}", templateId);

        List<JobCardSummary> response = jobCardService.getJobCardsByTemplate(templateId);

        return ResponseEntity.ok(
                ApiResponse.<List<JobCardSummary>>builder()
                        .success(true)
                        .message("Job cards retrieved successfully")
                        .data(response)
                        .build()
        );

    }

    @GetMapping("/statistics/average-completion-time")
    @Operation(
            summary = "Get average completion time",
            description = "Calculates the average completion time for completed job cards"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Average completion time calculated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Double.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Double>> getAverageCompletionTime() {

        log.debug("Calculating average completion time");

        Double response = jobCardService.getAverageCompletionTime();

        return ResponseEntity.ok(
                ApiResponse.<Double>builder()
                        .success(true)
                        .message("Average completion time calculated successfully")
                        .data(response)
                        .build()
        );


    }

    @GetMapping("/statistics/exceeding-estimated-time")
    @Operation(
            summary = "Get job cards exceeding estimated time",
            description = "Retrieves job cards that took longer than their estimated duration"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job cards retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<JobCardSummary>>> getJobCardsExceedingEstimatedTime() {

        log.debug("Retrieving job cards exceeding estimated time");

        List<JobCardSummary> response = jobCardService.getJobCardsExceedingEstimatedTime();

        return ResponseEntity.ok(
                ApiResponse.<List<JobCardSummary>>builder()
                        .success(true)
                        .message("Job cards retrieved successfully")
                        .data(response)
                        .build()
        );
    }
}