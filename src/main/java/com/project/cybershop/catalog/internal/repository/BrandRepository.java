package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    Optional<Brand> findByName(String name);
    boolean existsByName(String name);
}
