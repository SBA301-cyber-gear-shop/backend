package com.project.cybershop.aiconfig.internal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "config_key", nullable = false, length = 50)
    private String configKey;

    @Column(name = "config_name", nullable = false, length = 255)
    private String configName;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "api_key", nullable = false, columnDefinition = "TEXT")
    private String apiKey;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

