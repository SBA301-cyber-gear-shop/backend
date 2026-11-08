package com.project.cybershop.promotion.internal.controller;
import com.project.cybershop.promotion.api.dto.request.ProductSaleRequest;
import com.project.cybershop.promotion.api.dto.response.ProductSaleResponse;
import com.project.cybershop.promotion.api.dto.response.ProductSaleSearchResult;
import com.project.cybershop.promotion.internal.service.ProductSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/product-sales")
@PreAuthorize("hasRole('ADMIN')")
public class ProductSaleController {
    private final ProductSaleService service;
    @GetMapping public List<ProductSaleResponse> list() { return service.list(); }
    @GetMapping("/products") public List<ProductSaleSearchResult> search(@RequestParam(defaultValue="") String search) { return service.search(search); }
    @GetMapping("/{productId}") public ProductSaleResponse get(@PathVariable Integer productId) { return service.get(productId); }
    @PutMapping("/{productId}") public ProductSaleResponse save(@PathVariable Integer productId, @Valid @RequestBody ProductSaleRequest request) { return service.save(productId, request); }
    @DeleteMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Integer productId) { service.remove(productId); }
}
