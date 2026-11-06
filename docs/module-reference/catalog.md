# Tra cứu module catalog

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **POST `/api/admin/brands`** → `AdminBrandController.createBrand` [AdminBrandController.java:25](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L25)
  - `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo thương hiệu mới") public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request)`
- **GET `/api/admin/brands`** → `AdminBrandController.getAllBrands` [AdminBrandController.java:32](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L32)
  - `@GetMapping @Operation(summary = "Danh sách tất cả thương hiệu") public ResponseEntity<List<BrandResponse>> getAllBrands()`
- **GET `/api/admin/brands/{id}`** → `AdminBrandController.getBrandById` [AdminBrandController.java:38](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L38)
  - `@GetMapping("/{id}") @Operation(summary = "Chi tiết thương hiệu theo ID") public ResponseEntity<BrandResponse> getBrandById(@PathVariable Integer id)`
- **POST `/api/admin/categories`** → `AdminCategoryController.createCategory` [AdminCategoryController.java:27](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L27)
  - `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo danh mục mới (hỗ trợ phân cấp cha/con)") public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request)`
- **POST `/api/admin/categories/{id}/specifications`** → `AdminCategoryController.configureCategorySpecifications` [AdminCategoryController.java:34](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L34)
  - `@PostMapping("/{id}/specifications") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cấu hình thông số kỹ thuật cho danh mục (bộ lọc, cấu hình nổi bật, mega menu)") public ResponseEntity<List<CategorySpecResponse>> configureCategorySpecifications( @PathVariable Integer id, @Valid @RequestBody List<ConfigureCategorySpecRequest> requests)`
- **GET `/api/admin/categories`** → `AdminCategoryController.getCategoryTree` [AdminCategoryController.java:43](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L43)
  - `@GetMapping @Operation(summary = "Lấy cây danh mục sản phẩm") public ResponseEntity<List<CategoryResponse>> getCategoryTree()`
- **GET `/api/admin/categories/{id}`** → `AdminCategoryController.getCategoryById` [AdminCategoryController.java:49](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L49)
  - `@GetMapping("/{id}") @Operation(summary = "Chi tiết danh mục theo ID") public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer id)`
- **POST `/api/admin/products`** → `AdminProductController.createProductDraft` [AdminProductController.java:33](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L33)
  - `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo sản phẩm mới (Lưu bản nháp - DRAFT, kèm biến thể ban đầu nếu có)") public ResponseEntity<ProductResponse> createProductDraft(@Valid @RequestBody CreateProductRequest request)`
- **POST `/api/admin/products/{id}/variants`** → `AdminProductController.addVariant` [AdminProductController.java:40](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L40)
  - `@PostMapping("/{id}/variants") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Thêm biến thể / SKU mới vào sản phẩm đã có kèm thông số kỹ thuật") public ResponseEntity<ProductVariantResponse> addVariant( @PathVariable Integer id, @Valid @RequestBody CreateVariantDto variantDto)`
- **GET `/api/admin/products/{id}/variants`** → `AdminProductController.getVariants` [AdminProductController.java:49](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L49)
  - `@GetMapping("/{id}/variants") @Operation(summary = "Lấy danh sách các biến thể của sản phẩm") public ResponseEntity<List<ProductVariantResponse>> getVariants(@PathVariable Integer id)`
- **GET `/api/admin/products/{id}/variants/{variantId}`** → `AdminProductController.getVariantDetail` [AdminProductController.java:55](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L55)
  - `@GetMapping("/{id}/variants/{variantId}") @Operation(summary = "Xem chi tiết một biến thể cụ thể của sản phẩm") public ResponseEntity<ProductVariantResponse> getVariantDetail( @PathVariable Integer id, @PathVariable Integer variantId)`
- **PUT `/api/admin/products/{id}/variants/{variantId}`** → `AdminProductController.updateVariant` [AdminProductController.java:63](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L63)
  - `@PutMapping("/{id}/variants/{variantId}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cập nhật biến thể sản phẩm (SKU, giá bán, giá gốc, tồn kho, ảnh, thông số kỹ thuật)") public ResponseEntity<ProductVariantResponse> updateVariant( @PathVariable Integer id, @PathVariable Integer variantId, @Valid @RequestBody UpdateVariantRequest request)`
- **DELETE `/api/admin/products/{id}/variants/{variantId}`** → `AdminProductController.deleteVariant` [AdminProductController.java:73](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L73)
  - `@DeleteMapping("/{id}/variants/{variantId}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Xóa biến thể sản phẩm (Chặn xóa nếu đã phát sinh đơn hàng)") public ResponseEntity<Void> deleteVariant( @PathVariable Integer id, @PathVariable Integer variantId)`
- **PUT `/api/admin/products/{id}/publish`** → `AdminProductController.publishProduct` [AdminProductController.java:83](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L83)
  - `@PutMapping("/{id}/publish") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Kiểm tra tính hợp lệ và Đăng bán sản phẩm (Chuyển DRAFT -> ACTIVE)") public ResponseEntity<ProductResponse> publishProduct(@PathVariable Integer id)`
- **GET `/api/admin/products/{id}`** → `AdminProductController.getProductById` [AdminProductController.java:90](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L90)
  - `@GetMapping("/{id}") @Operation(summary = "Xem chi tiết sản phẩm dành cho Admin (Bao gồm cả bản nháp)") public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id)`
- **PUT `/api/admin/products/{id}`** → `AdminProductController.updateProduct` [AdminProductController.java:96](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L96)
  - `@PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cập nhật thông tin chung sản phẩm") public ResponseEntity<ProductResponse> updateProduct( @PathVariable Integer id, @Valid @RequestBody UpdateProductRequest request)`
- **DELETE `/api/admin/products/{id}`** → `AdminProductController.deleteProduct` [AdminProductController.java:105](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L105)
  - `@DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Xóa sản phẩm (Chặn xóa nếu đã có biến thể trong đơn hàng)") public ResponseEntity<Void> deleteProduct(@PathVariable Integer id)`
- **GET `/api/admin/products`** → `AdminProductController.listProducts` [AdminProductController.java:113](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L113)
  - `@GetMapping @Operation(summary = "Danh sách sản phẩm (Mặc định lấy tất cả; hỗ trợ lọc theo DRAFT, ACTIVE, ARCHIVED)") public ResponseEntity<List<ProductResponse>> listProducts( @RequestParam(required = false) ProductStatus status)`
- **POST `/api/admin/specifications`** → `AdminSpecificationController.createSpecification` [AdminSpecificationController.java:28](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L28)
  - `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo thông số kỹ thuật mới (VD: Socket, Chuẩn RAM)") public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request)`
- **GET `/api/admin/specifications`** → `AdminSpecificationController.getAllSpecifications` [AdminSpecificationController.java:35](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L35)
  - `@GetMapping @Operation(summary = "Danh sách tất cả thông số kỹ thuật") public ResponseEntity<List<SpecificationResponse>> getAllSpecifications()`
- **GET `/api/admin/specifications/{id}`** → `AdminSpecificationController.getSpecificationById` [AdminSpecificationController.java:41](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L41)
  - `@GetMapping("/{id}") @Operation(summary = "Chi tiết thông số theo ID") public ResponseEntity<SpecificationResponse> getSpecificationById(@PathVariable Integer id)`
- **POST `/api/admin/specifications/{id}/options`** → `AdminSpecificationController.addOption` [AdminSpecificationController.java:47](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L47)
  - `@PostMapping("/{id}/options") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Thêm tùy chọn giá trị chuẩn hóa cho thông số (VD: DDR5, LGA1700)") public ResponseEntity<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse> addOption( @PathVariable Integer id, @Valid @RequestBody com.project.cybershop.catalog.api.dto.request.CreateSpecificationOptionRequest request)`
- **GET `/api/admin/specifications/{id}/options`** → `AdminSpecificationController.getOptions` [AdminSpecificationController.java:56](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L56)
  - `@GetMapping("/{id}/options") @Operation(summary = "Lấy danh sách các tùy chọn giá trị của một thông số") public ResponseEntity<List<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse>> getOptions( @PathVariable Integer id)`
- **GET `/api/products`** → `CatalogController.list` [CatalogController.java:20](../../src/main/java/com/project/cybershop/catalog/internal/controller/CatalogController.java#L20)
  - `@GetMapping public List<CatalogProductResponse> list()`
- **GET `/api/catalog/products`** → `StorefrontCatalogController.getActiveProducts` [StorefrontCatalogController.java:28](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L28)
  - `@GetMapping("/products") @Operation(summary = "Lấy danh sách các sản phẩm đang được kinh doanh (status = ACTIVE)") public ResponseEntity<List<ProductResponse>> getActiveProducts()`
- **GET `/api/catalog/products/{id}`** → `StorefrontCatalogController.getProductDetail` [StorefrontCatalogController.java:34](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L34)
  - `@GetMapping("/products/{id}") @Operation(summary = "Xem chi tiết sản phẩm và các phiên bản đang bán") public ResponseEntity<ProductResponse> getProductDetail(@PathVariable Integer id)`
- **GET `/api/catalog/products/{id}/variants`** → `StorefrontCatalogController.getProductVariants` [StorefrontCatalogController.java:40](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L40)
  - `@GetMapping("/products/{id}/variants") @Operation(summary = "Lấy danh sách các biến thể của sản phẩm đang bán") public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Integer id)`
- **GET `/api/catalog/products/{id}/variants/{variantId}`** → `StorefrontCatalogController.getVariantDetail` [StorefrontCatalogController.java:46](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L46)
  - `@GetMapping("/products/{id}/variants/{variantId}") @Operation(summary = "Xem chi tiết một biến thể đang bán") public ResponseEntity<ProductVariantResponse> getVariantDetail( @PathVariable Integer id, @PathVariable Integer variantId)`
- **GET `/api/catalog/categories`** → `StorefrontCatalogController.getStorefrontCategories` [StorefrontCatalogController.java:54](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L54)
  - `@GetMapping("/categories") @Operation(summary = "Lấy menu danh mục sản phẩm (Mega menu kèm cấu hình thông số)") public ResponseEntity<List<CategoryResponse>> getStorefrontCategories()`

## Khai báo và lời gọi theo file

## `catalog/api/CatalogInternalApi.java`

### CatalogInternalApi — [CatalogInternalApi.java:9](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L9)

Khai báo: `public interface CatalogInternalApi`

- [CatalogInternalApi.java:10](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L10) `boolean isVariantInStock(Integer variantId, int requestedQty)`
- [CatalogInternalApi.java:11](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L11) `void deductStock(Integer variantId, int quantity)`
- [CatalogInternalApi.java:12](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L12) `void restoreStock(Integer variantId, int quantity)`
- [CatalogInternalApi.java:13](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L13) `ProductVariantResponse getVariantDetail(Integer variantId)`
- [CatalogInternalApi.java:15](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L15) `void lockVariantsForPurchase(List<Integer> variantIds)`
- [CatalogInternalApi.java:17](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L17) `PurchaseVariantResponse getPurchaseVariant(Integer variantId)`
- [CatalogInternalApi.java:19](../../src/main/java/com/project/cybershop/catalog/api/CatalogInternalApi.java#L19) `Optional<PurchaseVariantResponse> findActiveVariant(Integer variantId)`

## `catalog/api/CatalogPublicApi.java`

### CatalogPublicApi — [CatalogPublicApi.java:9](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L9)

Khai báo: `public interface CatalogPublicApi`

- [CatalogPublicApi.java:10](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L10) `List<ComponentCandidateDTO> getActiveCandidatesForBuild()`
- [CatalogPublicApi.java:13](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L13) `Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds)`
- [CatalogPublicApi.java:16](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L16) `List<ComponentCandidateDTO> getCandidatesByIds(List<Integer> variantIds)`
- [CatalogPublicApi.java:19](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L19) `List<CategorySpecDto> getSpecificationCatalog()`
- [CatalogPublicApi.java:21](../../src/main/java/com/project/cybershop/catalog/api/CatalogPublicApi.java#L21) `List<String> getAllBrandNames()`

## `catalog/api/dto/CategorySpecDto.java`

### CategorySpecDto — [CategorySpecDto.java:15](../../src/main/java/com/project/cybershop/catalog/api/dto/CategorySpecDto.java#L15)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class CategorySpecDto`

- Trường: `private Integer categoryId;`
- Trường: `private String categorySlug;`
- Trường: `private String categoryName;`
- Trường: `private Integer specificationId;`
- Trường: `private String specificationName;`
- Trường: `private boolean compatibility;`
- Trường: `private boolean primary;`
- Trường: `private boolean filterable;`
- Trường: `@Builder.Default private List<Option> options = new ArrayList<>();`

### Option — [CategorySpecDto.java:32](../../src/main/java/com/project/cybershop/catalog/api/dto/CategorySpecDto.java#L32)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class Option`

- Trường: `private String code;`
- Trường: `private String displayValue;`

## `catalog/api/dto/ComponentCandidateDTO.java`

### ComponentCandidateDTO — [ComponentCandidateDTO.java:12](../../src/main/java/com/project/cybershop/catalog/api/dto/ComponentCandidateDTO.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ComponentCandidateDTO`

- Trường: `private Integer variantId;`
- Trường: `private Integer productId;`
- Trường: `private Integer brandId;`
- Trường: `private String brandName;`
- Trường: `private Integer categoryId;`
- Trường: `private String categorySlug;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private BigDecimal price;`
- Trường: `private Integer stockQuantity;`
- Trường: `private String imageUrl;`
- Trường: `@Builder.Default private int quantity = 1;`
- Trường: `@Builder.Default private Map<String, String> specs = new HashMap<>();`
- Trường: `@Builder.Default private Map<Integer, String> specValuesById = new HashMap<>();`
- [ComponentCandidateDTO.java:38](../../src/main/java/com/project/cybershop/catalog/api/dto/ComponentCandidateDTO.java#L38) `public String getSpec(String key)`
  - Lời gọi xuất hiện: `specs.get()`
- [ComponentCandidateDTO.java:42](../../src/main/java/com/project/cybershop/catalog/api/dto/ComponentCandidateDTO.java#L42) `public String getSpecOrDefault(String key, String defaultValue)`
  - Lời gọi xuất hiện: `specs.getOrDefault()`
- [ComponentCandidateDTO.java:46](../../src/main/java/com/project/cybershop/catalog/api/dto/ComponentCandidateDTO.java#L46) `public String getSpecById(Integer specId)`
  - Lời gọi xuất hiện: `specValuesById.get()`
- [ComponentCandidateDTO.java:50](../../src/main/java/com/project/cybershop/catalog/api/dto/ComponentCandidateDTO.java#L50) `public String getSpecByIdOrDefault(Integer specId, String defaultValue)`
  - Lời gọi xuất hiện: `specValuesById.getOrDefault()`

## `catalog/api/dto/request/AttributeValueInputDto.java`

### AttributeValueInputDto — [AttributeValueInputDto.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/AttributeValueInputDto.java#L5)

Khai báo: `public record AttributeValueInputDto( @NotNull(message = "specificationId không được để trống") Integer specificationId, Integer optionId, String value )`


## `catalog/api/dto/request/ConfigureCategorySpecRequest.java`

### ConfigureCategorySpecRequest — [ConfigureCategorySpecRequest.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/ConfigureCategorySpecRequest.java#L5)

Khai báo: `public record ConfigureCategorySpecRequest( @NotNull(message = "specificationId không được để trống") Integer specificationId, Boolean isFilterable, Boolean isPrimary, Boolean showInMenu, Boolean isCompatibility )`


## `catalog/api/dto/request/CreateBrandRequest.java`

### CreateBrandRequest — [CreateBrandRequest.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateBrandRequest.java#L5)

Khai báo: `public record CreateBrandRequest( @NotBlank(message = "Tên thương hiệu không được để trống") String name, String logoUrl, String description )`


## `catalog/api/dto/request/CreateCategoryRequest.java`

### CreateCategoryRequest — [CreateCategoryRequest.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateCategoryRequest.java#L5)

Khai báo: `public record CreateCategoryRequest( @NotBlank(message = "Tên danh mục không được để trống") String name, String slug, Integer parentId, String description )`


## `catalog/api/dto/request/CreateProductRequest.java`

### CreateProductRequest — [CreateProductRequest.java:10](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateProductRequest.java#L10)

Khai báo: `public record CreateProductRequest( @NotBlank(message = "Tên sản phẩm không được để trống") String name, @NotNull(message = "brandId không được để trống") Integer brandId, @NotNull(message = "categoryId không được để trống") Integer categoryId, @NotNull(message = "warrantyPeriodMonths không được để trống") @Min(value = 0, message = "Thời gian bảo hành không được âm") Integer warrantyPeriodMonths, String description, @Valid List<CreateVariantDto> variants )`


## `catalog/api/dto/request/CreateSpecificationOptionRequest.java`

### CreateSpecificationOptionRequest — [CreateSpecificationOptionRequest.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateSpecificationOptionRequest.java#L5)

Khai báo: `public record CreateSpecificationOptionRequest( @NotBlank(message = "Mã giá trị (code) không được để trống") String code, @NotBlank(message = "Tên hiển thị (displayValue) không được để trống") String displayValue )`


## `catalog/api/dto/request/CreateSpecificationRequest.java`

### CreateSpecificationRequest — [CreateSpecificationRequest.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateSpecificationRequest.java#L5)

Khai báo: `public record CreateSpecificationRequest( @NotBlank(message = "Tên thông số kỹ thuật không được để trống") String name )`


## `catalog/api/dto/request/CreateVariantDto.java`

### CreateVariantDto — [CreateVariantDto.java:11](../../src/main/java/com/project/cybershop/catalog/api/dto/request/CreateVariantDto.java#L11)

Khai báo: `public record CreateVariantDto( @NotBlank(message = "Mã SKU (serial_code) không được để trống") String serialCode, String variantName, String slug, @NotNull(message = "Giá bán không được để trống") @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0") BigDecimal price, BigDecimal originalPrice, @Min(value = 0, message = "Số lượng tồn kho không được âm") Integer stockQuantity, String imageUrl, List<AttributeValueInputDto> attributes )`


## `catalog/api/dto/request/UpdateProductRequest.java`

### UpdateProductRequest — [UpdateProductRequest.java:8](../../src/main/java/com/project/cybershop/catalog/api/dto/request/UpdateProductRequest.java#L8)

Khai báo: `public record UpdateProductRequest( @NotBlank(message = "Tên sản phẩm không được để trống") String name, @NotNull(message = "brandId không được để trống") Integer brandId, @NotNull(message = "categoryId không được để trống") Integer categoryId, @NotNull(message = "warrantyPeriodMonths không được để trống") @Min(value = 0, message = "Thời gian bảo hành không được âm") Integer warrantyPeriodMonths, String description, ProductStatus status )`


## `catalog/api/dto/request/UpdateVariantRequest.java`

### UpdateVariantRequest — [UpdateVariantRequest.java:11](../../src/main/java/com/project/cybershop/catalog/api/dto/request/UpdateVariantRequest.java#L11)

Khai báo: `public record UpdateVariantRequest( @NotBlank(message = "Mã SKU (serial_code) không được để trống") String serialCode, String variantName, String slug, @NotNull(message = "Giá bán không được để trống") @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0") BigDecimal price, BigDecimal originalPrice, @NotNull(message = "Số lượng tồn kho không được để trống") @Min(value = 0, message = "Số lượng tồn kho không được âm") Integer stockQuantity, String imageUrl, List<AttributeValueInputDto> attributes )`


## `catalog/api/dto/response/BrandResponse.java`

### BrandResponse — [BrandResponse.java:3](../../src/main/java/com/project/cybershop/catalog/api/dto/response/BrandResponse.java#L3)

Khai báo: `public record BrandResponse( Integer id, String name, String logoUrl, String description )`


## `catalog/api/dto/response/CatalogProductResponse.java`

### CatalogProductResponse — [CatalogProductResponse.java:8](../../src/main/java/com/project/cybershop/catalog/api/dto/response/CatalogProductResponse.java#L8)

Khai báo: `public record CatalogProductResponse( String id, String productId, String name, String category, String categoryName, String brand, String sku, String variant, BigDecimal price, BigDecimal originalPrice, int stock, int warranty, String image, String description, String status, Map<String, String> specs, List<String> primarySpecs, List<String> filterSpecs, BigDecimal averageRating, long reviewCount, long buyerCount )`


## `catalog/api/dto/response/CategoryResponse.java`

### CategoryResponse — [CategoryResponse.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/response/CategoryResponse.java#L5)

Khai báo: `public record CategoryResponse( Integer id, String name, String slug, Integer parentId, String description, List<CategoryResponse> children, List<CategorySpecResponse> specifications )`


## `catalog/api/dto/response/CategorySpecResponse.java`

### CategorySpecResponse — [CategorySpecResponse.java:3](../../src/main/java/com/project/cybershop/catalog/api/dto/response/CategorySpecResponse.java#L3)

Khai báo: `public record CategorySpecResponse( Integer id, Integer specificationId, String specificationName, Boolean isFilterable, Boolean isPrimary, Boolean showInMenu, Boolean isCompatibility )`


## `catalog/api/dto/response/ProductResponse.java`

### ProductResponse — [ProductResponse.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/response/ProductResponse.java#L5)

Khai báo: `public record ProductResponse( Integer id, String name, Integer brandId, String brandName, Integer categoryId, String categoryName, Integer warrantyPeriodMonths, String description, String status, List<ProductVariantResponse> variants )`


## `catalog/api/dto/response/ProductVariantResponse.java`

### ProductVariantResponse — [ProductVariantResponse.java:6](../../src/main/java/com/project/cybershop/catalog/api/dto/response/ProductVariantResponse.java#L6)

Khai báo: `public record ProductVariantResponse( Integer id, Integer productId, String serialCode, String variantName, String slug, BigDecimal price, BigDecimal originalPrice, Integer stockQuantity, String imageUrl, List<SpecificationValueResponse> attributes )`


## `catalog/api/dto/response/PurchaseVariantResponse.java`

### PurchaseVariantResponse — [PurchaseVariantResponse.java:5](../../src/main/java/com/project/cybershop/catalog/api/dto/response/PurchaseVariantResponse.java#L5)

Khai báo: `public record PurchaseVariantResponse( Integer variantId, String productName, String variantName, BigDecimal price, Integer warrantyPeriodMonths, String status, Integer stockQuantity, String imageUrl )`


## `catalog/api/dto/response/SpecificationOptionResponse.java`

### SpecificationOptionResponse — [SpecificationOptionResponse.java:3](../../src/main/java/com/project/cybershop/catalog/api/dto/response/SpecificationOptionResponse.java#L3)

Khai báo: `public record SpecificationOptionResponse( Integer id, Integer specificationId, String code, String displayValue )`


## `catalog/api/dto/response/SpecificationResponse.java`

### SpecificationResponse — [SpecificationResponse.java:3](../../src/main/java/com/project/cybershop/catalog/api/dto/response/SpecificationResponse.java#L3)

Khai báo: `public record SpecificationResponse( Integer id, String name )`


## `catalog/api/dto/response/SpecificationValueResponse.java`

### SpecificationValueResponse — [SpecificationValueResponse.java:3](../../src/main/java/com/project/cybershop/catalog/api/dto/response/SpecificationValueResponse.java#L3)

Khai báo: `public record SpecificationValueResponse( Integer id, Integer specificationId, String specificationName, Integer optionId, String optionCode, String value )`


## `catalog/internal/controller/AdminBrandController.java`

### AdminBrandController — [AdminBrandController.java:17](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L17)

Khai báo: `@RestController @RequestMapping("/api/admin/brands") @RequiredArgsConstructor @Tag(name = "Admin - Brands", description = "Quản lý thương hiệu sản phẩm") public class AdminBrandController`

- Trường: `private final BrandService brandService;`
- [AdminBrandController.java:25](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L25) `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo thương hiệu mới") public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `brandService.createBrand()`
- [AdminBrandController.java:32](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L32) `@GetMapping @Operation(summary = "Danh sách tất cả thương hiệu") public ResponseEntity<List<BrandResponse>> getAllBrands()`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `brandService.getAllBrands()`
- [AdminBrandController.java:38](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminBrandController.java#L38) `@GetMapping("/{id}") @Operation(summary = "Chi tiết thương hiệu theo ID") public ResponseEntity<BrandResponse> getBrandById(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `brandService.getBrandById()`

## `catalog/internal/controller/AdminCategoryController.java`

### AdminCategoryController — [AdminCategoryController.java:19](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L19)

Khai báo: `@RestController @RequestMapping("/api/admin/categories") @RequiredArgsConstructor @Tag(name = "Admin - Categories", description = "Quản lý danh mục và cấu hình thông số kỹ thuật theo danh mục") public class AdminCategoryController`

- Trường: `private final CategoryService categoryService;`
- [AdminCategoryController.java:27](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L27) `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo danh mục mới (hỗ trợ phân cấp cha/con)") public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `categoryService.createCategory()`
- [AdminCategoryController.java:34](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L34) `@PostMapping("/{id}/specifications") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cấu hình thông số kỹ thuật cho danh mục (bộ lọc, cấu hình nổi bật, mega menu)") public ResponseEntity<List<CategorySpecResponse>> configureCategorySpecifications( @PathVariable Integer id, @Valid @RequestBody List<ConfigureCategorySpecRequest> requests)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `categoryService.configureCategorySpecifications()`
- [AdminCategoryController.java:43](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L43) `@GetMapping @Operation(summary = "Lấy cây danh mục sản phẩm") public ResponseEntity<List<CategoryResponse>> getCategoryTree()`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `categoryService.getCategoryTree()`
- [AdminCategoryController.java:49](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminCategoryController.java#L49) `@GetMapping("/{id}") @Operation(summary = "Chi tiết danh mục theo ID") public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `categoryService.getCategoryById()`

## `catalog/internal/controller/AdminProductController.java`

### AdminProductController — [AdminProductController.java:22](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L22)

Khai báo: `@RestController @RequestMapping("/api/admin/products") @Tag(name = "Admin - Products", description = "Quản lý sản phẩm: tạo bản nháp, thêm biến thể SKU, gán thông số EAV và đăng bán") public class AdminProductController`

- Trường: `private final ProductService productService;`
- [AdminProductController.java:29](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L29) `public AdminProductController(ProductService productService)`
- [AdminProductController.java:33](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L33) `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo sản phẩm mới (Lưu bản nháp - DRAFT, kèm biến thể ban đầu nếu có)") public ResponseEntity<ProductResponse> createProductDraft(@Valid @RequestBody CreateProductRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `productService.createProductDraft()`
- [AdminProductController.java:40](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L40) `@PostMapping("/{id}/variants") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Thêm biến thể / SKU mới vào sản phẩm đã có kèm thông số kỹ thuật") public ResponseEntity<ProductVariantResponse> addVariant( @PathVariable Integer id, @Valid @RequestBody CreateVariantDto variantDto)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `productService.addVariant()`
- [AdminProductController.java:49](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L49) `@GetMapping("/{id}/variants") @Operation(summary = "Lấy danh sách các biến thể của sản phẩm") public ResponseEntity<List<ProductVariantResponse>> getVariants(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getVariantsByProductId()`
- [AdminProductController.java:55](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L55) `@GetMapping("/{id}/variants/{variantId}") @Operation(summary = "Xem chi tiết một biến thể cụ thể của sản phẩm") public ResponseEntity<ProductVariantResponse> getVariantDetail( @PathVariable Integer id, @PathVariable Integer variantId)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getVariantById()`
- [AdminProductController.java:63](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L63) `@PutMapping("/{id}/variants/{variantId}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cập nhật biến thể sản phẩm (SKU, giá bán, giá gốc, tồn kho, ảnh, thông số kỹ thuật)") public ResponseEntity<ProductVariantResponse> updateVariant( @PathVariable Integer id, @PathVariable Integer variantId, @Valid @RequestBody UpdateVariantRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.updateVariant()`
- [AdminProductController.java:73](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L73) `@DeleteMapping("/{id}/variants/{variantId}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Xóa biến thể sản phẩm (Chặn xóa nếu đã phát sinh đơn hàng)") public ResponseEntity<Void> deleteVariant( @PathVariable Integer id, @PathVariable Integer variantId)`
  - Lời gọi xuất hiện: `productService.deleteVariant()`, `ResponseEntity.noContent().build()`, `ResponseEntity.noContent()`
- [AdminProductController.java:83](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L83) `@PutMapping("/{id}/publish") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Kiểm tra tính hợp lệ và Đăng bán sản phẩm (Chuyển DRAFT -> ACTIVE)") public ResponseEntity<ProductResponse> publishProduct(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.publishProduct()`
- [AdminProductController.java:90](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L90) `@GetMapping("/{id}") @Operation(summary = "Xem chi tiết sản phẩm dành cho Admin (Bao gồm cả bản nháp)") public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getProductById()`
- [AdminProductController.java:96](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L96) `@PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Cập nhật thông tin chung sản phẩm") public ResponseEntity<ProductResponse> updateProduct( @PathVariable Integer id, @Valid @RequestBody UpdateProductRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.updateProduct()`
- [AdminProductController.java:105](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L105) `@DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Xóa sản phẩm (Chặn xóa nếu đã có biến thể trong đơn hàng)") public ResponseEntity<Void> deleteProduct(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `productService.deleteProduct()`, `ResponseEntity.noContent().build()`, `ResponseEntity.noContent()`
- [AdminProductController.java:113](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminProductController.java#L113) `@GetMapping @Operation(summary = "Danh sách sản phẩm (Mặc định lấy tất cả; hỗ trợ lọc theo DRAFT, ACTIVE, ARCHIVED)") public ResponseEntity<List<ProductResponse>> listProducts( @RequestParam(required = false) ProductStatus status)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.listProductsByStatus()`, `productService.listAllProducts()`

## `catalog/internal/controller/AdminSpecificationController.java`

### AdminSpecificationController — [AdminSpecificationController.java:17](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L17)

Khai báo: `@RestController @RequestMapping("/api/admin/specifications") @Tag(name = "Admin - Specifications", description = "Quản lý từ điển thông số kỹ thuật toàn cục") public class AdminSpecificationController`

- Trường: `private final SpecificationService specificationService;`
- [AdminSpecificationController.java:24](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L24) `public AdminSpecificationController(SpecificationService specificationService)`
- [AdminSpecificationController.java:28](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L28) `@PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Tạo thông số kỹ thuật mới (VD: Socket, Chuẩn RAM)") public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `specificationService.createSpecification()`
- [AdminSpecificationController.java:35](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L35) `@GetMapping @Operation(summary = "Danh sách tất cả thông số kỹ thuật") public ResponseEntity<List<SpecificationResponse>> getAllSpecifications()`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `specificationService.getAllSpecifications()`
- [AdminSpecificationController.java:41](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L41) `@GetMapping("/{id}") @Operation(summary = "Chi tiết thông số theo ID") public ResponseEntity<SpecificationResponse> getSpecificationById(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `specificationService.getSpecificationById()`
- [AdminSpecificationController.java:47](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L47) `@PostMapping("/{id}/options") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Thêm tùy chọn giá trị chuẩn hóa cho thông số (VD: DDR5, LGA1700)") public ResponseEntity<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse> addOption( @PathVariable Integer id, @Valid @RequestBody com.project.cybershop.catalog.api.dto.request.CreateSpecificationOptionRequest request)`
  - Lời gọi xuất hiện: `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `specificationService.addOption()`
- [AdminSpecificationController.java:56](../../src/main/java/com/project/cybershop/catalog/internal/controller/AdminSpecificationController.java#L56) `@GetMapping("/{id}/options") @Operation(summary = "Lấy danh sách các tùy chọn giá trị của một thông số") public ResponseEntity<List<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse>> getOptions( @PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `specificationService.getOptionsBySpecificationId()`

## `catalog/internal/controller/CatalogController.java`

### CatalogController — [CatalogController.java:11](../../src/main/java/com/project/cybershop/catalog/internal/controller/CatalogController.java#L11)

Khai báo: `@RestController @RequestMapping("/api/products") public class CatalogController`

- Trường: `private final CatalogService service;`
- [CatalogController.java:16](../../src/main/java/com/project/cybershop/catalog/internal/controller/CatalogController.java#L16) `public CatalogController(CatalogService service)`
- [CatalogController.java:20](../../src/main/java/com/project/cybershop/catalog/internal/controller/CatalogController.java#L20) `@GetMapping public List<CatalogProductResponse> list()`
  - Lời gọi xuất hiện: `service.list()`

## `catalog/internal/controller/StorefrontCatalogController.java`

### StorefrontCatalogController — [StorefrontCatalogController.java:15](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L15)

Khai báo: `@RestController @RequestMapping("/api/catalog") @Tag(name = "Storefront - Catalog", description = "Các API công khai cho khách hàng duyệt sản phẩm và danh mục") public class StorefrontCatalogController`

- Trường: `private final ProductService productService;`
- Trường: `private final CategoryService categoryService;`
- [StorefrontCatalogController.java:23](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L23) `public StorefrontCatalogController(ProductService productService, CategoryService categoryService)`
- [StorefrontCatalogController.java:28](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L28) `@GetMapping("/products") @Operation(summary = "Lấy danh sách các sản phẩm đang được kinh doanh (status = ACTIVE)") public ResponseEntity<List<ProductResponse>> getActiveProducts()`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.listActiveProducts()`
- [StorefrontCatalogController.java:34](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L34) `@GetMapping("/products/{id}") @Operation(summary = "Xem chi tiết sản phẩm và các phiên bản đang bán") public ResponseEntity<ProductResponse> getProductDetail(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getProductById()`
- [StorefrontCatalogController.java:40](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L40) `@GetMapping("/products/{id}/variants") @Operation(summary = "Lấy danh sách các biến thể của sản phẩm đang bán") public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Integer id)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getVariantsByProductId()`
- [StorefrontCatalogController.java:46](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L46) `@GetMapping("/products/{id}/variants/{variantId}") @Operation(summary = "Xem chi tiết một biến thể đang bán") public ResponseEntity<ProductVariantResponse> getVariantDetail( @PathVariable Integer id, @PathVariable Integer variantId)`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `productService.getVariantById()`
- [StorefrontCatalogController.java:54](../../src/main/java/com/project/cybershop/catalog/internal/controller/StorefrontCatalogController.java#L54) `@GetMapping("/categories") @Operation(summary = "Lấy menu danh mục sản phẩm (Mega menu kèm cấu hình thông số)") public ResponseEntity<List<CategoryResponse>> getStorefrontCategories()`
  - Lời gọi xuất hiện: `ResponseEntity.ok()`, `categoryService.getCategoryTree()`

## `catalog/internal/entity/Brand.java`

### Brand — [Brand.java:8](../../src/main/java/com/project/cybershop/catalog/internal/entity/Brand.java#L8)

Khai báo: `@Entity @Table(name = "brands") @Getter @Setter @NoArgsConstructor public class Brand`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "name", nullable = false, length = 100) private String name;`
- Trường: `@Column(name = "logo_url", length = 255) private String logoUrl;`
- Trường: `@Column(name = "description", columnDefinition = "TEXT") private String description;`
- [Brand.java:28](../../src/main/java/com/project/cybershop/catalog/internal/entity/Brand.java#L28) `public Brand(String name, String logoUrl, String description)`

## `catalog/internal/entity/Category.java`

### Category — [Category.java:11](../../src/main/java/com/project/cybershop/catalog/internal/entity/Category.java#L11)

Khai báo: `@Entity @Table(name = "categories") @Getter @Setter @NoArgsConstructor public class Category`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "name", nullable = false, length = 100) private String name;`
- Trường: `@Column(name = "slug", unique = true, length = 150) private String slug;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Category parent;`
- Trường: `@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL) private List<Category> children = new ArrayList<>();`
- Trường: `@Column(name = "description", columnDefinition = "TEXT") private String description;`
- Trường: `@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true) private List<CategorySpecification> categorySpecifications = new ArrayList<>();`
- [Category.java:41](../../src/main/java/com/project/cybershop/catalog/internal/entity/Category.java#L41) `public Category(String name, String slug, Category parent, String description)`

## `catalog/internal/entity/CategorySpecification.java`

### CategorySpecification — [CategorySpecification.java:8](../../src/main/java/com/project/cybershop/catalog/internal/entity/CategorySpecification.java#L8)

Khai báo: `@Entity @Table(name = "category_specifications", uniqueConstraints =`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "specification_id", nullable = false) private Specification specification;`
- Trường: `@Column(name = "is_filterable") private Boolean isFilterable = false;`
- Trường: `@Column(name = "is_primary") private Boolean isPrimary = false;`
- Trường: `@Column(name = "show_in_menu") private Boolean showInMenu = false;`
- Trường: `@Column(name = "is_compatibility") private Boolean isCompatibility = false;`
- [CategorySpecification.java:41](../../src/main/java/com/project/cybershop/catalog/internal/entity/CategorySpecification.java#L41) `public CategorySpecification(Category category, Specification specification, Boolean isFilterable, Boolean isPrimary, Boolean showInMenu)`
- [CategorySpecification.java:45](../../src/main/java/com/project/cybershop/catalog/internal/entity/CategorySpecification.java#L45) `public CategorySpecification(Category category, Specification specification, Boolean isFilterable, Boolean isPrimary, Boolean showInMenu, Boolean isCompatibility)`

## `catalog/internal/entity/Product.java`

### Product — [Product.java:11](../../src/main/java/com/project/cybershop/catalog/internal/entity/Product.java#L11)

Khai báo: `@Entity @Table(name = "products") @Getter @Setter @NoArgsConstructor public class Product`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "brand_id", nullable = false) private Brand brand;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;`
- Trường: `@Column(name = "name", nullable = false, length = 255) private String name;`
- Trường: `@Column(name = "description", columnDefinition = "TEXT") private String description;`
- Trường: `@Column(name = "warranty_period_months", nullable = false) private Integer warrantyPeriodMonths;`
- Trường: `@Column(name = "status", length = 50) private ProductStatus status = ProductStatus.DRAFT;`
- Trường: `@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true) private List<ProductVariant> variants = new ArrayList<>();`
- [Product.java:45](../../src/main/java/com/project/cybershop/catalog/internal/entity/Product.java#L45) `public Product(String name, Brand brand, Category category, Integer warrantyPeriodMonths, String description)`
- [Product.java:54](../../src/main/java/com/project/cybershop/catalog/internal/entity/Product.java#L54) `public void addVariant(ProductVariant variant)`
  - Lời gọi xuất hiện: `variants.add()`, `variant.setProduct()`

## `catalog/internal/entity/ProductStatus.java`

### ProductStatus — [ProductStatus.java:3](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductStatus.java#L3)

Khai báo: `public enum ProductStatus`

- Trường: `DRAFT`
- Trường: `ACTIVE`
- Trường: `ARCHIVED`

## `catalog/internal/entity/ProductStatusConverter.java`

### ProductStatusConverter — [ProductStatusConverter.java:6](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductStatusConverter.java#L6)

Khai báo: `@Converter(autoApply = true) public class ProductStatusConverter implements AttributeConverter<ProductStatus, String>`

- [ProductStatusConverter.java:9](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductStatusConverter.java#L9) `@Override public String convertToDatabaseColumn(ProductStatus attribute)`
  - Lời gọi xuất hiện: `ProductStatus.DRAFT.name()`, `attribute.name()`
- [ProductStatusConverter.java:17](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductStatusConverter.java#L17) `@Override public ProductStatus convertToEntityAttribute(String dbData)`
  - Lời gọi xuất hiện: `dbData.isBlank()`, `ProductStatus.valueOf()`, `dbData.trim().toUpperCase()`, `dbData.trim()`

## `catalog/internal/entity/ProductVariant.java`

### ProductVariant — [ProductVariant.java:12](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductVariant.java#L12)

Khai báo: `@Entity @Table(name = "product_variants") @Getter @Setter @NoArgsConstructor public class ProductVariant`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private Product product;`
- Trường: `@Column(name = "serial_code", nullable = false, unique = true, length = 100) private String serialCode;`
- Trường: `@Column(name = "variant_name", length = 150) private String variantName;`
- Trường: `@Column(name = "slug", unique = true, length = 150) private String slug;`
- Trường: `@Column(name = "price", nullable = false, precision = 12, scale = 2) private BigDecimal price;`
- Trường: `@Column(name = "original_price", precision = 12, scale = 2) private BigDecimal originalPrice;`
- Trường: `@Column(name = "stock_quantity") private Integer stockQuantity = 0;`
- Trường: `@Column(name = "image_url", length = 255) private String imageUrl;`
- Trường: `@org.hibernate.annotations.BatchSize(size = 100) @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true) private List<SpecificationValue> attributeValues = new ArrayList<>();`
- [ProductVariant.java:52](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductVariant.java#L52) `public ProductVariant(Product product, String serialCode, String variantName, String slug, BigDecimal price, BigDecimal originalPrice, Integer stockQuantity, String imageUrl)`
- [ProductVariant.java:64](../../src/main/java/com/project/cybershop/catalog/internal/entity/ProductVariant.java#L64) `public void addAttributeValue(SpecificationValue value)`
  - Lời gọi xuất hiện: `attributeValues.add()`, `value.setProductVariant()`

## `catalog/internal/entity/Specification.java`

### Specification — [Specification.java:8](../../src/main/java/com/project/cybershop/catalog/internal/entity/Specification.java#L8)

Khai báo: `@Entity @Table(name = "specifications") @Getter @Setter @NoArgsConstructor public class Specification`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "name", nullable = false, length = 100) private String name;`
- [Specification.java:22](../../src/main/java/com/project/cybershop/catalog/internal/entity/Specification.java#L22) `public Specification(String name)`

## `catalog/internal/entity/SpecificationOption.java`

### SpecificationOption — [SpecificationOption.java:8](../../src/main/java/com/project/cybershop/catalog/internal/entity/SpecificationOption.java#L8)

Khai báo: `@Entity @Table(name = "specification_options", uniqueConstraints =`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "specification_id", nullable = false) private Specification specification;`
- Trường: `@Column(name = "code", nullable = false, length = 100) private String code;`
- Trường: `@Column(name = "display_value", nullable = false, length = 100) private String displayValue;`
- [SpecificationOption.java:31](../../src/main/java/com/project/cybershop/catalog/internal/entity/SpecificationOption.java#L31) `public SpecificationOption(Specification specification, String code, String displayValue)`

## `catalog/internal/entity/SpecificationValue.java`

### SpecificationValue — [SpecificationValue.java:8](../../src/main/java/com/project/cybershop/catalog/internal/entity/SpecificationValue.java#L8)

Khai báo: `@Entity @Table(name = "specification_values", uniqueConstraints =`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_variant_id", nullable = false) private ProductVariant productVariant;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "specification_id", nullable = false) private Specification specification;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "option_id") private SpecificationOption option;`
- Trường: `@Column(name = "value", columnDefinition = "TEXT") private String value;`
- [SpecificationValue.java:36](../../src/main/java/com/project/cybershop/catalog/internal/entity/SpecificationValue.java#L36) `public SpecificationValue(ProductVariant productVariant, Specification specification, String value)`
- [SpecificationValue.java:40](../../src/main/java/com/project/cybershop/catalog/internal/entity/SpecificationValue.java#L40) `public SpecificationValue(ProductVariant productVariant, Specification specification, SpecificationOption option, String value)`

## `catalog/internal/mapper/CatalogMapper.java`

### CatalogMapper — [CatalogMapper.java:15](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L15)

Khai báo: `@Component public class CatalogMapper`

- [CatalogMapper.java:18](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L18) `public BrandResponse toBrandResponse(Brand brand)`
  - Lời gọi xuất hiện: `brand.getId()`, `brand.getName()`, `brand.getLogoUrl()`, `brand.getDescription()`
- [CatalogMapper.java:23](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L23) `public SpecificationResponse toSpecResponse(Specification spec)`
  - Lời gọi xuất hiện: `spec.getId()`, `spec.getName()`
- [CatalogMapper.java:28](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L28) `public CategorySpecResponse toCategorySpecResponse(CategorySpecification cs)`
  - Lời gọi xuất hiện: `cs.getId()`, `cs.getSpecification().getId()`, `cs.getSpecification()`, `cs.getSpecification().getName()`, `cs.getIsFilterable()`, `cs.getIsPrimary()`, `cs.getShowInMenu()`, `cs.getIsCompatibility()`
- [CatalogMapper.java:41](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L41) `public com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse toSpecOptionResponse(SpecificationOption opt)`
  - Lời gọi xuất hiện: `opt.getId()`, `opt.getSpecification().getId()`, `opt.getSpecification()`, `opt.getCode()`, `opt.getDisplayValue()`
- [CatalogMapper.java:51](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L51) `public CategoryResponse toCategoryResponse(Category category)`
  - Lời gọi xuất hiện: `category.getChildren()`, `category.getChildren().stream().map(this::toCategoryResponse).toList()`, `category.getChildren().stream().map()`, `category.getChildren().stream()`, `List.of()`, `category.getCategorySpecifications()`, `category.getCategorySpecifications().stream().map(this::toCategorySpecResponse).toList()`, `category.getCategorySpecifications().stream().map()`, `category.getCategorySpecifications().stream()`, `category.getId()`, `category.getName()`, `category.getSlug()`, `category.getParent()`, `category.getParent().getId()`, `category.getDescription()`
- [CatalogMapper.java:73](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L73) `public SpecificationValueResponse toSpecValueResponse(SpecificationValue sv)`
  - Lời gọi xuất hiện: `sv.getOption()`, `sv.getOption().getId()`, `sv.getOption().getCode()`, `sv.getId()`, `sv.getSpecification().getId()`, `sv.getSpecification()`, `sv.getSpecification().getName()`, `sv.getValue()`
- [CatalogMapper.java:87](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L87) `public ProductVariantResponse toVariantResponse(ProductVariant variant)`
  - Lời gọi xuất hiện: `variant.getAttributeValues()`, `variant.getAttributeValues().stream().map(this::toSpecValueResponse).toList()`, `variant.getAttributeValues().stream().map()`, `variant.getAttributeValues().stream()`, `List.of()`, `variant.getId()`, `variant.getProduct()`, `variant.getProduct().getId()`, `variant.getSerialCode()`, `variant.getVariantName()`, `variant.getSlug()`, `variant.getPrice()`, `variant.getOriginalPrice()`, `variant.getStockQuantity()`, `variant.getImageUrl()`
- [CatalogMapper.java:108](../../src/main/java/com/project/cybershop/catalog/internal/mapper/CatalogMapper.java#L108) `public ProductResponse toProductResponse(Product product)`
  - Lời gọi xuất hiện: `product.getVariants()`, `product.getVariants().stream().map(this::toVariantResponse).toList()`, `product.getVariants().stream().map()`, `product.getVariants().stream()`, `List.of()`, `product.getId()`, `product.getName()`, `product.getBrand()`, `product.getBrand().getId()`, `product.getBrand().getName()`, `product.getCategory()`, `product.getCategory().getId()`, `product.getCategory().getName()`, `product.getWarrantyPeriodMonths()`, `product.getDescription()`, `product.getStatus()`, `product.getStatus().name()`

## `catalog/internal/repository/BrandRepository.java`

### BrandRepository — [BrandRepository.java:9](../../src/main/java/com/project/cybershop/catalog/internal/repository/BrandRepository.java#L9)

Khai báo: `@Repository public interface BrandRepository extends JpaRepository<Brand, Integer>`

- [BrandRepository.java:11](../../src/main/java/com/project/cybershop/catalog/internal/repository/BrandRepository.java#L11) `Optional<Brand> findByName(String name)`
- [BrandRepository.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/BrandRepository.java#L12) `boolean existsByName(String name)`

## `catalog/internal/repository/CategoryRepository.java`

### CategoryRepository — [CategoryRepository.java:11](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L11)

Khai báo: `@Repository public interface CategoryRepository extends JpaRepository<Category, Integer>`

- [CategoryRepository.java:13](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L13) `Optional<Category> findBySlug(String slug)`
- [CategoryRepository.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L14) `boolean existsBySlug(String slug)`
- [CategoryRepository.java:16](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L16) `List<Category> findByParentIsNull()`
- [CategoryRepository.java:17](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L17) `List<Category> findByParentId(Integer parentId)`
- [CategoryRepository.java:19](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategoryRepository.java#L19) `@Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL") List<Category> findAllRootCategoriesWithChildren()`

## `catalog/internal/repository/CategorySpecificationRepository.java`

### CategorySpecificationRepository — [CategorySpecificationRepository.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategorySpecificationRepository.java#L12)

Khai báo: `@Repository public interface CategorySpecificationRepository extends JpaRepository<CategorySpecification, Integer>`

- [CategorySpecificationRepository.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategorySpecificationRepository.java#L14) `List<CategorySpecification> findByCategoryId(Integer categoryId)`
- [CategorySpecificationRepository.java:16](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategorySpecificationRepository.java#L16) `@Query("SELECT cs FROM CategorySpecification cs JOIN FETCH cs.specification WHERE cs.category.id = :categoryId AND cs.isPrimary = true") List<CategorySpecification> findPrimarySpecsByCategoryId(@Param("categoryId") Integer categoryId)`
- [CategorySpecificationRepository.java:19](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategorySpecificationRepository.java#L19) `@Query("SELECT cs FROM CategorySpecification cs JOIN FETCH cs.specification WHERE cs.category.id = :categoryId AND cs.isFilterable = true") List<CategorySpecification> findFilterableSpecsByCategoryId(@Param("categoryId") Integer categoryId)`
- [CategorySpecificationRepository.java:22](../../src/main/java/com/project/cybershop/catalog/internal/repository/CategorySpecificationRepository.java#L22) `Optional<CategorySpecification> findByCategoryIdAndSpecificationId(Integer categoryId, Integer specificationId)`

## `catalog/internal/repository/ProductRepository.java`

### ProductRepository — [ProductRepository.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L14)

Khai báo: `@Repository public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product>`

- [ProductRepository.java:17](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L17) `@Query("SELECT DISTINCT p FROM Product p " + "LEFT JOIN FETCH p.variants v " + "LEFT JOIN FETCH p.brand " + "LEFT JOIN FETCH p.category " + "WHERE p.status = :status " + "ORDER BY p.id ASC") List<Product> findByStatusWithDetails(@Param("status") ProductStatus status)`
- [ProductRepository.java:25](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L25) `List<Product> findByStatus(ProductStatus status)`
- [ProductRepository.java:27](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L27) `List<Product> findByCategoryIdAndStatus(Integer categoryId, ProductStatus status)`
- [ProductRepository.java:29](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L29) `List<Product> findByBrandIdAndStatus(Integer brandId, ProductStatus status)`
- [ProductRepository.java:31](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L31) `@Query("SELECT DISTINCT p FROM Product p " + "LEFT JOIN FETCH p.variants v " + "LEFT JOIN FETCH p.brand " + "LEFT JOIN FETCH p.category " + "WHERE p.id = :id") Optional<Product> findWithDetailsById(@Param("id") Integer id)`
- [ProductRepository.java:38](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductRepository.java#L38) `@Query("SELECT DISTINCT p FROM Product p " + "LEFT JOIN FETCH p.variants v " + "LEFT JOIN FETCH p.brand " + "LEFT JOIN FETCH p.category " + "ORDER BY p.id ASC") List<Product> findAllWithDetails()`

## `catalog/internal/repository/ProductVariantRepository.java`

### ProductVariantRepository — [ProductVariantRepository.java:13](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L13)

Khai báo: `@Repository public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer>`

- [ProductVariantRepository.java:16](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L16) `@Query(value = """ SELECT pv.id AS "variantId", p.name AS "productName", pv.variant_name AS "variantName", pv.price AS "price", p.warranty_period_months AS "warrantyPeriodMonths", p.status AS "status", pv.stock_quantity AS "stockQuantity", pv.image_url AS "imageUrl" FROM product_variants pv JOIN products p ON p.id = pv.product_id WHERE pv.id = :variantId """, nativeQuery = true) Optional<PurchaseVariantProjection> findPurchaseVariant(@Param("variantId") Integer variantId)`
- [ProductVariantRepository.java:28](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L28) `@Query(value = """ SELECT pv.id FROM product_variants pv JOIN products p ON p.id = pv.product_id WHERE pv.id = :variantId FOR UPDATE """, nativeQuery = true) Optional<Integer> lockForPurchase(@Param("variantId") Integer variantId)`
- [ProductVariantRepository.java:35](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L35) `@Modifying(flushAutomatically = true) @Query(value = """ UPDATE product_variants SET stock_quantity = stock_quantity - :quantity WHERE id = :variantId AND stock_quantity >= :quantity """, nativeQuery = true) int deductAvailableStock(@Param("variantId") Integer variantId, @Param("quantity") int quantity)`
- [ProductVariantRepository.java:42](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L42) `@Modifying(flushAutomatically = true) @Query(value = """ UPDATE product_variants SET stock_quantity = coalesce(stock_quantity, 0) + :quantity WHERE id = :variantId """, nativeQuery = true) int restoreAvailableStock(@Param("variantId") Integer variantId, @Param("quantity") int quantity)`
- [ProductVariantRepository.java:49](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L49) `Optional<ProductVariant> findBySerialCode(String serialCode)`
- [ProductVariantRepository.java:50](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L50) `boolean existsBySerialCode(String serialCode)`
- [ProductVariantRepository.java:52](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L52) `Optional<ProductVariant> findBySlug(String slug)`
- [ProductVariantRepository.java:53](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L53) `boolean existsBySlug(String slug)`
- [ProductVariantRepository.java:55](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L55) `List<ProductVariant> findByProductId(Integer productId)`
- [ProductVariantRepository.java:57](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L57) `@Query("SELECT DISTINCT pv FROM ProductVariant pv " + "LEFT JOIN FETCH pv.attributeValues av " + "LEFT JOIN FETCH av.specification " + "LEFT JOIN FETCH av.option " + "WHERE pv.id = :id") Optional<ProductVariant> findWithAttributesById(@Param("id") Integer id)`
- [ProductVariantRepository.java:64](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L64) `@Query("SELECT DISTINCT pv FROM ProductVariant pv " + "LEFT JOIN FETCH pv.attributeValues av " + "LEFT JOIN FETCH av.specification " + "LEFT JOIN FETCH av.option " + "WHERE pv.product.id = :productId") List<ProductVariant> findWithAttributesByProductId(@Param("productId") Integer productId)`
- [ProductVariantRepository.java:70](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L70) `@Query("SELECT pv FROM ProductVariant pv " + "JOIN FETCH pv.product p " + "JOIN FETCH p.category c " + "LEFT JOIN FETCH p.brand b " + "WHERE p.status = com.project.cybershop.catalog.internal.entity.ProductStatus.ACTIVE AND pv.stockQuantity > 0") List<ProductVariant> findAllActiveInStock()`
- [ProductVariantRepository.java:77](../../src/main/java/com/project/cybershop/catalog/internal/repository/ProductVariantRepository.java#L77) `@Query("SELECT pv FROM ProductVariant pv " + "JOIN FETCH pv.product p " + "JOIN FETCH p.category c " + "WHERE p.status = com.project.cybershop.catalog.internal.entity.ProductStatus.ACTIVE AND pv.stockQuantity > 0 AND c.id = :categoryId") List<ProductVariant> findActiveByCategoryId(@Param("categoryId") Integer categoryId)`

## `catalog/internal/repository/PurchaseVariantProjection.java`

### PurchaseVariantProjection — [PurchaseVariantProjection.java:5](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L5)

Khai báo: `public interface PurchaseVariantProjection`

- [PurchaseVariantProjection.java:7](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L7) `Integer getVariantId()`
- [PurchaseVariantProjection.java:8](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L8) `String getProductName()`
- [PurchaseVariantProjection.java:9](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L9) `String getVariantName()`
- [PurchaseVariantProjection.java:10](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L10) `BigDecimal getPrice()`
- [PurchaseVariantProjection.java:11](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L11) `Integer getWarrantyPeriodMonths()`
- [PurchaseVariantProjection.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L12) `String getStatus()`
- [PurchaseVariantProjection.java:13](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L13) `Integer getStockQuantity()`
- [PurchaseVariantProjection.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/PurchaseVariantProjection.java#L14) `String getImageUrl()`

## `catalog/internal/repository/SpecificationOptionRepository.java`

### SpecificationOptionRepository — [SpecificationOptionRepository.java:10](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationOptionRepository.java#L10)

Khai báo: `@Repository public interface SpecificationOptionRepository extends JpaRepository<SpecificationOption, Integer>`

- [SpecificationOptionRepository.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationOptionRepository.java#L12) `List<SpecificationOption> findBySpecificationId(Integer specificationId)`
- [SpecificationOptionRepository.java:13](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationOptionRepository.java#L13) `Optional<SpecificationOption> findBySpecificationIdAndCode(Integer specificationId, String code)`
- [SpecificationOptionRepository.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationOptionRepository.java#L14) `boolean existsBySpecificationIdAndCode(Integer specificationId, String code)`

## `catalog/internal/repository/SpecificationRepository.java`

### SpecificationRepository — [SpecificationRepository.java:9](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationRepository.java#L9)

Khai báo: `@Repository public interface SpecificationRepository extends JpaRepository<Specification, Integer>`

- [SpecificationRepository.java:11](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationRepository.java#L11) `Optional<Specification> findByName(String name)`
- [SpecificationRepository.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationRepository.java#L12) `boolean existsByName(String name)`

## `catalog/internal/repository/SpecificationValueRepository.java`

### SpecificationValueRepository — [SpecificationValueRepository.java:12](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L12)

Khai báo: `@Repository public interface SpecificationValueRepository extends JpaRepository<SpecificationValue, Integer>`

- [SpecificationValueRepository.java:14](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L14) `List<SpecificationValue> findByProductVariantId(Integer variantId)`
- [SpecificationValueRepository.java:16](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L16) `@Query("SELECT sv FROM SpecificationValue sv JOIN FETCH sv.specification WHERE sv.productVariant.id = :variantId") List<SpecificationValue> findWithSpecByVariantId(@Param("variantId") Integer variantId)`
- [SpecificationValueRepository.java:19](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L19) `Optional<SpecificationValue> findByProductVariantIdAndSpecificationId(Integer variantId, Integer specificationId)`
- [SpecificationValueRepository.java:21](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L21) `void deleteByProductVariantId(Integer variantId)`
- [SpecificationValueRepository.java:23](../../src/main/java/com/project/cybershop/catalog/internal/repository/SpecificationValueRepository.java#L23) `@Query("SELECT sv FROM SpecificationValue sv " + "JOIN FETCH sv.specification s " + "WHERE sv.productVariant.id IN :variantIds") List<SpecificationValue> findByProductVariantIdIn(@Param("variantIds") List<Integer> variantIds)`

## `catalog/internal/service/BrandService.java`

### BrandService — [BrandService.java:16](../../src/main/java/com/project/cybershop/catalog/internal/service/BrandService.java#L16)

Khai báo: `@Service @RequiredArgsConstructor public class BrandService`

- Trường: `private final BrandRepository brandRepository;`
- Trường: `private final CatalogMapper catalogMapper;`
- [BrandService.java:23](../../src/main/java/com/project/cybershop/catalog/internal/service/BrandService.java#L23) `@Transactional public BrandResponse createBrand(CreateBrandRequest request)`
  - Lời gọi xuất hiện: `brandRepository.existsByName()`, `request.name()`, `request.logoUrl()`, `request.description()`, `catalogMapper.toBrandResponse()`, `brandRepository.save()`
- [BrandService.java:32](../../src/main/java/com/project/cybershop/catalog/internal/service/BrandService.java#L32) `@Transactional(readOnly = true) public List<BrandResponse> getAllBrands()`
  - Lời gọi xuất hiện: `brandRepository.findAll().stream() .map(catalogMapper::toBrandResponse).toList()`, `brandRepository.findAll().stream().map()`, `brandRepository.findAll().stream()`, `brandRepository.findAll()`
- [BrandService.java:39](../../src/main/java/com/project/cybershop/catalog/internal/service/BrandService.java#L39) `@Transactional(readOnly = true) public BrandResponse getBrandById(Integer id)`
  - Lời gọi xuất hiện: `brandRepository.findById(id).orElseThrow()`, `brandRepository.findById()`, `catalogMapper.toBrandResponse()`

## `catalog/internal/service/CatalogPublicApiImpl.java`

### CatalogPublicApiImpl — [CatalogPublicApiImpl.java:24](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L24)

Khai báo: `@Service @RequiredArgsConstructor public class CatalogPublicApiImpl implements CatalogPublicApi`

- Trường: `private final ProductVariantRepository variantRepository;`
- Trường: `private final SpecificationValueRepository specValueRepository;`
- Trường: `private final BrandRepository brandRepository;`
- Trường: `private final CategorySpecificationRepository categorySpecificationRepository;`
- Trường: `private final SpecificationOptionRepository specificationOptionRepository;`
- [CatalogPublicApiImpl.java:34](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L34) `@Override @Transactional(readOnly = true) public List<ComponentCandidateDTO> getActiveCandidatesForBuild()`
  - Lời gọi xuất hiện: `toCandidates()`, `variantRepository.findAllActiveInStock()`
- [CatalogPublicApiImpl.java:40](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L40) `@Override @Transactional(readOnly = true) public Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds)`
  - Lời gọi xuất hiện: `getCandidatesByIds(variantIds).stream().collect()`, `getCandidatesByIds(variantIds).stream()`, `getCandidatesByIds()`, `Collectors.toMap()`, `Function.identity()`
- [CatalogPublicApiImpl.java:47](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L47) `@Override @Transactional(readOnly = true) public List<ComponentCandidateDTO> getCandidatesByIds(List<Integer> variantIds)`
  - Lời gọi xuất hiện: `variantIds.isEmpty()`, `Collections.emptyList()`, `toCandidates()`, `variantRepository.findAllById()`
- [CatalogPublicApiImpl.java:55](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L55) `@Override @Transactional(readOnly = true) public List<CategorySpecDto> getSpecificationCatalog()`
  - Lời gọi xuất hiện: `specificationOptionRepository.findAll()`, `optionsBySpecId.computeIfAbsent(option.getSpecification().getId(), k -> new ArrayList<>()).add()`, `optionsBySpecId.computeIfAbsent()`, `option.getSpecification().getId()`, `option.getSpecification()`, `option.getCode()`, `option.getDisplayValue()`, `categorySpecificationRepository.findAll()`, `cs.getSpecification().getId()`, `cs.getSpecification()`, `result.add()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId) .specificationName(cs.getSpecification().getName()) .compatibility(Boolean.TRUE.equals(cs.getIsCompatibility())) .primary(Boolean.TRUE.equals(cs.getIsPrimary())) .filterable(Boolean.TRUE.equals(cs.getIsFilterable())) .options(optionsBySpecId.getOrDefault(specId, List.of())).build()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId) .specificationName(cs.getSpecification().getName()) .compatibility(Boolean.TRUE.equals(cs.getIsCompatibility())) .primary(Boolean.TRUE.equals(cs.getIsPrimary())) .filterable(Boolean.TRUE.equals(cs.getIsFilterable())).options()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId) .specificationName(cs.getSpecification().getName()) .compatibility(Boolean.TRUE.equals(cs.getIsCompatibility())) .primary(Boolean.TRUE.equals(cs.getIsPrimary())).filterable()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId) .specificationName(cs.getSpecification().getName()) .compatibility(Boolean.TRUE.equals(cs.getIsCompatibility())).primary()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId) .specificationName(cs.getSpecification().getName()).compatibility()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()) .specificationId(specId).specificationName()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()) .categoryName(cs.getCategory().getName()).specificationId()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()) .categorySlug(cs.getCategory().getSlug()).categoryName()`, `CategorySpecDto.builder() .categoryId(cs.getCategory().getId()).categorySlug()`, `CategorySpecDto.builder().categoryId()`, `CategorySpecDto.builder()`, `cs.getCategory().getId()`, `cs.getCategory()`, `cs.getCategory().getSlug()`, `cs.getCategory().getName()`, `cs.getSpecification().getName()`, `Boolean.TRUE.equals()`, `cs.getIsCompatibility()`, `cs.getIsPrimary()`, `cs.getIsFilterable()`, `optionsBySpecId.getOrDefault()`, `List.of()`
- [CatalogPublicApiImpl.java:82](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L82) `private List<ComponentCandidateDTO> toCandidates(List<ProductVariant> variants)`
  - Lời gọi xuất hiện: `variants.isEmpty()`, `Collections.emptyList()`, `variants.stream().map(ProductVariant::getId).toList()`, `variants.stream().map()`, `variants.stream()`, `specValueRepository.findByProductVariantIdIn()`, `specMap.computeIfAbsent(sv.getProductVariant().getId(), k -> new HashMap<>()).put()`, `specMap.computeIfAbsent()`, `sv.getProductVariant().getId()`, `sv.getProductVariant()`, `sv.getSpecification().getName()`, `sv.getSpecification()`, `sv.getValue()`, `sv.getSpecification().getId()`, `specIdMap.computeIfAbsent(sv.getProductVariant().getId(), k -> new HashMap<>()).put()`, `specIdMap.computeIfAbsent()`, `variants.stream().map(pv -> ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()) .imageUrl(pv.getImageUrl()) .quantity(1) .specs(specMap.getOrDefault(pv.getId(), Collections.emptyMap())) .specValuesById(specIdMap.getOrDefault(pv.getId(), Collections.emptyMap())) .build() ).toList()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()) .imageUrl(pv.getImageUrl()) .quantity(1) .specs(specMap.getOrDefault(pv.getId(), Collections.emptyMap())) .specValuesById(specIdMap.getOrDefault(pv.getId(), Collections.emptyMap())).build()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()) .imageUrl(pv.getImageUrl()) .quantity(1) .specs(specMap.getOrDefault(pv.getId(), Collections.emptyMap())).specValuesById()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()) .imageUrl(pv.getImageUrl()) .quantity(1).specs()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()) .imageUrl(pv.getImageUrl()).quantity()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()) .stockQuantity(pv.getStockQuantity()).imageUrl()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()) .price(pv.getPrice()).stockQuantity()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()) .variantName(pv.getVariantName()).price()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other") .productName(pv.getProduct().getName()).variantName()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null) .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other").productName()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null) .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null).categorySlug()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null) .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null).categoryId()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()) .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null).brandName()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()) .productId(pv.getProduct().getId()).brandId()`, `ComponentCandidateDTO.builder() .variantId(pv.getId()).productId()`, `ComponentCandidateDTO.builder().variantId()`, `ComponentCandidateDTO.builder()`, `pv.getId()`, `pv.getProduct().getId()`, `pv.getProduct()`, `pv.getProduct().getBrand()`, `pv.getProduct().getBrand().getId()`, `pv.getProduct().getBrand().getName()`, `pv.getProduct().getCategory()`, `pv.getProduct().getCategory().getId()`, `pv.getProduct().getCategory().getSlug()`, `pv.getProduct().getName()`, `pv.getVariantName()`, `pv.getPrice()`, `pv.getStockQuantity()`, `pv.getImageUrl()`, `specMap.getOrDefault()`, `Collections.emptyMap()`, `specIdMap.getOrDefault()`
- [CatalogPublicApiImpl.java:118](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogPublicApiImpl.java#L118) `@Override @Transactional(readOnly = true) public List<String> getAllBrandNames()`
  - Lời gọi xuất hiện: `brandRepository.findAll().stream() .map(Brand::getName) .filter(Objects::nonNull) .map(String::trim) .filter(name -> !name.isEmpty()) .distinct().toList()`, `brandRepository.findAll().stream() .map(Brand::getName) .filter(Objects::nonNull) .map(String::trim) .filter(name -> !name.isEmpty()).distinct()`, `brandRepository.findAll().stream() .map(Brand::getName) .filter(Objects::nonNull) .map(String::trim).filter()`, `brandRepository.findAll().stream() .map(Brand::getName) .filter(Objects::nonNull).map()`, `brandRepository.findAll().stream() .map(Brand::getName).filter()`, `brandRepository.findAll().stream().map()`, `brandRepository.findAll().stream()`, `brandRepository.findAll()`, `name.isEmpty()`

## `catalog/internal/service/CatalogService.java`

### CatalogService — [CatalogService.java:6](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogService.java#L6)

Khai báo: `public interface CatalogService`

- [CatalogService.java:7](../../src/main/java/com/project/cybershop/catalog/internal/service/CatalogService.java#L7) `List<CatalogProductResponse> list()`

## `catalog/internal/service/CategoryService.java`

### CategoryService — [CategoryService.java:26](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L26)

Khai báo: `@Service @RequiredArgsConstructor public class CategoryService`

- Trường: `private final CategoryRepository categoryRepository;`
- Trường: `private final SpecificationRepository specificationRepository;`
- Trường: `private final CategorySpecificationRepository categorySpecRepository;`
- Trường: `private final CatalogMapper catalogMapper;`
- [CategoryService.java:35](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L35) `@Transactional public CategoryResponse createCategory(CreateCategoryRequest request)`
  - Lời gọi xuất hiện: `request.slug()`, `slug.isBlank()`, `toSlug()`, `request.name()`, `categoryRepository.existsBySlug()`, `request.parentId()`, `categoryRepository.findById(request.parentId()).orElseThrow()`, `categoryRepository.findById()`, `request.description()`, `catalogMapper.toCategoryResponse()`, `categoryRepository.save()`
- [CategoryService.java:56](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L56) `@Transactional public List<CategorySpecResponse> configureCategorySpecifications(Integer categoryId, List<ConfigureCategorySpecRequest> requests)`
  - Lời gọi xuất hiện: `categoryRepository.findById(categoryId).orElseThrow()`, `categoryRepository.findById()`, `specificationRepository.findById(req.specificationId()).orElseThrow()`, `specificationRepository.findById()`, `req.specificationId()`, `categorySpecRepository .findByCategoryIdAndSpecificationId(categoryId, req.specificationId()).orElse()`, `categorySpecRepository.findByCategoryIdAndSpecificationId()`, `req.isFilterable()`, `req.isPrimary()`, `req.showInMenu()`, `catSpec.setIsFilterable()`, `catSpec.setIsPrimary()`, `catSpec.setShowInMenu()`, `catSpec.setIsCompatibility()`, `req.isCompatibility()`, `savedSpecs.add()`, `categorySpecRepository.save()`, `savedSpecs.stream().map(catalogMapper::toCategorySpecResponse).toList()`, `savedSpecs.stream().map()`, `savedSpecs.stream()`
- [CategoryService.java:81](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L81) `@Transactional(readOnly = true) public List<CategoryResponse> getCategoryTree()`
  - Lời gọi xuất hiện: `categoryRepository.findAllRootCategoriesWithChildren().stream() .map(catalogMapper::toCategoryResponse).toList()`, `categoryRepository.findAllRootCategoriesWithChildren().stream().map()`, `categoryRepository.findAllRootCategoriesWithChildren().stream()`, `categoryRepository.findAllRootCategoriesWithChildren()`
- [CategoryService.java:88](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L88) `@Transactional(readOnly = true) public CategoryResponse getCategoryById(Integer id)`
  - Lời gọi xuất hiện: `categoryRepository.findById(id).orElseThrow()`, `categoryRepository.findById()`, `catalogMapper.toCategoryResponse()`
- Trường: `private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");`
- Trường: `private static final Pattern WHITESPACE = Pattern.compile("[\\s]");`
- [CategoryService.java:98](../../src/main/java/com/project/cybershop/catalog/internal/service/CategoryService.java#L98) `private String toSlug(String input)`
  - Lời gọi xuất hiện: `WHITESPACE.matcher(input).replaceAll()`, `WHITESPACE.matcher()`, `Normalizer.normalize()`, `NONLATIN.matcher(normalized).replaceAll()`, `NONLATIN.matcher()`, `slug.toLowerCase()`

## `catalog/internal/service/impl/CatalogInternalApiImpl.java`

### CatalogInternalApiImpl — [CatalogInternalApiImpl.java:19](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L19)

Khai báo: `@Service @RequiredArgsConstructor public class CatalogInternalApiImpl implements CatalogInternalApi`

- Trường: `private final ProductVariantRepository productVariantRepository;`
- Trường: `private final CatalogMapper catalogMapper;`
- [CatalogInternalApiImpl.java:26](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L26) `@Override @Transactional(readOnly = true) public boolean isVariantInStock(Integer variantId, int requestedQty)`
  - Lời gọi xuất hiện: `productVariantRepository.findById(variantId).orElse()`, `productVariantRepository.findById()`, `variant.getStockQuantity()`
- [CatalogInternalApiImpl.java:34](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L34) `@Override @Transactional public void deductStock(Integer variantId, int quantity)`
  - Lời gọi xuất hiện: `productVariantRepository.deductAvailableStock()`
- [CatalogInternalApiImpl.java:46](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L46) `@Override @Transactional public void restoreStock(Integer variantId, int quantity)`
  - Lời gọi xuất hiện: `productVariantRepository.restoreAvailableStock()`
- [CatalogInternalApiImpl.java:58](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L58) `@Override @Transactional public void lockVariantsForPurchase(List<Integer> variantIds)`
  - Lời gọi xuất hiện: `variantIds.stream().distinct().sorted().toList()`, `variantIds.stream().distinct().sorted()`, `variantIds.stream().distinct()`, `variantIds.stream()`, `productVariantRepository.lockForPurchase()`
- [CatalogInternalApiImpl.java:67](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L67) `@Override @Transactional(readOnly = true) public PurchaseVariantResponse getPurchaseVariant(Integer variantId)`
  - Lời gọi xuất hiện: `productVariantRepository.findPurchaseVariant(variantId).orElseThrow()`, `productVariantRepository.findPurchaseVariant()`, `toPurchaseVariantResponse()`
- [CatalogInternalApiImpl.java:76](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L76) `@Override @Transactional(readOnly = true) public Optional<PurchaseVariantResponse> findActiveVariant(Integer variantId)`
  - Lời gọi xuất hiện: `productVariantRepository.findPurchaseVariant(variantId) .filter(product -> "active".equalsIgnoreCase(product.getStatus())).map()`, `productVariantRepository.findPurchaseVariant(variantId).filter()`, `productVariantRepository.findPurchaseVariant()`, `"active".equalsIgnoreCase()`, `product.getStatus()`
- [CatalogInternalApiImpl.java:84](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L84) `private PurchaseVariantResponse toPurchaseVariantResponse(PurchaseVariantProjection product)`
  - Lời gọi xuất hiện: `product.getVariantId()`, `product.getProductName()`, `product.getVariantName()`, `product.getPrice()`, `product.getWarrantyPeriodMonths()`, `product.getStatus()`, `product.getStockQuantity()`, `product.getImageUrl()`
- [CatalogInternalApiImpl.java:97](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogInternalApiImpl.java#L97) `@Override @Transactional(readOnly = true) public ProductVariantResponse getVariantDetail(Integer variantId)`
  - Lời gọi xuất hiện: `productVariantRepository.findWithAttributesById(variantId).orElseThrow()`, `productVariantRepository.findWithAttributesById()`, `catalogMapper.toVariantResponse()`

## `catalog/internal/service/impl/CatalogServiceImpl.java`

### CatalogServiceImpl — [CatalogServiceImpl.java:18](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogServiceImpl.java#L18)

Khai báo: `@Service public class CatalogServiceImpl implements CatalogService`

- Trường: `private final JdbcTemplate jdbc;`
- Trường: `private final ReviewApi reviewApi;`
- [CatalogServiceImpl.java:23](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogServiceImpl.java#L23) `public CatalogServiceImpl(JdbcTemplate jdbc, ReviewApi reviewApi)`
- [CatalogServiceImpl.java:28](../../src/main/java/com/project/cybershop/catalog/internal/service/impl/CatalogServiceImpl.java#L28) `@Override @Transactional(readOnly = true) public List<CatalogProductResponse> list()`
  - Lời gọi xuất hiện: `reviewApi.productSummaries()`, `jdbc.query()`, `rs.getInt()`, `rs.getString()`, `specs.computeIfAbsent(id, key -> new LinkedHashMap<>()).put()`, `specs.computeIfAbsent()`, `rs.getBoolean()`, `primary.computeIfAbsent(id, key -> new ArrayList<>()).add()`, `primary.computeIfAbsent()`, `filters.computeIfAbsent(id, key -> new ArrayList<>()).add()`, `filters.computeIfAbsent()`, `feedback.getOrDefault()`, `rs.getBigDecimal()`, `specs.getOrDefault()`, `Map.of()`, `primary.getOrDefault()`, `List.of()`, `filters.getOrDefault()`, `summary.averageRating()`, `summary.reviewCount()`, `summary.buyerCount()`

## `catalog/internal/service/ProductService.java`

### ProductService — [ProductService.java:26](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L26)

Khai báo: `@Service public class ProductService`

- Trường: `private static final BigDecimal PRICE_LIMIT = BigDecimal.valueOf(10_000_000_000L);`
- Trường: `private final ProductRepository productRepository;`
- Trường: `private final ProductVariantRepository productVariantRepository;`
- Trường: `private final BrandRepository brandRepository;`
- Trường: `private final CategoryRepository categoryRepository;`
- Trường: `private final SpecificationRepository specificationRepository;`
- Trường: `private final SpecificationOptionRepository specificationOptionRepository;`
- Trường: `private final CategorySpecificationRepository categorySpecRepository;`
- Trường: `private final SpecificationValueRepository specificationValueRepository;`
- Trường: `private final CatalogMapper catalogMapper;`
- Trường: `private final EntityManager em;`
- [ProductService.java:43](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L43) `public ProductService( ProductRepository productRepository, ProductVariantRepository productVariantRepository, BrandRepository brandRepository, CategoryRepository categoryRepository, SpecificationRepository specificationRepository, SpecificationOptionRepository specificationOptionRepository, CategorySpecificationRepository categorySpecRepository, SpecificationValueRepository specificationValueRepository, CatalogMapper catalogMapper, EntityManager em)`
- [ProductService.java:66](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L66) `private void validateVariantPriceAndStock(BigDecimal price, BigDecimal originalPrice, Integer stockQuantity)`
  - Lời gọi xuất hiện: `price.compareTo()`, `originalPrice.compareTo()`
- [ProductService.java:84](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L84) `private void attachAttributes(ProductVariant variant, List<AttributeValueInputDto> attributes)`
  - Lời gọi xuất hiện: `attributes.isEmpty()`, `specificationRepository.findById(attrDto.specificationId()).orElseThrow()`, `specificationRepository.findById()`, `attrDto.specificationId()`, `attrDto.value()`, `attrDto.optionId()`, `specificationOptionRepository.findById(attrDto.optionId()).orElseThrow()`, `specificationOptionRepository.findById()`, `option.getSpecification().getId().equals()`, `option.getSpecification().getId()`, `option.getSpecification()`, `spec.getId()`, `option.getDisplayValue()`, `spec.getName()`, `val.isBlank()`, `val.trim()`, `variant.addAttributeValue()`, `specificationValueRepository.save()`
- [ProductService.java:112](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L112) `@Transactional public ProductResponse createProductDraft(CreateProductRequest request)`
  - Lời gọi xuất hiện: `brandRepository.findById(request.brandId()).orElseThrow()`, `brandRepository.findById()`, `request.brandId()`, `categoryRepository.findById(request.categoryId()).orElseThrow()`, `categoryRepository.findById()`, `request.categoryId()`, `request.name()`, `request.warrantyPeriodMonths()`, `request.description()`, `product.setStatus()`, `productRepository.save()`, `request.variants()`, `request.variants().isEmpty()`, `addVariantInternal()`, `catalogMapper.toProductResponse()`, `productRepository.findWithDetailsById(savedProduct.getId()).orElse()`, `productRepository.findWithDetailsById()`, `savedProduct.getId()`
- [ProductService.java:139](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L139) `@Transactional public ProductVariantResponse addVariant(Integer productId, CreateVariantDto variantDto)`
  - Lời gọi xuất hiện: `productRepository.findById(productId).orElseThrow()`, `productRepository.findById()`, `addVariantInternal()`, `catalogMapper.toVariantResponse()`
- [ProductService.java:148](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L148) `private ProductVariant addVariantInternal(Product product, CreateVariantDto variantDto)`
  - Lời gọi xuất hiện: `validateVariantPriceAndStock()`, `variantDto.price()`, `variantDto.originalPrice()`, `variantDto.stockQuantity()`, `productVariantRepository.existsBySerialCode()`, `variantDto.serialCode()`, `variantDto.slug()`, `variantDto.slug().isBlank()`, `productVariantRepository.existsBySlug()`, `variantDto.serialCode().trim()`, `variantDto.variantName()`, `variantDto.imageUrl()`, `productVariantRepository.save()`, `attachAttributes()`, `variantDto.attributes()`
- [ProductService.java:176](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L176) `@Transactional public ProductVariantResponse updateVariant(Integer productId, Integer variantId, UpdateVariantRequest request)`
  - Lời gọi xuất hiện: `productRepository.existsById()`, `productVariantRepository.findWithAttributesById(variantId).orElseThrow()`, `productVariantRepository.findWithAttributesById()`, `variant.getProduct().getId().equals()`, `variant.getProduct().getId()`, `variant.getProduct()`, `validateVariantPriceAndStock()`, `request.price()`, `request.originalPrice()`, `request.stockQuantity()`, `request.serialCode().trim()`, `request.serialCode()`, `productVariantRepository.findBySerialCode()`, `existingWithSku.isPresent()`, `existingWithSku.get().getId().equals()`, `existingWithSku.get().getId()`, `existingWithSku.get()`, `request.slug()`, `request.slug().isBlank()`, `productVariantRepository.findBySlug()`, `request.slug().trim()`, `existingWithSlug.isPresent()`, `existingWithSlug.get().getId().equals()`, `existingWithSlug.get().getId()`, `existingWithSlug.get()`, `variant.setSerialCode()`, `variant.setVariantName()`, `request.variantName()`, `variant.setSlug()`, `variant.setPrice()`, `variant.setOriginalPrice()`, `variant.setStockQuantity()`, `variant.setImageUrl()`, `request.imageUrl()`, `request.attributes()`, `variant.getAttributeValues().clear()`, `variant.getAttributeValues()`, `productVariantRepository.flush()`, `attachAttributes()`, `productVariantRepository.save()`, `catalogMapper.toVariantResponse()`
- [ProductService.java:224](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L224) `@Transactional public void deleteVariant(Integer productId, Integer variantId)`
  - Lời gọi xuất hiện: `productRepository.existsById()`, `productVariantRepository.findById(variantId).orElseThrow()`, `productVariantRepository.findById()`, `variant.getProduct().getId().equals()`, `variant.getProduct().getId()`, `variant.getProduct()`, `em.createNativeQuery( "SELECT COUNT(*) FROM order_items WHERE product_variant_id = :variantId" ).setParameter("variantId", variantId).getSingleResult()`, `em.createNativeQuery( "SELECT COUNT(*) FROM order_items WHERE product_variant_id = :variantId" ).setParameter()`, `em.createNativeQuery()`, `orderItemCount.longValue()`, `variant.getVariantName()`, `variant.getSerialCode()`, `em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id = :variantId") .setParameter("variantId", variantId).executeUpdate()`, `em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id = :variantId").setParameter()`, `productVariantRepository.delete()`, `productVariantRepository.flush()`, `productVariantRepository.findByProductId()`, `remaining.isEmpty()`, `productRepository.findById(productId).orElse()`, `productRepository.findById()`, `product.getStatus()`, `product.setStatus()`, `productRepository.save()`
- [ProductService.java:265](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L265) `@Transactional(readOnly = true) public List<ProductVariantResponse> getVariantsByProductId(Integer productId)`
  - Lời gọi xuất hiện: `productRepository.existsById()`, `productVariantRepository.findWithAttributesByProductId(productId).stream() .map(catalogMapper::toVariantResponse).toList()`, `productVariantRepository.findWithAttributesByProductId(productId).stream().map()`, `productVariantRepository.findWithAttributesByProductId(productId).stream()`, `productVariantRepository.findWithAttributesByProductId()`
- [ProductService.java:275](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L275) `@Transactional(readOnly = true) public ProductVariantResponse getVariantById(Integer productId, Integer variantId)`
  - Lời gọi xuất hiện: `productVariantRepository.findWithAttributesById(variantId).orElseThrow()`, `productVariantRepository.findWithAttributesById()`, `variant.getProduct().getId().equals()`, `variant.getProduct().getId()`, `variant.getProduct()`, `catalogMapper.toVariantResponse()`
- [ProductService.java:287](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L287) `private void requirePublishable(Product product, Integer categoryId)`
  - Lời gọi xuất hiện: `product.getVariants()`, `product.getVariants().isEmpty()`, `categorySpecRepository.findPrimarySpecsByCategoryId()`, `primarySpecs.isEmpty()`, `primarySpecs.stream() .map(cs -> cs.getSpecification().getId()).collect()`, `primarySpecs.stream().map()`, `primarySpecs.stream()`, `cs.getSpecification().getId()`, `cs.getSpecification()`, `Collectors.toSet()`, `variant.getAttributeValues().stream() .map(av -> av.getSpecification().getId()).collect()`, `variant.getAttributeValues().stream().map()`, `variant.getAttributeValues().stream()`, `variant.getAttributeValues()`, `av.getSpecification().getId()`, `av.getSpecification()`, `filledSpecIds.contains()`, `variant.getVariantName()`, `cs.getSpecification().getName()`
- [ProductService.java:315](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L315) `@Transactional public ProductResponse publishProduct(Integer productId)`
  - Lời gọi xuất hiện: `productRepository.findWithDetailsById(productId).orElseThrow()`, `productRepository.findWithDetailsById()`, `requirePublishable()`, `product.getCategory().getId()`, `product.getCategory()`, `product.setStatus()`, `catalogMapper.toProductResponse()`, `productRepository.save()`
- [ProductService.java:326](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L326) `@Transactional(readOnly = true) public ProductResponse getProductById(Integer id)`
  - Lời gọi xuất hiện: `productRepository.findWithDetailsById(id).orElseThrow()`, `productRepository.findWithDetailsById()`, `catalogMapper.toProductResponse()`
- [ProductService.java:333](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L333) `@Transactional(readOnly = true) public List<ProductResponse> listActiveProducts()`
  - Lời gọi xuất hiện: `productRepository.findByStatus(ProductStatus.ACTIVE).stream() .map(catalogMapper::toProductResponse).toList()`, `productRepository.findByStatus(ProductStatus.ACTIVE).stream().map()`, `productRepository.findByStatus(ProductStatus.ACTIVE).stream()`, `productRepository.findByStatus()`
- [ProductService.java:340](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L340) `@Transactional(readOnly = true) public List<ProductResponse> listAllProducts()`
  - Lời gọi xuất hiện: `productRepository.findAllWithDetails().stream() .map(catalogMapper::toProductResponse).toList()`, `productRepository.findAllWithDetails().stream().map()`, `productRepository.findAllWithDetails().stream()`, `productRepository.findAllWithDetails()`
- [ProductService.java:347](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L347) `@Transactional(readOnly = true) public List<ProductResponse> listProductsByStatus(ProductStatus status)`
  - Lời gọi xuất hiện: `productRepository.findByStatusWithDetails(status).stream() .map(catalogMapper::toProductResponse).toList()`, `productRepository.findByStatusWithDetails(status).stream().map()`, `productRepository.findByStatusWithDetails(status).stream()`, `productRepository.findByStatusWithDetails()`
- [ProductService.java:354](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L354) `@Transactional public ProductResponse updateProduct(Integer productId, UpdateProductRequest request)`
  - Lời gọi xuất hiện: `productRepository.findById(productId).orElseThrow()`, `productRepository.findById()`, `brandRepository.findById(request.brandId()).orElseThrow()`, `brandRepository.findById()`, `request.brandId()`, `categoryRepository.findById(request.categoryId()).orElseThrow()`, `categoryRepository.findById()`, `request.categoryId()`, `request.status()`, `product.getStatus()`, `category.getId().equals()`, `category.getId()`, `product.getCategory().getId()`, `product.getCategory()`, `requirePublishable()`, `product.setName()`, `request.name().trim()`, `request.name()`, `product.setBrand()`, `product.setCategory()`, `product.setWarrantyPeriodMonths()`, `request.warrantyPeriodMonths()`, `product.setDescription()`, `request.description()`, `product.setStatus()`, `productRepository.save()`, `catalogMapper.toProductResponse()`, `productRepository.findWithDetailsById(saved.getId()).orElse()`, `productRepository.findWithDetailsById()`, `saved.getId()`
- [ProductService.java:384](../../src/main/java/com/project/cybershop/catalog/internal/service/ProductService.java#L384) `@Transactional public void deleteProduct(Integer productId)`
  - Lời gọi xuất hiện: `productRepository.findById(productId).orElseThrow()`, `productRepository.findById()`, `em.createNativeQuery( "SELECT COUNT(*) FROM order_items oi JOIN product_variants pv ON oi.product_variant_id = pv.id WHERE pv.product_id = :productId" ).setParameter("productId", productId).getSingleResult()`, `em.createNativeQuery( "SELECT COUNT(*) FROM order_items oi JOIN product_variants pv ON oi.product_variant_id = pv.id WHERE pv.product_id = :productId" ).setParameter()`, `em.createNativeQuery()`, `orderCount.longValue()`, `product.getName()`, `em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)") .setParameter("productId", productId).executeUpdate()`, `em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)").setParameter()`, `productRepository.delete()`

## `catalog/internal/service/SpecificationService.java`

### SpecificationService — [SpecificationService.java:16](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L16)

Khai báo: `@Service @RequiredArgsConstructor public class SpecificationService`

- Trường: `private final SpecificationRepository specificationRepository;`
- Trường: `private final com.project.cybershop.catalog.internal.repository.SpecificationOptionRepository specificationOptionRepository;`
- Trường: `private final CatalogMapper catalogMapper;`
- [SpecificationService.java:24](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L24) `@Transactional public SpecificationResponse createSpecification(CreateSpecificationRequest request)`
  - Lời gọi xuất hiện: `specificationRepository.existsByName()`, `request.name()`, `catalogMapper.toSpecResponse()`, `specificationRepository.save()`
- [SpecificationService.java:33](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L33) `@Transactional(readOnly = true) public List<SpecificationResponse> getAllSpecifications()`
  - Lời gọi xuất hiện: `specificationRepository.findAll().stream() .map(catalogMapper::toSpecResponse).toList()`, `specificationRepository.findAll().stream().map()`, `specificationRepository.findAll().stream()`, `specificationRepository.findAll()`
- [SpecificationService.java:40](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L40) `@Transactional(readOnly = true) public SpecificationResponse getSpecificationById(Integer id)`
  - Lời gọi xuất hiện: `specificationRepository.findById(id).orElseThrow()`, `specificationRepository.findById()`, `catalogMapper.toSpecResponse()`
- [SpecificationService.java:47](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L47) `@Transactional public com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse addOption( Integer specificationId, com.project.cybershop.catalog.api.dto.request.CreateSpecificationOptionRequest request)`
  - Lời gọi xuất hiện: `specificationRepository.findById(specificationId).orElseThrow()`, `specificationRepository.findById()`, `specificationOptionRepository.existsBySpecificationIdAndCode()`, `request.code()`, `request.displayValue()`, `catalogMapper.toSpecOptionResponse()`, `specificationOptionRepository.save()`
- [SpecificationService.java:63](../../src/main/java/com/project/cybershop/catalog/internal/service/SpecificationService.java#L63) `@Transactional(readOnly = true) public List<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse> getOptionsBySpecificationId(Integer specificationId)`
  - Lời gọi xuất hiện: `specificationRepository.existsById()`, `specificationOptionRepository.findBySpecificationId(specificationId).stream() .map(catalogMapper::toSpecOptionResponse).toList()`, `specificationOptionRepository.findBySpecificationId(specificationId).stream().map()`, `specificationOptionRepository.findBySpecificationId(specificationId).stream()`, `specificationOptionRepository.findBySpecificationId()`

