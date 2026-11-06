# Tra cứu module promotion

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **GET `/api/promotions`** → `PromotionController.visible` [PromotionController.java:20](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L20)
  - `@GetMapping("/api/promotions") public List<PromotionResponse> visible()`
- **GET `/api/admin/promotions`** → `PromotionController.list` [PromotionController.java:25](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L25)
  - `@GetMapping("/api/admin/promotions") @PreAuthorize("hasRole('ADMIN')") public List<PromotionResponse> list(Authentication authentication)`
- **POST `/api/admin/promotions`** → `PromotionController.create` [PromotionController.java:31](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L31)
  - `@PostMapping("/api/admin/promotions") @PreAuthorize("hasRole('ADMIN')") public PromotionResponse create( Authentication authentication, @Valid @RequestBody PromotionRequest request )`
- **PUT `/api/admin/promotions/{id}`** → `PromotionController.update` [PromotionController.java:40](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L40)
  - `@PutMapping("/api/admin/promotions/{id}") @PreAuthorize("hasRole('ADMIN')") public PromotionResponse update( Authentication authentication, @PathVariable Integer id, @Valid @RequestBody PromotionRequest request )`

## Khai báo và lời gọi theo file

## `promotion/api/dto/request/PromotionRequest.java`

### PromotionRequest — [PromotionRequest.java:7](../../src/main/java/com/project/cybershop/promotion/api/dto/request/PromotionRequest.java#L7)

Khai báo: `public record PromotionRequest( @NotBlank(message = "Vui lòng nhập mã khuyến mãi") @Size(max=50, message = "Mã khuyến mãi tối đa 50 ký tự") String code, @NotBlank(message = "Vui lòng nhập tên chương trình") @Size(max=150, message = "Tên chương trình tối đa 150 ký tự") String name, @NotBlank(message = "Vui lòng chọn loại giảm giá") String type, @NotNull(message = "Vui lòng nhập giá trị giảm") @DecimalMin(value="0",inclusive=false, message = "Giá trị giảm phải lớn hơn 0") @Digits(integer=10,fraction=2, message = "Giá trị giảm tối đa 10 chữ số và 2 số lẻ") BigDecimal value, @NotNull(message = "Vui lòng nhập số lượt dùng") @Min(value = 1, message = "Số lượt dùng phải từ 1 trở lên") Integer limit, @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDateTime start, @NotNull(message = "Vui lòng chọn ngày kết thúc") LocalDateTime end, boolean active, @Size(max=150, message = "Tiêu đề banner tối đa 150 ký tự") String bannerTitle, @Size(max=255, message = "Đường dẫn ảnh banner tối đa 255 ký tự") String bannerImage, @Size(max=255, message = "Liên kết banner tối đa 255 ký tự") String bannerLink, @Min(value = 0, message = "Thứ tự banner không được âm") int bannerOrder, boolean bannerActive )`


## `promotion/api/dto/response/PromotionResponse.java`

### PromotionResponse — [PromotionResponse.java:6](../../src/main/java/com/project/cybershop/promotion/api/dto/response/PromotionResponse.java#L6)

Khai báo: `public record PromotionResponse( Integer id, String code, String name, String type, BigDecimal value, int limit, LocalDateTime start, LocalDateTime end, boolean active, int used, int reserved, String bannerTitle, String bannerImage, String bannerLink, int bannerOrder, boolean bannerActive )`


## `promotion/api/PromotionApi.java`

### PromotionApi — [PromotionApi.java:5](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L5)

Khai báo: `public interface PromotionApi`

- [PromotionApi.java:8](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L8) `Quote quote(Integer userId, String code, BigDecimal total, boolean lock)`
- [PromotionApi.java:9](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L9) `void reserve(Quote quote, Integer userId, Integer orderId, boolean paid)`
- [PromotionApi.java:10](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L10) `void markUsed(Integer orderId)`
- [PromotionApi.java:11](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L11) `void release(Integer orderId)`

### Quote — [PromotionApi.java:6](../../src/main/java/com/project/cybershop/promotion/api/PromotionApi.java#L6)

Khai báo: `record Quote(Integer promotionId, String code, BigDecimal discount)`


## `promotion/internal/controller/PromotionController.java`

### PromotionController — [PromotionController.java:14](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L14)

Khai báo: `@RestController @RequiredArgsConstructor public class PromotionController`

- Trường: `private final PromotionService promotionService;`
- [PromotionController.java:20](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L20) `@GetMapping("/api/promotions") public List<PromotionResponse> visible()`
  - Lời gọi xuất hiện: `promotionService.publicList()`
- [PromotionController.java:25](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L25) `@GetMapping("/api/admin/promotions") @PreAuthorize("hasRole('ADMIN')") public List<PromotionResponse> list(Authentication authentication)`
  - Lời gọi xuất hiện: `promotionService.list()`, `authentication.getName()`
- [PromotionController.java:31](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L31) `@PostMapping("/api/admin/promotions") @PreAuthorize("hasRole('ADMIN')") public PromotionResponse create( Authentication authentication, @Valid @RequestBody PromotionRequest request )`
  - Lời gọi xuất hiện: `promotionService.save()`, `authentication.getName()`
- [PromotionController.java:40](../../src/main/java/com/project/cybershop/promotion/internal/controller/PromotionController.java#L40) `@PutMapping("/api/admin/promotions/{id}") @PreAuthorize("hasRole('ADMIN')") public PromotionResponse update( Authentication authentication, @PathVariable Integer id, @Valid @RequestBody PromotionRequest request )`
  - Lời gọi xuất hiện: `promotionService.save()`, `authentication.getName()`

## `promotion/internal/entity/Banner.java`

### Banner — [Banner.java:8](../../src/main/java/com/project/cybershop/promotion/internal/entity/Banner.java#L8)

Khai báo: `@Entity @Table(name = "banners") @Getter @Setter @NoArgsConstructor public class Banner`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "promotion_id") private Promotion promotion;`
- Trường: `@Column(name = "title", length = 150) private String title;`
- Trường: `@Column(name = "image_url", nullable = false, length = 255) private String imageUrl;`
- Trường: `@Column(name = "link_url", length = 255) private String linkUrl;`
- Trường: `@Column(name = "display_order") private Integer displayOrder = 0;`
- Trường: `@Column(name = "is_active") private Boolean isActive = true;`

## `promotion/internal/entity/Promotion.java`

### Promotion — [Promotion.java:11](../../src/main/java/com/project/cybershop/promotion/internal/entity/Promotion.java#L11)

Khai báo: `@Entity @Table(name = "promotions") @Getter @Setter @NoArgsConstructor public class Promotion`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "code", unique = true, length = 50) private String code;`
- Trường: `@Column(name = "name", nullable = false, length = 150) private String name;`
- Trường: `@Column(name = "discount_type", nullable = false, length = 20) private String discountType;`
- Trường: `@Column(name = "discount_value", nullable = false, precision = 12, scale = 2) private BigDecimal discountValue;`
- Trường: `@Column(name = "usage_limit", nullable = false) private Integer usageLimit;`
- Trường: `@Column(name = "start_date", nullable = false) private LocalDateTime startDate;`
- Trường: `@Column(name = "end_date", nullable = false) private LocalDateTime endDate;`
- Trường: `@Column(name = "is_active") private Boolean isActive = true;`

## `promotion/internal/entity/PromotionUsage.java`

### PromotionUsage — [PromotionUsage.java:10](../../src/main/java/com/project/cybershop/promotion/internal/entity/PromotionUsage.java#L10)

Khai báo: `@Entity @Table(name = "promotion_usages", uniqueConstraints =`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_id", nullable = false) private Promotion promotion;`
- Trường: `@Column(name = "user_id", nullable = false) private Integer userId;`
- Trường: `@Column(name = "order_id", nullable = false, unique = true) private Integer orderId;`
- Trường: `@Column(name = "status") private String status;`
- Trường: `@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount;`

## `promotion/internal/mapper/PromotionMapper.java`

### PromotionMapper — [PromotionMapper.java:12](../../src/main/java/com/project/cybershop/promotion/internal/mapper/PromotionMapper.java#L12)

Khai báo: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR) public interface PromotionMapper`

- [PromotionMapper.java:15](../../src/main/java/com/project/cybershop/promotion/internal/mapper/PromotionMapper.java#L15) `@Mapping(target = "id", ignore = true) @Mapping(target = "code", expression = "java(request.code().trim().toUpperCase(java.util.Locale.ROOT))") @Mapping(target = "name", expression = "java(request.name().trim())") @Mapping(target = "discountType", source = "type") @Mapping(target = "discountValue", source = "value") @Mapping(target = "usageLimit", source = "limit") @Mapping(target = "startDate", source = "start") @Mapping(target = "endDate", source = "end") @Mapping(target = "isActive", source = "active") void updatePromotion(PromotionRequest request, @MappingTarget Promotion promotion)`
- [PromotionMapper.java:26](../../src/main/java/com/project/cybershop/promotion/internal/mapper/PromotionMapper.java#L26) `@Mapping(target = "id", ignore = true) @Mapping(target = "promotion", ignore = true) @Mapping(target = "title", source = "bannerTitle") @Mapping(target = "imageUrl", source = "bannerImage") @Mapping(target = "linkUrl", source = "bannerLink") @Mapping(target = "displayOrder", source = "bannerOrder") @Mapping(target = "isActive", source = "bannerActive") void updateBanner(PromotionRequest request, @MappingTarget Banner banner)`
- [PromotionMapper.java:35](../../src/main/java/com/project/cybershop/promotion/internal/mapper/PromotionMapper.java#L35) `default PromotionResponse toResponse(Promotion promotion, Banner banner, int used, int reserved)`
  - Lời gọi xuất hiện: `promotion.getId()`, `promotion.getCode()`, `promotion.getName()`, `promotion.getDiscountType()`, `promotion.getDiscountValue()`, `promotion.getUsageLimit()`, `promotion.getStartDate()`, `promotion.getEndDate()`, `Boolean.TRUE.equals()`, `promotion.getIsActive()`, `banner.getTitle()`, `banner.getImageUrl()`, `banner.getLinkUrl()`, `banner.getDisplayOrder()`, `banner.getIsActive()`

## `promotion/internal/repository/BannerRepository.java`

### BannerRepository — [BannerRepository.java:9](../../src/main/java/com/project/cybershop/promotion/internal/repository/BannerRepository.java#L9)

Khai báo: `@Repository public interface BannerRepository extends JpaRepository<Banner, Integer>`

- [BannerRepository.java:12](../../src/main/java/com/project/cybershop/promotion/internal/repository/BannerRepository.java#L12) `Optional<Banner> findFirstByPromotionIdOrderByIdAsc(Integer promotionId)`

## `promotion/internal/repository/PromotionLockRepository.java`

### PromotionLockRepository — [PromotionLockRepository.java:8](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepository.java#L8)

Khai báo: `public interface PromotionLockRepository`

- [PromotionLockRepository.java:10](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepository.java#L10) `List<Promotion> findMatchingCodeForUpdate(String code)`
- [PromotionLockRepository.java:12](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepository.java#L12) `Optional<Promotion> findByIdForUpdate(Integer id)`

## `promotion/internal/repository/PromotionLockRepositoryImpl.java`

### PromotionLockRepositoryImpl — [PromotionLockRepositoryImpl.java:12](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepositoryImpl.java#L12)

Khai báo: `public class PromotionLockRepositoryImpl implements PromotionLockRepository`

- Trường: `@PersistenceContext private EntityManager entityManager;`
- [PromotionLockRepositoryImpl.java:17](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepositoryImpl.java#L17) `@Override public List<Promotion> findMatchingCodeForUpdate(String code)`
  - Lời gọi xuất hiện: `entityManager.createQuery(""" select p from Promotion p where lower(p.code) = lower(:code) order by p.id """, Promotion.class) .setParameter("code", code) .setMaxResults(2) .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList()`, `entityManager.createQuery(""" select p from Promotion p where lower(p.code) = lower(:code) order by p.id """, Promotion.class) .setParameter("code", code) .setMaxResults(2).setLockMode()`, `entityManager.createQuery(""" select p from Promotion p where lower(p.code) = lower(:code) order by p.id """, Promotion.class) .setParameter("code", code).setMaxResults()`, `entityManager.createQuery(""" select p from Promotion p where lower(p.code) = lower(:code) order by p.id """, Promotion.class).setParameter()`, `entityManager.createQuery()`, `promotions.forEach()`, `entityManager.refresh()`
- [PromotionLockRepositoryImpl.java:34](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionLockRepositoryImpl.java#L34) `@Override public Optional<Promotion> findByIdForUpdate(Integer id)`
  - Lời gọi xuất hiện: `entityManager.find()`, `entityManager.refresh()`, `Optional.ofNullable()`

## `promotion/internal/repository/PromotionRepository.java`

### PromotionRepository — [PromotionRepository.java:11](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionRepository.java#L11)

Khai báo: `@Repository public interface PromotionRepository extends JpaRepository<Promotion, Integer>, PromotionLockRepository`

- [PromotionRepository.java:14](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionRepository.java#L14) `List<Promotion> findAllByOrderByIdDesc()`
- [PromotionRepository.java:16](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionRepository.java#L16) `boolean existsByCodeIgnoreCase(String code)`
- [PromotionRepository.java:18](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionRepository.java#L18) `boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id)`
- [PromotionRepository.java:20](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionRepository.java#L20) `@Query(""" select new com.project.cybershop.promotion.internal.repository.PromotionTerms( p.id, p.code, p.discountType, p.discountValue, p.usageLimit, p.isActive, p.startDate, p.endDate ) from Promotion p where lower(p.code) = lower(:code) order by p.id """) List<PromotionTerms> findTermsByCodeIgnoreCase(@Param("code") String code)`

## `promotion/internal/repository/PromotionTerms.java`

### PromotionTerms — [PromotionTerms.java:9](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionTerms.java#L9)

Khai báo: `public record PromotionTerms( Integer id, String code, String discountType, BigDecimal discountValue, Integer usageLimit, Boolean active, LocalDateTime startDate, LocalDateTime endDate )`

- [PromotionTerms.java:19](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionTerms.java#L19) `public static PromotionTerms from(Promotion promotion)`
  - Lời gọi xuất hiện: `promotion.getId()`, `promotion.getCode()`, `promotion.getDiscountType()`, `promotion.getDiscountValue()`, `promotion.getUsageLimit()`, `promotion.getIsActive()`, `promotion.getStartDate()`, `promotion.getEndDate()`

## `promotion/internal/repository/PromotionUsageRepository.java`

### PromotionUsageRepository — [PromotionUsageRepository.java:10](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L10)

Khai báo: `@Repository public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer>`

- [PromotionUsageRepository.java:13](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L13) `boolean existsByPromotionIdAndUserId(Integer promotionId, Integer userId)`
- [PromotionUsageRepository.java:15](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L15) `boolean existsByPromotionId(Integer promotionId)`
- [PromotionUsageRepository.java:17](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L17) `int countByPromotionIdAndStatus(Integer promotionId, String status)`
- [PromotionUsageRepository.java:19](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L19) `@Query(""" select count(u) from PromotionUsage u where u.promotion.id = :promotionId and (u.status is null or u.status not in ('REVOKED', 'CANCELED')) """) int countAllocated(@Param("promotionId") Integer promotionId)`
- [PromotionUsageRepository.java:26](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L26) `@Modifying(flushAutomatically = true) @Query(""" update PromotionUsage u set u.status = 'USED' where u.orderId = :orderId and u.status = 'RESERVED' """) void markUsed(@Param("orderId") Integer orderId)`
- [PromotionUsageRepository.java:33](../../src/main/java/com/project/cybershop/promotion/internal/repository/PromotionUsageRepository.java#L33) `@Modifying(flushAutomatically = true) @Query(""" update PromotionUsage u set u.status = 'REVOKED' where u.orderId = :orderId and u.status in ('RESERVED', 'USED') """) void release(@Param("orderId") Integer orderId)`

## `promotion/internal/service/impl/PromotionServiceImpl.java`

### PromotionServiceImpl — [PromotionServiceImpl.java:29](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L29)

Khai báo: `@Service @RequiredArgsConstructor @Transactional public class PromotionServiceImpl implements PromotionService`

- Trường: `private final PromotionRepository promotionRepository;`
- Trường: `private final PromotionUsageRepository usageRepository;`
- Trường: `private final BannerRepository bannerRepository;`
- Trường: `private final PromotionMapper promotionMapper;`
- Trường: `private final IdentityApi identityApi;`
- Trường: `private final Clock clock;`
- [PromotionServiceImpl.java:41](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L41) `@Override public Quote quote(Integer userId, String code, BigDecimal total, boolean lock)`
  - Lời gọi xuất hiện: `code.isBlank()`, `code.trim()`, `promotionRepository.findMatchingCodeForUpdate(normalizedCode).stream() .map(PromotionTerms::from).toList()`, `promotionRepository.findMatchingCodeForUpdate(normalizedCode).stream().map()`, `promotionRepository.findMatchingCodeForUpdate(normalizedCode).stream()`, `promotionRepository.findMatchingCodeForUpdate()`, `promotionRepository.findTermsByCodeIgnoreCase()`, `matches.isEmpty()`, `matches.size()`, `matches.get()`, `LocalDateTime.now()`, `Boolean.TRUE.equals()`, `promotion.active()`, `now.isBefore()`, `promotion.startDate()`, `now.isAfter()`, `promotion.endDate()`, `usageRepository.existsByPromotionIdAndUserId()`, `promotion.id()`, `promotion.usageLimit()`, `usageRepository.countAllocated()`, `calculateDiscount()`, `promotion.code()`, `discount.min()`
- [PromotionServiceImpl.java:81](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L81) `@Override public void reserve(Quote quote, Integer userId, Integer orderId, boolean paid)`
  - Lời gọi xuất hiện: `quote.promotionId()`, `usage.setPromotion()`, `promotionRepository.getReferenceById()`, `usage.setUserId()`, `usage.setOrderId()`, `usage.setStatus()`, `usage.setDiscountAmount()`, `quote.discount()`, `usageRepository.saveAndFlush()`
- [PromotionServiceImpl.java:96](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L96) `@Override public void markUsed(Integer orderId)`
  - Lời gọi xuất hiện: `usageRepository.markUsed()`
- [PromotionServiceImpl.java:101](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L101) `@Override public void release(Integer orderId)`
  - Lời gọi xuất hiện: `usageRepository.release()`
- [PromotionServiceImpl.java:106](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L106) `@Override @Transactional(readOnly = true) public List<PromotionResponse> list(String email)`
  - Lời gọi xuất hiện: `requireAdmin()`, `readAll()`
- [PromotionServiceImpl.java:113](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L113) `@Override @Transactional(readOnly = true) public List<PromotionResponse> publicList()`
  - Lời gọi xuất hiện: `LocalDateTime.now()`, `readAll().stream() .filter(promotion -> promotion.active() && promotion.bannerActive()) .filter(promotion -> !now.isBefore(promotion.start()) && !now.isAfter(promotion.end())) .filter(promotion -> promotion.used() + promotion.reserved() < promotion.limit()).toList()`, `readAll().stream() .filter(promotion -> promotion.active() && promotion.bannerActive()) .filter(promotion -> !now.isBefore(promotion.start()) && !now.isAfter(promotion.end())).filter()`, `readAll().stream() .filter(promotion -> promotion.active() && promotion.bannerActive()).filter()`, `readAll().stream().filter()`, `readAll().stream()`, `readAll()`, `promotion.active()`, `promotion.bannerActive()`, `now.isBefore()`, `promotion.start()`, `now.isAfter()`, `promotion.end()`, `promotion.used()`, `promotion.reserved()`, `promotion.limit()`
- [PromotionServiceImpl.java:124](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L124) `@Override public PromotionResponse save(String email, Integer id, PromotionRequest request)`
  - Lời gọi xuất hiện: `requireAdmin()`, `validateRequest()`, `promotionRepository.findByIdForUpdate(id).orElseThrow()`, `promotionRepository.findByIdForUpdate()`, `request.code().trim().toUpperCase()`, `request.code().trim()`, `request.code()`, `validateExistingPromotion()`, `request.limit()`, `promotionRepository.existsByCodeIgnoreCase()`, `promotionRepository.existsByCodeIgnoreCaseAndIdNot()`, `promotionMapper.updatePromotion()`, `promotionRepository.saveAndFlush()`, `saveBanner()`, `toResponse()`
- [PromotionServiceImpl.java:152](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L152) `private BigDecimal calculateDiscount(PromotionTerms promotion, BigDecimal total)`
  - Lời gọi xuất hiện: `promotion.discountValue()`, `value.signum()`, `promotion.discountType()`, `promotion.discountType().trim().toLowerCase()`, `promotion.discountType().trim()`, `value.compareTo()`, `BigDecimal.valueOf()`, `total.multiply(value).divide()`, `total.multiply()`
- [PromotionServiceImpl.java:172](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L172) `private void validateRequest(PromotionRequest request)`
  - Lời gọi xuất hiện: `request.code().trim().toUpperCase()`, `request.code().trim()`, `request.code()`, `code.matches()`, `request.name().isBlank()`, `request.name()`, `List.of("percentage", "fixed_amount").contains()`, `List.of()`, `request.type()`, `request.value().signum()`, `request.value()`, `request.type().equals()`, `request.value().compareTo()`, `BigDecimal.valueOf()`, `request.end().isAfter()`, `request.end()`, `request.start()`, `bannerLink()`, `bannerImage()`, `link.startsWith()`, `link.contains()`, `image.startsWith()`, `image.isBlank()`, `image.contains()`, `request.bannerActive()`
- [PromotionServiceImpl.java:203](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L203) `private void validateExistingPromotion(Promotion promotion, String code, int limit)`
  - Lời gọi xuất hiện: `usageRepository.countAllocated()`, `promotion.getId()`, `promotion.getCode().equals()`, `promotion.getCode()`, `usageRepository.existsByPromotionId()`
- [PromotionServiceImpl.java:212](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L212) `private void saveBanner(Promotion promotion, PromotionRequest request)`
  - Lời gọi xuất hiện: `bannerRepository.findFirstByPromotionIdOrderByIdAsc(promotion.getId()).orElse()`, `bannerRepository.findFirstByPromotionIdOrderByIdAsc()`, `promotion.getId()`, `bannerImage(request).isBlank()`, `bannerImage()`, `banner.setPromotion()`, `promotionMapper.updateBanner()`, `banner.setImageUrl()`, `banner.setLinkUrl()`, `bannerLink()`, `bannerRepository.saveAndFlush()`
- [PromotionServiceImpl.java:229](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L229) `private String bannerImage(PromotionRequest request)`
  - Lời gọi xuất hiện: `request.bannerImage()`, `request.bannerImage().trim()`
- [PromotionServiceImpl.java:233](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L233) `private String bannerLink(PromotionRequest request)`
  - Lời gọi xuất hiện: `request.bannerLink()`, `request.bannerLink().isBlank()`, `request.bannerLink().trim()`
- [PromotionServiceImpl.java:238](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L238) `private void requireAdmin(String email)`
  - Lời gọi xuất hiện: `"Admin".equalsIgnoreCase()`, `identityApi.requireActiveUser(email).getRole()`, `identityApi.requireActiveUser()`
- [PromotionServiceImpl.java:244](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L244) `private List<PromotionResponse> readAll()`
  - Lời gọi xuất hiện: `promotionRepository.findAllByOrderByIdDesc().stream() .map(this::toResponse).toList()`, `promotionRepository.findAllByOrderByIdDesc().stream().map()`, `promotionRepository.findAllByOrderByIdDesc().stream()`, `promotionRepository.findAllByOrderByIdDesc()`
- [PromotionServiceImpl.java:250](../../src/main/java/com/project/cybershop/promotion/internal/service/impl/PromotionServiceImpl.java#L250) `private PromotionResponse toResponse(Promotion promotion)`
  - Lời gọi xuất hiện: `bannerRepository.findFirstByPromotionIdOrderByIdAsc(promotion.getId()).orElse()`, `bannerRepository.findFirstByPromotionIdOrderByIdAsc()`, `promotion.getId()`, `usageRepository.countByPromotionIdAndStatus()`, `promotionMapper.toResponse()`

## `promotion/internal/service/PromotionService.java`

### PromotionService — [PromotionService.java:8](../../src/main/java/com/project/cybershop/promotion/internal/service/PromotionService.java#L8)

Khai báo: `public interface PromotionService extends PromotionApi`

- [PromotionService.java:9](../../src/main/java/com/project/cybershop/promotion/internal/service/PromotionService.java#L9) `List<PromotionResponse> list(String email)`
- [PromotionService.java:10](../../src/main/java/com/project/cybershop/promotion/internal/service/PromotionService.java#L10) `PromotionResponse save(String email, Integer id, PromotionRequest request)`
- [PromotionService.java:11](../../src/main/java/com/project/cybershop/promotion/internal/service/PromotionService.java#L11) `List<PromotionResponse> publicList()`

