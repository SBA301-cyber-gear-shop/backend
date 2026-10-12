
package com.project.cybershop.order.internal.web;

import com.project.cybershop.order.api.dto.ProductResponse;
import com.project.cybershop.order.internal.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Lấy danh sách sản phẩm đang kinh doanh
    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    // Tìm kiếm sản phẩm theo tên
    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam(defaultValue = "") String keyword
    ) {
        return productService.searchProducts(keyword);
    }

    // Xem chi tiết một phiên bản sản phẩm
    @GetMapping("/variants/{variantId}")
    public ProductResponse getProductByVariantId(
            @PathVariable Integer variantId
    ) {
        return productService.getProductByVariantId(variantId);
    }

    // Trả về HTTP 404 khi không tìm thấy phiên bản
    // hoặc HTTP 400 khi ID không hợp lệ
    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<String>
    handleIllegalArgument(IllegalArgumentException ex) {

        HttpStatus status = ex.getMessage() != null
                && ex.getMessage().startsWith("ID phiên bản")
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.NOT_FOUND;

        return org.springframework.http.ResponseEntity
                .status(status)
                .body(ex.getMessage());
    }
}
