package com.protomil.core.jobcard.controller;

import com.protomil.core.jobcard.dto.JobCardTemplateResponse;
import com.protomil.core.jobcard.dto.TemplateFieldDefinitionDto;
import com.protomil.core.jobcard.service.JobCardTemplateService;
import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.shared.logging.LogExecutionTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-card-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Card Templates", description = "APIs for managing job card templates")
public class JobCardTemplateController {

    private final JobCardTemplateService templateService;

    @GetMapping
    @Operation(
            summary = "Get job card templates",
            description = "Retrieves paginated list of active job card templates"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Templates retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<Page<JobCardTemplateResponse>>> getTemplates(
            @RequestParam(required = false)
            @Parameter(description = "Filter by category")
            String category,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size", example = "20")
            int size,

            @RequestParam(defaultValue = "templateName")
            @Parameter(description = "Sort field", example = "templateName")
            String sortBy,

            @RequestParam(defaultValue = "ASC")
            @Parameter(description = "Sort direction", example = "ASC")
            Sort.Direction sortDir) {

        log.debug("Retrieving templates with category filter: {}", category);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<JobCardTemplateResponse> response = templateService.getTemplates(category, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Templates retrieved successfully")
        );
    }

    @GetMapping("/{templateId}")
    @Operation(
            summary = "Get template by ID",
            description = "Retrieves detailed template information including field definitions"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JobCardTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Template not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<JobCardTemplateResponse>> getTemplate(
            @PathVariable
            @Parameter(description = "Template ID", example = "1")
            Long templateId) {

        log.debug("Retrieving template with ID: {}", templateId);

        JobCardTemplateResponse response = templateService.getTemplateById(templateId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Template retrieved successfully")
        );
    }

    @GetMapping("/{templateId}/fields")
    @Operation(
            summary = "Get template field definitions",
            description = "Retrieves field definitions for a specific template"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Field definitions retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Template not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<TemplateFieldDefinitionDto>>> getTemplateFields(
            @PathVariable
            @Parameter(description = "Template ID", example = "1")
            Long templateId) {

        log.debug("Retrieving field definitions for template: {}", templateId);

        List<TemplateFieldDefinitionDto> response = templateService.getTemplateFields(templateId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Field definitions retrieved successfully")
        );
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Get template categories",
            description = "Retrieves list of all available template categories"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Categories retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'SUPERVISOR', 'ADMIN')")
    @LogExecutionTime
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {

        log.debug("Retrieving template categories");

        List<String> response = templateService.getAllCategories();

        return ResponseEntity.ok(
                ApiResponse.success(response, "Categories retrieved successfully")
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search templates",
            description = "Searches templates by name, description, or category"
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
    public ResponseEntity<ApiResponse<Page<JobCardTemplateResponse>>> searchTemplates(
            @RequestParam
            @Parameter(description = "Search term", example = "Assembly")
            String q,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size", example = "20")
            int size) {

        log.debug("Searching templates with term: {}", q);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "templateName"));
        Page<JobCardTemplateResponse> response = templateService.searchTemplates(q, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Template search completed successfully")
        );
    }
}