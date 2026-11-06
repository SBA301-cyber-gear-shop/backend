# Tra cứu module review

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **PUT `/api/order-items/{itemId}/review`** → `ReviewController.save` [ReviewController.java:20](../../src/main/java/com/project/cybershop/review/internal/controller/ReviewController.java#L20)
  - `@PutMapping("/api/order-items/{itemId}/review") @ResponseStatus(HttpStatus.NO_CONTENT) public void save( Authentication authentication, @PathVariable Integer itemId, @Valid @RequestBody ReviewRequest request )`
- **GET `/api/products/{variantId}/reviews`** → `ReviewController.list` [ReviewController.java:30](../../src/main/java/com/project/cybershop/review/internal/controller/ReviewController.java#L30)
  - `@GetMapping("/api/products/{variantId}/reviews") public List<ProductReviewResponse> list(@PathVariable Integer variantId)`

## Khai báo và lời gọi theo file

## `review/api/dto/request/ReviewRequest.java`

### ReviewRequest — [ReviewRequest.java:4](../../src/main/java/com/project/cybershop/review/api/dto/request/ReviewRequest.java#L4)

Khai báo: `public record ReviewRequest( @NotNull(message = "Vui lòng chọn số sao") @Min(value = 1, message = "Số sao phải từ 1 đến 5") @Max(value = 5, message = "Số sao phải từ 1 đến 5") Integer rating, @NotBlank(message = "Vui lòng nhập nội dung đánh giá") @Size(max=5000, message = "Nội dung đánh giá tối đa 5000 ký tự") String comment)`


## `review/api/dto/response/ProductFeedbackSummary.java`

### ProductFeedbackSummary — [ProductFeedbackSummary.java:5](../../src/main/java/com/project/cybershop/review/api/dto/response/ProductFeedbackSummary.java#L5)

Khai báo: `public record ProductFeedbackSummary( Integer productVariantId, BigDecimal averageRating, long reviewCount, long buyerCount )`


## `review/api/dto/response/ProductReviewResponse.java`

### ProductReviewResponse — [ProductReviewResponse.java:3](../../src/main/java/com/project/cybershop/review/api/dto/response/ProductReviewResponse.java#L3)

Khai báo: `public record ProductReviewResponse(String id, String productId, Integer rating, String comment, String name)`


## `review/api/ReviewApi.java`

### ReviewApi — [ReviewApi.java:7](../../src/main/java/com/project/cybershop/review/api/ReviewApi.java#L7)

Khai báo: `public interface ReviewApi`

- [ReviewApi.java:9](../../src/main/java/com/project/cybershop/review/api/ReviewApi.java#L9) `Map<Integer, ProductFeedbackSummary> productSummaries()`

## `review/internal/controller/ReviewController.java`

### ReviewController — [ReviewController.java:14](../../src/main/java/com/project/cybershop/review/internal/controller/ReviewController.java#L14)

Khai báo: `@RestController @RequiredArgsConstructor public class ReviewController`

- Trường: `private final ReviewService reviewService;`
- [ReviewController.java:20](../../src/main/java/com/project/cybershop/review/internal/controller/ReviewController.java#L20) `@PutMapping("/api/order-items/{itemId}/review") @ResponseStatus(HttpStatus.NO_CONTENT) public void save( Authentication authentication, @PathVariable Integer itemId, @Valid @RequestBody ReviewRequest request )`
  - Lời gọi xuất hiện: `reviewService.save()`, `authentication.getName()`
- [ReviewController.java:30](../../src/main/java/com/project/cybershop/review/internal/controller/ReviewController.java#L30) `@GetMapping("/api/products/{variantId}/reviews") public List<ProductReviewResponse> list(@PathVariable Integer variantId)`
  - Lời gọi xuất hiện: `reviewService.forVariant()`

## `review/internal/entity/Review.java`

### Review — [Review.java:10](../../src/main/java/com/project/cybershop/review/internal/entity/Review.java#L10)

Khai báo: `@Entity @Table(name = "reviews") @Getter @Setter @NoArgsConstructor public class Review`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "order_item_id", nullable = false) private Integer orderItemId;`
- Trường: `@Column(name = "rating", nullable = false) private Integer rating;`
- Trường: `@Column(name = "comment", columnDefinition = "TEXT") private String comment;`
- Trường: `@Column(name = "review_date") private LocalDateTime reviewDate;`

## `review/internal/mapper/ReviewMapper.java`

### ReviewMapper — [ReviewMapper.java:12](../../src/main/java/com/project/cybershop/review/internal/mapper/ReviewMapper.java#L12)

Khai báo: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR) public interface ReviewMapper`

- [ReviewMapper.java:15](../../src/main/java/com/project/cybershop/review/internal/mapper/ReviewMapper.java#L15) `@Mapping(target = "id", ignore = true) @Mapping(target = "orderItemId", ignore = true) @Mapping(target = "reviewDate", ignore = true) @Mapping(target = "comment", expression = "java(request.comment().trim())") void updateEntity(ReviewRequest request, @MappingTarget Review review)`
- [ReviewMapper.java:21](../../src/main/java/com/project/cybershop/review/internal/mapper/ReviewMapper.java#L21) `ProductReviewResponse toResponse(ProductReviewProjection review)`

## `review/internal/repository/ProductFeedbackProjection.java`

### ProductFeedbackProjection — [ProductFeedbackProjection.java:3](../../src/main/java/com/project/cybershop/review/internal/repository/ProductFeedbackProjection.java#L3)

Khai báo: `public interface ProductFeedbackProjection`

- [ProductFeedbackProjection.java:5](../../src/main/java/com/project/cybershop/review/internal/repository/ProductFeedbackProjection.java#L5) `Integer getProductVariantId()`
- [ProductFeedbackProjection.java:7](../../src/main/java/com/project/cybershop/review/internal/repository/ProductFeedbackProjection.java#L7) `Long getRatingTotal()`
- [ProductFeedbackProjection.java:9](../../src/main/java/com/project/cybershop/review/internal/repository/ProductFeedbackProjection.java#L9) `Long getReviewCount()`
- [ProductFeedbackProjection.java:11](../../src/main/java/com/project/cybershop/review/internal/repository/ProductFeedbackProjection.java#L11) `Long getBuyerCount()`

## `review/internal/repository/ProductReviewProjection.java`

### ProductReviewProjection — [ProductReviewProjection.java:3](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L3)

Khai báo: `public interface ProductReviewProjection`

- [ProductReviewProjection.java:5](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L5) `String getId()`
- [ProductReviewProjection.java:7](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L7) `String getProductId()`
- [ProductReviewProjection.java:9](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L9) `Integer getRating()`
- [ProductReviewProjection.java:11](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L11) `String getComment()`
- [ProductReviewProjection.java:13](../../src/main/java/com/project/cybershop/review/internal/repository/ProductReviewProjection.java#L13) `String getName()`

## `review/internal/repository/ReviewRepository.java`

### ReviewRepository — [ReviewRepository.java:12](../../src/main/java/com/project/cybershop/review/internal/repository/ReviewRepository.java#L12)

Khai báo: `@Repository public interface ReviewRepository extends JpaRepository<Review, Integer>`

- [ReviewRepository.java:15](../../src/main/java/com/project/cybershop/review/internal/repository/ReviewRepository.java#L15) `Optional<Review> findFirstByOrderItemIdOrderByIdAsc(Integer orderItemId)`
- [ReviewRepository.java:18](../../src/main/java/com/project/cybershop/review/internal/repository/ReviewRepository.java#L18) `@Query(value = """ SELECT o.status FROM orders o WHERE o.user_id = :userId AND o.id = (SELECT oi.order_id FROM order_items oi WHERE oi.id = :itemId) FOR UPDATE """, nativeQuery = true) List<String> findOwnedOrderStatusForUpdate( @Param("userId") Integer userId, @Param("itemId") Integer itemId )`
- [ReviewRepository.java:29](../../src/main/java/com/project/cybershop/review/internal/repository/ReviewRepository.java#L29) `@Query(value = """ SELECT CAST(r.id AS VARCHAR) AS id, CAST(oi.product_variant_id AS VARCHAR) AS "productId", r.rating AS rating, r.comment AS comment, u.full_name AS name FROM reviews r JOIN order_items oi ON oi.id = r.order_item_id JOIN orders o ON o.id = oi.order_id JOIN users u ON u.id = o.user_id WHERE oi.product_variant_id = :variantId AND o.status = 'completed' ORDER BY r.review_date DESC, r.id DESC """, nativeQuery = true) List<ProductReviewProjection> findPublishedByVariantId(@Param("variantId") Integer variantId)`
- [ReviewRepository.java:43](../../src/main/java/com/project/cybershop/review/internal/repository/ReviewRepository.java#L43) `@Query(value = """ SELECT pv.id AS "productVariantId", COALESCE(ratings.rating_total, 0) AS "ratingTotal", COALESCE(ratings.review_count, 0) AS "reviewCount", COALESCE(purchases.buyer_count, 0) AS "buyerCount" FROM product_variants pv JOIN products p ON p.id = pv.product_id AND lower(p.status) = 'active' LEFT JOIN ( SELECT oi.product_variant_id, SUM(r.rating) AS rating_total, COUNT(*) AS review_count FROM reviews r JOIN order_items oi ON oi.id = r.order_item_id JOIN orders o ON o.id = oi.order_id AND o.status = 'completed' GROUP BY oi.product_variant_id ) ratings ON ratings.product_variant_id = pv.id LEFT JOIN ( SELECT oi.product_variant_id, COUNT(DISTINCT o.user_id) AS buyer_count FROM order_items oi JOIN orders o ON o.id = oi.order_id AND o.status = 'completed' GROUP BY oi.product_variant_id ) purchases ON purchases.product_variant_id = pv.id """, nativeQuery = true) List<ProductFeedbackProjection> findProductSummaries()`

## `review/internal/service/impl/ReviewServiceImpl.java`

### ReviewServiceImpl — [ReviewServiceImpl.java:25](../../src/main/java/com/project/cybershop/review/internal/service/impl/ReviewServiceImpl.java#L25)

Khai báo: `@Service @RequiredArgsConstructor @Transactional public class ReviewServiceImpl implements ReviewService`

- Trường: `private final ReviewRepository reviewRepository;`
- Trường: `private final ReviewMapper reviewMapper;`
- Trường: `private final IdentityApi identityApi;`
- Trường: `private final Clock clock;`
- [ReviewServiceImpl.java:35](../../src/main/java/com/project/cybershop/review/internal/service/impl/ReviewServiceImpl.java#L35) `@Override public void save(String email, Integer itemId, ReviewRequest request)`
  - Lời gọi xuất hiện: `identityApi.requireActiveUser(email).getId()`, `identityApi.requireActiveUser()`, `reviewRepository.findOwnedOrderStatusForUpdate()`, `statuses.isEmpty()`, `"completed".equals()`, `statuses.get()`, `request.rating()`, `request.comment()`, `request.comment().isBlank()`, `reviewRepository.findFirstByOrderItemIdOrderByIdAsc(itemId).orElseGet()`, `reviewRepository.findFirstByOrderItemIdOrderByIdAsc()`, `created.setOrderItemId()`, `reviewMapper.updateEntity()`, `review.setReviewDate()`, `LocalDateTime.now()`, `reviewRepository.saveAndFlush()`
- [ReviewServiceImpl.java:62](../../src/main/java/com/project/cybershop/review/internal/service/impl/ReviewServiceImpl.java#L62) `@Override @Transactional(readOnly = true) public List<ProductReviewResponse> forVariant(Integer variantId)`
  - Lời gọi xuất hiện: `reviewRepository.findPublishedByVariantId(variantId).stream() .map(reviewMapper::toResponse).toList()`, `reviewRepository.findPublishedByVariantId(variantId).stream().map()`, `reviewRepository.findPublishedByVariantId(variantId).stream()`, `reviewRepository.findPublishedByVariantId()`
- [ReviewServiceImpl.java:70](../../src/main/java/com/project/cybershop/review/internal/service/impl/ReviewServiceImpl.java#L70) `@Override @Transactional(readOnly = true) public Map<Integer, ProductFeedbackSummary> productSummaries()`
  - Lời gọi xuất hiện: `reviewRepository.findProductSummaries()`, `row.getReviewCount()`, `BigDecimal.valueOf(row.getRatingTotal()).divide()`, `BigDecimal.valueOf()`, `row.getRatingTotal()`, `summaries.put()`, `row.getProductVariantId()`, `row.getBuyerCount()`

## `review/internal/service/ReviewService.java`

### ReviewService — [ReviewService.java:8](../../src/main/java/com/project/cybershop/review/internal/service/ReviewService.java#L8)

Khai báo: `public interface ReviewService extends ReviewApi`

- [ReviewService.java:9](../../src/main/java/com/project/cybershop/review/internal/service/ReviewService.java#L9) `void save(String email, Integer orderItemId, ReviewRequest request)`
- [ReviewService.java:10](../../src/main/java/com/project/cybershop/review/internal/service/ReviewService.java#L10) `List<ProductReviewResponse> forVariant(Integer variantId)`

