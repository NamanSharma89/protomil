// src/main/java/com/protomil/core/user/dto/NavigationItem.java
package com.protomil.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationItem {

    private String id;
    private String label;
    private String icon;
    private String url;
    private boolean active;
    private boolean enabled = true;
    private String tooltip;
    private int order;
}