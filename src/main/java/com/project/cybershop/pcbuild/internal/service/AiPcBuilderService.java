package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;

public interface AiPcBuilderService {
    AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId);
}
