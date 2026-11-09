package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PcCustomRepository extends JpaRepository<PcCustom, Integer> {
}
