#!/usr/bin/env python3
"""
Script to generate protomil-core project skeleton structure
Usage: python generate_structure.py
"""

import os
import sys
from pathlib import Path

def create_directory(path):
    """Create directory if it doesn't exist"""
    Path(path).mkdir(parents=True, exist_ok=True)
    print(f"Created directory: {path}")

def create_file(path, content=""):
    """Create file with optional content"""
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    if not Path(path).exists():
        with open(path, 'w') as f:
            f.write(content)
        print(f"Created file: {path}")

def generate_protomil_core_structure():
    """Generate the complete protomil-core directory structure"""

    # Base paths
    base_path = "protomil-core"
    java_base = f"{base_path}/src/main/java/com/protomil/core"
    resources_base = f"{base_path}/src/main/resources"
    test_base = f"{base_path}/src/test/java/com/protomil/core"

    print("Generating protomil-core project structure...")

    # Main application structure
    directories = [
        # Main Java packages
        f"{java_base}/config",

        # Shared domain
        f"{java_base}/shared/domain/enums",
        f"{java_base}/shared/dto",
        f"{java_base}/shared/exception",
        f"{java_base}/shared/security",
        f"{java_base}/shared/utils",

        # Job Card Domain
        f"{java_base}/jobcard/domain",
        f"{java_base}/jobcard/dto",
        f"{java_base}/jobcard/repository",
        f"{java_base}/jobcard/service",
        f"{java_base}/jobcard/controller",
        f"{java_base}/jobcard/events",

        # Personnel Domain
        f"{java_base}/personnel/domain",
        f"{java_base}/personnel/dto",
        f"{java_base}/personnel/repository",
        f"{java_base}/personnel/service",
        f"{java_base}/personnel/controller",
        f"{java_base}/personnel/events",

        # Equipment Domain
        f"{java_base}/equipment/domain",
        f"{java_base}/equipment/dto",
        f"{java_base}/equipment/repository",
        f"{java_base}/equipment/service",
        f"{java_base}/equipment/controller",
        f"{java_base}/equipment/events",

        # Workflow Domain
        f"{java_base}/workflow/domain",
        f"{java_base}/workflow/dto",
        f"{java_base}/workflow/service",
        f"{java_base}/workflow/controller",
        f"{java_base}/workflow/events",

        # Report Generation (Lambda-ready)
        f"{java_base}/reports/dto",
        f"{java_base}/reports/service",
        f"{java_base}/reports/controller",
        f"{java_base}/reports/lambda",
        f"{java_base}/reports/templates",

        # Integration layers
        f"{java_base}/integration/erp",
        f"{java_base}/integration/mes",
        f"{java_base}/integration/aws",

        # Resources
        f"{resources_base}/db/migration",
        f"{resources_base}/static",
        f"{resources_base}/templates",

        # Test structure
        f"{test_base}/jobcard",
        f"{test_base}/personnel",
        f"{test_base}/equipment",
        f"{test_base}/workflow",
        f"{test_base}/reports",
        f"{test_base}/integration",
        f"{test_base}/shared"
    ]

    # Create all directories
    for directory in directories:
        create_directory(directory)

    # Define files to create
    files = [
        # Main application (already exists, skip)
        # f"{java_base}/CoreApplication.java",

        # Configuration files
        f"{java_base}/config/SecurityConfig.java",
        f"{java_base}/config/DatabaseConfig.java",
        f"{java_base}/config/RedisConfig.java",
        f"{java_base}/config/ActuatorConfig.java",
        f"{java_base}/config/AwsConfig.java",

        # Shared domain
        f"{java_base}/shared/domain/BaseEntity.java",
        f"{java_base}/shared/domain/AuditableEntity.java",
        f"{java_base}/shared/domain/enums/Status.java",
        f"{java_base}/shared/domain/enums/Priority.java",

        # Shared DTOs
        f"{java_base}/shared/dto/PageResponse.java",
        f"{java_base}/shared/dto/ApiResponse.java",
        f"{java_base}/shared/dto/ErrorResponse.java",

        # Shared exceptions
        f"{java_base}/shared/exception/GlobalExceptionHandler.java",
        f"{java_base}/shared/exception/BusinessException.java",
        f"{java_base}/shared/exception/ResourceNotFoundException.java",

        # Shared security
        f"{java_base}/shared/security/JwtUtils.java",
        f"{java_base}/shared/security/SecurityContext.java",
        f"{java_base}/shared/security/UserPrincipal.java",

        # Shared utilities
        f"{java_base}/shared/utils/DateUtils.java",
        f"{java_base}/shared/utils/ValidationUtils.java",
        f"{java_base}/shared/utils/StringUtils.java",

        # Job Card Domain
        f"{java_base}/jobcard/domain/JobCard.java",
        f"{java_base}/jobcard/domain/JobStatus.java",
        f"{java_base}/jobcard/domain/JobType.java",
        f"{java_base}/jobcard/domain/WorkInstruction.java",

        f"{java_base}/jobcard/dto/JobCardRequest.java",
        f"{java_base}/jobcard/dto/JobCardResponse.java",
        f"{java_base}/jobcard/dto/JobCardSummary.java",
        f"{java_base}/jobcard/dto/JobAssignmentRequest.java",

        f"{java_base}/jobcard/repository/JobCardRepository.java",
        f"{java_base}/jobcard/repository/JobStatusRepository.java",
        f"{java_base}/jobcard/repository/WorkInstructionRepository.java",

        f"{java_base}/jobcard/service/JobCardService.java",
        f"{java_base}/jobcard/service/JobCardServiceImpl.java",
        f"{java_base}/jobcard/service/JobAssignmentService.java",
        f"{java_base}/jobcard/service/JobWorkflowService.java",

        f"{java_base}/jobcard/controller/JobCardController.java",
        f"{java_base}/jobcard/controller/JobWorkflowController.java",

        f"{java_base}/jobcard/events/JobCardCreatedEvent.java",
        f"{java_base}/jobcard/events/JobCardAssignedEvent.java",
        f"{java_base}/jobcard/events/JobCardCompletedEvent.java",

        # Personnel Domain
        f"{java_base}/personnel/domain/Personnel.java",
        f"{java_base}/personnel/domain/Skill.java",
        f"{java_base}/personnel/domain/PersonnelSkill.java",
        f"{java_base}/personnel/domain/Shift.java",
        f"{java_base}/personnel/domain/Availability.java",

        f"{java_base}/personnel/dto/PersonnelRequest.java",
        f"{java_base}/personnel/dto/PersonnelResponse.java",
        f"{java_base}/personnel/dto/SkillAssignmentRequest.java",
        f"{java_base}/personnel/dto/AvailabilityResponse.java",

        f"{java_base}/personnel/repository/PersonnelRepository.java",
        f"{java_base}/personnel/repository/SkillRepository.java",
        f"{java_base}/personnel/repository/AvailabilityRepository.java",

        f"{java_base}/personnel/service/PersonnelService.java",
        f"{java_base}/personnel/service/SkillManagementService.java",
        f"{java_base}/personnel/service/AvailabilityService.java",

        f"{java_base}/personnel/controller/PersonnelController.java",
        f"{java_base}/personnel/controller/SkillController.java",

        f"{java_base}/personnel/events/PersonnelCreatedEvent.java",
        f"{java_base}/personnel/events/SkillAssignedEvent.java",

        # Equipment Domain
        f"{java_base}/equipment/domain/Equipment.java",
        f"{java_base}/equipment/domain/EquipmentType.java",
        f"{java_base}/equipment/domain/MaintenanceSchedule.java",
        f"{java_base}/equipment/domain/EquipmentStatus.java",

        f"{java_base}/equipment/dto/EquipmentRequest.java",
        f"{java_base}/equipment/dto/EquipmentResponse.java",
        f"{java_base}/equipment/dto/MaintenanceRequest.java",

        f"{java_base}/equipment/repository/EquipmentRepository.java",
        f"{java_base}/equipment/repository/MaintenanceRepository.java",

        f"{java_base}/equipment/service/EquipmentService.java",
        f"{java_base}/equipment/service/MaintenanceService.java",

        f"{java_base}/equipment/controller/EquipmentController.java",

        f"{java_base}/equipment/events/EquipmentAssignedEvent.java",
        f"{java_base}/equipment/events/MaintenanceScheduledEvent.java",

        # Workflow Domain
        f"{java_base}/workflow/domain/WorkflowDefinition.java",
        f"{java_base}/workflow/domain/WorkflowInstance.java",
        f"{java_base}/workflow/domain/WorkflowStep.java",

        f"{java_base}/workflow/dto/WorkflowRequest.java",
        f"{java_base}/workflow/dto/WorkflowResponse.java",

        f"{java_base}/workflow/service/WorkflowService.java",
        f"{java_base}/workflow/service/WorkflowExecutionService.java",

        f"{java_base}/workflow/controller/WorkflowController.java",

        f"{java_base}/workflow/events/WorkflowCompletedEvent.java",

        # Report Generation (Lambda-ready)
        f"{java_base}/reports/dto/ReportRequest.java",
        f"{java_base}/reports/dto/JobMetricsResponse.java",
        f"{java_base}/reports/dto/PersonnelPerformanceResponse.java",
        f"{java_base}/reports/dto/LambdaReportRequest.java",

        f"{java_base}/reports/service/ReportingService.java",
        f"{java_base}/reports/service/LambdaReportService.java",
        f"{java_base}/reports/service/ReportTemplateService.java",

        f"{java_base}/reports/controller/ReportingController.java",

        f"{java_base}/reports/lambda/ReportGeneratorHandler.java",
        f"{java_base}/reports/lambda/LambdaReportConfig.java",

        f"{java_base}/reports/templates/JobCardReportTemplate.java",
        f"{java_base}/reports/templates/PersonnelReportTemplate.java",

        # Integration layers
        f"{java_base}/integration/erp/ErpIntegrationService.java",
        f"{java_base}/integration/erp/ErpDataMapper.java",

        f"{java_base}/integration/mes/MesIntegrationService.java",
        f"{java_base}/integration/mes/MesEventHandler.java",

        f"{java_base}/integration/aws/AwsLambdaClient.java",
        f"{java_base}/integration/aws/S3Service.java",
        f"{java_base}/integration/aws/SqsService.java",

        # Resources
        f"{resources_base}/application-dev.yml",
        f"{resources_base}/application-prod.yml",
        f"{resources_base}/application-aws.yml",

        # Database migrations
        f"{resources_base}/db/migration/V1__create_base_tables.sql",
        f"{resources_base}/db/migration/V2__create_jobcard_tables.sql",
        f"{resources_base}/db/migration/V3__create_personnel_tables.sql",
        f"{resources_base}/db/migration/V4__create_equipment_tables.sql",
        f"{resources_base}/db/migration/V5__create_workflow_tables.sql",

        # Test files
        f"{test_base}/jobcard/JobCardServiceTest.java",
        f"{test_base}/jobcard/JobCardControllerTest.java",
        f"{test_base}/jobcard/JobCardRepositoryTest.java",

        f"{test_base}/personnel/PersonnelServiceTest.java",
        f"{test_base}/personnel/PersonnelControllerTest.java",

        f"{test_base}/equipment/EquipmentServiceTest.java",
        f"{test_base}/equipment/EquipmentControllerTest.java",

        f"{test_base}/workflow/WorkflowServiceTest.java",

        f"{test_base}/reports/ReportingServiceTest.java",
        f"{test_base}/reports/LambdaReportServiceTest.java",

        f"{test_base}/integration/JobCardIntegrationTest.java",
        f"{test_base}/integration/PersonnelIntegrationTest.java",
        f"{test_base}/integration/AwsIntegrationTest.java",

        f"{test_base}/shared/utils/DateUtilsTest.java",
        f"{test_base}/shared/security/JwtUtilsTest.java",
    ]

    # Create all files
    for file_path in files:
        create_file(file_path)

    # Create additional documentation files
    docs = [
        f"{base_path}/README.md",
        f"{base_path}/ARCHITECTURE.md",
        f"{base_path}/AWS_DEPLOYMENT.md",
        f"{base_path}/DOMAIN_BOUNDARIES.md"
    ]

    for doc in docs:
        create_file(doc)

    print(f"\n✅ Successfully generated protomil-core project structure!")
    print(f"📁 Created {len(directories)} directories")
    print(f"📄 Created {len(files) + len(docs)} files")
    print(f"\n📋 Key features included:")
    print("   • Modular monolith structure with clear domain boundaries")
    print("   • Spring Modulith-ready package organization")
    print("   • Lambda-ready report generation service")
    print("   • AWS integration layer")
    print("   • Comprehensive test structure")
    print("   • Database migration files")
    print("\n🚀 Next steps:")
    print("   1. Run this script from your project root directory")
    print("   2. Start implementing domain entities")
    print("   3. Configure database connections")
    print("   4. Set up AWS Lambda for report generation")

def main():
    """Main function"""
    try:
        generate_protomil_core_structure()
    except Exception as e:
        print(f"❌ Error generating structure: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()