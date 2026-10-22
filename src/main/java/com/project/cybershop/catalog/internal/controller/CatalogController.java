package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.response.CatalogProductResponse;
import com.project.cybershop.catalog.internal.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogProductResponse> list() {
        return service.list();
    }
}
