// com/protomil/core/shared/domain/BaseEntity.java
package com.protomil.core.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class BaseEntity {

    @Version
    @Column(name = "version")
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}