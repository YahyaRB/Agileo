package com.agileo.AGILEO.entity.secondary;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    protected LocalDateTime  lastModifiedDate;

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
       this.lastModifiedBy = lastModifiedBy;
    }
    public void setLastModifiedDate(LocalDateTime  lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    public void setCreatedDate(LocalDateTime  createdDate) {
        this.createdDate = createdDate;
    }
}