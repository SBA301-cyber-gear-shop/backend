package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.PeripheralQueryRequest;
import com.project.cybershop.pcbuild.api.dto.response.PeripheralSuggestionResponse;

public interface PeripheralService {

    /** POST /peripherals: chọn màn, phím, chuột theo tiền còn lại; không gọi AI, không ghi DB. */
    PeripheralSuggestionResponse suggest(PeripheralQueryRequest request);
}
