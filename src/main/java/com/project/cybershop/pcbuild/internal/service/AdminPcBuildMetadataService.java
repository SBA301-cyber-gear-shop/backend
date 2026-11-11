package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.dto.admin.*;

import java.util.List;

public interface AdminPcBuildMetadataService {

    // Quản lý Template Slots
    List<TemplateSlotDto> getAllSlots();

    TemplateSlotDto createSlot(TemplateSlotRequest request);

    TemplateSlotDto updateSlot(Integer id, TemplateSlotRequest request);

    void deleteSlot(Integer id);

    List<TemplateSlotDto> reorderSlots(List<Integer> orderedSlotIds);

    MetadataCategoryOptionDto getAvailableCategoryOptions();

    // Quản lý Budget Profiles
    List<BudgetProfilePurposeDto> getAllBudgetProfilesGrouped();

    BudgetProfilePurposeDto createPurpose(BudgetProfileCreateRequest request);

    BudgetProfilePurposeDto updatePurpose(String purpose, BudgetProfileUpdateRequest request);

    void deletePurpose(String purpose);
}

