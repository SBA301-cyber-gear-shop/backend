package com.project.cybershop.pcbuild.internal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compatibility_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(name = "category_1_id", nullable = false)
    private Integer category1Id;

    @Column(name = "spec_1_id", nullable = false)
    private Integer spec1Id;

    @Column(name = "category_2_id", nullable = false)
    private Integer category2Id;

    @Column(name = "spec_2_id", nullable = false)
    private Integer spec2Id;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;
}
