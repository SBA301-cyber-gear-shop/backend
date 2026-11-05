package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Integer id);

    List<Category> findByParentIsNull();
    List<Category> findByParentId(Integer parentId);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.id ASC")
    List<Category> findAllRootCategoriesWithChildren();

    @Query("SELECT c FROM Category c ORDER BY c.name ASC")
    List<Category> findAllOrderByName();

    long countByParentId(Integer parentId);
}
