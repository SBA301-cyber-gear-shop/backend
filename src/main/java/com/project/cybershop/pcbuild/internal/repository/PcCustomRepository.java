package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcCustomRepository extends JpaRepository<PcCustom, Integer> {
    List<PcCustom> findByUserIdOrderByIdDesc(Integer userId);
}
