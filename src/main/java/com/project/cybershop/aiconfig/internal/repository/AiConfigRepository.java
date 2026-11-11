package com.project.cybershop.aiconfig.internal.repository;

import com.project.cybershop.aiconfig.internal.entity.AiConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiConfigRepository extends JpaRepository<AiConfigEntity, Integer> {

    Optional<AiConfigEntity> findByConfigKeyAndIsActiveTrue(String configKey);

    List<AiConfigEntity> findAllByOrderByConfigKeyAscIdAsc();

    List<AiConfigEntity> findByConfigKey(String configKey);

    long countByConfigKeyAndIsActiveTrue(String configKey);

    @Modifying
    @Query("UPDATE AiConfigEntity c SET c.isActive = false WHERE c.configKey = :configKey AND c.id <> :excludeId")
    void deactivateOthersInKey(@Param("configKey") String configKey, @Param("excludeId") Integer excludeId);
}

