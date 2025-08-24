package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.CommentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_card_comments", indexes = {
        @Index(name = "idx_job_card_comments_job", columnList = "job_card_id"),
        @Index(name = "idx_job_card_comments_created_by", columnList = "created_by"),
        @Index(name = "idx_job_card_comments_type", columnList = "comment_type"),
        @Index(name = "idx_job_card_comments_date", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", length = 50)
    @Builder.Default
    private CommentType commentType = CommentType.GENERAL;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = true;

    // Helper methods
    public boolean isQualityRelated() {
        return commentType == CommentType.QUALITY || commentType == CommentType.ISSUE;
    }

    public boolean isPublic() {
        return !Boolean.TRUE.equals(isInternal);
    }

    public String getCommentPreview(int maxLength) {
        if (commentText == null) return "";
        if (commentText.length() <= maxLength) return commentText;
        return commentText.substring(0, maxLength) + "...";
    }
}