package com.project.cybershop.support.internal.model;

public record PcBuildProfile(
        String profileType,       // "GAMING", "WORKSTATION_3D", "OFFICE", "BALANCED"
        boolean preferAesthetic,  // đẹp, nhiều đèn, rgb, argb, bể cá, show-case, trắng, white
        boolean preferWhite,      // trắng, white, ice, aero
        String preferredCpuBrand, // "intel", "amd", or null
        String preferredGpuBrand, // "nvidia", "amd", or null
        Integer minRamGb,         // 16, 32, 48, 64
        Integer minSsdGb          // 1000, 2000, 4000
) {}
