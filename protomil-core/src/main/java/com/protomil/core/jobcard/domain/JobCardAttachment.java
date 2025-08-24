package com.protomil.core.jobcard.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_card_attachments", indexes = {
        @Index(name = "idx_job_card_attachments_job_card", columnList = "job_card_id"),
        @Index(name = "idx_job_card_attachments_work_instruction", columnList = "work_instruction_id"),
        @Index(name = "idx_job_card_attachments_uploaded_by", columnList = "uploaded_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_instruction_id")
    private WorkInstruction workInstruction;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at")
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    // Helper methods
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }

    public boolean isImage() {
        String extension = getFileExtension();
        return extension.matches("jpg|jpeg|png|gif|bmp|webp");
    }

    public boolean isVideo() {
        String extension = getFileExtension();
        return extension.matches("mp4|avi|mov|wmv|flv|webm");
    }

    public boolean isDocument() {
        String extension = getFileExtension();
        return extension.matches("pdf|doc|docx|txt|rtf");
    }

    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "0 B";

        long bytes = fileSizeBytes;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}