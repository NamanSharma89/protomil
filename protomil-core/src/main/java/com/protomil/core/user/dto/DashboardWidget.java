package com.protomil.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget {
    private String id;
    private String title;
    private String type;
    private String content;
    private String icon;
    private int order;

    @Builder.Default
    private String size = "col-12";

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String url = ""; // Changed from null to empty string
}