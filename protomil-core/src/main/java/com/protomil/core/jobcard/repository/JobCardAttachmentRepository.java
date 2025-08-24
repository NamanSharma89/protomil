package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.JobCardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCardAttachmentRepository extends JpaRepository<JobCardAttachment, Long> {

    List<JobCardAttachment> findByJobCardIdOrderByUploadedAtDesc(Long jobCardId);

    List<JobCardAttachment> findByWorkInstructionIdOrderByUploadedAtDesc(Long workInstructionId);

    List<JobCardAttachment> findByFileTypeContainingIgnoreCase(String fileType);

    @Query("SELECT SUM(jca.fileSizeBytes) FROM JobCardAttachment jca WHERE jca.jobCard.id = :jobCardId")
    Long sumFileSizeByJobCardId(@Param("jobCardId") Long jobCardId);

    List<JobCardAttachment> findByUploadedBy(Long uploadedBy);
}