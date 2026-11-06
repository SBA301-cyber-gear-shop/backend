# Tra cứu module pcbuild

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **POST `/api/v1/pc-build/ai-suggest`** → `PcBuildController.suggestBuild` [PcBuildController.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L49)
  - `@PostMapping("/ai-suggest") @Operation(summary = "Đề xuất cấu hình PC bằng AI dựa trên Database thực tế") public ResponseEntity<ApiResponseDTO<AiBuildResponse>> suggestBuild( @Valid @RequestBody AiBuildRequest request, Principal principal, HttpServletRequest httpRequest )`
- **GET `/api/v1/pc-build/options`** → `PcBuildController.options` [PcBuildController.java:62](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L62)
  - `@GetMapping("/options") @Operation(summary = "Giới hạn ngân sách, từ điển metadata và câu gợi ý nhu cầu cho form xây dựng cấu hình") public ResponseEntity<ApiResponseDTO<BuildOptionsResponse>> options(Principal principal, HttpServletRequest httpRequest)`
- **POST `/api/v1/pc-build/validate`** → `PcBuildController.validateBuild` [PcBuildController.java:69](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L69)
  - `@PostMapping("/validate") @Operation(summary = "Kiểm tra tương thích, ngân sách và tồn kho của toàn bộ cấu hình (giá lấy từ server)") public ResponseEntity<ApiResponseDTO<ValidateBuildResponse>> validateBuild( @Valid @RequestBody ValidateBuildRequest request, Principal principal, HttpServletRequest httpRequest )`
- **POST `/api/v1/pc-build/compatible-replacements`** → `PcBuildController.queryReplacements` [PcBuildController.java:80](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L80)
  - `@PostMapping("/compatible-replacements") @Operation(summary = "Danh sách linh kiện thay thế, mỗi ứng viên được kiểm trên toàn bộ cấu hình hiện tại") public ResponseEntity<ApiResponseDTO<List<CompatibleReplacementResponse>>> queryReplacements( @Valid @RequestBody ReplacementQueryRequest request, Principal principal, HttpServletRequest httpRequest )`
- **POST `/api/v1/pc-build/peripherals`** → `PcBuildController.suggestPeripherals` [PcBuildController.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L91)
  - `@PostMapping("/peripherals") @Operation(summary = "Chọn màn hình, bàn phím, chuột cho chế độ Trọn bộ theo tiền còn lại (giá lấy từ server, không gọi AI)") public ResponseEntity<ApiResponseDTO<PeripheralSuggestionResponse>> suggestPeripherals( @Valid @RequestBody PeripheralQueryRequest request, Principal principal, HttpServletRequest httpRequest )`
- **POST `/api/v1/pc-build/save`** → `PcBuildController.saveCustomBuild` [PcBuildController.java:102](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L102)
  - `@PostMapping("/save") @Operation(summary = "Lưu cấu hình PC từ danh sách linh kiện đã chọn vào tài khoản") public ResponseEntity<ApiResponseDTO<Integer>> saveCustomBuild( @Valid @RequestBody SavePcBuildRequest request, Principal principal )`
- **GET `/api/v1/pc-build/my-builds`** → `PcBuildController.getMySavedBuilds` [PcBuildController.java:122](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L122)
  - `@GetMapping("/my-builds") @Operation(summary = "Lấy danh sách các cấu hình PC đã lưu của người dùng hiện tại") public ResponseEntity<ApiResponseDTO<List<SavedPcBuildSummaryResponse>>> getMySavedBuilds( Principal principal )`
- **GET `/api/v1/pc-build/{buildId}`** → `PcBuildController.getSavedBuildDetail` [PcBuildController.java:141](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L141)
  - `@GetMapping("/{buildId}") @Operation(summary = "Xem chi tiết một cấu hình PC đã lưu theo buildId") public ResponseEntity<ApiResponseDTO<SavedPcBuildDetailResponse>> getSavedBuildDetail( @PathVariable Integer buildId, Principal principal )`
- **DELETE `/api/v1/pc-build/{buildId}`** → `PcBuildController.deleteSavedBuild` [PcBuildController.java:161](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L161)
  - `@DeleteMapping("/{buildId}") @Operation(summary = "Xóa một cấu hình PC đã lưu") public ResponseEntity<ApiResponseDTO<Void>> deleteSavedBuild( @PathVariable Integer buildId, Principal principal )`

## Khai báo và lời gọi theo file

## `pcbuild/api/dto/profile/BalancePairDto.java`

### BalancePairDto — [BalancePairDto.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/BalancePairDto.java#L11)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class BalancePairDto`

- Trường: `private String a;`
- Trường: `private String b;`
- Trường: `private Double maxGap;`

## `pcbuild/api/dto/profile/DroppedItemDto.java`

### DroppedItemDto — [DroppedItemDto.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/DroppedItemDto.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class DroppedItemDto`

- Trường: `private String id;`
- Trường: `private String reason;`
- Trường: `private String detail;`

## `pcbuild/api/dto/profile/ProfileConstraintDto.java`

### ProfileConstraintDto — [ProfileConstraintDto.java:14](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/ProfileConstraintDto.java#L14)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class ProfileConstraintDto`

- Trường: `private String id;`
- Trường: `private String role;`
- Trường: `private String spec;`
- Trường: `private String op;`
- Trường: `private Double value;`
- Trường: `@Builder.Default private List<String> values = new ArrayList<>();`
- Trường: `private Integer priority;`
- Trường: `private String reason;`
- Trường: `private String label;`
- Trường: `private Boolean satisfied;`
- Trường: `private Boolean unsatisfiable;`

## `pcbuild/api/dto/profile/ProfileMetricDto.java`

### ProfileMetricDto — [ProfileMetricDto.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/ProfileMetricDto.java#L11)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class ProfileMetricDto`

- Trường: `private String id;`
- Trường: `private String metric;`
- Trường: `private String op;`
- Trường: `private Double value;`
- Trường: `private Integer priority;`
- Trường: `private String reason;`
- Trường: `private String label;`
- Trường: `private Boolean satisfied;`
- Trường: `private Boolean unsatisfiable;`

## `pcbuild/api/dto/profile/ProfilePreferenceDto.java`

### ProfilePreferenceDto — [ProfilePreferenceDto.java:14](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/ProfilePreferenceDto.java#L14)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class ProfilePreferenceDto`

- Trường: `private String id;`
- Trường: `private String role;`
- Trường: `private String spec;`
- Trường: `private String metric;`
- Trường: `private String direction;`
- Trường: `@Builder.Default private List<String> values = new ArrayList<>();`
- Trường: `private Double weight;`
- Trường: `private Double cap;`
- Trường: `private String reason;`
- [ProfilePreferenceDto.java:36](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/ProfilePreferenceDto.java#L36) `public boolean targetsMetric()`
  - Lời gọi xuất hiện: `metric.isBlank()`, `role.isBlank()`, `spec.isBlank()`

## `pcbuild/api/dto/profile/ProfileRoleRefDto.java`

### ProfileRoleRefDto — [ProfileRoleRefDto.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/ProfileRoleRefDto.java#L11)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class ProfileRoleRefDto`

- Trường: `private String role;`
- Trường: `private Integer priority;`
- Trường: `private String reason;`

## `pcbuild/api/dto/profile/RequirementProfileDto.java`

### RequirementProfileDto — [RequirementProfileDto.java:19](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/RequirementProfileDto.java#L19)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true) public class RequirementProfileDto`

- Trường: `private Integer profileVersion;`
- Trường: `private String source;`
- Trường: `private String summary;`
- Trường: `private String archetype;`
- Trường: `@Builder.Default private List<String> useCases = new ArrayList<>();`
- Trường: `@Builder.Default private Map<String, Double> roleWeights = new LinkedHashMap<>();`
- Trường: `@Builder.Default private List<ProfileRoleRefDto> requiredRoles = new ArrayList<>();`
- Trường: `@Builder.Default private List<ProfileRoleRefDto> excludedRoles = new ArrayList<>();`
- Trường: `@Builder.Default private List<ProfileConstraintDto> constraints = new ArrayList<>();`
- Trường: `@Builder.Default private List<ProfileMetricDto> metrics = new ArrayList<>();`
- Trường: `@Builder.Default private List<ProfilePreferenceDto> preferences = new ArrayList<>();`
- Trường: `@Builder.Default private List<BalancePairDto> balancePairs = new ArrayList<>();`
- Trường: `@Builder.Default private Brands brands = new Brands();`
- Trường: `@Builder.Default private List<String> unmetNeeds = new ArrayList<>();`
- Trường: `private String objective;`
- Trường: `private Double confidence;`
- Trường: `private String clarifyingQuestion;`
- Trường: `@Builder.Default private List<DroppedItemDto> droppedItems = new ArrayList<>();`

### Brands — [RequirementProfileDto.java:56](../../src/main/java/com/project/cybershop/pcbuild/api/dto/profile/RequirementProfileDto.java#L56)

Khai báo: `@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true) public static class Brands`

- Trường: `private List<String> prefer = new ArrayList<>();`
- Trường: `private List<String> avoid = new ArrayList<>();`

## `pcbuild/api/dto/request/AiBuildRequest.java`

### AiBuildRequest — [AiBuildRequest.java:19](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/AiBuildRequest.java#L19)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class AiBuildRequest`

- Trường: `@NotNull(message = "Vui lòng nhập ngân sách") @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0") @Digits(integer = 13, fraction = 0, message = "Ngân sách phải là số nguyên đồng") private BigDecimal budget;`
- Trường: `@Size(max = 300, message = "Nhu cầu tối đa 300 ký tự") private String purpose;`
- Trường: `private Integer ramGb;`
- Trường: `private String scope;`
- Trường: `@Size(max = 500, message = "Mong muốn thêm tối đa 500 ký tự") private String userPrompt;`
- Trường: `private RequirementProfileDto profile;`
- Trường: `@Digits(integer = 13, fraction = 0, message = "Ngân sách trọn bộ phải là số nguyên đồng") private BigDecimal fullSetBudget;`

## `pcbuild/api/dto/request/PeripheralQueryRequest.java`

### PeripheralQueryRequest — [PeripheralQueryRequest.java:18](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/PeripheralQueryRequest.java#L18)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class PeripheralQueryRequest`

- Trường: `@NotNull(message = "Ngân sách không được để trống") @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0") @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫") private BigDecimal budget;`
- Trường: `private String archetype;`
- Trường: `@Valid @NotEmpty(message = "Danh sách linh kiện không được để trống") @Size(max = 16, message = "Tối đa 16 dòng linh kiện") private List<ValidateBuildRequest.ItemPart> items;`

## `pcbuild/api/dto/request/ReplacementQueryRequest.java`

### ReplacementQueryRequest — [ReplacementQueryRequest.java:20](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/ReplacementQueryRequest.java#L20)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ReplacementQueryRequest`

- Trường: `@NotBlank(message = "Vai trò cần thay không được để trống") private String role;`
- Trường: `@DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0") @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫") private BigDecimal budget;`
- Trường: `@Valid @Builder.Default @Size(max = 16, message = "Tối đa 16 dòng linh kiện") private List<ValidateBuildRequest.ItemPart> items = new ArrayList<>();`
- Trường: `@Min(value = 0, message = "lineIndex không hợp lệ") private Integer lineIndex;`
- Trường: `@Min(value = 1, message = "limit tối thiểu là 1") @Max(value = 100, message = "limit tối đa là 100") private Integer limit;`
- Trường: `private com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto profile;`

## `pcbuild/api/dto/request/SavePcBuildRequest.java`

### SavePcBuildRequest — [SavePcBuildRequest.java:15](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/SavePcBuildRequest.java#L15)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class SavePcBuildRequest`

- Trường: `private String buildName;`
- Trường: `private BigDecimal totalPrice;`
- Trường: `@Valid @NotEmpty(message = "Danh sách linh kiện không được để trống") private List<SavePartItem> items;`

### SavePartItem — [SavePcBuildRequest.java:29](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/SavePcBuildRequest.java#L29)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class SavePartItem`

- Trường: `@NotNull(message = "Variant ID không được để trống") private Integer variantId;`
- Trường: `private String role;`
- Trường: `@NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng phải lớn hơn 0") private Integer quantity;`
- Trường: `private BigDecimal unitPrice;`

## `pcbuild/api/dto/request/ValidateBuildRequest.java`

### ValidateBuildRequest — [ValidateBuildRequest.java:22](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/ValidateBuildRequest.java#L22)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ValidateBuildRequest`

- Trường: `@DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0") @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫") private BigDecimal budget;`
- Trường: `@Valid @NotEmpty(message = "Danh sách linh kiện không được để trống") @Size(max = 16, message = "Tối đa 16 dòng linh kiện") private List<ItemPart> items;`
- Trường: `private RequirementProfileDto profile;`
- [ValidateBuildRequest.java:39](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/ValidateBuildRequest.java#L39) `public ValidateBuildRequest(BigDecimal budget, List<ItemPart> items)`

### ItemPart — [ValidateBuildRequest.java:43](../../src/main/java/com/project/cybershop/pcbuild/api/dto/request/ValidateBuildRequest.java#L43)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class ItemPart`

- Trường: `@NotBlank(message = "Vai trò linh kiện không được để trống") private String role;`
- Trường: `@NotNull(message = "Variant ID không được để trống") private Integer variantId;`
- Trường: `@NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng tối thiểu là 1") @Max(value = 8, message = "Số lượng tối đa là 8") private Integer quantity;`

## `pcbuild/api/dto/response/AiBuildMetadataResponse.java`

### AiBuildMetadataResponse — [AiBuildMetadataResponse.java:12](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/AiBuildMetadataResponse.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class AiBuildMetadataResponse`

- Trường: `private String requestId;`
- Trường: `private OffsetDateTime generatedAt;`
- Trường: `private String extractStatus;`
- Trường: `private String aiStatus;`
- Trường: `private String provider;`
- Trường: `private String model;`
- Trường: `private Map<String, String> promptVersions;`
- Trường: `private String dictionaryVersion;`
- Trường: `private Map<String, Long> latencyMs;`
- Trường: `private Integer candidateCount;`
- Trường: `private Integer solverIterations;`

## `pcbuild/api/dto/response/AiBuildResponse.java`

### AiBuildResponse — [AiBuildResponse.java:14](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/AiBuildResponse.java#L14)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class AiBuildResponse`

- Trường: `private Integer buildId;`
- Trường: `private String buildName;`
- Trường: `private String purpose;`
- Trường: `private BigDecimal totalPrice;`
- Trường: `private BigDecimal budget;`
- Trường: `private String aiReasoning;`
- Trường: `private String performanceHighlight;`
- Trường: `@JsonProperty("isCompatible") private boolean isCompatible;`
- Trường: `private List<CompatibilityCheckResponse> checks;`
- Trường: `private List<SelectedPartResponse> items;`
- Trường: `private String reasonCode;`
- Trường: `private BudgetFitResponse budgetFit;`
- Trường: `private RequirementProfileDto profile;`
- Trường: `private List<String> relaxedItems;`
- Trường: `private List<String> warnings;`
- Trường: `private CapacityResponse capacity;`
- Trường: `private Integer requiredPsuW;`
- Trường: `private BigDecimal floorPrice;`
- Trường: `private AiBuildMetadataResponse metadata;`

## `pcbuild/api/dto/response/BudgetFitResponse.java`

### BudgetFitResponse — [BudgetFitResponse.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/BudgetFitResponse.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class BudgetFitResponse`

- Trường: `private BigDecimal ratio;`
- Trường: `private BigDecimal percent;`
- Trường: `private String status;`
- Trường: `private BigDecimal windowMin;`
- Trường: `private BigDecimal windowMax;`
- Trường: `private BigDecimal leftover;`

## `pcbuild/api/dto/response/BuildOptionsResponse.java`

### BuildOptionsResponse — [BuildOptionsResponse.java:12](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/BuildOptionsResponse.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class BuildOptionsResponse`

- Trường: `private BigDecimal minBudget;`
- Trường: `private BigDecimal floorPrice;`
- Trường: `private BigDecimal maxBudget;`
- Trường: `private String dictionaryVersion;`
- Trường: `private MetadataDictionaryResponse dictionary;`
- Trường: `private List<String> examples;`
- Trường: `private List<Tier> tiers;`
- Trường: `private BigDecimal peripheralMinTotal;`
- Trường: `private BigDecimal peripheralMaxTotal;`

### Tier — [BuildOptionsResponse.java:28](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/BuildOptionsResponse.java#L28)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class Tier`

- Trường: `private String code;`
- Trường: `private Long min;`
- Trường: `private Long max;`

## `pcbuild/api/dto/response/CapacityResponse.java`

### CapacityResponse — [CapacityResponse.java:9](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CapacityResponse.java#L9)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class CapacityResponse`

- Trường: `private Integer ramSlotsUsed;`
- Trường: `private Integer ramSlotsMax;`
- Trường: `private Integer ramGb;`
- Trường: `private Integer ramMaxGb;`
- Trường: `private Integer m2Used;`
- Trường: `private Integer m2Max;`
- Trường: `private Integer gpuCount;`
- Trường: `private Integer gpuMax;`

## `pcbuild/api/dto/response/CompatibilityCheckResponse.java`

### CompatibilityCheckResponse — [CompatibilityCheckResponse.java:8](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L8)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class CompatibilityCheckResponse`

- Trường: `private String key;`
- Trường: `private String label;`
- Trường: `private String status;`
- Trường: `private String detail;`
- Trường: `private Boolean valid;`
- [CompatibilityCheckResponse.java:19](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L19) `public CompatibilityCheckResponse(String label, String status, String detail)`
  - Lời gọi xuất hiện: `isValidStatus()`
- [CompatibilityCheckResponse.java:26](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L26) `public CompatibilityCheckResponse(String key, String label, boolean valid, String detail)`
- [CompatibilityCheckResponse.java:34](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L34) `public static CompatibilityCheckResponse of(String key, String label, String status, String detail)`
  - Lời gọi xuất hiện: `isValidStatus()`
- [CompatibilityCheckResponse.java:39](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L39) `public boolean isValid()`
  - Lời gọi xuất hiện: `isValidStatus()`, `Boolean.TRUE.equals()`
- [CompatibilityCheckResponse.java:43](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibilityCheckResponse.java#L43) `private static boolean isValidStatus(String status)`
  - Lời gọi xuất hiện: `"fail".equalsIgnoreCase()`

## `pcbuild/api/dto/response/CompatibleReplacementResponse.java`

### CompatibleReplacementResponse — [CompatibleReplacementResponse.java:13](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/CompatibleReplacementResponse.java#L13)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class CompatibleReplacementResponse`

- Trường: `private Integer variantId;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private BigDecimal price;`
- Trường: `private String imageUrl;`
- Trường: `private Integer stockQuantity;`
- Trường: `private Map<String, String> specs;`
- Trường: `@JsonProperty("isCompatible") private boolean isCompatible;`
- Trường: `private String compatibilityNote;`
- Trường: `private String status;`
- Trường: `private List<String> failedChecks;`
- Trường: `private List<String> warningChecks;`
- Trường: `private BigDecimal priceDelta;`
- Trường: `private BigDecimal newTotal;`
- Trường: `private BigDecimal newPercent;`

## `pcbuild/api/dto/response/MetadataDictionaryResponse.java`

### MetadataDictionaryResponse — [MetadataDictionaryResponse.java:19](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/MetadataDictionaryResponse.java#L19)

Khai báo: `@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) public class MetadataDictionaryResponse`

- Trường: `private String dictionaryVersion;`
- Trường: `@Builder.Default private Map<String, RoleEntry> roles = new LinkedHashMap<>();`
- Trường: `@Builder.Default private Map<String, MetricEntry> metrics = new LinkedHashMap<>();`

### RoleEntry — [MetadataDictionaryResponse.java:31](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/MetadataDictionaryResponse.java#L31)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) public static class RoleEntry`

- Trường: `@JsonIgnore private Integer categoryId;`
- Trường: `@JsonIgnore private List<String> primarySpecs;`
- Trường: `private String label;`
- Trường: `private List<BigDecimal> priceRange;`
- Trường: `private List<String> brands;`
- Trường: `@Builder.Default private Map<String, SpecEntry> specs = new LinkedHashMap<>();`

### SpecEntry — [MetadataDictionaryResponse.java:49](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/MetadataDictionaryResponse.java#L49)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) public static class SpecEntry`

- Trường: `@JsonIgnore private Integer specId;`
- Trường: `private String type;`
- Trường: `private String unit;`
- Trường: `private Integer min;`
- Trường: `private Integer max;`
- Trường: `private Map<String, String> values;`

### MetricEntry — [MetadataDictionaryResponse.java:64](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/MetadataDictionaryResponse.java#L64)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL) public static class MetricEntry`

- Trường: `private String label;`
- Trường: `private String unit;`
- Trường: `private Integer min;`
- Trường: `private Integer max;`

## `pcbuild/api/dto/response/PeripheralSuggestionResponse.java`

### PeripheralSuggestionResponse — [PeripheralSuggestionResponse.java:12](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/PeripheralSuggestionResponse.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class PeripheralSuggestionResponse`

- Trường: `private BigDecimal pcTotal;`
- Trường: `private BigDecimal available;`
- Trường: `private BigDecimal spendLimit;`
- Trường: `private BigDecimal peripheralTotal;`
- Trường: `private BigDecimal grandTotal;`
- Trường: `private List<RoleEntry> roles;`

### RoleEntry — [PeripheralSuggestionResponse.java:24](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/PeripheralSuggestionResponse.java#L24)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class RoleEntry`

- Trường: `private String role;`
- Trường: `private String label;`
- Trường: `private Item picked;`
- Trường: `private List<Item> options;`

### Item — [PeripheralSuggestionResponse.java:35](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/PeripheralSuggestionResponse.java#L35)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public static class Item`

- Trường: `private Integer variantId;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private String brandName;`
- Trường: `private String imageUrl;`
- Trường: `private BigDecimal price;`
- Trường: `private Integer stockQuantity;`
- Trường: `private List<String> highlights;`
- Trường: `private String reason;`

## `pcbuild/api/dto/response/SavedPcBuildDetailResponse.java`

### SavedPcBuildDetailResponse — [SavedPcBuildDetailResponse.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/SavedPcBuildDetailResponse.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class SavedPcBuildDetailResponse`

- Trường: `private Integer buildId;`
- Trường: `private String buildName;`
- Trường: `private BigDecimal totalPrice;`
- Trường: `private Integer orderId;`
- Trường: `private boolean isOrdered;`
- Trường: `private List<SelectedPartResponse> items;`

## `pcbuild/api/dto/response/SavedPcBuildSummaryResponse.java`

### SavedPcBuildSummaryResponse — [SavedPcBuildSummaryResponse.java:10](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/SavedPcBuildSummaryResponse.java#L10)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class SavedPcBuildSummaryResponse`

- Trường: `private Integer buildId;`
- Trường: `private String buildName;`
- Trường: `private BigDecimal totalPrice;`
- Trường: `private Integer orderId;`
- Trường: `private boolean isOrdered;`
- Trường: `private Integer itemCount;`
- Trường: `private String cpuName;`
- Trường: `private String gpuName;`
- Trường: `private String mainboardName;`
- Trường: `private String ramSummary;`

## `pcbuild/api/dto/response/SelectedPartResponse.java`

### SelectedPartResponse — [SelectedPartResponse.java:11](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/SelectedPartResponse.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class SelectedPartResponse`

- Trường: `private String role;`
- Trường: `private Integer variantId;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private String imageUrl;`
- Trường: `private BigDecimal unitPrice;`
- Trường: `private Integer quantity;`
- Trường: `private BigDecimal subtotal;`
- Trường: `private Map<String, String> specs;`
- Trường: `private String aiReason;`

## `pcbuild/api/dto/response/ValidateBuildResponse.java`

### ValidateBuildResponse — [ValidateBuildResponse.java:13](../../src/main/java/com/project/cybershop/pcbuild/api/dto/response/ValidateBuildResponse.java#L13)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ValidateBuildResponse`

- Trường: `@JsonProperty("isCompatible") private boolean isCompatible;`
- Trường: `@JsonProperty("hasWarnings") private boolean hasWarnings;`
- Trường: `@JsonProperty("hasUnknown") private boolean hasUnknown;`
- Trường: `private BigDecimal totalPrice;`
- Trường: `private BudgetFitResponse budgetFit;`
- Trường: `private List<CompatibilityCheckResponse> checks;`
- Trường: `private int requiredPsuW;`
- Trường: `private CapacityResponse capacity;`
- Trường: `private List<SelectedPartResponse> items;`

## `pcbuild/internal/controller/PcBuildController.java`

### PcBuildController — [PcBuildController.java:37](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L37)

Khai báo: `@RestController @RequestMapping("/api/v1/pc-build") @RequiredArgsConstructor @Tag(name = "PC Build API", description = "Chức năng xây dựng cấu hình PC bằng AI & Kiểm tra tương thích") public class PcBuildController`

- Trường: `private final AiPcBuilderService aiPcBuilderService;`
- Trường: `private final PcCustomService pcCustomService;`
- Trường: `private final PeripheralService peripheralService;`
- Trường: `private final IdentityApi identityApi;`
- Trường: `private final RateLimiter rateLimiter;`
- [PcBuildController.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L49) `@PostMapping("/ai-suggest") @Operation(summary = "Đề xuất cấu hình PC bằng AI dựa trên Database thực tế") public ResponseEntity<ApiResponseDTO<AiBuildResponse>> suggestBuild( @Valid @RequestBody AiBuildRequest request, Principal principal, HttpServletRequest httpRequest )`
  - Lời gọi xuất hiện: `rateLimiter.acquire()`, `clientKey()`, `resolveUserId()`, `aiPcBuilderService.processAiBuild()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [PcBuildController.java:62](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L62) `@GetMapping("/options") @Operation(summary = "Giới hạn ngân sách, từ điển metadata và câu gợi ý nhu cầu cho form xây dựng cấu hình") public ResponseEntity<ApiResponseDTO<BuildOptionsResponse>> options(Principal principal, HttpServletRequest httpRequest)`
  - Lời gọi xuất hiện: `rateLimiter.acquire()`, `clientKey()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `aiPcBuilderService.options()`
- [PcBuildController.java:69](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L69) `@PostMapping("/validate") @Operation(summary = "Kiểm tra tương thích, ngân sách và tồn kho của toàn bộ cấu hình (giá lấy từ server)") public ResponseEntity<ApiResponseDTO<ValidateBuildResponse>> validateBuild( @Valid @RequestBody ValidateBuildRequest request, Principal principal, HttpServletRequest httpRequest )`
  - Lời gọi xuất hiện: `rateLimiter.acquire()`, `clientKey()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `pcCustomService.validateBuild()`
- [PcBuildController.java:80](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L80) `@PostMapping("/compatible-replacements") @Operation(summary = "Danh sách linh kiện thay thế, mỗi ứng viên được kiểm trên toàn bộ cấu hình hiện tại") public ResponseEntity<ApiResponseDTO<List<CompatibleReplacementResponse>>> queryReplacements( @Valid @RequestBody ReplacementQueryRequest request, Principal principal, HttpServletRequest httpRequest )`
  - Lời gọi xuất hiện: `rateLimiter.acquire()`, `clientKey()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `pcCustomService.getCompatibleReplacements()`
- [PcBuildController.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L91) `@PostMapping("/peripherals") @Operation(summary = "Chọn màn hình, bàn phím, chuột cho chế độ Trọn bộ theo tiền còn lại (giá lấy từ server, không gọi AI)") public ResponseEntity<ApiResponseDTO<PeripheralSuggestionResponse>> suggestPeripherals( @Valid @RequestBody PeripheralQueryRequest request, Principal principal, HttpServletRequest httpRequest )`
  - Lời gọi xuất hiện: `rateLimiter.acquire()`, `clientKey()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `peripheralService.suggest()`
- [PcBuildController.java:102](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L102) `@PostMapping("/save") @Operation(summary = "Lưu cấu hình PC từ danh sách linh kiện đã chọn vào tài khoản") public ResponseEntity<ApiResponseDTO<Integer>> saveCustomBuild( @Valid @RequestBody SavePcBuildRequest request, Principal principal )`
  - Lời gọi xuất hiện: `resolveUserId()`, `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.<Integer>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để lưu cấu hình") .data(null).build()`, `ApiResponseDTO.<Integer>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để lưu cấu hình").data()`, `ApiResponseDTO.<Integer>builder() .status(HttpStatus.UNAUTHORIZED.value()).message()`, `ApiResponseDTO.<Integer>builder().status()`, `ApiResponseDTO.builder()`, `HttpStatus.UNAUTHORIZED.value()`, `pcCustomService.saveNewBuild()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [PcBuildController.java:122](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L122) `@GetMapping("/my-builds") @Operation(summary = "Lấy danh sách các cấu hình PC đã lưu của người dùng hiện tại") public ResponseEntity<ApiResponseDTO<List<SavedPcBuildSummaryResponse>>> getMySavedBuilds( Principal principal )`
  - Lời gọi xuất hiện: `resolveUserId()`, `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.<List<SavedPcBuildSummaryResponse>>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để xem danh sách cấu hình đã lưu") .data(null).build()`, `ApiResponseDTO.<List<SavedPcBuildSummaryResponse>>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để xem danh sách cấu hình đã lưu").data()`, `ApiResponseDTO.<List<SavedPcBuildSummaryResponse>>builder() .status(HttpStatus.UNAUTHORIZED.value()).message()`, `ApiResponseDTO.<List<SavedPcBuildSummaryResponse>>builder().status()`, `ApiResponseDTO.builder()`, `HttpStatus.UNAUTHORIZED.value()`, `pcCustomService.getMySavedBuilds()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [PcBuildController.java:141](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L141) `@GetMapping("/{buildId}") @Operation(summary = "Xem chi tiết một cấu hình PC đã lưu theo buildId") public ResponseEntity<ApiResponseDTO<SavedPcBuildDetailResponse>> getSavedBuildDetail( @PathVariable Integer buildId, Principal principal )`
  - Lời gọi xuất hiện: `resolveUserId()`, `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.<SavedPcBuildDetailResponse>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để xem chi tiết cấu hình") .data(null).build()`, `ApiResponseDTO.<SavedPcBuildDetailResponse>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để xem chi tiết cấu hình").data()`, `ApiResponseDTO.<SavedPcBuildDetailResponse>builder() .status(HttpStatus.UNAUTHORIZED.value()).message()`, `ApiResponseDTO.<SavedPcBuildDetailResponse>builder().status()`, `ApiResponseDTO.builder()`, `HttpStatus.UNAUTHORIZED.value()`, `pcCustomService.getSavedBuildDetail()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [PcBuildController.java:161](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L161) `@DeleteMapping("/{buildId}") @Operation(summary = "Xóa một cấu hình PC đã lưu") public ResponseEntity<ApiResponseDTO<Void>> deleteSavedBuild( @PathVariable Integer buildId, Principal principal )`
  - Lời gọi xuất hiện: `resolveUserId()`, `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.<Void>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để thực hiện thao tác này") .data(null).build()`, `ApiResponseDTO.<Void>builder() .status(HttpStatus.UNAUTHORIZED.value()) .message("Vui lòng đăng nhập để thực hiện thao tác này").data()`, `ApiResponseDTO.<Void>builder() .status(HttpStatus.UNAUTHORIZED.value()).message()`, `ApiResponseDTO.<Void>builder().status()`, `ApiResponseDTO.builder()`, `HttpStatus.UNAUTHORIZED.value()`, `pcCustomService.deleteSavedBuild()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [PcBuildController.java:185](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L185) `private static String clientKey(Principal principal, HttpServletRequest httpRequest)`
  - Lời gọi xuất hiện: `principal.getName()`, `httpRequest.getRemoteAddr()`
- [PcBuildController.java:192](../../src/main/java/com/project/cybershop/pcbuild/internal/controller/PcBuildController.java#L192) `private Integer resolveUserId(Principal principal)`
  - Lời gọi xuất hiện: `principal.getName()`, `identityApi.getUserByEmail(principal.getName()) .map(UserDto::getId).orElse()`, `identityApi.getUserByEmail(principal.getName()).map()`, `identityApi.getUserByEmail()`, `SecurityContextHolder.getContext().getAuthentication()`, `SecurityContextHolder.getContext()`, `auth.isAuthenticated()`, `identityApi.getUserByEmail(auth.getName()) .map(UserDto::getId).orElse()`, `identityApi.getUserByEmail(auth.getName()).map()`, `auth.getName()`

## `pcbuild/internal/entity/CompatibilityRule.java`

### CompatibilityRule — [CompatibilityRule.java:9](../../src/main/java/com/project/cybershop/pcbuild/internal/entity/CompatibilityRule.java#L9)

Khai báo: `@Entity @Table(name = "compatibility_rules") @Getter @Setter @NoArgsConstructor @AllArgsConstructor public class CompatibilityRule`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "rule_name", nullable = false, length = 150) private String ruleName;`
- Trường: `@Column(name = "category_1_id", nullable = false) private Integer category1Id;`
- Trường: `@Column(name = "spec_1_id", nullable = false) private Integer spec1Id;`
- Trường: `@Column(name = "category_2_id", nullable = false) private Integer category2Id;`
- Trường: `@Column(name = "spec_2_id", nullable = false) private Integer spec2Id;`
- Trường: `@Column(name = "operator", nullable = false, length = 50) private String operator;`

## `pcbuild/internal/entity/PcCustom.java`

### PcCustom — [PcCustom.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/entity/PcCustom.java#L12)

Khai báo: `@Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter @Entity @Table(name = "pc_customs") public class PcCustom`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", nullable = false) private Integer id;`
- Trường: `@Column(name = "order_id") private Integer orderId;`
- Trường: `@NotNull @Column(name = "user_id", nullable = false) private Integer userId;`
- Trường: `@ColumnDefault("0") @Column(name = "total_price", precision = 12, scale = 2) private BigDecimal totalPrice;`
- Trường: `@Builder.Default @OneToMany(mappedBy = "pcCustom", cascade = CascadeType.ALL, orphanRemoval = true) private List<PcCustomItem> items = new ArrayList<>();`

## `pcbuild/internal/entity/PcCustomItem.java`

### PcCustomItem — [PcCustomItem.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/entity/PcCustomItem.java#L11)

Khai báo: `@Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter @Entity @Table(name = "pc_custom_items") public class PcCustomItem`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", nullable = false) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pc_custom_id", nullable = false) private PcCustom pcCustom;`
- Trường: `@NotNull @Column(name = "product_variant_id", nullable = false) private Integer productVariantId;`
- Trường: `@NotNull @ColumnDefault("1") @Column(name = "quantity", nullable = false) private Integer quantity;`
- Trường: `@NotNull @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;`
- Trường: `@Size(max = 50) @NotNull @Column(name = "component_role", nullable = false, length = 50) private String componentRole;`

## `pcbuild/internal/model/Archetype.java`

### Archetype — [Archetype.java:6](../../src/main/java/com/project/cybershop/pcbuild/internal/model/Archetype.java#L6)

Khai báo: `public enum Archetype`

- Trường: `GAMING`
- Trường: `CREATIVE`
- Trường: `AI_DATA`
- Trường: `OFFICE`
- Trường: `DEV`
- Trường: `GENERAL`

## `pcbuild/internal/model/BuildLine.java`

### BuildLine — [BuildLine.java:6](../../src/main/java/com/project/cybershop/pcbuild/internal/model/BuildLine.java#L6)

Khai báo: `public record BuildLine(String role, ComponentCandidateDTO part, int quantity)`


## `pcbuild/internal/model/BuildOption.java`

### BuildOption — [BuildOption.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/model/BuildOption.java#L12)

Khai báo: `public record BuildOption(List<BuildLine> lines, double utility, double ratio)`


## `pcbuild/internal/model/BuildTier.java`

### BuildTier — [BuildTier.java:6](../../src/main/java/com/project/cybershop/pcbuild/internal/model/BuildTier.java#L6)

Khai báo: `public enum BuildTier`

- Trường: `T1(0L)`
- Trường: `T2(10_000_000L)`
- Trường: `T3(20_000_000L)`
- Trường: `T4(40_000_000L)`
- Trường: `T5(80_000_000L)`
- Trường: `T6(150_000_000L)`
- Trường: `T7(300_000_000L)`

## `pcbuild/internal/model/CandidatePool.java`

### CandidatePool — [CandidatePool.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CandidatePool.java#L14)

Khai báo: `public record CandidatePool(Map<String, List<ComponentCandidateDTO>> byRole, Map<String, Map<Integer, int[]>> rangesByRole)`

- [CandidatePool.java:17](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CandidatePool.java#L17) `public CandidatePool`
  - Lời gọi xuất hiện: `byRole.entrySet().stream().collect()`, `byRole.entrySet().stream()`, `byRole.entrySet()`, `Collectors.toUnmodifiableMap()`, `List.copyOf()`, `entry.getValue()`, `Map.copyOf()`
- [CandidatePool.java:23](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CandidatePool.java#L23) `public List<ComponentCandidateDTO> of(String role)`
  - Lời gọi xuất hiện: `byRole.getOrDefault()`, `List.of()`
- [CandidatePool.java:28](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CandidatePool.java#L28) `public int[] range(String role, int specId)`
  - Lời gọi xuất hiện: `rangesByRole.get()`, `ranges.get()`
- [CandidatePool.java:33](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CandidatePool.java#L33) `public int size()`
  - Lời gọi xuất hiện: `byRole.values().stream().mapToInt(List::size).sum()`, `byRole.values().stream().mapToInt()`, `byRole.values().stream()`, `byRole.values()`

## `pcbuild/internal/model/CompatibilitySnapshot.java`

### CompatibilitySnapshot — [CompatibilitySnapshot.java:18](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L18)

Khai báo: `public record CompatibilitySnapshot( List<ComponentCandidateDTO> candidates, // ACTIVE, stock > 0, thuộc 8 vai trò Map<Integer, ParsedSpecs> parsedSpecsByVariantId, // spec đã phân tích sẵn của các ứng viên trên List<RuleDefinition> rules, // bản sao compatibility_rules Map<Integer, Set<Integer>> requiredSpecIdsByCategoryId, // category_specifications.is_compatibility = true (DD 4.3) Map<Integer, Map<String, String>> optionDisplayBySpecId, // specId → (code → display_value) Map<Integer, String> specNameById, // specId → tên thông số MetadataDictionaryResponse dictionary, // DD 5.2; null trong test engine dựng tay PerfIndexEstimator perfIndexEstimator, // DD 6.2.1; null = dùng số đo gốc Instant loadedAt, List<ComponentCandidateDTO> peripherals // ACTIVE, stock > 0, màn/phím/chuột; ngoài 8 vai trò nên solver và engine không dùng )`

- [CompatibilitySnapshot.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L32) `public CompatibilitySnapshot(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsedSpecsByVariantId, List<RuleDefinition> rules, Map<Integer, Set<Integer>> requiredSpecIdsByCategoryId, Map<Integer, Map<String, String>> optionDisplayBySpecId, Map<Integer, String> specNameById, MetadataDictionaryResponse dictionary, PerfIndexEstimator perfIndexEstimator, Instant loadedAt)`
  - Lời gọi xuất hiện: `List.of()`
- [CompatibilitySnapshot.java:40](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L40) `public CompatibilitySnapshot`
  - Lời gọi xuất hiện: `List.copyOf()`, `Map.copyOf()`, `requiredSpecIdsByCategoryId.entrySet().stream().collect()`, `requiredSpecIdsByCategoryId.entrySet().stream()`, `requiredSpecIdsByCategoryId.entrySet()`, `Collectors.toUnmodifiableMap()`, `Set.copyOf()`, `entry.getValue()`, `optionDisplayBySpecId.entrySet().stream().collect()`, `optionDisplayBySpecId.entrySet().stream()`, `optionDisplayBySpecId.entrySet()`
- [CompatibilitySnapshot.java:53](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L53) `public ParsedSpecs parsed(ComponentCandidateDTO part)`
  - Lời gọi xuất hiện: `part.getVariantId()`, `parsedSpecsByVariantId.get()`, `ParsedSpecs.of()`
- [CompatibilitySnapshot.java:59](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L59) `public Integer effectiveInt(ComponentCandidateDTO part, int specId)`
  - Lời gọi xuất hiện: `parsed()`, `perfIndexEstimator.perfIndex()`, `specs.ints().get()`, `specs.ints()`
- [CompatibilitySnapshot.java:67](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L67) `public Optional<ComponentCandidateDTO> candidate(Integer variantId)`
  - Lời gọi xuất hiện: `candidates.stream().filter(c -> c.getVariantId().equals(variantId)).findFirst()`, `candidates.stream().filter()`, `candidates.stream()`, `c.getVariantId().equals()`, `c.getVariantId()`
- [CompatibilitySnapshot.java:71](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L71) `public Set<Integer> requiredSpecIds(Integer categoryId)`
  - Lời gọi xuất hiện: `requiredSpecIdsByCategoryId.getOrDefault()`, `Set.of()`
- [CompatibilitySnapshot.java:75](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L75) `public String specName(int specId)`
  - Lời gọi xuất hiện: `specNameById.getOrDefault()`
- [CompatibilitySnapshot.java:80](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L80) `public Integer specId(String name)`
  - Lời gọi xuất hiện: `specNameById.entrySet().stream() .filter(entry -> entry.getValue().equalsIgnoreCase(name.trim())) .map(Map.Entry::getKey) .findFirst().orElse()`, `specNameById.entrySet().stream() .filter(entry -> entry.getValue().equalsIgnoreCase(name.trim())) .map(Map.Entry::getKey).findFirst()`, `specNameById.entrySet().stream() .filter(entry -> entry.getValue().equalsIgnoreCase(name.trim())).map()`, `specNameById.entrySet().stream().filter()`, `specNameById.entrySet().stream()`, `specNameById.entrySet()`, `entry.getValue().equalsIgnoreCase()`, `entry.getValue()`, `name.trim()`
- [CompatibilitySnapshot.java:88](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L88) `public String dictionaryVersion()`
  - Lời gọi xuất hiện: `dictionary.getDictionaryVersion()`
- [CompatibilitySnapshot.java:93](../../src/main/java/com/project/cybershop/pcbuild/internal/model/CompatibilitySnapshot.java#L93) `public String display(int specId, String value)`
  - Lời gọi xuất hiện: `optionDisplayBySpecId.get()`, `options.getOrDefault()`

## `pcbuild/internal/model/EngineReport.java`

### EngineReport — [EngineReport.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EngineReport.java#L13)

Khai báo: `public record EngineReport( boolean compatible, // = không có check fail boolean hasWarnings, boolean hasUnknown, List<CompatibilityCheckResponse> checks, int requiredPsuW, CapacityResponse capacity, BigDecimal total )`

- Trường: `private static final Map<String, Integer> STATUS_RANK = Map.of("fail", 0, "warning", 1, "unknown", 2, "pass", 3);`
- Trường: `public static final Comparator<CompatibilityCheckResponse> CHECK_ORDER = Comparator .comparing((CompatibilityCheckResponse check) -> STATUS_RANK.getOrDefault(check.getStatus(), 4)) .thenComparing(check -> check.getKey() != null ? check.getKey() : "");`
- [EngineReport.java:30](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EngineReport.java#L30) `public EngineReport`
  - Lời gọi xuất hiện: `checks.stream().sorted(CHECK_ORDER).toList()`, `checks.stream().sorted()`, `checks.stream()`
- [EngineReport.java:34](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EngineReport.java#L34) `public static EngineReport of(List<CompatibilityCheckResponse> checks, int requiredPsuW, CapacityResponse capacity, BigDecimal total)`
  - Lời gọi xuất hiện: `checks.stream().noneMatch()`, `checks.stream()`, `"fail".equals()`, `check.getStatus()`, `checks.stream().anyMatch()`, `"warning".equals()`, `"unknown".equals()`
- [EngineReport.java:44](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EngineReport.java#L44) `public EngineReport withExtraChecks(List<CompatibilityCheckResponse> extraChecks)`
  - Lời gọi xuất hiện: `extraChecks.isEmpty()`, `merged.addAll()`, `of()`

## `pcbuild/internal/model/EvaluationContext.java`

### EvaluationContext — [EvaluationContext.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EvaluationContext.java#L14)

Khai báo: `public record EvaluationContext(BigDecimal budget, ValidatedProfile profile, Mode mode, CompatibilitySnapshot snapshot)`

- [EvaluationContext.java:18](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EvaluationContext.java#L18) `public EvaluationContext`
  - Lời gọi xuất hiện: `Objects.requireNonNull()`
- [EvaluationContext.java:23](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EvaluationContext.java#L23) `public EvaluationContext(BigDecimal budget, Mode mode, CompatibilitySnapshot snapshot)`

### Mode — [EvaluationContext.java:16](../../src/main/java/com/project/cybershop/pcbuild/internal/model/EvaluationContext.java#L16)

Khai báo: `public enum Mode`

- Trường: `SOLVER`
- Trường: `USER`

## `pcbuild/internal/model/MetricKind.java`

### MetricKind — [MetricKind.java:7](../../src/main/java/com/project/cybershop/pcbuild/internal/model/MetricKind.java#L7)

Khai báo: `public enum MetricKind`

- Trường: `TOTAL_RAM_GB("ram", "GB", "Tổng RAM")`
- Trường: `TOTAL_STORAGE_GB("ssd", "GB", "Tổng dung lượng SSD")`
- Trường: `TOTAL_VRAM_GB("gpu", "GB", "Tổng VRAM")`
- Trường: `GPU_COUNT("gpu", null, "Số card đồ họa")`
- Trường: `CPU_COUNT("cpu", null, "Số CPU")`
- Trường: `TOTAL_CPU_CORES("cpu", null, "Tổng số nhân CPU")`

## `pcbuild/internal/model/ParsedSpecs.java`

### ParsedSpecs — [ParsedSpecs.java:18](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ParsedSpecs.java#L18)

Khai báo: `public record ParsedSpecs(Map<Integer, Integer> ints, Map<Integer, Set<String>> lists, Map<Integer, String> raw)`

- [ParsedSpecs.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ParsedSpecs.java#L20) `public ParsedSpecs`
  - Lời gọi xuất hiện: `Map.copyOf()`
- [ParsedSpecs.java:26](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ParsedSpecs.java#L26) `public static ParsedSpecs of(ComponentCandidateDTO part)`
  - Lời gọi xuất hiện: `part.getSpecValuesById()`, `Map.of()`, `values.entrySet()`, `entry.getKey()`, `entry.getValue()`, `entry.getValue().isBlank()`, `entry.getValue().trim()`, `raw.put()`, `lists.put()`, `Arrays.stream(value.split(",")) .map(code -> code.trim().toUpperCase(Locale.ROOT)) .filter(code -> !code.isEmpty()).collect()`, `Arrays.stream(value.split(",")) .map(code -> code.trim().toUpperCase(Locale.ROOT)).filter()`, `Arrays.stream(value.split(",")).map()`, `Arrays.stream()`, `value.split()`, `code.trim().toUpperCase()`, `code.trim()`, `code.isEmpty()`, `Collectors.toUnmodifiableSet()`, `ints.put()`, `Integer.valueOf()`
- [ParsedSpecs.java:50](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ParsedSpecs.java#L50) `public boolean has(int specId)`
  - Lời gọi xuất hiện: `raw.containsKey()`

## `pcbuild/internal/model/PowerRequirement.java`

### PowerRequirement — [PowerRequirement.java:4](../../src/main/java/com/project/cybershop/pcbuild/internal/model/PowerRequirement.java#L4)

Khai báo: `public record PowerRequirement(int loadW, int requiredW)`


## `pcbuild/internal/model/RuleDefinition.java`

### RuleDefinition — [RuleDefinition.java:4](../../src/main/java/com/project/cybershop/pcbuild/internal/model/RuleDefinition.java#L4)

Khai báo: `public record RuleDefinition(int id, String ruleName, int category1Id, int spec1Id, int category2Id, int spec2Id, String operator)`


## `pcbuild/internal/model/ScoreContext.java`

### ScoreContext — [ScoreContext.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L19)

Khai báo: `public final class ScoreContext`

- Trường: `private final ValidatedProfile profile;`
- Trường: `private final CandidatePool pool;`
- Trường: `private final CompatibilitySnapshot snapshot;`
- Trường: `private final BigDecimal budget;`
- Trường: `private final Map<String, List<ResolvedPreference>> preferencesByRole;`
- [ScoreContext.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L32) `private ScoreContext(ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot, BigDecimal budget, Map<String, List<ResolvedPreference>> preferencesByRole)`
- [ScoreContext.java:44](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L44) `public static ScoreContext of(ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot, BigDecimal budget, Function<String, List<ProfilePreferenceDto>> defaultPreferences)`
  - Lời gọi xuất hiện: `profile.profile().getPreferences()`, `profile.profile()`, `resolve()`, `byRole.computeIfAbsent(resolved.role(), key -> new ArrayList<>()).add()`, `byRole.computeIfAbsent()`, `resolved.role()`, `List.of()`, `byRole.containsKey()`, `defaultPreferences.apply(role).stream() .map(preference -> resolve(preference, snapshot)) .filter(java.util.Objects::nonNull).toList()`, `defaultPreferences.apply(role).stream() .map(preference -> resolve(preference, snapshot)).filter()`, `defaultPreferences.apply(role).stream().map()`, `defaultPreferences.apply(role).stream()`, `defaultPreferences.apply()`, `byRole.put()`, `byRole.entrySet().stream().collect()`, `byRole.entrySet().stream()`, `byRole.entrySet()`, `Collectors.toUnmodifiableMap()`, `List.copyOf()`, `entry.getValue()`
- [ScoreContext.java:68](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L68) `public ScoreContext withPool(CandidatePool otherPool)`
- [ScoreContext.java:72](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L72) `private static ResolvedPreference resolve(ProfilePreferenceDto preference, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `preference.getWeight()`, `preference.getValues()`, `Set.of()`, `preference.getValues().stream().map(value -> value.toUpperCase(Locale.ROOT)).collect()`, `preference.getValues().stream().map()`, `preference.getValues().stream()`, `value.toUpperCase()`, `Collectors.toUnmodifiableSet()`, `preference.getDirection()`, `preference.getMetric()`, `MetricKind.parse(preference.getMetric()) .map(kind -> new ResolvedPreference(kind.role(), kind, null, direction, values, weight, preference.getCap())).orElse()`, `MetricKind.parse(preference.getMetric()).map()`, `MetricKind.parse()`, `kind.role()`, `preference.getCap()`, `snapshot.specId()`, `preference.getSpec()`, `preference.getRole()`
- [ScoreContext.java:87](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L87) `public List<ResolvedPreference> preferences(String role)`
  - Lời gọi xuất hiện: `preferencesByRole.getOrDefault()`, `List.of()`
- [ScoreContext.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L91) `public ValidatedProfile profile()`
- [ScoreContext.java:95](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L95) `public CandidatePool pool()`
- [ScoreContext.java:99](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L99) `public CompatibilitySnapshot snapshot()`
- [ScoreContext.java:103](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L103) `public BigDecimal budget()`

### ResolvedPreference — [ScoreContext.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ScoreContext.java#L22)

Khai báo: `public record ResolvedPreference(String role, MetricKind metric, Integer specId, String direction, Set<String> values, double weight, Double cap)`


## `pcbuild/internal/model/SolverInput.java`

### SolverInput — [SolverInput.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/model/SolverInput.java#L12)

Khai báo: `public record SolverInput(BigDecimal budget, ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot)`


## `pcbuild/internal/model/SolverResult.java`

### SolverResult — [SolverResult.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/model/SolverResult.java#L13)

Khai báo: `public record SolverResult( List<BuildLine> lines, BigDecimal total, BigDecimal leftover, double utility, EngineReport compatibility, String reasonCode, List<String> relaxedItems, BigDecimal floorPrice, int iterations, List<String> warnings, ValidatedProfile profile )`

- [SolverResult.java:26](../../src/main/java/com/project/cybershop/pcbuild/internal/model/SolverResult.java#L26) `public SolverResult`
  - Lời gọi xuất hiện: `List.copyOf()`

## `pcbuild/internal/model/SpecType.java`

### SpecType — [SpecType.java:4](../../src/main/java/com/project/cybershop/pcbuild/internal/model/SpecType.java#L4)

Khai báo: `public enum SpecType`

- Trường: `ENUM`
- Trường: `LIST`
- Trường: `INT`
- Trường: `TEXT`

## `pcbuild/internal/model/ValidatedProfile.java`

### ValidatedProfile — [ValidatedProfile.java:15](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L15)

Khai báo: `public record ValidatedProfile(RequirementProfileDto profile, Archetype archetype, List<String> warnings)`

- Trường: `public static final String MAX_PRICE = "MAX_PRICE";`
- [ValidatedProfile.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L19) `public ValidatedProfile`
  - Lời gọi xuất hiện: `List.copyOf()`
- [ValidatedProfile.java:24](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L24) `public boolean maxPrice()`
  - Lời gọi xuất hiện: `MAX_PRICE.equals()`, `profile.getObjective()`
- [ValidatedProfile.java:28](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L28) `public double weight(String role)`
  - Lời gọi xuất hiện: `profile.getRoleWeights().get()`, `profile.getRoleWeights()`
- [ValidatedProfile.java:33](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L33) `public boolean isExcluded(String role)`
  - Lời gọi xuất hiện: `profile.getExcludedRoles().stream().anyMatch()`, `profile.getExcludedRoles().stream()`, `profile.getExcludedRoles()`, `role.equals()`, `ref.getRole()`
- [ValidatedProfile.java:37](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L37) `public boolean isRequired(String role)`
  - Lời gọi xuất hiện: `profile.getRequiredRoles().stream().anyMatch()`, `profile.getRequiredRoles().stream()`, `profile.getRequiredRoles()`, `role.equals()`, `ref.getRole()`
- [ValidatedProfile.java:41](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L41) `public List<ProfileConstraintDto> constraints()`
  - Lời gọi xuất hiện: `profile.getConstraints()`
- [ValidatedProfile.java:45](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L45) `public List<ProfileMetricDto> metrics()`
  - Lời gọi xuất hiện: `profile.getMetrics()`
- [ValidatedProfile.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/model/ValidatedProfile.java#L49) `public ValidatedProfile withProfile(RequirementProfileDto changed)`

## `pcbuild/internal/repository/PcCustomRepository.java`

### PcCustomRepository — [PcCustomRepository.java:9](../../src/main/java/com/project/cybershop/pcbuild/internal/repository/PcCustomRepository.java#L9)

Khai báo: `@Repository public interface PcCustomRepository extends JpaRepository<PcCustom, Integer>`

- [PcCustomRepository.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/repository/PcCustomRepository.java#L11) `List<PcCustom> findByUserIdOrderByIdDesc(Integer userId)`

## `pcbuild/internal/service/AiPcBuilderService.java`

### AiPcBuilderService — [AiPcBuilderService.java:7](../../src/main/java/com/project/cybershop/pcbuild/internal/service/AiPcBuilderService.java#L7)

Khai báo: `public interface AiPcBuilderService`

- [AiPcBuilderService.java:10](../../src/main/java/com/project/cybershop/pcbuild/internal/service/AiPcBuilderService.java#L10) `AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId)`
- [AiPcBuilderService.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/AiPcBuilderService.java#L13) `BuildOptionsResponse options()`

## `pcbuild/internal/service/BuildExplainer.java`

### BuildExplainer — [BuildExplainer.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildExplainer.java#L12)

Khai báo: `public interface BuildExplainer`

- Trường: `String PROMPT_VERSION = "EXPLAIN_V4";`
- [BuildExplainer.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildExplainer.java#L22) `Explanation explain(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt, CompatibilitySnapshot snapshot, Instant deadline, BigDecimal fullSetBudget)`

### Explanation — [BuildExplainer.java:17](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildExplainer.java#L17)

Khai báo: `record Explanation(String buildName, String aiReasoning, String performanceHighlight, Map<Integer, String> partReasons, String aiStatus)`


## `pcbuild/internal/service/BuildSolver.java`

### BuildSolver — [BuildSolver.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildSolver.java#L13)

Khai báo: `public interface BuildSolver`

- [BuildSolver.java:16](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildSolver.java#L16) `SolverResult solve(SolverInput input)`
- [BuildSolver.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/service/BuildSolver.java#L19) `BigDecimal floorPrice(CompatibilitySnapshot snapshot)`

## `pcbuild/internal/service/CandidateRepositoryFacade.java`

### CandidateRepositoryFacade — [CandidateRepositoryFacade.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/CandidateRepositoryFacade.java#L13)

Khai báo: `public interface CandidateRepositoryFacade`

- [CandidateRepositoryFacade.java:15](../../src/main/java/com/project/cybershop/pcbuild/internal/service/CandidateRepositoryFacade.java#L15) `CompatibilitySnapshot snapshot()`
- [CandidateRepositoryFacade.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/service/CandidateRepositoryFacade.java#L22) `CandidatePool buildPool(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget)`

## `pcbuild/internal/service/CompatibilityEngine.java`

### CompatibilityEngine — [CompatibilityEngine.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/CompatibilityEngine.java#L13)

Khai báo: `public interface CompatibilityEngine`

- [CompatibilityEngine.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/service/CompatibilityEngine.java#L14) `EngineReport evaluate(List<BuildLine> lines, EvaluationContext ctx)`

## `pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java`

### AiPcBuilderServiceImpl — [AiPcBuilderServiceImpl.java:62](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L62)

Khai báo: `@Slf4j @Service public class AiPcBuilderServiceImpl implements AiPcBuilderService`

- Trường: `private static final List<String> EXAMPLES = List.of( "Chơi game 2K, thỉnh thoảng stream", "Dựng phim 4K bằng DaVinci Resolve", "Học và chạy thử model AI nhỏ", "Văn phòng, Excel, họp online", "Lập trình web, chạy Docker");`
- Trường: `private static final BigDecimal WINDOW_MIN = new BigDecimal("0.90");`
- Trường: `private static final Pattern MOST_EXPENSIVE = Pattern.compile( "\\b(dat|mac)( tien| do)? nhat\\b|\\bgia cao nhat\\b|\\bmost expensive\\b|\\bpriciest\\b");`
- Trường: `private final CandidateRepositoryFacade candidateRepositoryFacade;`
- Trường: `private final RequestValidator requestValidator;`
- Trường: `private final ProfileExtractor profileExtractor;`
- Trường: `private final ProfilePresets profilePresets;`
- Trường: `private final ProfileValidator profileValidator;`
- Trường: `private final BuildSolver buildSolver;`
- Trường: `private final BuildExplainer buildExplainer;`
- Trường: `private final CompatibilityEngine compatibilityEngine;`
- Trường: `private final Clock clock;`
- Trường: `private final long requestDeadlineMs;`
- Trường: `private final double needsUnclearConfidence;`
- Trường: `private final String provider;`
- Trường: `private final String model;`
- [AiPcBuilderServiceImpl.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L91) `public AiPcBuilderServiceImpl(CandidateRepositoryFacade candidateRepositoryFacade, RequestValidator requestValidator, ProfileExtractor profileExtractor, ProfilePresets profilePresets, ProfileValidator profileValidator, BuildSolver buildSolver, BuildExplainer buildExplainer, CompatibilityEngine compatibilityEngine, Clock clock, @Value("${app.ai.request-deadline-ms:14000}") long requestDeadlineMs, @Value("${app.pcbuild.needs-unclear-confidence:0.4}") double needsUnclearConfidence, @Value("${app.ai.provider:gemini}") String provider, @Value("${app.ai.model:gemini-3.5-flash-lite}") String model)`
- [AiPcBuilderServiceImpl.java:114](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L114) `@Override public AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId)`
  - Lời gọi xuất hiện: `clock.instant()`, `started.plusMillis()`, `UUID.randomUUID().toString()`, `UUID.randomUUID()`, `candidateRepositoryFacade.snapshot()`, `requestValidator.validate()`, `buildSolver.floorPrice()`, `request.getBudget()`, `BuildTier.of()`, `System.nanoTime()`, `request.getProfile()`, `profileExtractor.extract()`, `request.getPurpose()`, `request.getUserPrompt()`, `extraction.draft()`, `MOST_EXPENSIVE.matcher(TextNormalizer.normalize(request.getPurpose() + " " + request.getUserPrompt())).find()`, `MOST_EXPENSIVE.matcher()`, `TextNormalizer.normalize()`, `draft.setObjective()`, `extraction.status()`, `profileValidator.validate()`, `profilePresets.fillMissing()`, `snapshot.dictionary()`, `request.getRamGb()`, `elapsedMs()`, `ProfileValidator.SOURCE_AI.equals()`, `rejectIfUnclear()`, `profile.profile().getDroppedItems().isEmpty()`, `profile.profile().getDroppedItems()`, `profile.profile()`, `log.info()`, `candidateRepositoryFacade.buildPool()`, `buildSolver.solve()`, `buildExplainer.explain()`, `result.profile()`, `request.getFullSetBudget()`, `compatibilityEngine.evaluate()`, `result.lines()`, `Duration.between(started, clock.instant()).toMillis()`, `Duration.between()`, `latency.put()`, `profile.warnings()`, `warnings.addAll()`, `result.warnings()`, `report.checks().stream().anyMatch()`, `report.checks().stream()`, `report.checks()`, `"J4b".equals()`, `check.getKey()`, `warnings.add()`, `hash()`, `profile.archetype()`, `profile.constraints().size()`, `profile.constraints()`, `profile.metrics().size()`, `profile.metrics()`, `result.relaxedItems()`, `result.total()`, `percent()`, `result.reasonCode()`, `explanation.aiStatus()`, `result.iterations()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()) .warnings(warnings) .capacity(report.capacity()) .requiredPsuW(report.requiredPsuW()) .floorPrice(result.floorPrice()) .metadata(AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)) .dictionaryVersion(snapshot.dictionaryVersion()) .latencyMs(latency) .candidateCount(pool.size()) .solverIterations(result.iterations()) .build()).build()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()) .warnings(warnings) .capacity(report.capacity()) .requiredPsuW(report.requiredPsuW()) .floorPrice(result.floorPrice()).metadata()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()) .warnings(warnings) .capacity(report.capacity()) .requiredPsuW(report.requiredPsuW()).floorPrice()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()) .warnings(warnings) .capacity(report.capacity()).requiredPsuW()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()) .warnings(warnings).capacity()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)) .relaxedItems(result.relaxedItems()).warnings()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)) .profile(withSatisfaction(profile, result.lines(), snapshot)).relaxedItems()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()) .budgetFit(budgetFit(result, budget)).profile()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)) .reasonCode(result.reasonCode()).budgetFit()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()) .items(items(result.lines(), explanation)).reasonCode()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()) .checks(report.checks()).items()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()) .isCompatible(report.compatible()).checks()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()) .performanceHighlight(explanation.performanceHighlight()).isCompatible()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget) .aiReasoning(explanation.aiReasoning()).performanceHighlight()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()) .budget(budget).aiReasoning()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()) .totalPrice(result.total()).budget()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()) .purpose(request.getPurpose()).totalPrice()`, `AiBuildResponse.builder() .buildId(null) .buildName(explanation.buildName()).purpose()`, `AiBuildResponse.builder() .buildId(null).buildName()`, `AiBuildResponse.builder().buildId()`, `AiBuildResponse.builder()`, `explanation.buildName()`, `explanation.aiReasoning()`, `explanation.performanceHighlight()`, `report.compatible()`, `items()`, `budgetFit()`, `withSatisfaction()`, `report.capacity()`, `report.requiredPsuW()`, `result.floorPrice()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)) .dictionaryVersion(snapshot.dictionaryVersion()) .latencyMs(latency) .candidateCount(pool.size()) .solverIterations(result.iterations()).build()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)) .dictionaryVersion(snapshot.dictionaryVersion()) .latencyMs(latency) .candidateCount(pool.size()).solverIterations()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)) .dictionaryVersion(snapshot.dictionaryVersion()) .latencyMs(latency).candidateCount()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)) .dictionaryVersion(snapshot.dictionaryVersion()).latencyMs()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model) .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION)).dictionaryVersion()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider) .model(model).promptVersions()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()) .provider(provider).model()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus) .aiStatus(explanation.aiStatus()).provider()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)) .extractStatus(extractStatus).aiStatus()`, `AiBuildMetadataResponse.builder() .requestId(requestId) .generatedAt(OffsetDateTime.now(clock)).extractStatus()`, `AiBuildMetadataResponse.builder() .requestId(requestId).generatedAt()`, `AiBuildMetadataResponse.builder().requestId()`, `AiBuildMetadataResponse.builder()`, `OffsetDateTime.now()`, `Map.of()`, `snapshot.dictionaryVersion()`, `pool.size()`
- [AiPcBuilderServiceImpl.java:228](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L228) `@Override public BuildOptionsResponse options()`
  - Lời gọi xuất hiện: `candidateRepositoryFacade.snapshot()`, `BuildTier.values()`, `tiers.add()`, `tier.name()`, `tier.lowerBound()`, `tier.upperBound()`, `PeripheralSelector.budgetRange()`, `snapshot.peripherals()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)) .maxBudget(requestValidator.maxBudget()) .dictionaryVersion(snapshot.dictionaryVersion()) .dictionary(snapshot.dictionary()) .examples(EXAMPLES) .tiers(tiers).build()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)) .maxBudget(requestValidator.maxBudget()) .dictionaryVersion(snapshot.dictionaryVersion()) .dictionary(snapshot.dictionary()) .examples(EXAMPLES).tiers()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)) .maxBudget(requestValidator.maxBudget()) .dictionaryVersion(snapshot.dictionaryVersion()) .dictionary(snapshot.dictionary()).examples()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)) .maxBudget(requestValidator.maxBudget()) .dictionaryVersion(snapshot.dictionaryVersion()).dictionary()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)) .maxBudget(requestValidator.maxBudget()).dictionaryVersion()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()) .floorPrice(buildSolver.floorPrice(snapshot)).maxBudget()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null) .minBudget(requestValidator.minBudget()).floorPrice()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null) .peripheralMaxTotal(peripherals != null ? peripherals.max() : null).minBudget()`, `BuildOptionsResponse.builder() .peripheralMinTotal(peripherals != null ? peripherals.min() : null).peripheralMaxTotal()`, `BuildOptionsResponse.builder().peripheralMinTotal()`, `BuildOptionsResponse.builder()`, `peripherals.min()`, `peripherals.max()`, `requestValidator.minBudget()`, `buildSolver.floorPrice()`, `requestValidator.maxBudget()`, `snapshot.dictionaryVersion()`, `snapshot.dictionary()`
- [AiPcBuilderServiceImpl.java:250](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L250) `private void rejectIfUnclear(ValidatedProfile profile)`
  - Lời gọi xuất hiện: `profile.profile()`, `dto.getConfidence()`, `dto.getConstraints().isEmpty()`, `dto.getConstraints()`, `dto.getMetrics().isEmpty()`, `dto.getMetrics()`, `dto.getPreferences().isEmpty()`, `dto.getPreferences()`, `dto.getClarifyingQuestion()`, `dto.getClarifyingQuestion().isBlank()`, `Map.of()`
- [AiPcBuilderServiceImpl.java:263](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L263) `private static List<SelectedPartResponse> items(List<BuildLine> lines, BuildExplainer.Explanation explanation)`
  - Lời gọi xuất hiện: `lines.stream().map(line -> { BigDecimal unitPrice = line.part().getPrice(); String reason = explanation.partReasons().get(line.part().getVariantId()); return SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))) .specs(line.part().getSpecs()) .aiReason(reason != null && !reason.isBlank() ? reason : "—") .build(); }).toList()`, `lines.stream().map()`, `lines.stream()`, `line.part().getPrice()`, `line.part()`, `explanation.partReasons().get()`, `explanation.partReasons()`, `line.part().getVariantId()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))) .specs(line.part().getSpecs()) .aiReason(reason != null && !reason.isBlank() ? reason : "—").build()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))) .specs(line.part().getSpecs()).aiReason()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))).specs()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()).subtotal()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()) .unitPrice(unitPrice).quantity()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()) .imageUrl(line.part().getImageUrl()).unitPrice()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()) .variantName(line.part().getVariantName()).imageUrl()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()) .productName(line.part().getProductName()).variantName()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(line.part().getVariantId()).productName()`, `SelectedPartResponse.builder() .role(line.role()).variantId()`, `SelectedPartResponse.builder().role()`, `SelectedPartResponse.builder()`, `line.role()`, `line.part().getProductName()`, `line.part().getVariantName()`, `line.part().getImageUrl()`, `line.quantity()`, `unitPrice.multiply()`, `BigDecimal.valueOf()`, `line.part().getSpecs()`, `reason.isBlank()`
- [AiPcBuilderServiceImpl.java:282](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L282) `private static BudgetFitResponse budgetFit(SolverResult result, BigDecimal budget)`
  - Lời gọi xuất hiện: `result.total().divide()`, `result.total()`, `budget.multiply(WINDOW_MIN).setScale()`, `budget.multiply()`, `result.total().compareTo()`, `"CATALOG_CEILING".equals()`, `result.reasonCode()`, `"NEEDS_SATURATED".equals()`, `"MAX_PRICE_REACHED".equals()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin) .windowMax(budget) .leftover(result.leftover()).build()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin) .windowMax(budget).leftover()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin).windowMax()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status).windowMin()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)).status()`, `BudgetFitResponse.builder() .ratio(ratio).percent()`, `BudgetFitResponse.builder().ratio()`, `BudgetFitResponse.builder()`, `ratio.multiply(BigDecimal.valueOf(100)).setScale()`, `ratio.multiply()`, `BigDecimal.valueOf()`, `result.leftover()`
- [AiPcBuilderServiceImpl.java:308](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L308) `private static RequirementProfileDto withSatisfaction(ValidatedProfile profile, List<BuildLine> lines, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `profile.profile()`, `BuildMetrics.of()`, `dto.getConstraints().stream().map(constraint -> { List<BuildLine> roleLines = lines.stream().filter(line -> constraint.getRole().equals(line.role())).toList(); boolean satisfied = !roleLines.isEmpty() && roleLines.stream() .allMatch(line -> ProfileMatcher.matches(constraint, line.part(), snapshot)); return constraint.toBuilder().satisfied(satisfied).build(); }).toList()`, `dto.getConstraints().stream().map()`, `dto.getConstraints().stream()`, `dto.getConstraints()`, `lines.stream().filter(line -> constraint.getRole().equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `constraint.getRole().equals()`, `constraint.getRole()`, `line.role()`, `roleLines.isEmpty()`, `roleLines.stream().allMatch()`, `roleLines.stream()`, `ProfileMatcher.matches()`, `line.part()`, `constraint.toBuilder().satisfied(satisfied).build()`, `constraint.toBuilder().satisfied()`, `constraint.toBuilder()`, `dto.getMetrics().stream() .map(metric -> metric.toBuilder().satisfied(ProfileMatcher.matches(metric, metrics)).build()).toList()`, `dto.getMetrics().stream().map()`, `dto.getMetrics().stream()`, `dto.getMetrics()`, `metric.toBuilder().satisfied(ProfileMatcher.matches(metric, metrics)).build()`, `metric.toBuilder().satisfied()`, `metric.toBuilder()`, `dto.toBuilder().constraints(new ArrayList<>(constraints)).metrics(new ArrayList<>(metricItems)).build()`, `dto.toBuilder().constraints(new ArrayList<>(constraints)).metrics()`, `dto.toBuilder().constraints()`, `dto.toBuilder()`
- [AiPcBuilderServiceImpl.java:323](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L323) `private static BigDecimal percent(BigDecimal total, BigDecimal budget)`
  - Lời gọi xuất hiện: `total.multiply(BigDecimal.valueOf(100)).divide()`, `total.multiply()`, `BigDecimal.valueOf()`
- [AiPcBuilderServiceImpl.java:327](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L327) `private static long elapsedMs(long startedNanos)`
  - Lời gọi xuất hiện: `System.nanoTime()`
- [AiPcBuilderServiceImpl.java:332](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/AiPcBuilderServiceImpl.java#L332) `private static String hash(String text)`
  - Lời gọi xuất hiện: `MessageDigest.getInstance("SHA-256").digest()`, `MessageDigest.getInstance()`, `TextNormalizer.normalize(text).getBytes()`, `TextNormalizer.normalize()`, `HexFormat.of().formatHex(digest).substring()`, `HexFormat.of().formatHex()`, `HexFormat.of()`

## `pcbuild/internal/service/impl/BuildExplainerImpl.java`

### BuildExplainerImpl — [BuildExplainerImpl.java:36](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L36)

Khai báo: `@Slf4j @Service @RequiredArgsConstructor public class BuildExplainerImpl implements BuildExplainer`

- Trường: `static final String PROMPT_EXPLAIN_V4 = """ BẠN LÀ KỸ SƯ TƯ VẤN PC CỦA CYBERSHOP. Hệ thống đã hiểu nhu cầu của khách thành "profile" và đã chọn sẵn MỘT bộ máy. Bộ máy ĐÃ được kiểm tra tương thích, giá là giá thật và tổng giá KHÔNG vượt ngân sách. NHIỆM VỤ: Viết buildName (≤ 60 ký tự), aiReasoning (3–6 câu), performanceHighlight (1–2 câu), và lý ...`
- Trường: `static final String MAX_PRICE_RULE = """ - profile.objective là MAX_PRICE: nói rõ bộ máy được chọn theo tổng giá cao nhất đúng theo yêu cầu của khách, không phải cấu hình tối ưu cho nhu cầu. KHÔNG khen linh kiện là lựa chọn phù hợp nhất hay đáng tiền nhất. KHÔNG viết chữ "MAX_PRICE" hay "objective" trong câu trả lời. Tiền dư là do đã hết linh ki...`
- Trường: `static final String DIACRITICS_RETRY = """ - LẦN TRƯỚC bạn viết tiếng Việt KHÔNG DẤU. Viết lại toàn bộ bằng tiếng Việt CÓ DẤU đầy đủ. """;`
- Trường: `private static final Map<String, String> REASON_MEANINGS = Map.of( "NEEDS_SATURATED", "Nhu cầu trong hồ sơ đã được đáp ứng đủ; linh kiện đắt hơn không cải thiện thêm cho nhu cầu này nên còn dư tiền. " + "KHÔNG nói cửa hàng đã hết linh kiện đắt hơn.", "CATALOG_CEILING", "Đã dùng linh kiện mạnh nhất cửa hàng đang có cho nhu cầu này nên còn dư tiền...`
- Trường: `private static final Pattern MONEY = Pattern.compile( "(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})\\s*(?:₫|VNĐ|VND|vnđ|vnd|đồng|dong|đ)(?![\\p{L}\\p{N}])");`
- Trường: `private static final int MAX_BUILD_NAME = 60;`
- Trường: `private final LlmClientService llmClientService;`
- Trường: `private final ObjectMapper objectMapper;`
- [BuildExplainerImpl.java:87](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L87) `@Override public Explanation explain(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt, CompatibilitySnapshot snapshot, Instant deadline, BigDecimal fullSetBudget)`
  - Lời gọi xuất hiện: `"MAX_PRICE".equals()`, `profile.profile().getObjective()`, `profile.profile()`, `context()`, `llmClientService.explainBuild()`, `isBlank()`, `draft.getBuildName()`, `draft.getAiReasoning()`, `lacksDiacritics()`, `rewriteWithDiacritics()`, `result.lines().stream().map(line -> line.part().getVariantId()).collect()`, `result.lines().stream().map()`, `result.lines().stream()`, `result.lines()`, `line.part().getVariantId()`, `line.part()`, `Collectors.toSet()`, `draft.getPartReasons()`, `partReason.getVariantId()`, `buildVariantIds.contains()`, `reasons.putIfAbsent()`, `normalizeMoney()`, `partReason.getReason()`, `log.warn()`, `draft.getBuildName().trim()`, `name.length()`, `name.substring()`, `draft.getAiReasoning().trim()`, `draft.getPerformanceHighlight()`, `draft.getPerformanceHighlight().trim()`
- [BuildExplainerImpl.java:122](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L122) `private LlmClientService.ExplanationDraft rewriteWithDiacritics(String prompt, String context, Instant deadline, LlmClientService.ExplanationDraft first)`
  - Lời gọi xuất hiện: `llmClientService.explainBuild()`, `isBlank()`, `second.getBuildName()`, `second.getAiReasoning()`, `lacksDiacritics()`, `log.warn()`, `e.getMessage()`
- [BuildExplainerImpl.java:137](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L137) `static boolean lacksDiacritics(String text)`
  - Lời gọi xuất hiện: `text.codePoints().filter(Character::isLetter).count()`, `text.codePoints().filter()`, `text.codePoints()`, `text.codePoints().filter(cp -> Character.isLetter(cp) && cp > 127).count()`, `Character.isLetter()`
- [BuildExplainerImpl.java:145](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L145) `static String normalizeMoney(String text)`
  - Lời gọi xuất hiện: `MONEY.matcher(text).replaceAll()`, `MONEY.matcher()`, `vnd()`, `match.group(1).replaceAll()`, `match.group()`
- [BuildExplainerImpl.java:150](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L150) `static String vnd(BigDecimal amount)`
  - Lời gọi xuất hiện: `symbols.setGroupingSeparator()`, `new DecimalFormat("#,##0", symbols).format()`
- [BuildExplainerImpl.java:157](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L157) `private String context(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt, CompatibilitySnapshot snapshot, BigDecimal fullSetBudget)`
  - Lời gọi xuất hiện: `profileView.put()`, `profile.profile().getSummary()`, `profile.profile()`, `profile.constraints().stream().map(BuildExplainerImpl::constraintView).toList()`, `profile.constraints().stream().map()`, `profile.constraints().stream()`, `profile.constraints()`, `profile.metrics().stream().map(BuildExplainerImpl::metricView).toList()`, `profile.metrics().stream().map()`, `profile.metrics().stream()`, `profile.metrics()`, `profile.profile().getUnmetNeeds()`, `profile.profile().getObjective()`, `result.lines()`, `part.put()`, `line.role()`, `line.part().getVariantId()`, `line.part()`, `line.part().getProductName()`, `line.quantity()`, `line.part().getPrice()`, `keySpecs()`, `parts.add()`, `result.compatibility().checks().stream() .filter(check -> "warning".equals(check.getStatus())) .map(CompatibilityCheckResponse::getDetail).toList()`, `result.compatibility().checks().stream() .filter(check -> "warning".equals(check.getStatus())).map()`, `result.compatibility().checks().stream().filter()`, `result.compatibility().checks().stream()`, `result.compatibility().checks()`, `result.compatibility()`, `"warning".equals()`, `check.getStatus()`, `build.put()`, `result.total()`, `result.total().multiply(BigDecimal.valueOf(100)).divide()`, `result.total().multiply()`, `BigDecimal.valueOf()`, `result.leftover()`, `content.put()`, `Map.of()`, `result.reasonCode()`, `REASON_MEANINGS.containsKey()`, `REASON_MEANINGS.get()`, `moneyText.put()`, `vnd()`, `fullSet.put()`, `fullSetBudget.subtract()`, `result.warnings().stream().filter(warning -> warning.startsWith("REQUIREMENT_RELAXED")).toList()`, `result.warnings().stream().filter()`, `result.warnings().stream()`, `result.warnings()`, `warning.startsWith()`, `objectMapper.writeValueAsString()`
- [BuildExplainerImpl.java:220](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L220) `private static Map<String, String> keySpecs(BuildLine line, ValidatedProfile profile, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `snapshot.dictionary()`, `dictionary.getRoles().get()`, `dictionary.getRoles()`, `line.role()`, `dictionary.getRoles().get(line.role()).getPrimarySpecs()`, `names.addAll()`, `profile.constraints().stream().filter(c -> line.role().equals(c.getRole())).map(ProfileConstraintDto::getSpec).forEach()`, `profile.constraints().stream().filter(c -> line.role().equals(c.getRole())).map()`, `profile.constraints().stream().filter()`, `profile.constraints().stream()`, `profile.constraints()`, `line.role().equals()`, `c.getRole()`, `profile.profile().getPreferences().stream().filter(p -> line.role().equals(p.getRole())) .map(ProfilePreferenceDto::getSpec).forEach()`, `profile.profile().getPreferences().stream().filter(p -> line.role().equals(p.getRole())).map()`, `profile.profile().getPreferences().stream().filter()`, `profile.profile().getPreferences().stream()`, `profile.profile().getPreferences()`, `profile.profile()`, `p.getRole()`, `line.part().getSpec()`, `line.part()`, `specs.put()`
- [BuildExplainerImpl.java:238](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L238) `private static Map<String, Object> constraintView(ProfileConstraintDto constraint)`
  - Lời gọi xuất hiện: `view.put()`, `constraint.getReason()`, `constraint.getSpec()`, `constraint.getOp()`, `constraint.getValue()`, `constraint.getValues()`
- [BuildExplainerImpl.java:247](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L247) `private static Map<String, Object> metricView(ProfileMetricDto metric)`
  - Lời gọi xuất hiện: `view.put()`, `metric.getReason()`, `metric.getMetric()`, `metric.getOp()`, `metric.getValue()`
- [BuildExplainerImpl.java:256](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/BuildExplainerImpl.java#L256) `private static boolean isBlank(String value)`
  - Lời gọi xuất hiện: `value.isBlank()`

## `pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java`

### CandidateRepositoryFacadeImpl — [CandidateRepositoryFacadeImpl.java:38](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L38)

Khai báo: `@Slf4j @Service public class CandidateRepositoryFacadeImpl implements CandidateRepositoryFacade`

- Trường: `private final CatalogPublicApi catalogPublicApi;`
- Trường: `private final CompatibilityRuleRepository ruleRepository;`
- Trường: `private final MetadataDictionaryService dictionaryService;`
- Trường: `private final CandidatePoolBuilder candidatePoolBuilder;`
- Trường: `private final Clock clock;`
- Trường: `private final Duration cacheTtl;`
- Trường: `private final Object reloadLock = new Object();`
- Trường: `private volatile CompatibilitySnapshot current;`
- [CandidateRepositoryFacadeImpl.java:52](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L52) `public CandidateRepositoryFacadeImpl(CatalogPublicApi catalogPublicApi, CompatibilityRuleRepository ruleRepository, MetadataDictionaryService dictionaryService, PartScorer partScorer, ProfilePresets profilePresets, Clock clock, @Value("${app.pcbuild.candidate-cache-seconds:60}") long cacheSeconds)`
  - Lời gọi xuất hiện: `Duration.ofSeconds()`
- [CandidateRepositoryFacadeImpl.java:68](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L68) `@Override public CompatibilitySnapshot snapshot()`
  - Lời gọi xuất hiện: `isFresh()`, `load()`, `log.warn()`, `snapshot.loadedAt()`, `e.getMessage()`, `log.error()`
- [CandidateRepositoryFacadeImpl.java:96](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L96) `private boolean isFresh(CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `Duration.between(snapshot.loadedAt(), clock.instant()).compareTo()`, `Duration.between()`, `snapshot.loadedAt()`, `clock.instant()`
- [CandidateRepositoryFacadeImpl.java:100](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L100) `private CompatibilitySnapshot load()`
  - Lời gọi xuất hiện: `catalogPublicApi.getActiveCandidatesForBuild()`, `withSlugIn()`, `catalogPublicApi.getSpecificationCatalog()`, `ruleRepository.findAll().stream() .sorted(Comparator.comparing(CompatibilityRule::getId)) .map(CandidateRepositoryFacadeImpl::toRuleDefinition).toList()`, `ruleRepository.findAll().stream() .sorted(Comparator.comparing(CompatibilityRule::getId)).map()`, `ruleRepository.findAll().stream().sorted()`, `ruleRepository.findAll().stream()`, `ruleRepository.findAll()`, `Comparator.comparing()`, `parsedSpecsByVariantId.put()`, `candidate.getVariantId()`, `ParsedSpecs.of()`, `row.isCompatibility()`, `requiredSpecIdsByCategoryId.computeIfAbsent(row.getCategoryId(), key -> new HashSet<>()).add()`, `requiredSpecIdsByCategoryId.computeIfAbsent()`, `row.getCategoryId()`, `row.getSpecificationId()`, `specNameById.put()`, `row.getSpecificationName()`, `optionDisplayBySpecId.computeIfAbsent()`, `row.getOptions()`, `options.put()`, `option.getCode()`, `option.getDisplayValue()`, `PerfIndexEstimator.build()`, `candidates.stream().filter(candidate -> "gpu".equalsIgnoreCase(candidate.getCategorySlug())).toList()`, `candidates.stream().filter()`, `candidates.stream()`, `"gpu".equalsIgnoreCase()`, `candidate.getCategorySlug()`, `dictionaryService.build()`, `clock.instant()`, `log.info()`, `candidates.size()`, `rules.size()`, `dictionary.getDictionaryVersion()`
- [CandidateRepositoryFacadeImpl.java:144](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L144) `private static List<ComponentCandidateDTO> withSlugIn(List<ComponentCandidateDTO> items, List<String> slugs)`
  - Lời gọi xuất hiện: `items.stream() .filter(item -> item.getCategorySlug() != null && slugs.contains(item.getCategorySlug().toLowerCase())).toList()`, `items.stream().filter()`, `items.stream()`, `item.getCategorySlug()`, `slugs.contains()`, `item.getCategorySlug().toLowerCase()`
- [CandidateRepositoryFacadeImpl.java:150](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L150) `@Override public CandidatePool buildPool(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget)`
  - Lời gọi xuất hiện: `candidatePoolBuilder.build()`
- [CandidateRepositoryFacadeImpl.java:155](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/CandidateRepositoryFacadeImpl.java#L155) `private static RuleDefinition toRuleDefinition(CompatibilityRule rule)`
  - Lời gọi xuất hiện: `rule.getId()`, `rule.getRuleName()`, `rule.getCategory1Id()`, `rule.getSpec1Id()`, `rule.getCategory2Id()`, `rule.getSpec2Id()`, `rule.getOperator()`

## `pcbuild/internal/service/impl/GreedyBuildSolver.java`

### GreedyBuildSolver — [GreedyBuildSolver.java:54](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L54)

Khai báo: `@Slf4j @Service public class GreedyBuildSolver implements BuildSolver`

- Trường: `static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");`
- Trường: `private static final List<String> SCORED_ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "case", "cooler");`
- Trường: `private static final int MAX_ITERATIONS = 200;`
- Trường: `private static final int MAX_REFINE_ROUNDS = 50;`
- Trường: `private static final int SWAP2_TOP = 15;`
- Trường: `private static final int REPAIR_TOP = 8;`
- Trường: `private static final int REPAIR_STEPS = 12;`
- Trường: `private static final int MAX_QUANTITY = 8;`
- Trường: `private static final double EPS = 1e-4;`
- Trường: `private static final BigDecimal WINDOW_MIN = new BigDecimal("0.90");`
- Trường: `private static final int SOCKET = 1;`
- Trường: `private static final int MEMORY_TYPE = 4;`
- Trường: `private static final int RAM_SLOTS = 5;`
- Trường: `private static final int MAX_RAM_GB = 6;`
- Trường: `private static final int FORM_FACTOR = 7;`
- Trường: `private static final int M2_SLOTS = 8;`
- Trường: `private static final int CAPACITY_GB = 9;`
- Trường: `private static final int LENGTH_MM = 11;`
- Trường: `private static final int VRAM_GB = 12;`
- Trường: `private static final int WATTAGE = 14;`
- Trường: `private static final int MAX_GPU_LENGTH_MM = 16;`
- Trường: `private static final int MAX_COOLER_HEIGHT_MM = 17;`
- Trường: `private static final int SUPPORTED_MOTHERBOARDS = 18;`
- Trường: `private static final int HEIGHT_MM = 19;`
- Trường: `private static final int COOLER_TYPE = 20;`
- Trường: `private static final int SUPPORTED_SOCKETS = 21;`
- Trường: `private static final int CORE_COUNT = 22;`
- Trường: `private static final int HAS_IGPU = 26;`
- Trường: `private static final int INCLUDES_COOLER = 27;`
- Trường: `private static final int MAX_CPU_SOCKETS = 28;`
- Trường: `private static final int CPU_SOCKET_COUNT = 31;`
- Trường: `private static final int PCIE_X16_SLOTS = 32;`
- Trường: `private static final int MODULES_COUNT = 34;`
- Trường: `private static final int M2_SLOTS_USED = 40;`
- Trường: `private static final int GPU_COOLER_DESIGN = 53;`
- Trường: `private static final int MAX_GPU_QTY = 54;`
- Trường: `private static final int MAX_RADIATOR_MM = 59;`
- Trường: `private static final int RADIATOR_SIZE_MM = 60;`
- Trường: `private final CompatibilityEngine compatibilityEngine;`
- Trường: `private final PsuSizer psuSizer;`
- Trường: `private final PartScorer partScorer;`
- Trường: `private final CandidatePoolBuilder candidatePoolBuilder;`
- Trường: `private final ProfilePresets profilePresets;`
- Trường: `private volatile FloorPriceCache floorPriceCache;`
- [GreedyBuildSolver.java:110](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L110) `public GreedyBuildSolver(CompatibilityEngine compatibilityEngine, PsuSizer psuSizer, PartScorer partScorer, CandidatePoolBuilder candidatePoolBuilder, ProfilePresets profilePresets)`
- [GreedyBuildSolver.java:119](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L119) `@Override public SolverResult solve(SolverInput input)`
  - Lời gọi xuất hiện: `new Run(input.budget(), input.snapshot()).solve()`, `input.budget()`, `input.snapshot()`, `input.profile()`, `input.pool()`
- [GreedyBuildSolver.java:124](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L124) `@Override public BigDecimal floorPrice(CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `cache.snapshot()`, `cache.price()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(Archetype.GENERAL.name()) .roleWeights(new LinkedHashMap<>(profilePresets.roleWeights(Archetype.GENERAL))).build()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(Archetype.GENERAL.name()).roleWeights()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype()`, `RequirementProfileDto.builder() .profileVersion(1).source()`, `RequirementProfileDto.builder().profileVersion()`, `RequirementProfileDto.builder()`, `Archetype.GENERAL.name()`, `profilePresets.roleWeights()`, `List.of()`, `run.cheapestValidBuild()`, `candidatePoolBuilder.build()`, `totalOf()`
- [GreedyBuildSolver.java:1031](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1031) `private static Selection cheaper(Selection current, Selection candidate)`
  - Lời gọi xuất hiện: `candidate.cost().compareTo()`, `candidate.cost()`, `current.cost()`, `candidate.quantity()`, `current.quantity()`, `candidate.part().getVariantId()`, `candidate.part()`, `current.part().getVariantId()`, `current.part()`
- [GreedyBuildSolver.java:1041](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1041) `static BigDecimal totalOf(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `line.part()`, `line.part().getPrice()`, `total.add()`, `line.part().getPrice().multiply()`, `BigDecimal.valueOf()`, `line.quantity()`
- [GreedyBuildSolver.java:1051](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1051) `private static List<BuildLine> ordered(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `lines.stream().sorted(Comparator .comparing((BuildLine line) -> ROLE_ORDER.indexOf(line.role())) .thenComparing(line -> line.part().getVariantId())).toList()`, `lines.stream().sorted()`, `lines.stream()`, `Comparator .comparing((BuildLine line) -> ROLE_ORDER.indexOf(line.role())).thenComparing()`, `Comparator.comparing()`, `ROLE_ORDER.indexOf()`, `line.role()`, `line.part().getVariantId()`, `line.part()`
- [GreedyBuildSolver.java:1057](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1057) `private static List<BuildLine> replaceLine(List<BuildLine> lines, BuildLine target, BuildLine replacement)`
  - Lời gọi xuất hiện: `lines.size()`, `result.add()`
- [GreedyBuildSolver.java:1072](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1072) `private static List<BuildLine> withRole(List<BuildLine> lines, String role, List<BuildLine> replacement)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> !role.equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`, `result.addAll()`
- [GreedyBuildSolver.java:1078](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1078) `private static List<BuildLine> linesOf(List<BuildLine> lines, String role)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> role.equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`
- [GreedyBuildSolver.java:1082](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1082) `private static BuildLine first(List<BuildLine> lines, String role)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> role.equals(line.role())).findFirst().orElse()`, `lines.stream().filter(line -> role.equals(line.role())).findFirst()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`
- [GreedyBuildSolver.java:1086](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1086) `private static int quantity(List<BuildLine> lines, String role)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> role.equals(line.role())).mapToInt(BuildLine::quantity).sum()`, `lines.stream().filter(line -> role.equals(line.role())).mapToInt()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`
- [GreedyBuildSolver.java:1090](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1090) `private static long idSum(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `lines.stream().mapToLong(line -> line.part().getVariantId()).sum()`, `lines.stream().mapToLong()`, `lines.stream()`, `line.part().getVariantId()`, `line.part()`
- [GreedyBuildSolver.java:1094](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1094) `private static boolean isCheaper(ComponentCandidateDTO candidate, ComponentCandidateDTO current)`
  - Lời gọi xuất hiện: `candidate.getPrice().compareTo()`, `candidate.getPrice()`, `current.getPrice()`, `candidate.getVariantId()`, `current.getVariantId()`
- [GreedyBuildSolver.java:1099](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1099) `private static int compareCost(BigDecimal first, BigDecimal second)`
  - Lời gọi xuất hiện: `first.compareTo()`
- [GreedyBuildSolver.java:1106](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1106) `private static int ceilDiv(int value, int divisor)`
- [GreedyBuildSolver.java:1110](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1110) `private static List<Integer> distinct(int first, int second)`
  - Lời gọi xuất hiện: `List.of()`
- [GreedyBuildSolver.java:1114](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1114) `private static boolean equalsIgnoreCase(String first, String second)`
  - Lời gọi xuất hiện: `first.equalsIgnoreCase()`
- [GreedyBuildSolver.java:1118](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1118) `private static RequirementProfileDto copy(RequirementProfileDto profile)`
  - Lời gọi xuất hiện: `profile.toBuilder() .constraints(profile.getConstraints().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new))) .metrics(profile.getMetrics().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new))) .requiredRoles(new ArrayList<>(profile.getRequiredRoles())).build()`, `profile.toBuilder() .constraints(profile.getConstraints().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new))) .metrics(profile.getMetrics().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new))).requiredRoles()`, `profile.toBuilder() .constraints(profile.getConstraints().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new))).metrics()`, `profile.toBuilder().constraints()`, `profile.toBuilder()`, `profile.getConstraints().stream().map(item -> item.toBuilder().build()).collect()`, `profile.getConstraints().stream().map()`, `profile.getConstraints().stream()`, `profile.getConstraints()`, `item.toBuilder().build()`, `item.toBuilder()`, `Collectors.toCollection()`, `profile.getMetrics().stream().map(item -> item.toBuilder().build()).collect()`, `profile.getMetrics().stream().map()`, `profile.getMetrics().stream()`, `profile.getMetrics()`, `profile.getRequiredRoles()`
- [GreedyBuildSolver.java:1126](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1126) `private static String reasonOf(String reason, String fallback)`
  - Lời gọi xuất hiện: `reason.isBlank()`
- [GreedyBuildSolver.java:1130](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1130) `private static String format(double value)`
  - Lời gọi xuất hiện: `Math.rint()`, `String.valueOf()`
- [GreedyBuildSolver.java:1134](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1134) `private static String vnd(BigDecimal amount)`
  - Lời gọi xuất hiện: `symbols.setGroupingSeparator()`, `new DecimalFormat("#,##0", symbols).format()`, `amount.setScale()`

### FloorPriceCache — [GreedyBuildSolver.java:107](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L107)

Khai báo: `private record FloorPriceCache(CompatibilitySnapshot snapshot, BigDecimal price)`


### Run — [GreedyBuildSolver.java:144](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L144)

Khai báo: `private final class Run`

- Trường: `private final BigDecimal budget;`
- Trường: `private final CompatibilitySnapshot snapshot;`
- Trường: `private ValidatedProfile profile;`
- Trường: `private CandidatePool pool;`
- Trường: `private ScoreContext ctx;`
- Trường: `private final List<String> relaxedItems = new ArrayList<>();`
- Trường: `private final List<String> warnings = new ArrayList<>();`
- Trường: `private int maxPriceSteps;`
- [GreedyBuildSolver.java:154](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L154) `private Run(BigDecimal budget, CompatibilitySnapshot snapshot)`
- [GreedyBuildSolver.java:159](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L159) `private void use(ValidatedProfile nextProfile, CandidatePool nextPool)`
  - Lời gọi xuất hiện: `ScoreContext.of()`
- [GreedyBuildSolver.java:165](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L165) `private SolverResult solve(ValidatedProfile originalProfile, CandidatePool originalPool)`
  - Lời gọi xuất hiện: `use()`, `cheapestValidBuild()`, `totalOf()`, `totalOf(start).compareTo()`, `relaxOneStep()`, `noFeasibleBuild()`, `profile.maxPrice()`, `highestTotalBuild()`, `result()`, `utility()`, `relaxedItems.isEmpty()`, `budget.multiply()`, `upgradeMoves()`, `psuSizer.fix()`, `nextTotal.compareTo()`, `satisfiesProfile()`, `Math.max()`, `nextTotal.subtract(currentTotal).doubleValue()`, `nextTotal.subtract()`, `currentTotal.compareTo()`, `best.ratio()`, `isCompatible()`, `best.lines()`, `best.utility()`, `log.warn()`, `neighbours()`, `Math.abs()`, `total.compareTo()`, `isCatalogCeiling()`
- [GreedyBuildSolver.java:251](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L251) `private SolverResult result(List<BuildLine> lines, double lineUtility, String reasonCode, BigDecimal floorFirst, int iterations)`
  - Lời gọi xuất hiện: `totalOf()`, `compatibilityEngine.evaluate()`, `ordered()`, `budget.subtract()`
- [GreedyBuildSolver.java:262](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L262) `private List<BuildLine> highestTotalBuild(List<BuildLine> start)`
  - Lời gọi xuất hiện: `withPriciestPsu()`, `climbTotal()`, `onePlatformPerSocket()`, `totalOf(climbed).compareTo()`, `totalOf()`
- [GreedyBuildSolver.java:272](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L272) `private List<List<BuildLine>> onePlatformPerSocket(List<BuildLine> start)`
  - Lời gọi xuất hiện: `platformSwaps()`, `snapshot.parsed(first(platform, "cpu").part()).raw().get()`, `snapshot.parsed(first(platform, "cpu").part()).raw()`, `snapshot.parsed()`, `first(platform, "cpu").part()`, `first()`, `bySocket.containsKey()`, `totalOf(platform).compareTo()`, `totalOf()`, `withinStock()`, `satisfiesProfile()`, `isCompatible()`, `bySocket.put()`, `bySocket.values()`
- [GreedyBuildSolver.java:284](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L284) `private List<BuildLine> climbTotal(List<BuildLine> start)`
  - Lời gọi xuất hiện: `neighbours()`, `addQuantityMoves()`, `addQuantityJumps()`, `totalOf()`, `psuSizer.fix()`, `nextTotal.compareTo()`, `withinStock()`, `satisfiesProfile()`, `isCompatible()`
- [GreedyBuildSolver.java:309](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L309) `private void addQuantityJumps(List<List<BuildLine>> moves, List<BuildLine> current)`
  - Lời gọi xuất hiện: `List.of()`, `first()`, `pool.of()`, `candidate.getStockQuantity()`, `Math.min()`, `moves.add()`, `withRole()`
- [GreedyBuildSolver.java:322](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L322) `private List<BuildLine> withPriciestPsu(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `psuSizer.requiredW()`, `totalOf()`, `lines.stream().filter(line -> !"psu".equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `"psu".equals()`, `line.role()`, `pool.of()`, `snapshot.parsed(psu).ints().getOrDefault()`, `snapshot.parsed(psu).ints()`, `snapshot.parsed()`, `need.requiredW()`, `withoutPsu.add(psu.getPrice()).compareTo()`, `withoutPsu.add()`, `psu.getPrice()`, `withRole()`, `List.of()`, `satisfiesProfile()`, `isCompatible()`
- [GreedyBuildSolver.java:336](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L336) `private boolean withinStock(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `lines.stream().allMatch()`, `lines.stream()`, `line.part().getStockQuantity()`, `line.part()`, `line.quantity()`
- [GreedyBuildSolver.java:342](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L342) `private boolean considered(String role)`
  - Lời gọi xuất hiện: `profile.maxPrice()`, `profile.weight()`
- [GreedyBuildSolver.java:348](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L348) `private List<BuildLine> cheapestValidBuild(ValidatedProfile currentProfile, CandidatePool currentPool)`
  - Lời gọi xuất hiện: `use()`, `findCheapest()`
- [GreedyBuildSolver.java:362](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L362) `private List<BuildLine> findCheapest()`
  - Lời gọi xuất hiện: `Needs.of()`, `pool.of()`, `snapshot.parsed()`, `assemble()`, `totalOf()`, `total.compareTo()`, `satisfiesProfile()`, `isCompatible()`, `utility()`, `Math.abs()`, `idSum()`
- [GreedyBuildSolver.java:390](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L390) `private List<BuildLine> assemble(ComponentCandidateDTO cpu, ParsedSpecs cpuSpecs, ComponentCandidateDTO board, Needs needs)`
  - Lời gọi xuất hiện: `snapshot.parsed()`, `equalsIgnoreCase()`, `cpuSpecs.raw().get()`, `cpuSpecs.raw()`, `boardSpecs.raw().get()`, `boardSpecs.raw()`, `boardSpecs.ints().getOrDefault()`, `boardSpecs.ints()`, `cpuSpecs.ints().getOrDefault()`, `cpuSpecs.ints()`, `needs.cpu()`, `needs.maxCpu()`, `needs.cores()`, `"YES".equalsIgnoreCase()`, `profile.isExcluded()`, `needs.gpuRequired()`, `cheapestRam()`, `cheapestGpu()`, `cheapestSsd()`, `profile.isRequired()`, `pool.of()`, `snapshot.parsed(cooler).lists().getOrDefault(SUPPORTED_SOCKETS, java.util.Set.of()).contains()`, `snapshot.parsed(cooler).lists().getOrDefault()`, `snapshot.parsed(cooler).lists()`, `java.util.Set.of()`, `socket.toUpperCase()`, `coolerOptions.add()`, `coolerOptions.isEmpty()`, `cheapestCase()`, `gpu.part()`, `pcCase.getPrice().add()`, `pcCase.getPrice()`, `cooler.getPrice().multiply()`, `cooler.getPrice()`, `BigDecimal.valueOf()`, `cost.compareTo()`, `lines.add()`, `ram.part()`, `ram.quantity()`, `gpu.quantity()`, `ssd.part()`, `ssd.quantity()`, `psuSizer.fix()`
- [GreedyBuildSolver.java:454](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L454) `private Selection cheapestRam(ParsedSpecs board, Needs needs)`
  - Lời gọi xuất hiện: `pool.of()`, `snapshot.parsed()`, `equalsIgnoreCase()`, `specs.raw().get()`, `specs.raw()`, `board.raw().get()`, `board.raw()`, `specs.ints().get()`, `specs.ints()`, `Math.max()`, `ceilDiv()`, `needs.ramGb()`, `board.ints().getOrDefault()`, `board.ints()`, `cheaper()`
- [GreedyBuildSolver.java:470](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L470) `private Selection cheapestGpu(ParsedSpecs board, Needs needs)`
  - Lời gọi xuất hiện: `board.ints().getOrDefault()`, `board.ints()`, `pool.of()`, `snapshot.parsed()`, `specs.ints().getOrDefault()`, `specs.ints()`, `needs.vramGb()`, `Math.max()`, `needs.gpu()`, `ceilDiv()`, `needs.maxGpu()`, `"BLOWER".equalsIgnoreCase()`, `specs.raw().get()`, `specs.raw()`, `cheaper()`
- [GreedyBuildSolver.java:485](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L485) `private Selection cheapestSsd(ParsedSpecs board, Needs needs)`
  - Lời gọi xuất hiện: `pool.of()`, `snapshot.parsed()`, `specs.ints().get()`, `specs.ints()`, `Math.max()`, `ceilDiv()`, `needs.storageGb()`, `specs.ints().getOrDefault()`, `board.ints().getOrDefault()`, `board.ints()`, `cheaper()`
- [GreedyBuildSolver.java:500](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L500) `private ComponentCandidateDTO cheapestCase(ParsedSpecs board, ComponentCandidateDTO gpu, ComponentCandidateDTO cooler)`
  - Lời gọi xuất hiện: `snapshot.parsed()`, `"Liquid".equalsIgnoreCase()`, `coolerSpecs.raw().get()`, `coolerSpecs.raw()`, `board.raw().get()`, `board.raw()`, `pool.of()`, `specs.lists().getOrDefault(SUPPORTED_MOTHERBOARDS, java.util.Set.of()).contains()`, `specs.lists().getOrDefault()`, `specs.lists()`, `java.util.Set.of()`, `formFactor.toUpperCase()`, `specs.ints().get()`, `specs.ints()`, `gpuSpecs.ints().get()`, `gpuSpecs.ints()`, `coolerSpecs.ints().get()`, `coolerSpecs.ints()`, `specs.ints().getOrDefault()`, `coolerSpecs.ints().getOrDefault()`, `isCheaper()`
- [GreedyBuildSolver.java:529](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L529) `private boolean relaxOneStep()`
  - Lời gọi xuất hiện: `relaxationsAt()`, `options.isEmpty()`, `options.stream().filter(Relaxation::unsatisfiable).toList()`, `options.stream().filter()`, `options.stream()`, `unsatisfiable.isEmpty()`, `candidatePoolBuilder.build()`, `option.profile()`, `cheapestValidBuild()`, `totalOf()`, `compareRelaxation()`, `option.id().compareTo()`, `option.id()`, `chosen.id()`, `relaxedItems.contains()`, `relaxedItems.add()`, `warnings.add()`, `chosen.change()`, `use()`, `chosen.profile()`
- [GreedyBuildSolver.java:563](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L563) `private int compareRelaxation(BigDecimal cost, BigDecimal chosenCost)`
  - Lời gọi xuất hiện: `cost.compareTo()`, `chosenCost.compareTo()`, `compareCost()`
- [GreedyBuildSolver.java:574](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L574) `private List<Relaxation> relaxationsAt(int priority)`
  - Lời gọi xuất hiện: `profile.profile()`, `current.getConstraints()`, `Objects.equals()`, `constraint.getPriority()`, `options.add()`, `relaxConstraint()`, `current.getMetrics()`, `metric.getPriority()`, `relaxMetric()`, `current.getRequiredRoles()`, `required.getPriority()`, `copy()`, `changed.getRequiredRoles().removeIf()`, `changed.getRequiredRoles()`, `ref.getRole().equals()`, `ref.getRole()`, `required.getRole()`, `profile.withProfile()`
- [GreedyBuildSolver.java:595](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L595) `private Relaxation relaxConstraint(ProfileConstraintDto constraint)`
  - Lời gọi xuất hiện: `copy()`, `profile.profile()`, `withoutIt.getConstraints().removeIf()`, `withoutIt.getConstraints()`, `item.getId().equals()`, `item.getId()`, `constraint.getId()`, `profile.withProfile()`, `constraint.getValue()`, `format()`, `String.join()`, `constraint.getValues()`, `snapshot.specId()`, `constraint.getSpec()`, `"GTE".equals()`, `constraint.getOp()`, `"LTE".equals()`, `candidatePoolBuilder.build(snapshot, dropped, budget).of()`, `candidatePoolBuilder.build()`, `constraint.getRole()`, `snapshot.effectiveInt()`, `value.doubleValue()`, `Boolean.TRUE.equals()`, `constraint.getUnsatisfiable()`, `stepped.getConstraints()`, `item.setValue()`, `item.setUnsatisfiable()`, `item.setLabel()`, `item.getLabel()`, `item.getLabel().replace()`
- [GreedyBuildSolver.java:627](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L627) `private Relaxation relaxMetric(ProfileMetricDto metric)`
  - Lời gọi xuất hiện: `copy()`, `profile.profile()`, `Boolean.TRUE.equals()`, `metric.getUnsatisfiable()`, `format()`, `metric.getValue()`, `"GTE".equals()`, `metric.getOp()`, `Math.floor()`, `changed.getMetrics()`, `item.getId().equals()`, `item.getId()`, `metric.getId()`, `item.setValue()`, `item.setUnsatisfiable()`, `item.setLabel()`, `item.getLabel()`, `item.getLabel().replace()`, `profile.withProfile()`, `changed.getMetrics().removeIf()`
- [GreedyBuildSolver.java:646](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L646) `private BuildRuleException noFeasibleBuild(ValidatedProfile original, BigDecimal floorFirst)`
  - Lời gọi xuất hiện: `copy()`, `original.profile()`, `priorityOne.getConstraints().removeIf()`, `priorityOne.getConstraints()`, `Objects.equals()`, `item.getPriority()`, `priorityOne.getMetrics().removeIf()`, `priorityOne.getMetrics()`, `priorityOne.getRequiredRoles().removeIf()`, `priorityOne.getRequiredRoles()`, `original.withProfile()`, `cheapestValidBuild()`, `candidatePoolBuilder.build()`, `totalOf()`, `priorityOne.getConstraints().forEach()`, `blocking.add()`, `Map.of()`, `item.getId()`, `reasonOf()`, `item.getReason()`, `item.getLabel()`, `priorityOne.getMetrics().forEach()`, `priorityOne.getRequiredRoles().forEach()`, `item.getRole()`, `blocking.stream().map(item -> item.get("reason")).collect()`, `blocking.stream().map()`, `blocking.stream()`, `item.get()`, `Collectors.joining()`, `reasons.isEmpty()`, `vnd()`, `data.put()`
- [GreedyBuildSolver.java:672](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L672) `private List<List<BuildLine>> upgradeMoves(List<BuildLine> current)`
  - Lời gọi xuất hiện: `profile.weight()`, `"psu".equals()`, `linesOf()`, `roleLines.isEmpty()`, `roleLines.get()`, `partScorer.partScore()`, `target.part()`, `pool.of()`, `candidate.getPrice().compareTo()`, `candidate.getPrice()`, `target.part().getPrice()`, `addReplacements()`, `moves.addAll()`, `platformSwaps()`, `addQuantityMoves()`
- [GreedyBuildSolver.java:691](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L691) `private void addQuantityMoves(List<List<BuildLine>> moves, List<BuildLine> current)`
  - Lời gọi xuất hiện: `first()`, `ram.quantity()`, `moves.add()`, `replaceLine()`, `ram.part()`, `considered()`, `pool.of()`, `linesOf(current, "ssd").stream() .filter(line -> line.part().getVariantId().equals(ssd.getVariantId())).findFirst().orElse()`, `linesOf(current, "ssd").stream() .filter(line -> line.part().getVariantId().equals(ssd.getVariantId())).findFirst()`, `linesOf(current, "ssd").stream().filter()`, `linesOf(current, "ssd").stream()`, `linesOf()`, `line.part().getVariantId().equals()`, `line.part().getVariantId()`, `line.part()`, `ssd.getVariantId()`, `existing.quantity()`, `added.add()`, `profile.metrics().stream().anyMatch()`, `profile.metrics().stream()`, `profile.metrics()`, `MetricKind.GPU_COUNT.name().equals()`, `MetricKind.GPU_COUNT.name()`, `metric.getMetric()`, `"EQ".equals()`, `metric.getOp()`, `"LTE".equals()`, `profile.isExcluded()`, `gpu.quantity()`, `gpu.part()`, `Math.max()`, `quantity()`
- [GreedyBuildSolver.java:737](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L737) `private List<List<BuildLine>> platformSwaps(List<BuildLine> current, boolean onlyBetterCpu)`
  - Lời gọi xuất hiện: `first()`, `snapshot.parsed(currentCpu.part()).raw().get()`, `snapshot.parsed(currentCpu.part()).raw()`, `snapshot.parsed()`, `currentCpu.part()`, `partScorer.partScore()`, `Needs.of()`, `pool.of()`, `equalsIgnoreCase()`, `cpuSpecs.raw().get()`, `cpuSpecs.raw()`, `pool.of("mainboard").stream() .sorted(Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing(ComponentCandidateDTO::getVariantId)).toList()`, `pool.of("mainboard").stream().sorted()`, `pool.of("mainboard").stream()`, `Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing()`, `Comparator.comparing()`, `assemble()`, `List.of()`, `linesOf()`, `kept.isEmpty()`, `platform.stream().filter(line -> !role.equals(line.role())).toList()`, `platform.stream().filter()`, `platform.stream()`, `role.equals()`, `line.role()`, `trial.addAll()`, `psuSizer.fix()`, `satisfiesProfile()`, `isCompatible()`, `moves.add()`
- [GreedyBuildSolver.java:770](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L770) `private List<List<BuildLine>> neighbours(List<BuildLine> current)`
  - Lời gọi xuất hiện: `SCORED_ROLES.stream().filter(this::considered).toList()`, `SCORED_ROLES.stream().filter()`, `SCORED_ROLES.stream()`, `first()`, `pool.of()`, `candidate.getVariantId().equals()`, `candidate.getVariantId()`, `target.part().getVariantId()`, `target.part()`, `addReplacements()`, `totalOf()`, `budget.subtract(total).add()`, `budget.subtract()`, `target.part().getPrice().multiply()`, `target.part().getPrice()`, `BigDecimal.valueOf()`, `target.quantity()`, `pool.of(role).stream() .filter(candidate -> !candidate.getVariantId().equals(target.part().getVariantId())) .filter(candidate -> candidate.getPrice().multiply(BigDecimal.valueOf(target.quantity())).compareTo(room) <= 0) .limit(SWAP2_TOP).forEach()`, `pool.of(role).stream() .filter(candidate -> !candidate.getVariantId().equals(target.part().getVariantId())) .filter(candidate -> candidate.getPrice().multiply(BigDecimal.valueOf(target.quantity())).compareTo(room) <= 0).limit()`, `pool.of(role).stream() .filter(candidate -> !candidate.getVariantId().equals(target.part().getVariantId())).filter()`, `pool.of(role).stream().filter()`, `pool.of(role).stream()`, `candidate.getPrice().multiply(BigDecimal.valueOf(target.quantity())).compareTo()`, `candidate.getPrice().multiply()`, `candidate.getPrice()`, `options.add()`, `List.of()`, `options.addAll()`, `reductions()`, `options.isEmpty()`, `alternatives.put()`, `roles.stream().filter(alternatives::containsKey).toList()`, `roles.stream().filter()`, `roles.stream()`, `swappable.size()`, `swappable.get()`, `alternatives.get()`, `withRole()`, `moves.add()`, `addQuantityShifts()`, `moves.addAll()`, `upgradesWithRepair()`, `platformSwaps()`
- [GreedyBuildSolver.java:828](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L828) `private List<List<BuildLine>> upgradesWithRepair(List<BuildLine> current, List<String> roles)`
  - Lời gọi xuất hiện: `first()`, `partScorer.partScore()`, `target.part()`, `pool.of(role).stream() .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) > 0) .filter(candidate -> profile.maxPrice() || partScorer.partScore(role, candidate, ctx) > targetScore + 1e-9) .limit(REPAIR_TOP).toList()`, `pool.of(role).stream() .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) > 0) .filter(candidate -> profile.maxPrice() || partScorer.partScore(role, candidate, ctx) > targetScore + 1e-9).limit()`, `pool.of(role).stream() .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) > 0).filter()`, `pool.of(role).stream().filter()`, `pool.of(role).stream()`, `pool.of()`, `candidate.getPrice().compareTo()`, `candidate.getPrice()`, `target.part().getPrice()`, `profile.maxPrice()`, `psuSizer.fix()`, `replaceLine()`, `target.quantity()`, `repairToBudget()`, `moves.add()`
- [GreedyBuildSolver.java:849](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L849) `private List<BuildLine> repairToBudget(List<BuildLine> lines, String lockedRole)`
  - Lời gọi xuất hiện: `totalOf(current).compareTo()`, `totalOf()`, `utility()`, `role.equals()`, `first()`, `reductions()`, `pool.of(role).stream() .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) < 0) .limit(REPAIR_TOP).forEach()`, `pool.of(role).stream() .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) < 0).limit()`, `pool.of(role).stream().filter()`, `pool.of(role).stream()`, `pool.of()`, `candidate.getPrice().compareTo()`, `candidate.getPrice()`, `target.part().getPrice()`, `target.part()`, `options.add()`, `List.of()`, `target.quantity()`, `psuSizer.fix()`, `withRole()`, `currentTotal.subtract(totalOf(next)).doubleValue()`, `currentTotal.subtract()`, `satisfiesProfile()`
- [GreedyBuildSolver.java:884](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L884) `private List<List<BuildLine>> reductions(List<BuildLine> current, String role)`
  - Lời gọi xuất hiện: `List.of("ram", "gpu", "ssd").contains()`, `List.of()`, `linesOf()`, `roleLines.isEmpty()`, `roleLines.get()`, `roleLines.size()`, `options.add()`, `head.quantity()`, `minusOne.set()`, `head.part()`
- [GreedyBuildSolver.java:904](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L904) `private void addQuantityShifts(List<List<BuildLine>> moves, List<BuildLine> current)`
  - Lời gọi xuất hiện: `BuildMetrics.of()`, `Needs.of()`, `first()`, `pool.of()`, `snapshot.parsed(candidate).ints().getOrDefault()`, `snapshot.parsed(candidate).ints()`, `snapshot.parsed()`, `distinct()`, `Math.max()`, `ceilDiv()`, `needs.ramGb()`, `metrics.get()`, `ram.quantity()`, `candidate.getVariantId().equals()`, `candidate.getVariantId()`, `ram.part().getVariantId()`, `ram.part()`, `moves.add()`, `replaceLine()`, `linesOf()`, `ssds.isEmpty()`, `needs.storageGb()`, `current.stream().filter(line -> !"ssd".equals(line.role())).toList()`, `current.stream().filter()`, `current.stream()`, `"ssd".equals()`, `line.role()`, `shifted.add()`
- [GreedyBuildSolver.java:939](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L939) `private boolean isCatalogCeiling(List<BuildLine> current)`
  - Lời gọi xuất hiện: `profile.weight()`, `profile.isExcluded()`, `pool.of(role).isEmpty()`, `pool.of()`, `linesOf()`, `roleLines.isEmpty()`, `partScorer.partScore()`, `pool.of(role).get()`, `line.part()`, `addQuantityMoves()`, `psuSizer.fix()`, `totalOf(fixed).compareTo()`, `totalOf()`, `satisfiesProfile()`, `isCompatible()`
- [GreedyBuildSolver.java:963](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L963) `private boolean satisfiesProfile(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `BuildMetrics.of()`, `profile.metrics()`, `ProfileMatcher.matches()`, `profile.profile().getRequiredRoles()`, `profile.profile()`, `quantity()`, `required.getRole()`
- [GreedyBuildSolver.java:975](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L975) `private boolean isCompatible(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `compatibilityEngine.evaluate(lines, new EvaluationContext(null, EvaluationContext.Mode.SOLVER, snapshot)).compatible()`, `compatibilityEngine.evaluate()`
- [GreedyBuildSolver.java:979](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L979) `private double utility(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `partScorer.utility()`
- [GreedyBuildSolver.java:983](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L983) `private void addReplacements(List<List<BuildLine>> moves, List<BuildLine> current, BuildLine target, ComponentCandidateDTO candidate)`
  - Lời gọi xuất hiện: `moves.add()`, `replaceLine()`, `target.role()`, `target.quantity()`, `"ram".equals()`, `snapshot.parsed(candidate).ints().getOrDefault()`, `snapshot.parsed(candidate).ints()`, `snapshot.parsed()`, `Math.max()`, `ceilDiv()`, `Needs.of(profile).ramGb()`, `Needs.of()`

### Relaxation — [GreedyBuildSolver.java:571](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L571)

Khai báo: `private record Relaxation(String id, boolean unsatisfiable, ValidatedProfile profile, String change)`


### Needs — [GreedyBuildSolver.java:998](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L998)

Khai báo: `private record Needs(int cpu, int maxCpu, int cores, int ramGb, int gpu, int maxGpu, int vramGb, int storageGb, boolean gpuRequired)`

- [GreedyBuildSolver.java:1001](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1001) `static Needs of(ValidatedProfile profile)`
  - Lời gọi xuất hiện: `Math.max()`, `value()`, `Math.min()`, `profile.isRequired()`
- [GreedyBuildSolver.java:1016](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1016) `private static int value(ValidatedProfile profile, MetricKind kind, String op, int fallback)`
  - Lời gọi xuất hiện: `profile.metrics().stream() .filter(metric -> kind.name().equals(metric.getMetric()) && op.equals(metric.getOp()) && metric.getValue() != null) .map(metric -> metric.getValue().intValue()) .findFirst().orElse()`, `profile.metrics().stream() .filter(metric -> kind.name().equals(metric.getMetric()) && op.equals(metric.getOp()) && metric.getValue() != null) .map(metric -> metric.getValue().intValue()).findFirst()`, `profile.metrics().stream() .filter(metric -> kind.name().equals(metric.getMetric()) && op.equals(metric.getOp()) && metric.getValue() != null).map()`, `profile.metrics().stream().filter()`, `profile.metrics().stream()`, `profile.metrics()`, `kind.name().equals()`, `kind.name()`, `metric.getMetric()`, `op.equals()`, `metric.getOp()`, `metric.getValue()`, `metric.getValue().intValue()`

### Selection — [GreedyBuildSolver.java:1024](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1024)

Khai báo: `private record Selection(ComponentCandidateDTO part, int quantity)`

- [GreedyBuildSolver.java:1025](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/GreedyBuildSolver.java#L1025) `BigDecimal cost()`
  - Lời gọi xuất hiện: `part.getPrice().multiply()`, `part.getPrice()`, `BigDecimal.valueOf()`

## `pcbuild/internal/service/impl/LlmClientServiceImpl.java`

### LlmClientServiceImpl — [LlmClientServiceImpl.java:30](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L30)

Khai báo: `@Slf4j @Service @RequiredArgsConstructor public class LlmClientServiceImpl implements LlmClientService`

- Trường: `private final ObjectMapper objectMapper;`
- Trường: `private final AiCircuitBreaker circuitBreaker;`
- Trường: `private final Clock clock;`
- Trường: `private HttpClient httpClient;`
- Trường: `private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models/";`
- Trường: `private String openAiUrl = "https://api.openai.com/v1/chat/completions";`
- Trường: `@Value("${app.ai.api-key}") private String apiKey;`
- Trường: `@Value("${app.ai.provider}") private String provider;`
- Trường: `@Value("${app.ai.model}") private String modelName;`
- Trường: `@Value("${app.ai.timeout-ms}") private long timeoutMs;`
- Trường: `@Value("${app.ai.profile-timeout-ms:6000}") private long profileTimeoutMs;`
- Trường: `@Value("${app.ai.max-retries:1}") private int maxRetries;`
- Trường: `@Value("${app.ai.min-call-ms:2000}") private long minCallMs;`
- Trường: `@Value("${app.ai.profile-thinking-level:low}") private String profileThinkingLevel;`
- Trường: `@Value("${app.ai.explain-thinking-level:minimal}") private String explainThinkingLevel;`
- [LlmClientServiceImpl.java:72](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L72) `@PostConstruct void init()`
  - Lời gọi xuất hiện: `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()`, `HttpClient.newBuilder().connectTimeout()`, `HttpClient.newBuilder()`, `Duration.ofSeconds()`
- [LlmClientServiceImpl.java:77](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L77) `private RestClient clientWithTimeout(long readTimeoutMs)`
  - Lời gọi xuất hiện: `factory.setReadTimeout()`, `Duration.ofMillis()`, `RestClient.builder().requestFactory(factory).build()`, `RestClient.builder().requestFactory()`, `RestClient.builder()`
- [LlmClientServiceImpl.java:83](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L83) `@Override public RequirementProfileDto extractProfile(String systemPrompt, String userJson, Instant deadline)`
  - Lời gọi xuất hiện: `callJson()`, `profileSchema()`
- [LlmClientServiceImpl.java:89](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L89) `@Override public ExplanationDraft explainBuild(String systemPrompt, String userJson, Instant deadline)`
  - Lời gọi xuất hiện: `callJson()`, `explanationSchema()`
- [LlmClientServiceImpl.java:99](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L99) `private <T> T callJson(String step, String systemPrompt, String userJson, Map<String, Object> schema, String thinkingLevel, long stepTimeoutMs, Instant deadline, Class<T> type)`
  - Lời gọi xuất hiện: `apiKey.isBlank()`, `log.error()`, `Duration.between(clock.instant(), deadline).toMillis()`, `Duration.between()`, `clock.instant()`, `log.warn()`, `circuitBreaker.beforeCall()`, `"gemini".equalsIgnoreCase()`, `postGemini()`, `Math.min()`, `postOpenAi()`, `circuitBreaker.onSuccess()`, `isTimeout()`, `circuitBreaker.onProviderFailure()`, `lastError.name()`, `e.getStatusCode().value()`, `e.getStatusCode()`, `circuitBreaker.onNeutralOutcome()`, `objectMapper.readValue()`, `stripCodeFence()`
- [LlmClientServiceImpl.java:145](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L145) `private String postGemini(String systemPrompt, String userJson, Map<String, Object> schema, String thinkingLevel, long readTimeoutMs) throws ResourceAccessException`
  - Lời gọi xuất hiện: `generationConfig.put()`, `Map.of()`, `List.of()`, `clientWithTimeout(readTimeoutMs).post() .uri(geminiBaseUrl + modelName + ":generateContent") .header("x-goog-api-key", apiKey) .contentType(MediaType.APPLICATION_JSON) .body(body) .retrieve().body()`, `clientWithTimeout(readTimeoutMs).post() .uri(geminiBaseUrl + modelName + ":generateContent") .header("x-goog-api-key", apiKey) .contentType(MediaType.APPLICATION_JSON) .body(body).retrieve()`, `clientWithTimeout(readTimeoutMs).post() .uri(geminiBaseUrl + modelName + ":generateContent") .header("x-goog-api-key", apiKey) .contentType(MediaType.APPLICATION_JSON).body()`, `clientWithTimeout(readTimeoutMs).post() .uri(geminiBaseUrl + modelName + ":generateContent") .header("x-goog-api-key", apiKey).contentType()`, `clientWithTimeout(readTimeoutMs).post() .uri(geminiBaseUrl + modelName + ":generateContent").header()`, `clientWithTimeout(readTimeoutMs).post().uri()`, `clientWithTimeout(readTimeoutMs).post()`, `clientWithTimeout()`, `geminiText()`
- [LlmClientServiceImpl.java:166](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L166) `private String postOpenAi(String systemPrompt, String userJson, long readTimeoutMs)`
  - Lời gọi xuất hiện: `Map.of()`, `List.of()`, `clientWithTimeout(readTimeoutMs).post() .uri(openAiUrl) .header("Authorization", "Bearer " + apiKey) .contentType(MediaType.APPLICATION_JSON) .body(body) .retrieve().body()`, `clientWithTimeout(readTimeoutMs).post() .uri(openAiUrl) .header("Authorization", "Bearer " + apiKey) .contentType(MediaType.APPLICATION_JSON) .body(body).retrieve()`, `clientWithTimeout(readTimeoutMs).post() .uri(openAiUrl) .header("Authorization", "Bearer " + apiKey) .contentType(MediaType.APPLICATION_JSON).body()`, `clientWithTimeout(readTimeoutMs).post() .uri(openAiUrl) .header("Authorization", "Bearer " + apiKey).contentType()`, `clientWithTimeout(readTimeoutMs).post() .uri(openAiUrl).header()`, `clientWithTimeout(readTimeoutMs).post().uri()`, `clientWithTimeout(readTimeoutMs).post()`, `clientWithTimeout()`, `objectMapper.readTree(response).path("choices").path(0).path("message").path("content").asText()`, `objectMapper.readTree(response).path("choices").path(0).path("message").path()`, `objectMapper.readTree(response).path("choices").path(0).path()`, `objectMapper.readTree(response).path("choices").path()`, `objectMapper.readTree(response).path()`, `objectMapper.readTree()`
- [LlmClientServiceImpl.java:186](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L186) `private String geminiText(String response)`
  - Lời gọi xuất hiện: `objectMapper.readTree(response).path("candidates").path(0).path("content").path()`, `objectMapper.readTree(response).path("candidates").path(0).path()`, `objectMapper.readTree(response).path("candidates").path()`, `objectMapper.readTree(response).path()`, `objectMapper.readTree()`, `part.hasNonNull()`, `part.path("thought").asBoolean()`, `part.path()`, `part.path("text").asText()`, `log.warn()`
- [LlmClientServiceImpl.java:199](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L199) `private static boolean isTimeout(Throwable error)`
  - Lời gọi xuất hiện: `cause.getCause()`
- [LlmClientServiceImpl.java:206](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L206) `private static String stripCodeFence(String text)`
  - Lời gọi xuất hiện: `text.trim()`, `trimmed.startsWith()`, `trimmed.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim()`, `trimmed.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll()`, `trimmed.replaceAll()`
- [LlmClientServiceImpl.java:216](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L216) `private static Map<String, Object> type(String type)`
  - Lời gọi xuất hiện: `Map.of()`
- [LlmClientServiceImpl.java:220](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L220) `private static Map<String, Object> nullable(String type)`
  - Lời gọi xuất hiện: `Map.of()`
- [LlmClientServiceImpl.java:224](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L224) `private static Map<String, Object> array(Map<String, Object> items)`
  - Lời gọi xuất hiện: `Map.of()`
- [LlmClientServiceImpl.java:228](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L228) `private static Map<String, Object> object(Map<String, Object> properties, List<String> required)`
  - Lời gọi xuất hiện: `schema.put()`, `required.isEmpty()`
- [LlmClientServiceImpl.java:236](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L236) `static Map<String, Object> profileSchema()`
  - Lời gọi xuất hiện: `List.of()`, `roleWeights.put()`, `type()`, `constraint.put()`, `Map.of()`, `nullable()`, `array()`, `metric.put()`, `preference.put()`, `properties.put()`, `object()`
- [LlmClientServiceImpl.java:287](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/LlmClientServiceImpl.java#L287) `static Map<String, Object> explanationSchema()`
  - Lời gọi xuất hiện: `properties.put()`, `type()`, `array()`, `object()`, `Map.of()`, `List.of()`

## `pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java`

### MetadataDictionaryServiceImpl — [MetadataDictionaryServiceImpl.java:34](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L34)

Khai báo: `@Slf4j @Service @RequiredArgsConstructor public class MetadataDictionaryServiceImpl implements MetadataDictionaryService`

- Trường: `private static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");`
- Trường: `private static final Pattern INTEGER = Pattern.compile("^\\d+$");`
- Trường: `private static final int MAX_RAM_GB = 6;`
- Trường: `private static final int M2_SLOTS = 8;`
- Trường: `private static final int CAPACITY_GB = 9;`
- Trường: `private static final int VRAM_GB = 12;`
- Trường: `private static final int CORE_COUNT = 22;`
- Trường: `private static final int MAX_CPU_SOCKETS = 28;`
- Trường: `private static final int CPU_SOCKET_COUNT = 31;`
- Trường: `private static final int PCIE_X16_SLOTS = 32;`
- Trường: `private static final int MAX_GPU_QTY = 54;`
- Trường: `private static final int MAX_SSD_LINES = 8;`
- Trường: `private final ObjectMapper objectMapper;`
- [MetadataDictionaryServiceImpl.java:56](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L56) `@Override public MetadataDictionaryResponse build(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsedByVariantId, List<CategorySpecDto> specificationCatalog, PerfIndexEstimator perfIndexEstimator)`
  - Lời gọi xuất hiện: `inferTypes()`, `specificationCatalog.stream() .filter(row -> role.equalsIgnoreCase(row.getCategorySlug())).toList()`, `specificationCatalog.stream().filter()`, `specificationCatalog.stream()`, `role.equalsIgnoreCase()`, `row.getCategorySlug()`, `rows.isEmpty()`, `candidates.stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())).toList()`, `candidates.stream().filter()`, `candidates.stream()`, `candidate.getCategorySlug()`, `roles.put()`, `buildRole()`, `MetadataDictionaryResponse.builder() .roles(roles) .metrics(buildMetrics(candidates, parsedByVariantId)).build()`, `MetadataDictionaryResponse.builder() .roles(roles).metrics()`, `MetadataDictionaryResponse.builder().roles()`, `MetadataDictionaryResponse.builder()`, `buildMetrics()`, `dictionary.setDictionaryVersion()`, `versionOf()`
- [MetadataDictionaryServiceImpl.java:85](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L85) `private Map<Integer, SpecType> inferTypes(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsed, List<CategorySpecDto> catalog)`
  - Lời gọi xuất hiện: `hasOptions.merge()`, `row.getSpecificationId()`, `row.getOptions().isEmpty()`, `row.getOptions()`, `parsedOf(candidate, parsed).raw().forEach()`, `parsedOf(candidate, parsed).raw()`, `parsedOf()`, `valuesBySpec.computeIfAbsent(specId, key -> new ArrayList<>()).add()`, `valuesBySpec.computeIfAbsent()`, `INTEGER.matcher(value).matches()`, `INTEGER.matcher()`, `nonNumericVariants.computeIfAbsent(specId, key -> new ArrayList<>()).add()`, `nonNumericVariants.computeIfAbsent()`, `candidate.getVariantId()`, `hasOptions.keySet()`, `valuesBySpec.getOrDefault()`, `List.of()`, `nonNumericVariants.getOrDefault()`, `Boolean.TRUE.equals()`, `hasOptions.get()`, `values.stream().anyMatch()`, `values.stream()`, `value.contains()`, `values.isEmpty()`, `dirty.isEmpty()`, `dirty.size()`, `values.size()`, `log.warn()`, `types.put()`
- [MetadataDictionaryServiceImpl.java:127](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L127) `private MetadataDictionaryResponse.RoleEntry buildRole(String role, List<CategorySpecDto> rows, List<ComponentCandidateDTO> roleCandidates, Map<Integer, ParsedSpecs> parsed, Map<Integer, SpecType> typeBySpecId, PerfIndexEstimator estimator)`
  - Lời gọi xuất hiện: `typeBySpecId.getOrDefault()`, `row.getSpecificationId()`, `enumEntry()`, `listEntry()`, `intEntry()`, `specs.put()`, `row.getSpecificationName()`, `roleCandidates.stream().map(ComponentCandidateDTO::getPrice) .filter(Objects::nonNull).sorted().toList()`, `roleCandidates.stream().map(ComponentCandidateDTO::getPrice) .filter(Objects::nonNull).sorted()`, `roleCandidates.stream().map(ComponentCandidateDTO::getPrice).filter()`, `roleCandidates.stream().map()`, `roleCandidates.stream()`, `roleCandidates.stream().map(ComponentCandidateDTO::getBrandName).filter(Objects::nonNull).forEach()`, `roleCandidates.stream().map(ComponentCandidateDTO::getBrandName).filter()`, `brands.isEmpty()`, `brands.forEach()`, `brandValues.put()`, `MetadataDictionaryResponse.SpecEntry.builder() .type(SpecType.ENUM.name()).values(brandValues).build()`, `MetadataDictionaryResponse.SpecEntry.builder() .type(SpecType.ENUM.name()).values()`, `MetadataDictionaryResponse.SpecEntry.builder().type()`, `MetadataDictionaryResponse.SpecEntry.builder()`, `SpecType.ENUM.name()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()) .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()) .label(rows.get(0).getCategoryName()) .priceRange(prices.isEmpty() ? null : List.of(prices.get(0), prices.get(prices.size() - 1))) .brands(new ArrayList<>(brands)) .specs(specs).build()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()) .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()) .label(rows.get(0).getCategoryName()) .priceRange(prices.isEmpty() ? null : List.of(prices.get(0), prices.get(prices.size() - 1))) .brands(new ArrayList<>(brands)).specs()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()) .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()) .label(rows.get(0).getCategoryName()) .priceRange(prices.isEmpty() ? null : List.of(prices.get(0), prices.get(prices.size() - 1))).brands()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()) .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()) .label(rows.get(0).getCategoryName()).priceRange()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()) .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()).label()`, `MetadataDictionaryResponse.RoleEntry.builder() .categoryId(rows.get(0).getCategoryId()).primarySpecs()`, `MetadataDictionaryResponse.RoleEntry.builder().categoryId()`, `MetadataDictionaryResponse.RoleEntry.builder()`, `rows.get(0).getCategoryId()`, `rows.get()`, `rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList()`, `rows.stream().filter(CategorySpecDto::isPrimary).map()`, `rows.stream().filter()`, `rows.stream()`, `rows.get(0).getCategoryName()`, `prices.isEmpty()`, `List.of()`, `prices.get()`, `prices.size()`
- [MetadataDictionaryServiceImpl.java:167](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L167) `private MetadataDictionaryResponse.SpecEntry enumEntry(CategorySpecDto row)`
  - Lời gọi xuất hiện: `row.getOptions().forEach()`, `row.getOptions()`, `values.put()`, `option.getCode()`, `option.getDisplayValue()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type(SpecType.ENUM.name()).values(values).build()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type(SpecType.ENUM.name()).values()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type()`, `MetadataDictionaryResponse.SpecEntry.builder().specId()`, `MetadataDictionaryResponse.SpecEntry.builder()`, `row.getSpecificationId()`, `SpecType.ENUM.name()`
- [MetadataDictionaryServiceImpl.java:174](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L174) `private MetadataDictionaryResponse.SpecEntry listEntry(CategorySpecDto row, List<ComponentCandidateDTO> roleCandidates, Map<Integer, ParsedSpecs> parsed)`
  - Lời gọi xuất hiện: `parsedOf(candidate, parsed).raw().get()`, `parsedOf(candidate, parsed).raw()`, `parsedOf()`, `row.getSpecificationId()`, `raw.split()`, `code.isBlank()`, `codes.add()`, `code.trim()`, `codes.isEmpty()`, `codes.forEach()`, `values.put()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type(SpecType.LIST.name()).values(values).build()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type(SpecType.LIST.name()).values()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(row.getSpecificationId()).type()`, `MetadataDictionaryResponse.SpecEntry.builder().specId()`, `MetadataDictionaryResponse.SpecEntry.builder()`, `SpecType.LIST.name()`
- [MetadataDictionaryServiceImpl.java:192](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L192) `private MetadataDictionaryResponse.SpecEntry intEntry(CategorySpecDto row, List<ComponentCandidateDTO> roleCandidates, Map<Integer, ParsedSpecs> parsed, PerfIndexEstimator estimator)`
  - Lời gọi xuất hiện: `row.getSpecificationId()`, `parsedOf()`, `estimator.perfIndex()`, `specs.ints().get()`, `specs.ints()`, `Math.min()`, `Math.max()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(specId).type(SpecType.INT.name()).unit(unitOf(row.getSpecificationName())) .min(min).max(max).build()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(specId).type(SpecType.INT.name()).unit(unitOf(row.getSpecificationName())) .min(min).max()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(specId).type(SpecType.INT.name()).unit(unitOf(row.getSpecificationName())).min()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(specId).type(SpecType.INT.name()).unit()`, `MetadataDictionaryResponse.SpecEntry.builder() .specId(specId).type()`, `MetadataDictionaryResponse.SpecEntry.builder().specId()`, `MetadataDictionaryResponse.SpecEntry.builder()`, `SpecType.INT.name()`, `unitOf()`, `row.getSpecificationName()`
- [MetadataDictionaryServiceImpl.java:215](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L215) `private Map<String, MetadataDictionaryResponse.MetricEntry> buildMetrics(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsed)`
  - Lời gọi xuất hiện: `specsOf()`, `maxOf()`, `specs.ints().getOrDefault()`, `specs.ints()`, `Math.max()`, `metrics.put()`, `MetricKind.TOTAL_RAM_GB.name()`, `metric()`, `minOf()`, `specs.ints().get()`, `MetricKind.TOTAL_STORAGE_GB.name()`, `Math.min()`, `MetricKind.TOTAL_VRAM_GB.name()`, `gpuQuantityLimit()`, `MetricKind.GPU_COUNT.name()`, `MetricKind.CPU_COUNT.name()`, `MetricKind.TOTAL_CPU_CORES.name()`
- [MetadataDictionaryServiceImpl.java:248](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L248) `private static int gpuQuantityLimit(ParsedSpecs gpu, int maxBoardSlots)`
  - Lời gọi xuất hiện: `Math.min()`, `gpu.ints().getOrDefault()`, `gpu.ints()`
- [MetadataDictionaryServiceImpl.java:252](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L252) `private static MetadataDictionaryResponse.MetricEntry metric(MetricKind kind, Integer min, Integer max)`
  - Lời gọi xuất hiện: `MetadataDictionaryResponse.MetricEntry.builder() .label(kind.label()).unit(kind.unit()).min(min).max(max).build()`, `MetadataDictionaryResponse.MetricEntry.builder() .label(kind.label()).unit(kind.unit()).min(min).max()`, `MetadataDictionaryResponse.MetricEntry.builder() .label(kind.label()).unit(kind.unit()).min()`, `MetadataDictionaryResponse.MetricEntry.builder() .label(kind.label()).unit()`, `MetadataDictionaryResponse.MetricEntry.builder().label()`, `MetadataDictionaryResponse.MetricEntry.builder()`, `kind.label()`, `kind.unit()`
- [MetadataDictionaryServiceImpl.java:259](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L259) `static String unitOf(String specName)`
  - Lời gọi xuất hiện: `specName.equals()`, `specName.endsWith()`, `List.of()`
- [MetadataDictionaryServiceImpl.java:275](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L275) `private static ParsedSpecs parsedOf(ComponentCandidateDTO candidate, Map<Integer, ParsedSpecs> parsed)`
  - Lời gọi xuất hiện: `parsed.get()`, `candidate.getVariantId()`, `ParsedSpecs.of()`
- [MetadataDictionaryServiceImpl.java:280](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L280) `private static List<ParsedSpecs> specsOf(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsed, String role)`
  - Lời gọi xuất hiện: `candidates.stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())) .map(candidate -> parsedOf(candidate, parsed)).toList()`, `candidates.stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())).map()`, `candidates.stream().filter()`, `candidates.stream()`, `role.equalsIgnoreCase()`, `candidate.getCategorySlug()`, `parsedOf()`
- [MetadataDictionaryServiceImpl.java:288](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L288) `private static int maxOf(List<ParsedSpecs> specs, ToIntFunction<ParsedSpecs> value)`
  - Lời gọi xuất hiện: `specs.stream().mapToInt(value).max().orElse()`, `specs.stream().mapToInt(value).max()`, `specs.stream().mapToInt()`, `specs.stream()`
- [MetadataDictionaryServiceImpl.java:292](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L292) `private static Integer minOf(List<ParsedSpecs> specs, java.util.function.Function<ParsedSpecs, Integer> value)`
  - Lời gọi xuất hiện: `specs.stream().map(value).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse()`, `specs.stream().map(value).filter(Objects::nonNull).min()`, `specs.stream().map(value).filter()`, `specs.stream().map()`, `specs.stream()`, `Comparator.naturalOrder()`
- [MetadataDictionaryServiceImpl.java:297](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/MetadataDictionaryServiceImpl.java#L297) `private String versionOf(MetadataDictionaryResponse dictionary)`
  - Lời gọi xuất hiện: `MessageDigest.getInstance("SHA-256").digest()`, `MessageDigest.getInstance()`, `objectMapper.writeValueAsString(dictionary).getBytes()`, `objectMapper.writeValueAsString()`, `HexFormat.of().formatHex(hash).substring()`, `HexFormat.of().formatHex()`, `HexFormat.of()`

## `pcbuild/internal/service/impl/PartScorerImpl.java`

### PartScorerImpl — [PartScorerImpl.java:21](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L21)

Khai báo: `@Service public class PartScorerImpl implements PartScorer`

- Trường: `private static final List<String> ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");`
- Trường: `private static final int PRICE_STATUS = 66;`
- Trường: `private static final int LIFECYCLE = 63;`
- Trường: `private static final Set<String> ESTIMATED_PRICE = Set.of("ESTIMATE", "BUILD_ONLY");`
- Trường: `private static final BigDecimal LEGACY_PENALTY_FROM = BigDecimal.valueOf(20_000_000L);`
- Trường: `private static final double BALANCE_PENALTY = 0.15;`
- [PartScorerImpl.java:31](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L31) `@Override public double utility(List<BuildLine> lines, ScoreContext ctx)`
  - Lời gọi xuất hiện: `BuildMetrics.of()`, `ctx.snapshot()`, `ctx.profile().weight()`, `ctx.profile()`, `roleScore()`, `scores.put()`, `ctx.profile().profile().getBalancePairs()`, `ctx.profile().profile()`, `Math.abs()`, `scores.getOrDefault()`, `pair.getA()`, `pair.getB()`, `Math.max()`, `pair.getMaxGap()`
- [PartScorerImpl.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L49) `@Override public double roleScore(String role, List<BuildLine> lines, ScoreContext ctx)`
  - Lời gọi xuất hiện: `roleScore()`, `BuildMetrics.of()`, `ctx.snapshot()`
- [PartScorerImpl.java:54](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L54) `@Override public double partScore(String role, ComponentCandidateDTO part, ScoreContext ctx)`
  - Lời gọi xuất hiện: `List.of()`, `roleScore()`, `BuildMetrics.of()`, `ctx.snapshot()`
- [PartScorerImpl.java:60](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L60) `private double roleScore(String role, List<BuildLine> lines, Map<MetricKind, Integer> metrics, ScoreContext ctx)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> line != null && role.equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`, `roleLines.isEmpty()`, `"cooler".equals()`, `ctx.preferences()`, `preference.metric()`, `metricScore()`, `metrics.getOrDefault()`, `lineAverage()`, `preference.weight()`, `ctx.profile().profile().getBrands().getPrefer()`, `ctx.profile().profile().getBrands()`, `ctx.profile().profile()`, `ctx.profile()`, `preferredBrands.isEmpty()`, `roleLines.stream().allMatch()`, `roleLines.stream()`, `containsIgnoreCase()`, `line.part().getBrandName()`, `line.part()`, `Math.min()`, `roleLines.stream().anyMatch()`, `ESTIMATED_PRICE.contains()`, `upper()`, `raw()`, `ctx.budget()`, `ctx.budget().compareTo()`, `"LEGACY".equals()`
- [PartScorerImpl.java:92](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L92) `private double lineAverage(ScoreContext.ResolvedPreference preference, List<BuildLine> roleLines, String role, ScoreContext ctx)`
  - Lời gọi xuất hiện: `partValue()`, `line.part()`, `line.quantity()`
- [PartScorerImpl.java:102](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L102) `private double partValue(ScoreContext.ResolvedPreference preference, ComponentCandidateDTO part, String role, ScoreContext ctx)`
  - Lời gọi xuất hiện: `preference.specId()`, `"PREFER".equals()`, `preference.direction()`, `ctx.snapshot().parsed()`, `ctx.snapshot()`, `specs.lists().getOrDefault()`, `specs.lists()`, `Set.of()`, `codes.stream().anyMatch()`, `codes.stream()`, `preference.values()`, `ctx.snapshot().effectiveInt()`, `ctx.pool()`, `ctx.pool().range()`, `Math.min()`, `preference.cap()`, `clamp()`, `"MIN".equals()`
- [PartScorerImpl.java:120](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L120) `private double metricScore(ScoreContext.ResolvedPreference preference, int value, ScoreContext ctx)`
  - Lời gọi xuất hiện: `preference.cap()`, `ctx.snapshot().dictionary()`, `ctx.snapshot()`, `dictionary.getMetrics().get()`, `dictionary.getMetrics()`, `preference.metric().name()`, `preference.metric()`, `entry.getMax()`, `Math.min()`, `Double.isInfinite()`, `clamp()`, `"MIN".equals()`, `preference.direction()`
- [PartScorerImpl.java:131](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L131) `private static String raw(BuildLine line, int specId, ScoreContext ctx)`
  - Lời gọi xuất hiện: `ctx.snapshot().parsed(line.part()).raw().get()`, `ctx.snapshot().parsed(line.part()).raw()`, `ctx.snapshot().parsed()`, `ctx.snapshot()`, `line.part()`
- [PartScorerImpl.java:135](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L135) `private static boolean containsIgnoreCase(List<String> values, String value)`
  - Lời gọi xuất hiện: `values.stream().anyMatch()`, `values.stream()`, `candidate.equalsIgnoreCase()`
- [PartScorerImpl.java:139](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L139) `private static String upper(String value)`
  - Lời gọi xuất hiện: `value.toUpperCase()`
- [PartScorerImpl.java:143](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PartScorerImpl.java#L143) `private static double clamp(double value)`
  - Lời gọi xuất hiện: `Math.max()`, `Math.min()`

## `pcbuild/internal/service/impl/PcCustomServiceImpl.java`

### PcCustomServiceImpl — [PcCustomServiceImpl.java:44](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L44)

Khai báo: `@Service @RequiredArgsConstructor public class PcCustomServiceImpl implements PcCustomService`

- Trường: `private static final BigDecimal BUDGET_WINDOW_MIN = new BigDecimal("0.90");`
- Trường: `private static final int DEFAULT_REPLACEMENT_LIMIT = 30;`
- Trường: `private final PcCustomRepository pcCustomRepository;`
- Trường: `private final CatalogPublicApi catalogPublicApi;`
- Trường: `private final CandidateRepositoryFacade candidateRepositoryFacade;`
- Trường: `private final CompatibilityEngine compatibilityEngine;`
- Trường: `private final ProfileValidator profileValidator;`
- [PcCustomServiceImpl.java:58](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L58) `private ValidatedProfile validatedProfile(RequirementProfileDto profile, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `profileValidator.validate()`, `snapshot.dictionary()`
- [PcCustomServiceImpl.java:63](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L63) `@Override @Transactional public Integer saveNewBuild(SavePcBuildRequest request, Integer userId)`
  - Lời gọi xuất hiện: `request.getItems()`, `List.of()`, `items.isEmpty()`, `Collections.emptyMap()`, `catalogPublicApi.getCandidatesByIds(items.stream() .map(SavePcBuildRequest.SavePartItem::getVariantId).distinct().toList()) .stream().collect()`, `catalogPublicApi.getCandidatesByIds(items.stream() .map(SavePcBuildRequest.SavePartItem::getVariantId).distinct().toList()).stream()`, `catalogPublicApi.getCandidatesByIds()`, `items.stream() .map(SavePcBuildRequest.SavePartItem::getVariantId).distinct().toList()`, `items.stream() .map(SavePcBuildRequest.SavePartItem::getVariantId).distinct()`, `items.stream().map()`, `items.stream()`, `Collectors.toMap()`, `Function.identity()`, `catalog.get()`, `itemDto.getVariantId()`, `candidate.getPrice()`, `isPeripheral()`, `itemDto.getRole()`, `candidate.getCategorySlug()`, `normalizeRole(itemDto.getRole()).equals()`, `normalizeRole()`, `Map.of()`, `lines.add()`, `itemDto.getQuantity()`, `compatibilityEngine.evaluate()`, `candidateRepositoryFacade.snapshot()`, `report.compatible()`, `report.checks()`, `custom.setUserId()`, `item.setPcCustom()`, `item.setProductVariantId()`, `item.setComponentRole()`, `item.setQuantity()`, `item.setUnitPrice()`, `custom.getItems().add()`, `custom.getItems()`, `total.add()`, `candidate.getPrice().multiply()`, `BigDecimal.valueOf()`, `custom.setTotalPrice()`, `pcCustomRepository.save()`, `saved.getId()`
- [PcCustomServiceImpl.java:123](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L123) `@Override @Transactional(readOnly = true) public List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId)`
  - Lời gọi xuất hiện: `Collections.emptyList()`, `pcCustomRepository.findByUserIdOrderByIdDesc()`, `customs.isEmpty()`, `customs.stream() .filter(c -> c.getItems() != null) .flatMap(c -> c.getItems().stream()) .map(PcCustomItem::getProductVariantId) .filter(Objects::nonNull) .distinct().toList()`, `customs.stream() .filter(c -> c.getItems() != null) .flatMap(c -> c.getItems().stream()) .map(PcCustomItem::getProductVariantId) .filter(Objects::nonNull).distinct()`, `customs.stream() .filter(c -> c.getItems() != null) .flatMap(c -> c.getItems().stream()) .map(PcCustomItem::getProductVariantId).filter()`, `customs.stream() .filter(c -> c.getItems() != null) .flatMap(c -> c.getItems().stream()).map()`, `customs.stream() .filter(c -> c.getItems() != null).flatMap()`, `customs.stream().filter()`, `customs.stream()`, `c.getItems()`, `c.getItems().stream()`, `allVariantIds.isEmpty()`, `Collections.emptyMap()`, `catalogPublicApi.getCandidateMapByIds()`, `customs.stream().map(custom -> { String cpuName = null; String gpuName = null; String mainboardName = null; String ramSummary = null; if (custom.getItems() != null) { for (PcCustomItem item : custom.getItems()) { String role = item.getComponentRole() != null ? item.getComponentRole().trim().toLowerCase() : ""; ComponentCandidateDTO candidate = candidateMap.get(item.getProductVariantId()); String pName = candidate != null ? candidate.getProductName() : ("Linh kiện #" + item.getProductVariantId()); switch (role) { case "cpu" -> cpuName = pName; case "gpu", "vga" -> gpuName = pName; case "mainboard", "motherboard" -> mainboardName = pName; case "ram" -> { int qty = item.getQuantity() != null ? item.getQuantity() : 1; ramSummary = pName + (qty > 1 ? " (x" + qty + ")" : ""); } default -> {} } } } String buildName = generateBuildName(custom.getId(), cpuName, gpuName); int itemCount = custom.getItems() != null ? custom.getItems().size() : 0; return SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount) .cpuName(cpuName) .gpuName(gpuName) .mainboardName(mainboardName) .ramSummary(ramSummary) .build(); }).toList()`, `customs.stream().map()`, `custom.getItems()`, `item.getComponentRole()`, `item.getComponentRole().trim().toLowerCase()`, `item.getComponentRole().trim()`, `candidateMap.get()`, `item.getProductVariantId()`, `candidate.getProductName()`, `item.getQuantity()`, `generateBuildName()`, `custom.getId()`, `custom.getItems().size()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount) .cpuName(cpuName) .gpuName(gpuName) .mainboardName(mainboardName) .ramSummary(ramSummary).build()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount) .cpuName(cpuName) .gpuName(gpuName) .mainboardName(mainboardName).ramSummary()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount) .cpuName(cpuName) .gpuName(gpuName).mainboardName()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount) .cpuName(cpuName).gpuName()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .itemCount(itemCount).cpuName()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null).itemCount()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()).isOrdered()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()).orderId()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()) .buildName(buildName).totalPrice()`, `SavedPcBuildSummaryResponse.builder() .buildId(custom.getId()).buildName()`, `SavedPcBuildSummaryResponse.builder().buildId()`, `SavedPcBuildSummaryResponse.builder()`, `custom.getTotalPrice()`, `custom.getOrderId()`
- [PcCustomServiceImpl.java:190](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L190) `@Override @Transactional(readOnly = true) public SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId)`
  - Lời gọi xuất hiện: `pcCustomRepository.findById(buildId).orElseThrow()`, `pcCustomRepository.findById()`, `Objects.equals()`, `custom.getUserId()`, `custom.getItems()`, `custom.getItems().stream().map(PcCustomItem::getProductVariantId).filter(Objects::nonNull).distinct().toList()`, `custom.getItems().stream().map(PcCustomItem::getProductVariantId).filter(Objects::nonNull).distinct()`, `custom.getItems().stream().map(PcCustomItem::getProductVariantId).filter()`, `custom.getItems().stream().map()`, `custom.getItems().stream()`, `Collections.emptyList()`, `variantIds.isEmpty()`, `Collections.emptyMap()`, `catalogPublicApi.getCandidateMapByIds()`, `item.getComponentRole()`, `item.getComponentRole().trim().toLowerCase()`, `item.getComponentRole().trim()`, `candidateMap.get()`, `item.getProductVariantId()`, `candidate.getProductName()`, `candidate.getVariantName()`, `candidate.getImageUrl()`, `candidate.getSpecs()`, `item.getUnitPrice()`, `candidate.getPrice()`, `item.getQuantity()`, `unitPrice.multiply()`, `BigDecimal.valueOf()`, `"cpu".equals()`, `"gpu".equals()`, `"vga".equals()`, `items.add()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl) .unitPrice(unitPrice) .quantity(qty) .subtotal(subtotal) .specs(specs) .aiReason(null).build()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl) .unitPrice(unitPrice) .quantity(qty) .subtotal(subtotal) .specs(specs).aiReason()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl) .unitPrice(unitPrice) .quantity(qty) .subtotal(subtotal).specs()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl) .unitPrice(unitPrice) .quantity(qty).subtotal()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl) .unitPrice(unitPrice).quantity()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName) .imageUrl(imageUrl).unitPrice()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName) .variantName(variantName).imageUrl()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()) .productName(productName).variantName()`, `SelectedPartResponse.builder() .role(item.getComponentRole()) .variantId(item.getProductVariantId()).productName()`, `SelectedPartResponse.builder() .role(item.getComponentRole()).variantId()`, `SelectedPartResponse.builder().role()`, `SelectedPartResponse.builder()`, `generateBuildName()`, `custom.getId()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null) .items(items).build()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()) .isOrdered(custom.getOrderId() != null).items()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()) .orderId(custom.getOrderId()).isOrdered()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()) .buildName(buildName) .totalPrice(custom.getTotalPrice()).orderId()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()) .buildName(buildName).totalPrice()`, `SavedPcBuildDetailResponse.builder() .buildId(custom.getId()).buildName()`, `SavedPcBuildDetailResponse.builder().buildId()`, `SavedPcBuildDetailResponse.builder()`, `custom.getTotalPrice()`, `custom.getOrderId()`
- [PcCustomServiceImpl.java:257](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L257) `@Override @Transactional public void deleteSavedBuild(Integer buildId, Integer userId)`
  - Lời gọi xuất hiện: `pcCustomRepository.findById(buildId).orElseThrow()`, `pcCustomRepository.findById()`, `Objects.equals()`, `custom.getUserId()`, `custom.getOrderId()`, `pcCustomRepository.delete()`
- [PcCustomServiceImpl.java:274](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L274) `private String generateBuildName(Integer buildId, String cpuName, String gpuName)`
  - Lời gọi xuất hiện: `cpuName.isBlank()`, `gpuName.isBlank()`
- [PcCustomServiceImpl.java:286](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L286) `@Override public ValidateBuildResponse validateBuild(ValidateBuildRequest request)`
  - Lời gọi xuất hiện: `candidateRepositoryFacade.snapshot()`, `resolveBuild()`, `request.getItems()`, `compatibilityEngine.evaluate(build.lines(), new EvaluationContext(request.getBudget(), validatedProfile(request.getProfile(), snapshot), EvaluationContext.Mode.USER, snapshot)).withExtraChecks()`, `compatibilityEngine.evaluate()`, `build.lines()`, `request.getBudget()`, `validatedProfile()`, `request.getProfile()`, `build.stockChecks()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()) .budgetFit(buildBudgetFit(report.total(), request.getBudget())) .checks(report.checks()) .requiredPsuW(report.requiredPsuW()) .capacity(report.capacity()) .items(build.lines().stream().filter(Objects::nonNull).map(PcCustomServiceImpl::toSelectedPart).toList()).build()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()) .budgetFit(buildBudgetFit(report.total(), request.getBudget())) .checks(report.checks()) .requiredPsuW(report.requiredPsuW()) .capacity(report.capacity()).items()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()) .budgetFit(buildBudgetFit(report.total(), request.getBudget())) .checks(report.checks()) .requiredPsuW(report.requiredPsuW()).capacity()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()) .budgetFit(buildBudgetFit(report.total(), request.getBudget())) .checks(report.checks()).requiredPsuW()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()) .budgetFit(buildBudgetFit(report.total(), request.getBudget())).checks()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()) .totalPrice(report.total()).budgetFit()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()) .hasUnknown(report.hasUnknown()).totalPrice()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()) .hasWarnings(report.hasWarnings()).hasUnknown()`, `ValidateBuildResponse.builder() .isCompatible(report.compatible()).hasWarnings()`, `ValidateBuildResponse.builder().isCompatible()`, `ValidateBuildResponse.builder()`, `report.compatible()`, `report.hasWarnings()`, `report.hasUnknown()`, `report.total()`, `buildBudgetFit()`, `report.checks()`, `report.requiredPsuW()`, `report.capacity()`, `build.lines().stream().filter(Objects::nonNull).map(PcCustomServiceImpl::toSelectedPart).toList()`, `build.lines().stream().filter(Objects::nonNull).map()`, `build.lines().stream().filter()`, `build.lines().stream()`
- [PcCustomServiceImpl.java:308](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L308) `@Override public List<CompatibleReplacementResponse> getCompatibleReplacements(ReplacementQueryRequest request)`
  - Lời gọi xuất hiện: `normalizeRole()`, `request.getRole()`, `candidateRepositoryFacade.snapshot()`, `request.getItems()`, `List.of()`, `resolveBuild(items, snapshot).lines()`, `resolveBuild()`, `findReplacementIndex()`, `request.getLineIndex()`, `items.get(targetIndex).getQuantity()`, `items.get()`, `totalOf()`, `request.getBudget()`, `validatedProfile()`, `request.getProfile()`, `preExistingIssues()`, `Comparator .comparing((CompatibleReplacementResponse response) -> !response.isCompatible()).thenComparing()`, `Comparator.comparing()`, `response.isCompatible()`, `response.getWarningChecks().isEmpty()`, `response.getWarningChecks()`, `order.thenComparing()`, `response.getNewTotal().subtract(budget).abs()`, `response.getNewTotal().subtract()`, `response.getNewTotal()`, `Comparator.nullsLast()`, `Comparator.naturalOrder()`, `request.getLimit()`, `snapshot.candidates().stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())) .map(candidate -> { List<BuildLine> lines = new ArrayList<>(currentLines); BuildLine replacement = new BuildLine(role, candidate, quantity); if (targetIndex >= 0) { lines.set(targetIndex, replacement); } else { lines.add(replacement); } return toReplacement(candidate, compatibilityEngine.evaluate(lines, ctx), preExisting, currentTotal, budget); }) .sorted(order) .limit(limit).toList()`, `snapshot.candidates().stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())) .map(candidate -> { List<BuildLine> lines = new ArrayList<>(currentLines); BuildLine replacement = new BuildLine(role, candidate, quantity); if (targetIndex >= 0) { lines.set(targetIndex, replacement); } else { lines.add(replacement); } return toReplacement(candidate, compatibilityEngine.evaluate(lines, ctx), preExisting, currentTotal, budget); }) .sorted(order).limit()`, `snapshot.candidates().stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())) .map(candidate -> { List<BuildLine> lines = new ArrayList<>(currentLines); BuildLine replacement = new BuildLine(role, candidate, quantity); if (targetIndex >= 0) { lines.set(targetIndex, replacement); } else { lines.add(replacement); } return toReplacement(candidate, compatibilityEngine.evaluate(lines, ctx), preExisting, currentTotal, budget); }).sorted()`, `snapshot.candidates().stream() .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug())).map()`, `snapshot.candidates().stream().filter()`, `snapshot.candidates().stream()`, `snapshot.candidates()`, `role.equalsIgnoreCase()`, `candidate.getCategorySlug()`, `lines.set()`, `lines.add()`, `toReplacement()`, `compatibilityEngine.evaluate()`
- [PcCustomServiceImpl.java:354](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L354) `private ResolvedBuild resolveBuild(List<ValidateBuildRequest.ItemPart> items, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `items.stream() .map(ValidateBuildRequest.ItemPart::getVariantId) .filter(id -> snapshot.candidate(id).isEmpty()) .distinct().toList()`, `items.stream() .map(ValidateBuildRequest.ItemPart::getVariantId) .filter(id -> snapshot.candidate(id).isEmpty()).distinct()`, `items.stream() .map(ValidateBuildRequest.ItemPart::getVariantId).filter()`, `items.stream().map()`, `items.stream()`, `snapshot.candidate(id).isEmpty()`, `snapshot.candidate()`, `outOfStockIds.isEmpty()`, `Collections.emptyMap()`, `catalogPublicApi.getCandidatesByIds(outOfStockIds).stream().collect()`, `catalogPublicApi.getCandidatesByIds(outOfStockIds).stream()`, `catalogPublicApi.getCandidatesByIds()`, `Collectors.toMap()`, `Function.identity()`, `item.getQuantity()`, `item.getVariantId()`, `inStock.orElse()`, `outOfSnapshot.get()`, `lines.add()`, `stockChecks.add()`, `stockFail()`, `item.getRole()`, `inStock.isEmpty()`, `part.getProductName()`, `part.getStockQuantity()`
- [PcCustomServiceImpl.java:387](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L387) `private static CompatibilityCheckResponse stockFail(String detail)`
  - Lời gọi xuất hiện: `CompatibilityCheckResponse.of()`
- [PcCustomServiceImpl.java:391](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L391) `private static int findReplacementIndex(List<ValidateBuildRequest.ItemPart> items, String role, Integer lineIndex)`
  - Lời gọi xuất hiện: `items.size()`, `role.equals()`, `normalizeRole()`, `items.get(lineIndex).getRole()`, `items.get()`, `items.get(index).getRole()`
- [PcCustomServiceImpl.java:410](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L410) `private Set<String> preExistingIssues(List<BuildLine> currentLines, int targetIndex, EvaluationContext ctx)`
  - Lời gọi xuất hiện: `withoutTarget.set()`, `compatibilityEngine.evaluate(withoutTarget, ctx).checks().stream() .filter(check -> !"pass".equals(check.getStatus())) .map(PcCustomServiceImpl::issueSignature).collect()`, `compatibilityEngine.evaluate(withoutTarget, ctx).checks().stream() .filter(check -> !"pass".equals(check.getStatus())).map()`, `compatibilityEngine.evaluate(withoutTarget, ctx).checks().stream().filter()`, `compatibilityEngine.evaluate(withoutTarget, ctx).checks().stream()`, `compatibilityEngine.evaluate(withoutTarget, ctx).checks()`, `compatibilityEngine.evaluate()`, `"pass".equals()`, `check.getStatus()`, `Collectors.toSet()`
- [PcCustomServiceImpl.java:421](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L421) `private static String issueSignature(CompatibilityCheckResponse check)`
  - Lời gọi xuất hiện: `check.getKey()`, `check.getStatus()`, `check.getDetail()`
- [PcCustomServiceImpl.java:425](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L425) `private static CompatibleReplacementResponse toReplacement(ComponentCandidateDTO candidate, EngineReport report, Set<String> preExisting, BigDecimal currentTotal, BigDecimal budget)`
  - Lời gọi xuất hiện: `report.checks().stream() .filter(check -> !"CORE".equals(check.getKey())) .filter(check -> "pass".equals(check.getStatus()) || !preExisting.contains(issueSignature(check))).toList()`, `report.checks().stream() .filter(check -> !"CORE".equals(check.getKey())).filter()`, `report.checks().stream().filter()`, `report.checks().stream()`, `report.checks()`, `"CORE".equals()`, `check.getKey()`, `"pass".equals()`, `check.getStatus()`, `preExisting.contains()`, `issueSignature()`, `keysWithStatus()`, `checks.stream().anyMatch()`, `checks.stream()`, `"unknown".equals()`, `failed.isEmpty()`, `warnings.isEmpty()`, `checks.stream() .filter(check -> "fail".equals(check.getStatus())) .map(CompatibilityCheckResponse::getDetail) .findFirst().orElse()`, `checks.stream() .filter(check -> "fail".equals(check.getStatus())) .map(CompatibilityCheckResponse::getDetail).findFirst()`, `checks.stream() .filter(check -> "fail".equals(check.getStatus())).map()`, `checks.stream().filter()`, `"fail".equals()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status) .failedChecks(failed) .warningChecks(warnings) .priceDelta(report.total().subtract(currentTotal)) .newTotal(report.total()) .newPercent(budget != null ? report.total().multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP) : null).build()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status) .failedChecks(failed) .warningChecks(warnings) .priceDelta(report.total().subtract(currentTotal)) .newTotal(report.total()).newPercent()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status) .failedChecks(failed) .warningChecks(warnings) .priceDelta(report.total().subtract(currentTotal)).newTotal()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status) .failedChecks(failed) .warningChecks(warnings).priceDelta()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status) .failedChecks(failed).warningChecks()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note) .status(status).failedChecks()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()) .compatibilityNote(note).status()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()) .isCompatible(failed.isEmpty()).compatibilityNote()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()) .specs(candidate.getSpecs()).isCompatible()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()) .stockQuantity(candidate.getStockQuantity()).specs()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()) .imageUrl(candidate.getImageUrl()).stockQuantity()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()) .price(candidate.getPrice()).imageUrl()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()) .variantName(candidate.getVariantName()).price()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()) .productName(candidate.getProductName()).variantName()`, `CompatibleReplacementResponse.builder() .variantId(candidate.getVariantId()).productName()`, `CompatibleReplacementResponse.builder().variantId()`, `CompatibleReplacementResponse.builder()`, `candidate.getVariantId()`, `candidate.getProductName()`, `candidate.getVariantName()`, `candidate.getPrice()`, `candidate.getImageUrl()`, `candidate.getStockQuantity()`, `candidate.getSpecs()`, `report.total().subtract()`, `report.total()`, `report.total().multiply(BigDecimal.valueOf(100)).divide()`, `report.total().multiply()`, `BigDecimal.valueOf()`
- [PcCustomServiceImpl.java:464](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L464) `private static List<String> keysWithStatus(List<CompatibilityCheckResponse> checks, String status)`
  - Lời gọi xuất hiện: `checks.stream() .filter(check -> status.equals(check.getStatus())) .map(CompatibilityCheckResponse::getKey) .distinct().toList()`, `checks.stream() .filter(check -> status.equals(check.getStatus())) .map(CompatibilityCheckResponse::getKey).distinct()`, `checks.stream() .filter(check -> status.equals(check.getStatus())).map()`, `checks.stream().filter()`, `checks.stream()`, `status.equals()`, `check.getStatus()`
- [PcCustomServiceImpl.java:472](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L472) `private static BudgetFitResponse buildBudgetFit(BigDecimal total, BigDecimal budget)`
  - Lời gọi xuất hiện: `budget.signum()`, `total.divide()`, `budget.multiply(BUDGET_WINDOW_MIN).setScale()`, `budget.multiply()`, `total.compareTo()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin) .windowMax(budget) .leftover(budget.subtract(total)).build()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin) .windowMax(budget).leftover()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status) .windowMin(windowMin).windowMax()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)) .status(status).windowMin()`, `BudgetFitResponse.builder() .ratio(ratio) .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)).status()`, `BudgetFitResponse.builder() .ratio(ratio).percent()`, `BudgetFitResponse.builder().ratio()`, `BudgetFitResponse.builder()`, `ratio.multiply(BigDecimal.valueOf(100)).setScale()`, `ratio.multiply()`, `BigDecimal.valueOf()`, `budget.subtract()`
- [PcCustomServiceImpl.java:488](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L488) `private static SelectedPartResponse toSelectedPart(BuildLine line)`
  - Lời gọi xuất hiện: `line.part()`, `part.getPrice()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()) .imageUrl(part.getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))) .specs(part.getSpecs()).build()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()) .imageUrl(part.getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()) .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity()))).specs()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()) .imageUrl(part.getImageUrl()) .unitPrice(unitPrice) .quantity(line.quantity()).subtotal()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()) .imageUrl(part.getImageUrl()) .unitPrice(unitPrice).quantity()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()) .imageUrl(part.getImageUrl()).unitPrice()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()) .variantName(part.getVariantName()).imageUrl()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()) .productName(part.getProductName()).variantName()`, `SelectedPartResponse.builder() .role(line.role()) .variantId(part.getVariantId()).productName()`, `SelectedPartResponse.builder() .role(line.role()).variantId()`, `SelectedPartResponse.builder().role()`, `SelectedPartResponse.builder()`, `line.role()`, `part.getVariantId()`, `part.getProductName()`, `part.getVariantName()`, `part.getImageUrl()`, `line.quantity()`, `unitPrice.multiply()`, `BigDecimal.valueOf()`, `part.getSpecs()`
- [PcCustomServiceImpl.java:504](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L504) `private static BigDecimal totalOf(List<BuildLine> lines)`
  - Lời gọi xuất hiện: `lines.stream() .filter(line -> line != null && line.part().getPrice() != null) .map(line -> line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity()))).reduce()`, `lines.stream() .filter(line -> line != null && line.part().getPrice() != null).map()`, `lines.stream().filter()`, `lines.stream()`, `line.part().getPrice()`, `line.part()`, `line.part().getPrice().multiply()`, `BigDecimal.valueOf()`, `line.quantity()`
- [PcCustomServiceImpl.java:511](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L511) `private static String normalizeRole(String role)`
  - Lời gọi xuất hiện: `role.trim().toLowerCase()`, `role.trim()`
- [PcCustomServiceImpl.java:515](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L515) `private static boolean isPeripheral(String role)`
  - Lời gọi xuất hiện: `PeripheralSelector.ROLES.contains()`, `normalizeRole()`

### ResolvedBuild — [PcCustomServiceImpl.java:351](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PcCustomServiceImpl.java#L351)

Khai báo: `private record ResolvedBuild(List<BuildLine> lines, List<CompatibilityCheckResponse> stockChecks)`


## `pcbuild/internal/service/impl/PeripheralServiceImpl.java`

### PeripheralServiceImpl — [PeripheralServiceImpl.java:26](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L26)

Khai báo: `@Service @RequiredArgsConstructor public class PeripheralServiceImpl implements PeripheralService`

- Trường: `private static final Map<String, String> LABELS = Map.of( PeripheralSelector.MONITOR, "Màn hình", PeripheralSelector.KEYBOARD, "Bàn phím", PeripheralSelector.MOUSE, "Chuột");`
- Trường: `private static final String REASON_BUDGET = "Giá cân đối với cấu hình thùng máy";`
- Trường: `private static final int SCREEN_SIZE_INCH = 68;`
- Trường: `private static final int RESOLUTION = 69;`
- Trường: `private static final int PANEL_TYPE = 71;`
- Trường: `private static final int KEYBOARD_SWITCH = 72;`
- Trường: `private static final int KEYBOARD_LAYOUT = 73;`
- Trường: `private static final int CONNECTION_TYPE = 74;`
- Trường: `private static final int MOUSE_DPI = 75;`
- Trường: `private static final int MOUSE_WEIGHT_G = 76;`
- Trường: `private final CandidateRepositoryFacade candidateRepositoryFacade;`
- Trường: `private final CatalogPublicApi catalogPublicApi;`
- [PeripheralServiceImpl.java:48](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L48) `@Override public PeripheralSuggestionResponse suggest(PeripheralQueryRequest request)`
  - Lời gọi xuất hiện: `candidateRepositoryFacade.snapshot()`, `pcTotal()`, `request.getItems()`, `PeripheralSelector.select()`, `snapshot.peripherals()`, `request.getBudget()`, `request.getArchetype()`, `selection.picks().get()`, `selection.picks()`, `roles.add()`, `PeripheralSuggestionResponse.RoleEntry.builder() .role(role) .label(LABELS.get(role)) .picked(toItem(role, picked, snapshot, reasonOf(role, picked, request.getArchetype(), selection))) .options(PeripheralSelector.optionsOf(snapshot.peripherals(), role).stream() .map(option -> toItem(role, option, snapshot, null)).toList()).build()`, `PeripheralSuggestionResponse.RoleEntry.builder() .role(role) .label(LABELS.get(role)) .picked(toItem(role, picked, snapshot, reasonOf(role, picked, request.getArchetype(), selection))).options()`, `PeripheralSuggestionResponse.RoleEntry.builder() .role(role) .label(LABELS.get(role)).picked()`, `PeripheralSuggestionResponse.RoleEntry.builder() .role(role).label()`, `PeripheralSuggestionResponse.RoleEntry.builder().role()`, `PeripheralSuggestionResponse.RoleEntry.builder()`, `LABELS.get()`, `toItem()`, `reasonOf()`, `PeripheralSelector.optionsOf(snapshot.peripherals(), role).stream() .map(option -> toItem(role, option, snapshot, null)).toList()`, `PeripheralSelector.optionsOf(snapshot.peripherals(), role).stream().map()`, `PeripheralSelector.optionsOf(snapshot.peripherals(), role).stream()`, `PeripheralSelector.optionsOf()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal) .available(selection.available()) .spendLimit(selection.spendLimit()) .peripheralTotal(selection.peripheralTotal()) .grandTotal(pcTotal.add(selection.peripheralTotal())) .roles(roles).build()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal) .available(selection.available()) .spendLimit(selection.spendLimit()) .peripheralTotal(selection.peripheralTotal()) .grandTotal(pcTotal.add(selection.peripheralTotal())).roles()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal) .available(selection.available()) .spendLimit(selection.spendLimit()) .peripheralTotal(selection.peripheralTotal()).grandTotal()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal) .available(selection.available()) .spendLimit(selection.spendLimit()).peripheralTotal()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal) .available(selection.available()).spendLimit()`, `PeripheralSuggestionResponse.builder() .pcTotal(pcTotal).available()`, `PeripheralSuggestionResponse.builder().pcTotal()`, `PeripheralSuggestionResponse.builder()`, `selection.available()`, `selection.spendLimit()`, `selection.peripheralTotal()`, `pcTotal.add()`
- [PeripheralServiceImpl.java:77](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L77) `private BigDecimal pcTotal(List<ValidateBuildRequest.ItemPart> items, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `items.stream().map(ValidateBuildRequest.ItemPart::getVariantId) .filter(id -> snapshot.candidate(id).isEmpty()).distinct().toList()`, `items.stream().map(ValidateBuildRequest.ItemPart::getVariantId) .filter(id -> snapshot.candidate(id).isEmpty()).distinct()`, `items.stream().map(ValidateBuildRequest.ItemPart::getVariantId).filter()`, `items.stream().map()`, `items.stream()`, `snapshot.candidate(id).isEmpty()`, `snapshot.candidate()`, `outOfSnapshot.isEmpty()`, `Map.of()`, `catalogPublicApi.getCandidatesByIds(outOfSnapshot).stream().collect()`, `catalogPublicApi.getCandidatesByIds(outOfSnapshot).stream()`, `catalogPublicApi.getCandidatesByIds()`, `Collectors.toMap()`, `Function.identity()`, `snapshot.candidate(item.getVariantId()).orElse()`, `item.getVariantId()`, `fromDb.get()`, `part.getPrice()`, `isBuildPart()`, `total.add()`, `part.getPrice().multiply()`, `BigDecimal.valueOf()`, `item.getQuantity()`
- [PeripheralServiceImpl.java:97](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L97) `private static boolean isBuildPart(ComponentCandidateDTO part)`
  - Lời gọi xuất hiện: `part.getCategorySlug()`, `CandidatePoolBuilder.BUILD_ROLES.contains()`, `part.getCategorySlug().toLowerCase()`
- [PeripheralServiceImpl.java:102](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L102) `private static String reasonOf(String role, ComponentCandidateDTO picked, String archetype, PeripheralSelector.Selection selection)`
  - Lời gọi xuất hiện: `PeripheralSelector.MONITOR.equals()`, `"GAMING".equalsIgnoreCase()`, `selection.gamingFallback()`, `picked.getSpecById()`
- [PeripheralServiceImpl.java:109](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L109) `private static PeripheralSuggestionResponse.Item toItem(String role, ComponentCandidateDTO item, CompatibilitySnapshot snapshot, String reason)`
  - Lời gọi xuất hiện: `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()) .imageUrl(item.getImageUrl()) .price(item.getPrice()) .stockQuantity(item.getStockQuantity()) .highlights(highlights(role, item, snapshot)) .reason(reason).build()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()) .imageUrl(item.getImageUrl()) .price(item.getPrice()) .stockQuantity(item.getStockQuantity()) .highlights(highlights(role, item, snapshot)).reason()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()) .imageUrl(item.getImageUrl()) .price(item.getPrice()) .stockQuantity(item.getStockQuantity()).highlights()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()) .imageUrl(item.getImageUrl()) .price(item.getPrice()).stockQuantity()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()) .imageUrl(item.getImageUrl()).price()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()) .brandName(item.getBrandName()).imageUrl()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()) .variantName(item.getVariantName()).brandName()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()) .productName(item.getProductName()).variantName()`, `PeripheralSuggestionResponse.Item.builder() .variantId(item.getVariantId()).productName()`, `PeripheralSuggestionResponse.Item.builder().variantId()`, `PeripheralSuggestionResponse.Item.builder()`, `item.getVariantId()`, `item.getProductName()`, `item.getVariantName()`, `item.getBrandName()`, `item.getImageUrl()`, `item.getPrice()`, `item.getStockQuantity()`, `highlights()`
- [PeripheralServiceImpl.java:123](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L123) `private static List<String> highlights(String role, ComponentCandidateDTO item, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `addValue()`, `addOption()`
- [PeripheralServiceImpl.java:146](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L146) `private static void addValue(List<String> result, ComponentCandidateDTO item, int specId, String unit)`
  - Lời gọi xuất hiện: `item.getSpecById()`, `result.add()`
- [PeripheralServiceImpl.java:151](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PeripheralServiceImpl.java#L151) `private static void addOption(List<String> result, ComponentCandidateDTO item, int specId, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `snapshot.display()`, `item.getSpecById()`, `result.add()`

## `pcbuild/internal/service/impl/ProfileExtractorImpl.java`

### ProfileExtractorImpl — [ProfileExtractorImpl.java:36](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L36)

Khai báo: `@Slf4j @Service public class ProfileExtractorImpl implements ProfileExtractor`

- Trường: `static final String PROMPT_PROFILE_V1 = """ BẠN LÀ CHUYÊN GIA TƯ VẤN CẤU HÌNH PC CỦA CYBERSHOP. NHIỆM VỤ: Đọc nhu cầu của khách (tiếng Việt hoặc tiếng Anh) và chuyển thành HỒ SƠ YÊU CẦU (JSON) để hệ thống chọn linh kiện. QUY TẮC BẮT BUỘC: 1. CHỈ dùng các role, spec, metric và mã giá trị có trong "dictionary". Không tự đặt tên thông số mới. Nhu c...`
- Trường: `private final LlmClientService llmClientService;`
- Trường: `private final ProfilePresets profilePresets;`
- Trường: `private final ObjectMapper objectMapper;`
- Trường: `private final Clock clock;`
- Trường: `private final Duration ttl;`
- Trường: `private final int maxEntries;`
- Trường: `private final Map<String, CachedProfile> cache;`
- Trường: `private final ConcurrentHashMap<String, CompletableFuture<RequirementProfileDto>> inFlight = new ConcurrentHashMap<>();`
- [ProfileExtractorImpl.java:90](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L90) `public ProfileExtractorImpl(LlmClientService llmClientService, ProfilePresets profilePresets, ObjectMapper objectMapper, Clock clock, @Value("${app.pcbuild.profile-cache-hours:24}") long cacheHours, @Value("${app.pcbuild.profile-cache-max-entries:5000}") int maxEntries)`
  - Lời gọi xuất hiện: `Duration.ofHours()`, `size()`
- [ProfileExtractorImpl.java:108](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L108) `@Override public Extraction extract(String purpose, String userPrompt, BigDecimal budget, BuildTier tier, CompatibilitySnapshot snapshot, Instant deadline)`
  - Lời gọi xuất hiện: `cacheKey()`, `snapshot.dictionaryVersion()`, `fromCache()`, `copy()`, `inFlight.putIfAbsent()`, `await()`, `llmClientService.extractProfile()`, `userContent()`, `cache.put()`, `clock.instant()`, `mine.complete()`, `mine.completeExceptionally()`, `inFlight.remove()`
- [ProfileExtractorImpl.java:142](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L142) `private RequirementProfileDto await(CompletableFuture<RequirementProfileDto> future, Instant deadline)`
  - Lời gọi xuất hiện: `Duration.between(clock.instant(), deadline).toMillis()`, `Duration.between()`, `clock.instant()`, `future.get()`, `Math.max()`, `e.getCause()`, `unavailable.detail()`, `unavailable.getRetryAfterSeconds()`, `Thread.currentThread().interrupt()`, `Thread.currentThread()`
- [ProfileExtractorImpl.java:159](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L159) `private RequirementProfileDto fromCache(String key)`
  - Lời gọi xuất hiện: `cache.get()`, `Duration.between(entry.storedAt(), clock.instant()).compareTo()`, `Duration.between()`, `entry.storedAt()`, `clock.instant()`, `cache.remove()`, `entry.profile()`
- [ProfileExtractorImpl.java:172](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L172) `static String cacheKey(String purpose, String userPrompt, BuildTier tier, String dictionaryVersion)`
  - Lời gọi xuất hiện: `TextNormalizer.normalize()`, `HexFormat.of().formatHex()`, `HexFormat.of()`, `MessageDigest.getInstance("SHA-256").digest()`, `MessageDigest.getInstance()`, `raw.getBytes()`
- [ProfileExtractorImpl.java:181](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L181) `private String userContent(String purpose, String userPrompt, BigDecimal budget, BuildTier tier, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `Archetype.values()`, `presets.add()`, `Map.of()`, `archetype.name()`, `profilePresets.preset()`, `content.put()`, `tier.name()`, `purpose.trim()`, `userPrompt.trim()`, `snapshot.dictionary()`, `objectMapper.writeValueAsString()`
- [ProfileExtractorImpl.java:201](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L201) `private RequirementProfileDto copy(RequirementProfileDto profile)`
  - Lời gọi xuất hiện: `objectMapper.convertValue()`

### CachedProfile — [ProfileExtractorImpl.java:87](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileExtractorImpl.java#L87)

Khai báo: `private record CachedProfile(RequirementProfileDto profile, Instant storedAt)`


## `pcbuild/internal/service/impl/ProfilePresetsImpl.java`

### ProfilePresetsImpl — [ProfilePresetsImpl.java:24](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L24)

Khai báo: `@Service public class ProfilePresetsImpl implements ProfilePresets`

- Trường: `static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");`
- Trường: `private static final List<String> MIDDLE_TIERS_AND_UP = List.of("T3", "T4", "T5", "T6", "T7");`
- Trường: `private static final Map<Archetype, double[]> ROLE_WEIGHTS = new EnumMap<>(Map.of( // cpu mainboard ram gpu ssd psu case cooler Archetype.GAMING, new double[]{0.22, 0.05, 0.10, 0.45, 0.08, 0.04, 0.03, 0.03}, Archetype.CREATIVE, new double[]{0.28, 0.05, 0.22, 0.25, 0.12, 0.03, 0.02, 0.03}, Archetype.AI_DATA, new double[]{0.12, 0.04, 0.18, 0.50, 0...`
- Trường: `private static final Map<Archetype, Map<String, Double>> CAPS = new EnumMap<>(Map.of( Archetype.GAMING, Map.of("Core_Count", 16.0, "TOTAL_RAM_GB", 64.0, "TOTAL_STORAGE_GB", 4000.0, "VRAM_GB", 32.0), Archetype.CREATIVE, Map.of("Core_Count", 128.0, "TOTAL_RAM_GB", 512.0, "GPU_COUNT", 2.0), Archetype.AI_DATA, Map.of("TOTAL_RAM_GB", 2048.0, "GPU_COU...`
- [ProfilePresetsImpl.java:48](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L48) `@Override public Map<String, Double> roleWeights(Archetype archetype)`
  - Lời gọi xuất hiện: `ROLE_WEIGHTS.get()`, `ROLE_ORDER.size()`, `result.put()`, `ROLE_ORDER.get()`
- [ProfilePresetsImpl.java:58](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L58) `@Override public List<ProfilePreferenceDto> defaultPreferences(String role)`
  - Lời gọi xuất hiện: `List.of()`, `spec()`, `prefer()`, `metric()`
- [ProfilePresetsImpl.java:86](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L86) `private List<ProfilePreferenceDto> archetypePreferences(Archetype archetype, String role)`
  - Lời gọi xuất hiện: `"gpu".equals()`, `List.of()`, `spec()`, `metric()`, `defaultPreferences()`
- [ProfilePresetsImpl.java:98](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L98) `@Override public RequirementProfileDto preset(Archetype archetype, BuildTier tier)`
  - Lời gọi xuất hiện: `archetype.name()`, `tier.name()`, `required.add()`, `roleRef()`, `pick()`, `constraints.add()`, `intConstraint()`, `metrics.add()`, `metricConstraint()`, `balancePairs.add()`, `MIDDLE_TIERS_AND_UP.contains()`, `ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op("IN") .values(new ArrayList<>(List.of("CUDA", "ROCM"))).priority(2).reason(reason).build()`, `ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op("IN") .values(new ArrayList<>(List.of("CUDA", "ROCM"))).priority(2).reason()`, `ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op("IN") .values(new ArrayList<>(List.of("CUDA", "ROCM"))).priority()`, `ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op("IN").values()`, `ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op()`, `ProfileConstraintDto.builder().role("gpu").spec()`, `ProfileConstraintDto.builder().role()`, `ProfileConstraintDto.builder()`, `List.of()`, `unmetNeeds.add()`, `excluded.add()`, `roleWeights()`, `weights.get()`, `archetypePreferences(archetype, role).forEach()`, `archetypePreferences()`, `preferences.add()`, `preference.toBuilder().build()`, `preference.toBuilder()`, `applyCaps()`, `numberIds()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs) .brands(new RequirementProfileDto.Brands()) .unmetNeeds(unmetNeeds) .confidence(1.0).clarifyingQuestion(null) .droppedItems(new ArrayList<>()).build()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs) .brands(new RequirementProfileDto.Brands()) .unmetNeeds(unmetNeeds) .confidence(1.0).clarifyingQuestion(null).droppedItems()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs) .brands(new RequirementProfileDto.Brands()) .unmetNeeds(unmetNeeds) .confidence(1.0).clarifyingQuestion()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs) .brands(new RequirementProfileDto.Brands()) .unmetNeeds(unmetNeeds).confidence()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs) .brands(new RequirementProfileDto.Brands()).unmetNeeds()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences) .balancePairs(balancePairs).brands()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences(preferences).balancePairs()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics(metrics).preferences()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded) .constraints(constraints).metrics()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles(excluded).constraints()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights) .requiredRoles(required).excludedRoles()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()) .roleWeights(weights).requiredRoles()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases(new ArrayList<>()).roleWeights()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()) .summary(reason).useCases()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype(archetype.name()).summary()`, `RequirementProfileDto.builder() .profileVersion(1).source("AI").archetype()`, `RequirementProfileDto.builder() .profileVersion(1).source()`, `RequirementProfileDto.builder().profileVersion()`, `RequirementProfileDto.builder()`
- [ProfilePresetsImpl.java:183](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L183) `@Override public RequirementProfileDto fillMissing(RequirementProfileDto profile, BuildTier tier)`
  - Lời gọi xuất hiện: `Archetype.parse()`, `profile.getArchetype()`, `profile.toBuilder() .roleWeights(normalizedWeightKeys(nullSafe(profile.getRoleWeights()))) .preferences(new ArrayList<>(nullSafe(profile.getPreferences()))).build()`, `profile.toBuilder() .roleWeights(normalizedWeightKeys(nullSafe(profile.getRoleWeights()))).preferences()`, `profile.toBuilder().roleWeights()`, `profile.toBuilder()`, `normalizedWeightKeys()`, `nullSafe()`, `profile.getRoleWeights()`, `profile.getPreferences()`, `filled.getRoleWeights().values().stream().noneMatch()`, `filled.getRoleWeights().values().stream()`, `filled.getRoleWeights().values()`, `filled.getRoleWeights()`, `filled.setRoleWeights()`, `roleWeights()`, `fillWeightGaps()`, `filled.getRoleWeights().get()`, `hasPreferenceFor()`, `filled.getPreferences()`, `archetypePreferences(archetype, role).forEach()`, `archetypePreferences()`, `filled.getPreferences().add()`, `preference.toBuilder().build()`, `preference.toBuilder()`, `CAPS.get()`, `preference.targetsMetric()`, `preference.getMetric()`, `preference.getSpec()`, `preference.getCap()`, `caps.containsKey()`, `preference.setCap()`, `caps.get()`
- [ProfilePresetsImpl.java:217](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L217) `private static Map<String, Double> normalizedWeightKeys(Map<String, Double> weights)`
  - Lời gọi xuất hiện: `weights.forEach()`, `key.trim().toLowerCase()`, `key.trim()`, `ROLE_ORDER.contains()`, `normalized.put()`
- [ProfilePresetsImpl.java:235](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L235) `private void fillWeightGaps(RequirementProfileDto filled, Archetype archetype)`
  - Lời gọi xuất hiện: `filled.getRoleWeights()`, `roleWeights()`, `weights.containsKey()`, `weights.put()`, `preset.get()`, `weights.get()`, `isDemanded()`, `general.get()`
- [ProfilePresetsImpl.java:250](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L250) `private static boolean isDemanded(RequirementProfileDto profile, String role)`
  - Lời gọi xuất hiện: `nullSafe(profile.getExcludedRoles()).stream().anyMatch()`, `nullSafe(profile.getExcludedRoles()).stream()`, `nullSafe()`, `profile.getExcludedRoles()`, `role.equalsIgnoreCase()`, `trimmed()`, `ref.getRole()`, `nullSafe(profile.getRequiredRoles()).stream().anyMatch()`, `nullSafe(profile.getRequiredRoles()).stream()`, `profile.getRequiredRoles()`, `nullSafe(profile.getConstraints()).stream().anyMatch()`, `nullSafe(profile.getConstraints()).stream()`, `profile.getConstraints()`, `item.getRole()`, `nullSafe(profile.getMetrics()).stream().anyMatch()`, `nullSafe(profile.getMetrics()).stream()`, `profile.getMetrics()`, `demandsRole()`
- [ProfilePresetsImpl.java:259](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L259) `private static boolean demandsRole(ProfileMetricDto metric, String role)`
  - Lời gọi xuất hiện: `metric.getValue()`, `"GTE".equalsIgnoreCase()`, `trimmed()`, `metric.getOp()`, `"EQ".equalsIgnoreCase()`, `MetricKind.parse(metric.getMetric()).map(kind -> kind.role().equals(role)).orElse()`, `MetricKind.parse(metric.getMetric()).map()`, `MetricKind.parse()`, `metric.getMetric()`, `kind.role().equals()`, `kind.role()`
- [ProfilePresetsImpl.java:265](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L265) `private static String trimmed(String value)`
  - Lời gọi xuất hiện: `value.trim()`
- [ProfilePresetsImpl.java:269](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L269) `private static boolean hasPreferenceFor(List<ProfilePreferenceDto> preferences, String role)`
  - Lời gọi xuất hiện: `preferences.stream().anyMatch()`, `preferences.stream()`, `preference.targetsMetric()`, `MetricKind.parse(preference.getMetric()).map(kind -> kind.role().equals(role)).orElse()`, `MetricKind.parse(preference.getMetric()).map()`, `MetricKind.parse()`, `preference.getMetric()`, `kind.role().equals()`, `kind.role()`, `role.equalsIgnoreCase()`, `preference.getRole()`
- [ProfilePresetsImpl.java:275](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L275) `private static void applyCaps(List<ProfilePreferenceDto> preferences, Archetype archetype)`
  - Lời gọi xuất hiện: `CAPS.get()`, `preference.getMetric()`, `preference.getSpec()`, `caps.containsKey()`, `preference.setCap()`, `caps.get()`
- [ProfilePresetsImpl.java:285](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L285) `private static void numberIds(List<ProfileConstraintDto> constraints, List<ProfileMetricDto> metrics, List<ProfilePreferenceDto> preferences)`
  - Lời gọi xuất hiện: `constraints.size()`, `constraints.get(index).setId()`, `constraints.get()`, `metrics.size()`, `metrics.get(index).setId()`, `metrics.get()`, `preferences.size()`, `preferences.get(index).setId()`, `preferences.get()`
- [ProfilePresetsImpl.java:293](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L293) `private static Integer pick(String tier, Integer... valuesByTier)`
  - Lời gọi xuất hiện: `Integer.parseInt()`, `tier.substring()`
- [ProfilePresetsImpl.java:297](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L297) `private static ProfileRoleRefDto roleRef(String role, String reason)`
  - Lời gọi xuất hiện: `ProfileRoleRefDto.builder().role(role).priority(2).reason(reason).build()`, `ProfileRoleRefDto.builder().role(role).priority(2).reason()`, `ProfileRoleRefDto.builder().role(role).priority()`, `ProfileRoleRefDto.builder().role()`, `ProfileRoleRefDto.builder()`
- [ProfilePresetsImpl.java:301](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L301) `private static ProfileConstraintDto intConstraint(String role, String spec, String op, int value, String reason)`
  - Lời gọi xuất hiện: `ProfileConstraintDto.builder().role(role).spec(spec).op(op).value((double) value) .values(new ArrayList<>()).priority(2).reason(reason).build()`, `ProfileConstraintDto.builder().role(role).spec(spec).op(op).value((double) value) .values(new ArrayList<>()).priority(2).reason()`, `ProfileConstraintDto.builder().role(role).spec(spec).op(op).value((double) value) .values(new ArrayList<>()).priority()`, `ProfileConstraintDto.builder().role(role).spec(spec).op(op).value((double) value).values()`, `ProfileConstraintDto.builder().role(role).spec(spec).op(op).value()`, `ProfileConstraintDto.builder().role(role).spec(spec).op()`, `ProfileConstraintDto.builder().role(role).spec()`, `ProfileConstraintDto.builder().role()`, `ProfileConstraintDto.builder()`
- [ProfilePresetsImpl.java:306](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L306) `private static ProfileMetricDto metricConstraint(String metric, String op, int value, String reason)`
  - Lời gọi xuất hiện: `ProfileMetricDto.builder().metric(metric).op(op).value((double) value).priority(2).reason(reason).build()`, `ProfileMetricDto.builder().metric(metric).op(op).value((double) value).priority(2).reason()`, `ProfileMetricDto.builder().metric(metric).op(op).value((double) value).priority()`, `ProfileMetricDto.builder().metric(metric).op(op).value()`, `ProfileMetricDto.builder().metric(metric).op()`, `ProfileMetricDto.builder().metric()`, `ProfileMetricDto.builder()`
- [ProfilePresetsImpl.java:310](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L310) `private static ProfilePreferenceDto spec(String role, String spec, String direction, double weight)`
  - Lời gọi xuất hiện: `ProfilePreferenceDto.builder().role(role).spec(spec).direction(direction).weight(weight) .values(new ArrayList<>()).build()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction(direction).weight(weight).values()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction(direction).weight()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction()`, `ProfilePreferenceDto.builder().role(role).spec()`, `ProfilePreferenceDto.builder().role()`, `ProfilePreferenceDto.builder()`
- [ProfilePresetsImpl.java:315](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L315) `private static ProfilePreferenceDto prefer(String role, String spec, double weight, String... values)`
  - Lời gọi xuất hiện: `ProfilePreferenceDto.builder().role(role).spec(spec).direction("PREFER").weight(weight) .values(new ArrayList<>(List.of(values))).build()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction("PREFER").weight(weight).values()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction("PREFER").weight()`, `ProfilePreferenceDto.builder().role(role).spec(spec).direction()`, `ProfilePreferenceDto.builder().role(role).spec()`, `ProfilePreferenceDto.builder().role()`, `ProfilePreferenceDto.builder()`, `List.of()`
- [ProfilePresetsImpl.java:320](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L320) `private static ProfilePreferenceDto metric(String metric, double weight, Double cap)`
  - Lời gọi xuất hiện: `ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight(weight).cap(cap) .values(new ArrayList<>()).build()`, `ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight(weight).cap(cap).values()`, `ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight(weight).cap()`, `ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight()`, `ProfilePreferenceDto.builder().metric(metric).direction()`, `ProfilePreferenceDto.builder().metric()`, `ProfilePreferenceDto.builder()`
- [ProfilePresetsImpl.java:325](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L325) `private static <T> T nullSafe(T value, T fallback)`
  - Lời gọi xuất hiện: `Objects.requireNonNullElse()`
- [ProfilePresetsImpl.java:329](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L329) `private static Map<String, Double> nullSafe(Map<String, Double> value)`
  - Lời gọi xuất hiện: `nullSafe()`, `Map.of()`
- [ProfilePresetsImpl.java:333](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfilePresetsImpl.java#L333) `private static <T> List<T> nullSafe(List<T> value)`
  - Lời gọi xuất hiện: `nullSafe()`, `List.of()`

## `pcbuild/internal/service/impl/ProfileValidatorImpl.java`

### ProfileValidatorImpl — [ProfileValidatorImpl.java:35](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L35)

Khai báo: `@Service @RequiredArgsConstructor public class ProfileValidatorImpl implements ProfileValidator`

- Trường: `private static final Set<String> ROLES = Set.copyOf(ProfilePresetsImpl.ROLE_ORDER);`
- Trường: `private static final Set<String> REQUIRABLE_ROLES = Set.of("gpu", "cooler");`
- Trường: `private static final Set<String> EXCLUDABLE_ROLES = Set.of("gpu");`
- Trường: `private static final Set<String> INT_OPS = Set.of("GTE", "LTE", "EQ");`
- Trường: `private static final Set<String> ENUM_OPS = Set.of("IN", "NOT_IN");`
- Trường: `private static final Set<String> LIST_OPS = Set.of("CONTAINS");`
- Trường: `private static final int MAX_CONSTRAINTS = 20;`
- Trường: `private static final int MAX_METRICS = 10;`
- Trường: `private static final int MAX_PREFERENCES = 20;`
- Trường: `private static final int MAX_TEXT = 200;`
- Trường: `private static final int MAX_USE_CASES = 5;`
- Trường: `private static final int MAX_UNMET_NEEDS = 10;`
- Trường: `private final ProfilePresets profilePresets;`
- [ProfileValidatorImpl.java:54](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L54) `@Override public ValidatedProfile validate(RequirementProfileDto input, MetadataDictionaryResponse dictionary, String source, Integer legacyRamGb)`
  - Lời gọi xuất hiện: `Archetype.parse()`, `in.getArchetype()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())) .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES)) .unmetNeeds(truncateAll(in.getUnmetNeeds(), MAX_UNMET_NEEDS)) .objective(ValidatedProfile.MAX_PRICE.equals(in.getObjective()) ? ValidatedProfile.MAX_PRICE : null) .confidence(in.getConfidence() == null ? 1.0 : clamp(in.getConfidence())) .clarifyingQuestion(truncate(in.getClarifyingQuestion())).build()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())) .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES)) .unmetNeeds(truncateAll(in.getUnmetNeeds(), MAX_UNMET_NEEDS)) .objective(ValidatedProfile.MAX_PRICE.equals(in.getObjective()) ? ValidatedProfile.MAX_PRICE : null) .confidence(in.getConfidence() == null ? 1.0 : clamp(in.getConfidence())).clarifyingQuestion()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())) .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES)) .unmetNeeds(truncateAll(in.getUnmetNeeds(), MAX_UNMET_NEEDS)) .objective(ValidatedProfile.MAX_PRICE.equals(in.getObjective()) ? ValidatedProfile.MAX_PRICE : null).confidence()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())) .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES)) .unmetNeeds(truncateAll(in.getUnmetNeeds(), MAX_UNMET_NEEDS)).objective()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())) .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES)).unmetNeeds()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()) .summary(truncate(in.getSummary())).useCases()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source) .archetype(archetype.name()).summary()`, `RequirementProfileDto.builder() .profileVersion(1) .source(source).archetype()`, `RequirementProfileDto.builder() .profileVersion(1).source()`, `RequirementProfileDto.builder().profileVersion()`, `RequirementProfileDto.builder()`, `archetype.name()`, `truncate()`, `in.getSummary()`, `truncateAll()`, `in.getUseCases()`, `in.getUnmetNeeds()`, `ValidatedProfile.MAX_PRICE.equals()`, `in.getObjective()`, `in.getConfidence()`, `clamp()`, `in.getClarifyingQuestion()`, `out.setRoleWeights()`, `run.roleWeights()`, `in.getRoleWeights()`, `out.setRequiredRoles()`, `run.roleRefs()`, `in.getRequiredRoles()`, `out.setExcludedRoles()`, `in.getExcludedRoles()`, `out.setConstraints()`, `run.constraints()`, `nullSafe()`, `in.getConstraints()`, `out.setMetrics()`, `run.metrics()`, `in.getMetrics()`, `out.setPreferences()`, `run.preferences()`, `in.getPreferences()`, `out.setBalancePairs()`, `run.balancePairs()`, `in.getBalancePairs()`, `out.setBrands()`, `run.brands()`, `in.getBrands()`, `out.getBrands().getPrefer().removeIf()`, `out.getBrands().getPrefer()`, `out.getBrands()`, `out.getConstraints().stream().anyMatch()`, `out.getConstraints().stream()`, `out.getConstraints()`, `ProfileMatcher.BRAND_SPEC.equals()`, `item.getSpec()`, `"IN".equals()`, `item.getOp()`, `item.getValues().contains()`, `item.getValues()`, `run.missingBrands.isEmpty()`, `run.missingBrands.addAll()`, `out.getUnmetNeeds()`, `out.setUnmetNeeds()`, `run.resolveRoleConflicts()`, `run.implyRequiredRoles()`, `out.setDroppedItems()`
- [ProfileValidatorImpl.java:554](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L554) `private static List<String> assignIds(List<String> requested, String prefix)`
  - Lời gọi xuất hiện: `assignIds()`, `requested.size()`
- [ProfileValidatorImpl.java:558](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L558) `private static List<String> assignIds(List<String> requested, String prefix, int size)`
  - Lời gọi xuất hiện: `id.isBlank()`, `used.add()`, `id.trim()`, `result.add()`, `result.size()`, `result.get()`, `used.contains()`, `result.set()`
- [ProfileValidatorImpl.java:580](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L580) `private static Optional<Map.Entry<String, MetadataDictionaryResponse.SpecEntry>> findSpec( MetadataDictionaryResponse.RoleEntry role, String specName)`
  - Lời gọi xuất hiện: `Optional.empty()`, `role.getSpecs().entrySet().stream() .filter(entry -> entry.getKey().equalsIgnoreCase(specName.trim())).findFirst()`, `role.getSpecs().entrySet().stream().filter()`, `role.getSpecs().entrySet().stream()`, `role.getSpecs().entrySet()`, `role.getSpecs()`, `entry.getKey().equalsIgnoreCase()`, `entry.getKey()`, `specName.trim()`
- [ProfileValidatorImpl.java:588](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L588) `private static Set<String> opsFor(SpecType type)`
  - Lời gọi xuất hiện: `Set.of()`
- [ProfileValidatorImpl.java:597](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L597) `static double[] interval(String op, Double value)`
- [ProfileValidatorImpl.java:607](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L607) `private static boolean isOutOfRange(String op, double value, Integer min, Integer max)`
- [ProfileValidatorImpl.java:616](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L616) `private static Double capOf(Double cap, Integer max)`
  - Lời gọi xuất hiện: `cap.isNaN()`, `max.doubleValue()`
- [ProfileValidatorImpl.java:621](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L621) `private static String label(String specName, String op, double value, String unit)`
  - Lời gọi xuất hiện: `readable()`, `symbol()`, `formatNumber()`
- [ProfileValidatorImpl.java:626](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L626) `private static String readable(String specName)`
  - Lời gọi xuất hiện: `ProfileMatcher.BRAND_SPEC.equals()`, `specName.replaceAll()`, `name.replace()`
- [ProfileValidatorImpl.java:632](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L632) `private static String symbol(String op)`
- [ProfileValidatorImpl.java:640](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L640) `private static String formatNumber(double value)`
  - Lời gọi xuất hiện: `Math.rint()`, `String.valueOf()`
- [ProfileValidatorImpl.java:644](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L644) `private static boolean isInteger(Double value)`
  - Lời gọi xuất hiện: `value.isNaN()`, `value.isInfinite()`, `Math.rint()`
- [ProfileValidatorImpl.java:648](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L648) `private static int priority(Integer priority)`
  - Lời gọi xuất hiện: `Math.max()`, `Math.min()`
- [ProfileValidatorImpl.java:652](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L652) `private static double clamp(double value)`
  - Lời gọi xuất hiện: `Math.max()`, `Math.min()`
- [ProfileValidatorImpl.java:656](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L656) `private static String normalizeRole(String role)`
  - Lời gọi xuất hiện: `role.trim().toLowerCase()`, `role.trim()`
- [ProfileValidatorImpl.java:660](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L660) `private static String upper(String value)`
  - Lời gọi xuất hiện: `value.trim().toUpperCase()`, `value.trim()`
- [ProfileValidatorImpl.java:664](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L664) `private static String truncate(String value)`
  - Lời gọi xuất hiện: `value.trim()`, `trimmed.length()`, `trimmed.substring()`
- [ProfileValidatorImpl.java:670](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L670) `private static List<String> truncateAll(List<String> values, int maxItems)`
  - Lời gọi xuất hiện: `nullSafe(values).stream().filter(Objects::nonNull).map(ProfileValidatorImpl::truncate) .filter(value -> !value.isEmpty()).limit(maxItems).collect()`, `nullSafe(values).stream().filter(Objects::nonNull).map(ProfileValidatorImpl::truncate) .filter(value -> !value.isEmpty()).limit()`, `nullSafe(values).stream().filter(Objects::nonNull).map(ProfileValidatorImpl::truncate).filter()`, `nullSafe(values).stream().filter(Objects::nonNull).map()`, `nullSafe(values).stream().filter()`, `nullSafe(values).stream()`, `nullSafe()`, `value.isEmpty()`, `Collectors.toCollection()`
- [ProfileValidatorImpl.java:675](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L675) `private static Double findIgnoreCase(Map<String, Double> map, String key)`
  - Lời gọi xuất hiện: `map.entrySet().stream() .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key)) .map(Map.Entry::getValue) .filter(Objects::nonNull) .findFirst().orElse()`, `map.entrySet().stream() .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key)) .map(Map.Entry::getValue) .filter(Objects::nonNull).findFirst()`, `map.entrySet().stream() .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key)) .map(Map.Entry::getValue).filter()`, `map.entrySet().stream() .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key)).map()`, `map.entrySet().stream().filter()`, `map.entrySet().stream()`, `map.entrySet()`, `entry.getKey()`, `entry.getKey().trim().equalsIgnoreCase()`, `entry.getKey().trim()`
- [ProfileValidatorImpl.java:683](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L683) `private static <T> List<T> nullSafe(List<T> list)`
  - Lời gọi xuất hiện: `List.of()`

### Run — [ProfileValidatorImpl.java:95](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L95)

Khai báo: `private final class Run`

- Trường: `private final MetadataDictionaryResponse dictionary;`
- Trường: `private final String source;`
- Trường: `private final List<DroppedItemDto> dropped = new ArrayList<>();`
- Trường: `private final List<String> warnings = new ArrayList<>();`
- Trường: `private final List<String> softBrands = new ArrayList<>();`
- Trường: `private final List<String> missingBrands = new ArrayList<>();`
- [ProfileValidatorImpl.java:103](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L103) `private Run(MetadataDictionaryResponse dictionary, String source)`
- [ProfileValidatorImpl.java:108](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L108) `private void drop(String id, String reason, String detail)`
  - Lời gọi xuất hiện: `dropped.add()`, `DroppedItemDto.builder().id(id).reason(reason).detail(truncate(detail)).build()`, `DroppedItemDto.builder().id(id).reason(reason).detail()`, `DroppedItemDto.builder().id(id).reason()`, `DroppedItemDto.builder().id()`, `DroppedItemDto.builder()`, `truncate()`
- [ProfileValidatorImpl.java:113](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L113) `private Map<String, Double> roleWeights(Map<String, Double> weights, Archetype archetype)`
  - Lời gọi xuất hiện: `Map.of()`, `findIgnoreCase()`, `clamped.put()`, `value.isNaN()`, `clamp()`, `given.keySet().stream() .filter(key -> key == null || !ROLES.contains(key.trim().toLowerCase(Locale.ROOT))).forEach()`, `given.keySet().stream().filter()`, `given.keySet().stream()`, `given.keySet()`, `ROLES.contains()`, `key.trim().toLowerCase()`, `key.trim()`, `drop()`, `clamped.values().stream().mapToDouble(Double::doubleValue).sum()`, `clamped.values().stream().mapToDouble()`, `clamped.values().stream()`, `clamped.values()`, `profilePresets.roleWeights()`, `clamped.forEach()`, `normalized.put()`
- [ProfileValidatorImpl.java:132](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L132) `private List<ProfileRoleRefDto> roleRefs(List<ProfileRoleRefDto> refs, Set<String> allowed, String kind)`
  - Lời gọi xuất hiện: `nullSafe()`, `normalizeRole()`, `ref.getRole()`, `ROLES.contains()`, `allowed.contains()`, `drop()`, `seen.add()`, `result.add()`, `ProfileRoleRefDto.builder().role(role).priority(priority(ref.getPriority())) .reason(truncate(ref.getReason())).build()`, `ProfileRoleRefDto.builder().role(role).priority(priority(ref.getPriority())).reason()`, `ProfileRoleRefDto.builder().role(role).priority()`, `ProfileRoleRefDto.builder().role()`, `ProfileRoleRefDto.builder()`, `priority()`, `ref.getPriority()`, `truncate()`, `ref.getReason()`
- [ProfileValidatorImpl.java:147](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L147) `private List<ProfileConstraintDto> constraints(List<ProfileConstraintDto> items)`
  - Lời gọi xuất hiện: `assignIds()`, `items.stream().map(ProfileConstraintDto::getId).toList()`, `items.stream().map()`, `items.stream()`, `items.size()`, `items.get()`, `ids.get()`, `normalizeRole()`, `item.getRole()`, `dictionary.getRoles().get()`, `dictionary.getRoles()`, `drop()`, `findSpec()`, `item.getSpec()`, `spec.isEmpty()`, `spec.get().getKey()`, `spec.get()`, `spec.get().getValue()`, `SpecType.valueOf()`, `entry.getType()`, `normalizeOp()`, `upper()`, `item.getOp()`, `item.getValues()`, `opsFor(type).contains()`, `opsFor()`, `ProfileConstraintDto.builder() .id(id).role(role).spec(specName).op(op).priority(priority(item.getPriority())) .reason(truncate(item.getReason())).values()`, `ProfileConstraintDto.builder() .id(id).role(role).spec(specName).op(op).priority(priority(item.getPriority())).reason()`, `ProfileConstraintDto.builder() .id(id).role(role).spec(specName).op(op).priority()`, `ProfileConstraintDto.builder() .id(id).role(role).spec(specName).op()`, `ProfileConstraintDto.builder() .id(id).role(role).spec()`, `ProfileConstraintDto.builder() .id(id).role()`, `ProfileConstraintDto.builder().id()`, `ProfileConstraintDto.builder()`, `priority()`, `item.getPriority()`, `truncate()`, `item.getReason()`, `isInteger()`, `item.getValue()`, `isOutOfRange()`, `entry.getMin()`, `entry.getMax()`, `warnings.add()`, `result.value(value).unsatisfiable(unsatisfiable ? Boolean.TRUE : null).label()`, `result.value(value).unsatisfiable()`, `result.value()`, `label()`, `entry.getUnit()`, `matchValues()`, `codes.isEmpty()`, `ProfileMatcher.BRAND_SPEC.equals()`, `"IN".equals()`, `missingBrands.add()`, `roleEntry.getLabel()`, `String.join()`, `nullSafe()`, `enumLabel()`, `result.values(codes).label()`, `result.values()`, `valid.add()`, `result.build()`, `capCount()`, `resolveConflicts()`
- [ProfileValidatorImpl.java:213](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L213) `private String normalizeOp(String id, SpecType type, String op, List<String> values)`
  - Lời gọi xuất hiện: `values.isEmpty()`, `Set.of("EQ", "CONTAINS").contains()`, `Set.of()`, `Set.of("EQ", "IN").contains()`, `normalized.equals()`, `warnings.add()`
- [ProfileValidatorImpl.java:227](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L227) `private List<ProfileMetricDto> metrics(List<ProfileMetricDto> items, Integer legacyRamGb)`
  - Lời gọi xuất hiện: `assignIds()`, `items.stream().map(ProfileMetricDto::getId).toList()`, `items.stream().map()`, `items.stream()`, `items.size()`, `items.get()`, `ids.get()`, `MetricKind.parse()`, `item.getMetric()`, `kind.isEmpty()`, `drop()`, `upper()`, `item.getOp()`, `INT_OPS.contains()`, `isInteger()`, `item.getValue()`, `valid.add()`, `metric()`, `kind.get()`, `priority()`, `item.getPriority()`, `truncate()`, `item.getReason()`, `valid.removeIf()`, `MetricKind.TOTAL_RAM_GB.name().equals()`, `MetricKind.TOTAL_RAM_GB.name()`, `"GTE".equals()`, `valid.stream().map(ProfileMetricDto::getId).collect()`, `valid.stream().map()`, `valid.stream()`, `Collectors.toSet()`, `usedIds.contains()`, `legacyRamGb.doubleValue()`, `capCount()`, `resolveConflicts()`, `interval()`, `List.of()`
- [ProfileValidatorImpl.java:265](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L265) `private ProfileMetricDto metric(String id, MetricKind kind, String op, double value, int priority, String reason)`
  - Lời gọi xuất hiện: `dictionary.getMetrics().get()`, `dictionary.getMetrics()`, `kind.name()`, `isOutOfRange()`, `range.getMin()`, `range.getMax()`, `warnings.add()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority(priority) .reason(reason).unsatisfiable(unsatisfiable ? Boolean.TRUE : null) .label(kind.label() + " " + symbol(op) + " " + formatNumber(value) + (kind.unit() != null ? " " + kind.unit() : "")).build()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority(priority) .reason(reason).unsatisfiable(unsatisfiable ? Boolean.TRUE : null).label()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority(priority) .reason(reason).unsatisfiable()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority(priority).reason()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value()`, `ProfileMetricDto.builder().id(id).metric(kind.name()).op()`, `ProfileMetricDto.builder().id(id).metric()`, `ProfileMetricDto.builder().id()`, `ProfileMetricDto.builder()`, `kind.label()`, `symbol()`, `formatNumber()`, `kind.unit()`
- [ProfileValidatorImpl.java:277](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L277) `private List<ProfilePreferenceDto> preferences(List<ProfilePreferenceDto> items)`
  - Lời gọi xuất hiện: `assignIds()`, `items.stream().map(ProfilePreferenceDto::getId).toList()`, `items.stream().map()`, `items.stream()`, `items.size()`, `items.get()`, `ids.get()`, `ProfileMatcher.BRAND_SPEC.equalsIgnoreCase()`, `String.valueOf(item.getSpec()).trim()`, `String.valueOf()`, `item.getSpec()`, `softBrands.addAll()`, `nullSafe()`, `item.getValues()`, `upper()`, `item.getDirection()`, `item.getWeight()`, `item.getWeight().isNaN()`, `clamp()`, `ProfilePreferenceDto.builder() .id(id).direction(direction).weight(weight).reason(truncate(item.getReason())).values()`, `ProfilePreferenceDto.builder() .id(id).direction(direction).weight(weight).reason()`, `ProfilePreferenceDto.builder() .id(id).direction(direction).weight()`, `ProfilePreferenceDto.builder() .id(id).direction()`, `ProfilePreferenceDto.builder().id()`, `ProfilePreferenceDto.builder()`, `truncate()`, `item.getReason()`, `item.targetsMetric()`, `MetricKind.parse()`, `item.getMetric()`, `kind.isEmpty()`, `drop()`, `Set.of("MAX", "MIN").contains()`, `Set.of()`, `dictionary.getMetrics().get()`, `dictionary.getMetrics()`, `kind.get().name()`, `kind.get()`, `valid.add()`, `result.metric(kind.get().name()).cap(capOf(item.getCap(), range != null ? range.getMax() : null)).build()`, `result.metric(kind.get().name()).cap()`, `result.metric()`, `capOf()`, `item.getCap()`, `range.getMax()`, `normalizeRole()`, `item.getRole()`, `dictionary.getRoles().get()`, `dictionary.getRoles()`, `findSpec()`, `spec.isEmpty()`, `spec.get().getValue()`, `spec.get()`, `SpecType.valueOf()`, `entry.getType()`, `"PREFER".equals()`, `result.role(role).spec()`, `result.role()`, `spec.get().getKey()`, `result.cap()`, `entry.getMax()`, `matchValues()`, `codes.isEmpty()`, `result.values()`, `result.build()`, `valid.stream() .sorted(Comparator.comparing(ProfilePreferenceDto::getWeight).reversed()).toList()`, `valid.stream().sorted()`, `valid.stream()`, `Comparator.comparing(ProfilePreferenceDto::getWeight).reversed()`, `Comparator.comparing()`, `sorted.stream().skip(MAX_PREFERENCES).forEach()`, `sorted.stream().skip()`, `sorted.stream()`, `item.getId()`, `sorted.stream().limit(MAX_PREFERENCES).map(ProfilePreferenceDto::getId).collect()`, `sorted.stream().limit(MAX_PREFERENCES).map()`, `sorted.stream().limit()`, `Collectors.toSet()`, `valid.stream().filter(item -> kept.contains(item.getId())).collect()`, `valid.stream().filter()`, `kept.contains()`, `Collectors.toCollection()`
- [ProfileValidatorImpl.java:348](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L348) `private List<BalancePairDto> balancePairs(List<BalancePairDto> pairs)`
  - Lời gọi xuất hiện: `normalizeRole()`, `pair.getA()`, `pair.getB()`, `ROLES.contains()`, `first.equals()`, `drop()`, `pair.getMaxGap()`, `clamp()`, `result.add()`
- [ProfileValidatorImpl.java:364](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L364) `private RequirementProfileDto.Brands brands(RequirementProfileDto.Brands brands)`
  - Lời gọi xuất hiện: `dictionary.getRoles().values().forEach()`, `dictionary.getRoles().values()`, `dictionary.getRoles()`, `nullSafe(role.getBrands()).forEach()`, `nullSafe()`, `role.getBrands()`, `available.putIfAbsent()`, `TextNormalizer.normalize()`, `prefer.addAll()`, `brands.getPrefer()`, `result.setAvoid()`, `matchBrands()`, `brands.getAvoid()`, `result.setPrefer()`
- [ProfileValidatorImpl.java:378](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L378) `private List<String> matchBrands(List<String> names, Map<String, String> available)`
  - Lời gọi xuất hiện: `nullSafe()`, `available.get()`, `TextNormalizer.normalize()`, `drop()`, `result.add()`
- [ProfileValidatorImpl.java:396](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L396) `private void implyRequiredRoles(RequirementProfileDto profile)`
  - Lời gọi xuất hiện: `REQUIRABLE_ROLES.stream().sorted().toList()`, `REQUIRABLE_ROLES.stream().sorted()`, `REQUIRABLE_ROLES.stream()`, `profile.getRequiredRoles().stream().anyMatch()`, `profile.getRequiredRoles().stream()`, `profile.getRequiredRoles()`, `role.equals()`, `ref.getRole()`, `profile.getExcludedRoles().stream().anyMatch()`, `profile.getExcludedRoles().stream()`, `profile.getExcludedRoles()`, `profile.getConstraints()`, `constraint.getRole()`, `Set.of("GTE", "EQ", "IN", "CONTAINS").contains()`, `Set.of()`, `constraint.getOp()`, `constraint.getPriority()`, `constraint.getLabel()`, `"gpu".equals()`, `profile.getMetrics()`, `MetricKind.GPU_COUNT.name().equals()`, `MetricKind.GPU_COUNT.name()`, `metric.getMetric()`, `Set.of("GTE", "EQ").contains()`, `metric.getOp()`, `metric.getValue()`, `MetricKind.TOTAL_VRAM_GB.name().equals()`, `MetricKind.TOTAL_VRAM_GB.name()`, `metric.getPriority()`, `metric.getLabel()`, `profile.getRequiredRoles().add()`, `ProfileRoleRefDto.builder().role(role).priority(priority) .reason("Suy ra từ yêu cầu " + reason).build()`, `ProfileRoleRefDto.builder().role(role).priority(priority).reason()`, `ProfileRoleRefDto.builder().role(role).priority()`, `ProfileRoleRefDto.builder().role()`, `ProfileRoleRefDto.builder()`, `warnings.add()`
- [ProfileValidatorImpl.java:430](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L430) `private void resolveRoleConflicts(RequirementProfileDto profile)`
  - Lời gọi xuất hiện: `profile.getExcludedRoles()`, `profile.getRequiredRoles().stream() .filter(ref -> ref.getRole().equals(excluded.getRole())).findFirst()`, `profile.getRequiredRoles().stream().filter()`, `profile.getRequiredRoles().stream()`, `profile.getRequiredRoles()`, `ref.getRole().equals()`, `ref.getRole()`, `excluded.getRole()`, `required.isEmpty()`, `required.get().getPriority()`, `required.get()`, `excluded.getPriority()`, `profile.getRequiredRoles().remove()`, `drop()`, `profile.getExcludedRoles().remove()`
- [ProfileValidatorImpl.java:452](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L452) `private <T> List<T> resolveConflicts(List<T> items, java.util.function.Function<T, String> keyOf, java.util.function.Function<T, Integer> priorityOf, java.util.function.Function<T, String> idOf, java.util.function.Function<T, double[]> intervalOf, java.util.function.Function<T, String> opOf, java.util.function.Function<T, List<String>> valuesOf)`
  - Lời gọi xuất hiện: `items.stream().sorted(Comparator.comparing(priorityOf)).toList()`, `items.stream().sorted()`, `items.stream()`, `Comparator.comparing()`, `keyOf.apply()`, `opOf.apply()`, `INT_OPS.contains()`, `intervals.getOrDefault()`, `intervalOf.apply()`, `Math.max()`, `Math.min()`, `intervals.put()`, `allowed.get()`, `banned.getOrDefault()`, `Set.of()`, `valuesOf.apply()`, `"IN".equals()`, `nextAllowed.retainAll()`, `nextAllowed.removeAll()`, `nextAllowed.isEmpty()`, `allowed.put()`, `"NOT_IN".equals()`, `nextBanned.addAll()`, `nextBanned.containsAll()`, `banned.put()`, `drop()`, `idOf.apply()`, `kept.add()`, `items.stream().filter(item -> kept.contains(idOf.apply(item))).collect()`, `items.stream().filter()`, `kept.contains()`, `Collectors.toCollection()`
- [ProfileValidatorImpl.java:503](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L503) `private <T> List<T> capCount(List<T> items, int max, java.util.function.Function<T, Integer> priorityOf, java.util.function.Function<T, String> idOf)`
  - Lời gọi xuất hiện: `items.size()`, `items.stream().sorted(Comparator.comparing(priorityOf)).toList()`, `items.stream().sorted()`, `items.stream()`, `Comparator.comparing()`, `sorted.stream().skip(max).forEach()`, `sorted.stream().skip()`, `sorted.stream()`, `drop()`, `idOf.apply()`, `sorted.stream().limit(max).map(idOf).collect()`, `sorted.stream().limit(max).map()`, `sorted.stream().limit()`, `Collectors.toSet()`, `items.stream().filter(item -> kept.contains(idOf.apply(item))).collect()`, `items.stream().filter()`, `kept.contains()`, `Collectors.toCollection()`
- [ProfileValidatorImpl.java:512](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L512) `private double[] constraintInterval(ProfileConstraintDto item)`
  - Lời gọi xuất hiện: `item.getValue()`, `interval()`, `item.getOp()`
- [ProfileValidatorImpl.java:518](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L518) `private List<String> matchValues(String id, MetadataDictionaryResponse.SpecEntry entry, List<String> requested)`
  - Lời gọi xuất hiện: `entry.getValues()`, `Map.of()`, `nullSafe()`, `TextNormalizer.normalize()`, `values.entrySet().stream() .filter(option -> TextNormalizer.normalize(option.getKey()).equals(normalized) || TextNormalizer.normalize(option.getValue()).equals(normalized)) .map(Map.Entry::getKey).findFirst()`, `values.entrySet().stream() .filter(option -> TextNormalizer.normalize(option.getKey()).equals(normalized) || TextNormalizer.normalize(option.getValue()).equals(normalized)).map()`, `values.entrySet().stream().filter()`, `values.entrySet().stream()`, `values.entrySet()`, `TextNormalizer.normalize(option.getKey()).equals()`, `option.getKey()`, `TextNormalizer.normalize(option.getValue()).equals()`, `option.getValue()`, `code.isPresent()`, `result.add()`, `code.get()`, `drop()`
- [ProfileValidatorImpl.java:537](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/ProfileValidatorImpl.java#L537) `private String enumLabel(String specName, String op, List<String> codes, MetadataDictionaryResponse.SpecEntry entry)`
  - Lời gọi xuất hiện: `codes.stream() .map(code -> entry.getValues() != null ? entry.getValues().getOrDefault(code, code) : code).collect()`, `codes.stream().map()`, `codes.stream()`, `entry.getValues()`, `entry.getValues().getOrDefault()`, `Collectors.joining()`, `readable()`

## `pcbuild/internal/service/impl/PsuSizerImpl.java`

### PsuSizerImpl — [PsuSizerImpl.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L19)

Khai báo: `@Service public class PsuSizerImpl implements PsuSizer`

- Trường: `private static final int TDP_POWER = 2;`
- Trường: `private static final int WATTAGE = 14;`
- Trường: `private static final int REC_PSU_W = 50;`
- Trường: `private static final int BASE_LOAD_W = 80;`
- Trường: `private static final BigDecimal HEADROOM_ONE_GPU = new BigDecimal("1.30");`
- Trường: `private static final BigDecimal HEADROOM_MULTI_GPU = new BigDecimal("1.35");`
- Trường: `private static final BigDecimal PREMIUM_TOLERANCE = new BigDecimal("1.10");`
- Trường: `private final PartScorer partScorer;`
- [PsuSizerImpl.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L32) `@Autowired public PsuSizerImpl(PartScorer partScorer)`
- [PsuSizerImpl.java:38](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L38) `public PsuSizerImpl()`
- [PsuSizerImpl.java:42](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L42) `@Override public PowerRequirement requiredW(List<BuildLine> lines, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `line.part()`, `snapshot.parsed()`, `"cpu".equals()`, `line.role()`, `specs.ints().get()`, `specs.ints()`, `line.quantity()`, `"gpu".equals()`, `Math.max()`, `BigDecimal.valueOf(load).multiply(headroom).setScale(0, RoundingMode.CEILING).intValueExact()`, `BigDecimal.valueOf(load).multiply(headroom).setScale()`, `BigDecimal.valueOf(load).multiply()`, `BigDecimal.valueOf()`
- [PsuSizerImpl.java:75](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L75) `@Override public List<BuildLine> fix(List<BuildLine> lines, ScoreContext ctx)`
  - Lời gọi xuất hiện: `requiredW()`, `ctx.snapshot()`, `ctx.pool().of("psu").stream() .filter(psu -> ctx.snapshot().parsed(psu).ints().getOrDefault(WATTAGE, 0) >= need.requiredW()).toList()`, `ctx.pool().of("psu").stream().filter()`, `ctx.pool().of("psu").stream()`, `ctx.pool().of()`, `ctx.pool()`, `ctx.snapshot().parsed(psu).ints().getOrDefault()`, `ctx.snapshot().parsed(psu).ints()`, `ctx.snapshot().parsed()`, `need.requiredW()`, `enough.isEmpty()`, `enough.stream().map(ComponentCandidateDTO::getPrice).min(BigDecimal::compareTo).orElseThrow()`, `enough.stream().map(ComponentCandidateDTO::getPrice).min()`, `enough.stream().map()`, `enough.stream()`, `cheapest.multiply()`, `psu.getPrice().compareTo()`, `psu.getPrice()`, `partScorer.partScore()`, `Math.abs()`, `isCheaper()`, `"psu".equals()`, `line.role()`, `result.add()`
- [PsuSizerImpl.java:107](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/PsuSizerImpl.java#L107) `private static boolean isCheaper(ComponentCandidateDTO candidate, ComponentCandidateDTO current)`
  - Lời gọi xuất hiện: `candidate.getPrice().compareTo()`, `candidate.getPrice()`, `current.getPrice()`, `candidate.getVariantId()`, `current.getVariantId()`

## `pcbuild/internal/service/impl/RequestValidatorImpl.java`

### RequestValidatorImpl — [RequestValidatorImpl.java:21](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L21)

Khai báo: `@Service public class RequestValidatorImpl implements RequestValidator`

- Trường: `private static final Set<Integer> LEGACY_RAM_OPTIONS = Set.of(8, 16, 32, 64, 96, 128, 256, 512);`
- Trường: `private static final int MAX_PROFILE_BYTES = 32 * 1024;`
- Trường: `private final ObjectMapper objectMapper;`
- Trường: `private final BigDecimal minBudget;`
- Trường: `private final BigDecimal maxBudget;`
- [RequestValidatorImpl.java:31](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L31) `public RequestValidatorImpl(ObjectMapper objectMapper, @Value("${app.pcbuild.min-budget:8000000}") BigDecimal minBudget, @Value("${app.pcbuild.max-budget:9999999999}") BigDecimal maxBudget)`
- [RequestValidatorImpl.java:39](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L39) `@Override public void validate(AiBuildRequest request, BigDecimal floorPrice)`
  - Lời gọi xuất hiện: `request.getBudget()`, `floorPrice.compareTo()`, `budget.compareTo()`, `data.put()`, `vnd()`, `Map.of()`, `request.getFullSetBudget()`, `fullSetBudget.compareTo()`, `request.getProfile()`, `request.getPurpose()`, `request.getPurpose().trim()`, `purpose.length()`, `sizeOf()`, `request.getUserPrompt()`, `request.getUserPrompt().trim().length()`, `request.getUserPrompt().trim()`, `request.getRamGb()`, `LEGACY_RAM_OPTIONS.contains()`
- [RequestValidatorImpl.java:81](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L81) `@Override public BigDecimal minBudget()`
- [RequestValidatorImpl.java:86](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L86) `@Override public BigDecimal maxBudget()`
- [RequestValidatorImpl.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L91) `private int sizeOf(AiBuildRequest request)`
  - Lời gọi xuất hiện: `objectMapper.writeValueAsString(request.getProfile()).getBytes()`, `objectMapper.writeValueAsString()`, `request.getProfile()`
- [RequestValidatorImpl.java:99](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RequestValidatorImpl.java#L99) `private static String vnd(BigDecimal amount)`
  - Lời gọi xuất hiện: `symbols.setGroupingSeparator()`, `new DecimalFormat("#,##0", symbols).format()`

## `pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java`

### RuleBasedCompatibilityEngine — [RuleBasedCompatibilityEngine.java:41](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L41)

Khai báo: `@Slf4j @Service public class RuleBasedCompatibilityEngine implements CompatibilityEngine`

- Trường: `private static final int SOCKET = 1;`
- Trường: `private static final int TDP_POWER = 2;`
- Trường: `private static final int MEMORY_TYPE = 4;`
- Trường: `private static final int RAM_SLOTS = 5;`
- Trường: `private static final int MAX_RAM_GB = 6;`
- Trường: `private static final int M2_SLOTS = 8;`
- Trường: `private static final int CAPACITY_GB = 9;`
- Trường: `private static final int WATTAGE = 14;`
- Trường: `private static final int MAX_GPU_LENGTH_MM = 16;`
- Trường: `private static final int MAX_COOLER_HEIGHT_MM = 17;`
- Trường: `private static final int HEIGHT_MM = 19;`
- Trường: `private static final int COOLER_TYPE = 20;`
- Trường: `private static final int HAS_IGPU = 26;`
- Trường: `private static final int INCLUDES_COOLER = 27;`
- Trường: `private static final int CHIPSET = 30;`
- Trường: `private static final int CPU_SOCKET_COUNT = 31;`
- Trường: `private static final int PCIE_X16_SLOTS = 32;`
- Trường: `private static final int MODULES_COUNT = 34;`
- Trường: `private static final int M2_SLOTS_USED = 40;`
- Trường: `private static final int REC_PSU_W = 50;`
- Trường: `private static final int GPU_COOLER_DESIGN = 53;`
- Trường: `private static final int MAX_GPU_QTY = 54;`
- Trường: `private static final int PRICE_STATUS = 66;`
- Trường: `private static final int COMPAT_NOTE = 67;`
- Trường: `private static final int CATEGORY_CASE = 7;`
- Trường: `private static final int CATEGORY_COOLER = 8;`
- Trường: `private static final List<String> CORE_ROLES = List.of("cpu", "mainboard", "ram", "ssd", "psu", "case");`
- Trường: `private static final List<String> SINGLE_LINE_ROLES = List.of("cpu", "mainboard", "ram", "psu", "case", "cooler");`
- Trường: `private static final Map<String, String> ROLE_LABELS = Map.of( "cpu", "CPU", "mainboard", "bo mạch chủ", "ram", "RAM", "gpu", "card đồ họa", "ssd", "SSD", "psu", "nguồn", "case", "vỏ case", "cooler", "tản nhiệt");`
- Trường: `private static final Set<String> REFERENCE_PRICE_STATUSES = Set.of("ESTIMATE", "BUILD_ONLY", "PREORDER", "SNIPPET");`
- Trường: `private static final Set<Integer> SP5_MODULES_PER_SOCKET = Set.of(1, 2, 4, 6, 8, 10, 12);`
- Trường: `private static final Set<Integer> WRX90_MODULES = Set.of(4, 8);`
- Trường: `private static final BigDecimal BUDGET_WINDOW_MIN = new BigDecimal("0.90");`
- Trường: `private static final int PSU_HIGH_WATTAGE_W = 1600;`
- Trường: `private final PsuSizer psuSizer;`
- [RuleBasedCompatibilityEngine.java:90](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L90) `@Autowired public RuleBasedCompatibilityEngine(PsuSizer psuSizer)`
- [RuleBasedCompatibilityEngine.java:95](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L95) `public RuleBasedCompatibilityEngine()`
- [RuleBasedCompatibilityEngine.java:99](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L99) `@Override public EngineReport evaluate(List<BuildLine> lines, EvaluationContext ctx)`
  - Lời gọi xuất hiện: `new Evaluation(lines, ctx).run()`
- [RuleBasedCompatibilityEngine.java:629](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L629) `private static String reasonOf(String reason, String label)`
  - Lời gọi xuất hiện: `reason.isBlank()`
- [RuleBasedCompatibilityEngine.java:633](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L633) `private static String normalizeRole(String role)`
  - Lời gọi xuất hiện: `role.trim().toLowerCase()`, `role.trim()`
- [RuleBasedCompatibilityEngine.java:637](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L637) `private static String roleLabel(String role)`
  - Lời gọi xuất hiện: `ROLE_LABELS.getOrDefault()`
- [RuleBasedCompatibilityEngine.java:641](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L641) `private static String name(BuildLine line)`
  - Lời gọi xuất hiện: `line.part().getProductName()`, `line.part()`, `line.part().getVariantId()`
- [RuleBasedCompatibilityEngine.java:646](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L646) `private static Integer multiply(Integer value, int quantity)`
- [RuleBasedCompatibilityEngine.java:650](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L650) `private static String operatorSymbol(String operator)`
  - Lời gọi xuất hiện: `operator.trim().toUpperCase()`, `operator.trim()`
- [RuleBasedCompatibilityEngine.java:666](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L666) `private static String vnd(BigDecimal amount)`
  - Lời gọi xuất hiện: `symbols.setGroupingSeparator()`, `new DecimalFormat("#,##0", symbols).format()`, `amount.setScale()`

### Evaluation — [RuleBasedCompatibilityEngine.java:105](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L105)

Khai báo: `private final class Evaluation`

- Trường: `private final List<BuildLine> lines;`
- Trường: `private final EvaluationContext ctx;`
- Trường: `private final CompatibilitySnapshot snapshot;`
- Trường: `private final Map<BuildLine, ParsedSpecs> parsedByLine = new IdentityHashMap<>();`
- Trường: `private final List<CompatibilityCheckResponse> checks = new ArrayList<>();`
- Trường: `private int requiredPsuW;`
- [RuleBasedCompatibilityEngine.java:113](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L113) `private Evaluation(List<BuildLine> lines, EvaluationContext ctx)`
  - Lời gọi xuất hiện: `lines.stream() .filter(line -> line != null && line.part() != null && line.quantity() > 0) .map(line -> new BuildLine(normalizeRole(line.role()), line.part(), line.quantity())).toList()`, `lines.stream() .filter(line -> line != null && line.part() != null && line.quantity() > 0).map()`, `lines.stream().filter()`, `lines.stream()`, `line.part()`, `line.quantity()`, `normalizeRole()`, `line.role()`, `ctx.snapshot()`
- [RuleBasedCompatibilityEngine.java:122](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L122) `private EngineReport run()`
  - Lời gọi xuất hiện: `checkCoreParts()`, `checkRoles()`, `checkDuplicates()`, `checkRequiredSpecs()`, `checkDataRules()`, `checkDisplayOutput()`, `checkCpuCooler()`, `checkMemoryAndStorage()`, `checkPower()`, `checkGpuCount()`, `checkCpuSockets()`, `checkMemoryChannels()`, `checkBudget()`, `checkNotesAndPrices()`, `checkProfile()`, `EngineReport.of()`, `capacity()`, `total()`
- [RuleBasedCompatibilityEngine.java:143](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L143) `private void checkProfile()`
  - Lời gọi xuất hiện: `ctx.profile()`, `ctx.mode()`, `profile.constraints()`, `linesOf()`, `constraint.getRole()`, `ProfileMatcher.matches()`, `line.part()`, `add()`, `constraint.getId()`, `reasonOf()`, `constraint.getReason()`, `constraint.getLabel()`, `name()`, `BuildMetrics.of()`, `profile.metrics()`, `MetricKind.parse(metric.getMetric()).map(metrics::get).orElse()`, `MetricKind.parse(metric.getMetric()).map()`, `MetricKind.parse()`, `metric.getMetric()`, `metric.getId()`, `metric.getReason()`, `metric.getLabel()`, `profile.profile().getRequiredRoles()`, `profile.profile()`, `quantity()`, `required.getRole()`, `roleLabel()`, `required.getReason()`
- [RuleBasedCompatibilityEngine.java:173](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L173) `private void checkCoreParts()`
  - Lời gọi xuất hiện: `CORE_ROLES.stream().filter(role -> quantity(role) < 1).toList()`, `CORE_ROLES.stream().filter()`, `CORE_ROLES.stream()`, `quantity()`, `missing.isEmpty()`, `add()`, `missing.stream() .map(RuleBasedCompatibilityEngine::roleLabel).collect()`, `missing.stream().map()`, `missing.stream()`, `Collectors.joining()`
- [RuleBasedCompatibilityEngine.java:183](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L183) `private void checkRoles()`
  - Lời gọi xuất hiện: `line.part().getCategorySlug()`, `line.part()`, `slug.trim().equalsIgnoreCase()`, `slug.trim()`, `line.role()`, `add()`, `name()`, `roleLabel()`
- [RuleBasedCompatibilityEngine.java:193](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L193) `private void checkDuplicates()`
  - Lời gọi xuất hiện: `linesOf(role).size()`, `linesOf()`, `add()`, `roleLabel()`
- [RuleBasedCompatibilityEngine.java:203](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L203) `private void checkRequiredSpecs()`
  - Lời gọi xuất hiện: `quantity()`, `linesOf("cooler").stream().anyMatch()`, `linesOf("cooler").stream()`, `linesOf()`, `isLiquid()`, `line.part().getCategoryId()`, `line.part()`, `parsed()`, `snapshot.requiredSpecIds(categoryId).stream() .filter(specId -> !specs.has(specId)) .filter(specId -> !isAcceptedMissingSpec(categoryId, specId, line, hasGpu, hasAirCooler)) .sorted().toList()`, `snapshot.requiredSpecIds(categoryId).stream() .filter(specId -> !specs.has(specId)) .filter(specId -> !isAcceptedMissingSpec(categoryId, specId, line, hasGpu, hasAirCooler)).sorted()`, `snapshot.requiredSpecIds(categoryId).stream() .filter(specId -> !specs.has(specId)).filter()`, `snapshot.requiredSpecIds(categoryId).stream().filter()`, `snapshot.requiredSpecIds(categoryId).stream()`, `snapshot.requiredSpecIds()`, `specs.has()`, `isAcceptedMissingSpec()`, `missing.isEmpty()`, `add()`, `missingStatus()`, `name()`, `specNames()`
- [RuleBasedCompatibilityEngine.java:221](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L221) `private boolean isAcceptedMissingSpec(Integer categoryId, int specId, BuildLine line, boolean hasGpu, boolean hasAirCooler)`
  - Lời gọi xuất hiện: `isLiquid()`
- [RuleBasedCompatibilityEngine.java:232](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L232) `private void checkDataRules()`
  - Lời gọi xuất hiện: `snapshot.rules()`, `linesOfCategory()`, `rule.category1Id()`, `rule.category2Id()`, `isSkippedForLiquidCooler()`, `evaluateRule()`
- [RuleBasedCompatibilityEngine.java:247](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L247) `private boolean isSkippedForLiquidCooler(RuleDefinition rule, BuildLine first, BuildLine second)`
  - Lời gọi xuất hiện: `rule.spec1Id()`, `isLiquid()`, `rule.spec2Id()`
- [RuleBasedCompatibilityEngine.java:251](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L251) `private void evaluateRule(RuleDefinition rule, BuildLine first, BuildLine second)`
  - Lời gọi xuất hiện: `rule.id()`, `parsed()`, `firstSpecs.raw().get()`, `firstSpecs.raw()`, `rule.spec1Id()`, `secondSpecs.raw().get()`, `secondSpecs.raw()`, `rule.spec2Id()`, `missing.add()`, `snapshot.specName()`, `name()`, `add()`, `rule.ruleName()`, `missingStatus()`, `String.join()`, `compare()`, `snapshot.display()`, `operatorSymbol()`, `rule.operator()`, `ctx.mode()`, `log.warn()`, `first.part().getVariantId()`, `first.part()`, `second.part().getVariantId()`, `second.part()`
- [RuleBasedCompatibilityEngine.java:285](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L285) `private Boolean compare(RuleDefinition rule, ParsedSpecs firstSpecs, ParsedSpecs secondSpecs, String firstValue, String secondValue)`
  - Lời gọi xuất hiện: `rule.operator()`, `rule.operator().trim().toUpperCase()`, `rule.operator().trim()`, `firstValue.equalsIgnoreCase()`, `secondSpecs.lists().get(rule.spec2Id()).contains()`, `secondSpecs.lists().get()`, `secondSpecs.lists()`, `rule.spec2Id()`, `firstValue.toUpperCase()`, `firstSpecs.lists().get(rule.spec1Id()).contains()`, `firstSpecs.lists().get()`, `firstSpecs.lists()`, `rule.spec1Id()`, `secondValue.toUpperCase()`, `firstSpecs.ints().get()`, `firstSpecs.ints()`, `secondSpecs.ints().get()`, `secondSpecs.ints()`, `first.compareTo()`, `"LESS_THAN_OR_EQUAL".equals()`, `log.warn()`, `rule.id()`
- [RuleBasedCompatibilityEngine.java:313](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L313) `private void checkDisplayOutput()`
  - Lời gọi xuất hiện: `first()`, `quantity()`, `add()`, `parsed(cpu).raw().get()`, `parsed(cpu).raw()`, `parsed()`, `"YES".equalsIgnoreCase()`, `name()`, `"NO".equalsIgnoreCase()`, `missingStatus()`
- [RuleBasedCompatibilityEngine.java:333](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L333) `private void checkCpuCooler()`
  - Lời gọi xuất hiện: `first()`, `quantity()`, `add()`, `parsed(cpu).raw().get()`, `parsed(cpu).raw()`, `parsed()`, `"YES".equalsIgnoreCase()`, `name()`, `"NO".equalsIgnoreCase()`, `missingStatus()`
- [RuleBasedCompatibilityEngine.java:352](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L352) `private void checkMemoryAndStorage()`
  - Lời gọi xuất hiện: `first()`, `parsed()`, `multiply()`, `memory.ints().get()`, `memory.ints()`, `ram.quantity()`, `board.ints().get()`, `board.ints()`, `add()`, `missingStatus()`, `"DDR5".equalsIgnoreCase()`, `memory.raw().get()`, `memory.raw()`, `linesOf()`, `ssds.isEmpty()`, `parsed(ssd).ints().get()`, `parsed(ssd).ints()`, `ssd.quantity()`
- [RuleBasedCompatibilityEngine.java:405](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L405) `private void checkPower()`
  - Lời gọi xuất hiện: `psuSizer.requiredW()`, `estimate.requiredW()`, `first()`, `parsed(psu).ints().get()`, `parsed(psu).ints()`, `parsed()`, `add()`, `missingStatus()`, `estimate.loadW()`
- [RuleBasedCompatibilityEngine.java:435](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L435) `private void checkGpuCount()`
  - Lời gọi xuất hiện: `linesOf()`, `quantity()`, `first()`, `parsed(mainboard).ints().get()`, `parsed(mainboard).ints()`, `parsed()`, `add()`, `missingStatus()`, `parsed(gpu).ints().getOrDefault()`, `parsed(gpu).ints()`, `name()`, `parsed(gpu).raw().get()`, `parsed(gpu).raw()`, `"BLOWER".equalsIgnoreCase()`, `snapshot.display()`
- [RuleBasedCompatibilityEngine.java:477](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L477) `private void checkCpuSockets()`
  - Lời gọi xuất hiện: `quantity()`, `first()`, `parsed(mainboard).ints().get()`, `parsed(mainboard).ints()`, `parsed()`, `add()`, `missingStatus()`
- [RuleBasedCompatibilityEngine.java:499](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L499) `private void checkMemoryChannels()`
  - Lời gọi xuất hiện: `first()`, `parsed()`, `multiply()`, `parsed(ram).ints().get()`, `parsed(ram).ints()`, `ram.quantity()`, `"WRX90".equalsIgnoreCase()`, `board.raw().get()`, `board.raw()`, `WRX90_MODULES.contains()`, `"SP5".equalsIgnoreCase()`, `board.ints().getOrDefault()`, `board.ints()`, `SP5_MODULES_PER_SOCKET.contains()`, `add()`
- [RuleBasedCompatibilityEngine.java:521](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L521) `private void checkBudget()`
  - Lời gọi xuất hiện: `ctx.budget()`, `budget.signum()`, `total()`, `total.compareTo()`, `add()`, `vnd()`, `total.subtract()`, `budget.multiply()`, `total.multiply(BigDecimal.valueOf(100)).divide()`, `total.multiply()`, `BigDecimal.valueOf()`, `budget.subtract()`
- [RuleBasedCompatibilityEngine.java:539](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L539) `private void checkNotesAndPrices()`
  - Lời gọi xuất hiện: `parsed()`, `specs.raw().get()`, `specs.raw()`, `add()`, `name()`, `REFERENCE_PRICE_STATUSES.contains()`, `priceStatus.toUpperCase()`
- [RuleBasedCompatibilityEngine.java:556](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L556) `private CapacityResponse capacity()`
  - Lời gọi xuất hiện: `first()`, `parsed()`, `linesOf()`, `multiply()`, `parsed(ssd).ints().get()`, `parsed(ssd).ints()`, `ssd.quantity()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null) .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null) .m2Used(m2Used) .m2Max(board != null ? board.ints().get(M2_SLOTS) : null) .gpuCount(quantity("gpu")) .gpuMax(board != null ? board.ints().get(PCIE_X16_SLOTS) : null).build()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null) .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null) .m2Used(m2Used) .m2Max(board != null ? board.ints().get(M2_SLOTS) : null) .gpuCount(quantity("gpu")).gpuMax()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null) .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null) .m2Used(m2Used) .m2Max(board != null ? board.ints().get(M2_SLOTS) : null).gpuCount()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null) .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null) .m2Used(m2Used).m2Max()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null) .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null).m2Used()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null) .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null).ramMaxGb()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null) .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null).ramGb()`, `CapacityResponse.builder() .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null).ramSlotsMax()`, `CapacityResponse.builder().ramSlotsUsed()`, `CapacityResponse.builder()`, `memory.ints().get()`, `memory.ints()`, `ram.quantity()`, `board.ints().get()`, `board.ints()`, `quantity()`
- [RuleBasedCompatibilityEngine.java:578](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L578) `private BigDecimal total()`
  - Lời gọi xuất hiện: `line.part().getPrice()`, `line.part()`, `total.add()`, `line.part().getPrice().multiply()`, `BigDecimal.valueOf()`, `line.quantity()`
- [RuleBasedCompatibilityEngine.java:590](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L590) `private void add(String key, String label, String status, String detail)`
  - Lời gọi xuất hiện: `checks.add()`, `CompatibilityCheckResponse.of()`
- [RuleBasedCompatibilityEngine.java:594](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L594) `private String missingStatus()`
  - Lời gọi xuất hiện: `ctx.mode()`
- [RuleBasedCompatibilityEngine.java:598](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L598) `private ParsedSpecs parsed(BuildLine line)`
  - Lời gọi xuất hiện: `parsedByLine.computeIfAbsent()`, `snapshot.parsed()`, `key.part()`
- [RuleBasedCompatibilityEngine.java:602](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L602) `private List<BuildLine> linesOf(String role)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> role.equals(line.role())).toList()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`
- [RuleBasedCompatibilityEngine.java:606](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L606) `private List<BuildLine> linesOfCategory(int categoryId)`
  - Lời gọi xuất hiện: `lines.stream() .filter(line -> line.part().getCategoryId() != null && line.part().getCategoryId() == categoryId).toList()`, `lines.stream().filter()`, `lines.stream()`, `line.part().getCategoryId()`, `line.part()`
- [RuleBasedCompatibilityEngine.java:612](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L612) `private BuildLine first(String role)`
  - Lời gọi xuất hiện: `lines.stream().filter(line -> role.equals(line.role())).findFirst().orElse()`, `lines.stream().filter(line -> role.equals(line.role())).findFirst()`, `lines.stream().filter()`, `lines.stream()`, `role.equals()`, `line.role()`
- [RuleBasedCompatibilityEngine.java:616](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L616) `private int quantity(String role)`
  - Lời gọi xuất hiện: `linesOf(role).stream().mapToInt(BuildLine::quantity).sum()`, `linesOf(role).stream().mapToInt()`, `linesOf(role).stream()`, `linesOf()`
- [RuleBasedCompatibilityEngine.java:620](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L620) `private boolean isLiquid(BuildLine line)`
  - Lời gọi xuất hiện: `"Liquid".equalsIgnoreCase()`, `parsed(line).raw().get()`, `parsed(line).raw()`, `parsed()`
- [RuleBasedCompatibilityEngine.java:624](../../src/main/java/com/project/cybershop/pcbuild/internal/service/impl/RuleBasedCompatibilityEngine.java#L624) `private String specNames(List<Integer> specIds)`
  - Lời gọi xuất hiện: `specIds.stream().map(snapshot::specName).collect()`, `specIds.stream().map()`, `specIds.stream()`, `Collectors.joining()`

## `pcbuild/internal/service/LlmClientService.java`

### LlmClientService — [LlmClientService.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/service/LlmClientService.java#L11)

Khai báo: `public interface LlmClientService`

- [LlmClientService.java:36](../../src/main/java/com/project/cybershop/pcbuild/internal/service/LlmClientService.java#L36) `RequirementProfileDto extractProfile(String systemPrompt, String userJson, Instant deadline)`
- [LlmClientService.java:39](../../src/main/java/com/project/cybershop/pcbuild/internal/service/LlmClientService.java#L39) `ExplanationDraft explainBuild(String systemPrompt, String userJson, Instant deadline)`

### ExplanationDraft — [LlmClientService.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/service/LlmClientService.java#L14)

Khai báo: `@Data @JsonIgnoreProperties(ignoreUnknown = true) class ExplanationDraft`

- Trường: `private String buildName;`
- Trường: `private String aiReasoning;`
- Trường: `private String performanceHighlight;`
- Trường: `private List<PartReason> partReasons = new ArrayList<>();`

### PartReason — [LlmClientService.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/service/LlmClientService.java#L22)

Khai báo: `@Data @JsonIgnoreProperties(ignoreUnknown = true) public static class PartReason`

- Trường: `private Integer variantId;`
- Trường: `private String reason;`

## `pcbuild/internal/service/MetadataDictionaryService.java`

### MetadataDictionaryService — [MetadataDictionaryService.java:16](../../src/main/java/com/project/cybershop/pcbuild/internal/service/MetadataDictionaryService.java#L16)

Khai báo: `public interface MetadataDictionaryService`

- [MetadataDictionaryService.java:17](../../src/main/java/com/project/cybershop/pcbuild/internal/service/MetadataDictionaryService.java#L17) `MetadataDictionaryResponse build(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsedByVariantId, List<CategorySpecDto> specificationCatalog, PerfIndexEstimator perfIndexEstimator)`

## `pcbuild/internal/service/PartScorer.java`

### PartScorer — [PartScorer.java:10](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PartScorer.java#L10)

Khai báo: `public interface PartScorer`

- [PartScorer.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PartScorer.java#L13) `double utility(List<BuildLine> lines, ScoreContext ctx)`
- [PartScorer.java:16](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PartScorer.java#L16) `double roleScore(String role, List<BuildLine> lines, ScoreContext ctx)`
- [PartScorer.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PartScorer.java#L19) `double partScore(String role, ComponentCandidateDTO part, ScoreContext ctx)`

## `pcbuild/internal/service/PcCustomService.java`

### PcCustomService — [PcCustomService.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L13)

Khai báo: `public interface PcCustomService`

- [PcCustomService.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L14) `Integer saveNewBuild(SavePcBuildRequest request, Integer userId)`
- [PcCustomService.java:15](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L15) `List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId)`
- [PcCustomService.java:16](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L16) `SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId)`
- [PcCustomService.java:17](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L17) `void deleteSavedBuild(Integer buildId, Integer userId)`
- [PcCustomService.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L20) `ValidateBuildResponse validateBuild(ValidateBuildRequest request)`
- [PcCustomService.java:23](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PcCustomService.java#L23) `List<CompatibleReplacementResponse> getCompatibleReplacements(ReplacementQueryRequest request)`

## `pcbuild/internal/service/PeripheralService.java`

### PeripheralService — [PeripheralService.java:6](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PeripheralService.java#L6)

Khai báo: `public interface PeripheralService`

- [PeripheralService.java:9](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PeripheralService.java#L9) `PeripheralSuggestionResponse suggest(PeripheralQueryRequest request)`

## `pcbuild/internal/service/ProfileExtractor.java`

### ProfileExtractor — [ProfileExtractor.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfileExtractor.java#L14)

Khai báo: `public interface ProfileExtractor`

- Trường: `String PROMPT_VERSION = "PROFILE_V2";`
- [ProfileExtractor.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfileExtractor.java#L22) `Extraction extract(String purpose, String userPrompt, BigDecimal budget, BuildTier tier, CompatibilitySnapshot snapshot, Instant deadline)`

### Extraction — [ProfileExtractor.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfileExtractor.java#L19)

Khai báo: `record Extraction(RequirementProfileDto draft, String status)`


## `pcbuild/internal/service/ProfilePresets.java`

### ProfilePresets — [ProfilePresets.java:15](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfilePresets.java#L15)

Khai báo: `public interface ProfilePresets`

- [ProfilePresets.java:18](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfilePresets.java#L18) `RequirementProfileDto preset(Archetype archetype, BuildTier tier)`
- [ProfilePresets.java:21](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfilePresets.java#L21) `Map<String, Double> roleWeights(Archetype archetype)`
- [ProfilePresets.java:24](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfilePresets.java#L24) `List<ProfilePreferenceDto> defaultPreferences(String role)`
- [ProfilePresets.java:30](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfilePresets.java#L30) `RequirementProfileDto fillMissing(RequirementProfileDto profile, BuildTier tier)`

## `pcbuild/internal/service/ProfileValidator.java`

### ProfileValidator — [ProfileValidator.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfileValidator.java#L11)

Khai báo: `public interface ProfileValidator`

- Trường: `String SOURCE_AI = "AI";`
- Trường: `String SOURCE_USER_EDITED = "USER_EDITED";`
- [ProfileValidator.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/service/ProfileValidator.java#L20) `ValidatedProfile validate(RequirementProfileDto input, MetadataDictionaryResponse dictionary, String source, Integer legacyRamGb)`

## `pcbuild/internal/service/PsuSizer.java`

### PsuSizer — [PsuSizer.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PsuSizer.java#L11)

Khai báo: `public interface PsuSizer`

- [PsuSizer.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PsuSizer.java#L14) `PowerRequirement requiredW(List<BuildLine> lines, CompatibilitySnapshot snapshot)`
- [PsuSizer.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/service/PsuSizer.java#L20) `List<BuildLine> fix(List<BuildLine> lines, ScoreContext ctx)`

## `pcbuild/internal/service/RequestValidator.java`

### RequestValidator — [RequestValidator.java:8](../../src/main/java/com/project/cybershop/pcbuild/internal/service/RequestValidator.java#L8)

Khai báo: `public interface RequestValidator`

- [RequestValidator.java:11](../../src/main/java/com/project/cybershop/pcbuild/internal/service/RequestValidator.java#L11) `void validate(AiBuildRequest request, BigDecimal floorPrice)`
- [RequestValidator.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/service/RequestValidator.java#L13) `BigDecimal minBudget()`
- [RequestValidator.java:15](../../src/main/java/com/project/cybershop/pcbuild/internal/service/RequestValidator.java#L15) `BigDecimal maxBudget()`

## `pcbuild/internal/support/AiCircuitBreaker.java`

### AiCircuitBreaker — [AiCircuitBreaker.java:17](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L17)

Khai báo: `@Slf4j @Component public class AiCircuitBreaker`

- Trường: `private final Clock clock;`
- Trường: `private final int failureThreshold;`
- Trường: `private final Duration openDuration;`
- Trường: `private State state = State.CLOSED;`
- Trường: `private int consecutiveFailures;`
- Trường: `private Instant openedAt;`
- Trường: `private boolean trialInProgress;`
- [AiCircuitBreaker.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L32) `public AiCircuitBreaker(Clock clock, @Value("${app.ai.circuit-failure-threshold:5}") int failureThreshold, @Value("${app.ai.circuit-open-seconds:30}") long openSeconds)`
  - Lời gọi xuất hiện: `Duration.ofSeconds()`
- [AiCircuitBreaker.java:41](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L41) `public synchronized void beforeCall()`
  - Lời gọi xuất hiện: `Duration.between()`, `clock.instant()`, `elapsed.compareTo()`, `remainingSeconds()`, `transition()`, `Math.max()`, `openDuration.toSeconds()`
- [AiCircuitBreaker.java:57](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L57) `public synchronized void onSuccess()`
  - Lời gọi xuất hiện: `transition()`
- [AiCircuitBreaker.java:66](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L66) `public synchronized void onProviderFailure(String failureType)`
  - Lời gọi xuất hiện: `open()`
- [AiCircuitBreaker.java:77](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L77) `public synchronized void onNeutralOutcome()`
- [AiCircuitBreaker.java:81](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L81) `public synchronized State state()`
  - Lời gọi xuất hiện: `Duration.between(openedAt, clock.instant()).compareTo()`, `Duration.between()`, `clock.instant()`
- [AiCircuitBreaker.java:88](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L88) `private void open(String reason)`
  - Lời gọi xuất hiện: `clock.instant()`, `transition()`
- [AiCircuitBreaker.java:93](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L93) `private long remainingSeconds()`
  - Lời gọi xuất hiện: `openDuration.minus()`, `Duration.between()`, `clock.instant()`, `Math.max()`, `remaining.toMillis()`
- [AiCircuitBreaker.java:98](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L98) `private void transition(State next, String reason)`
  - Lời gọi xuất hiện: `log.warn()`

### State — [AiCircuitBreaker.java:21](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiCircuitBreaker.java#L21)

Khai báo: `public enum State`

- Trường: `CLOSED`
- Trường: `OPEN`
- Trường: `HALF_OPEN`

## `pcbuild/internal/support/AiUnavailableException.java`

### AiUnavailableException — [AiUnavailableException.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L12)

Khai báo: `public class AiUnavailableException extends BusinessException`

- Trường: `public static final String MESSAGE = "Trợ lý AI đang bận hoặc phản hồi chậm. Vui lòng thử lại sau ít phút.";`
- Trường: `private static final long DEFAULT_RETRY_AFTER_SECONDS = 30;`
- Trường: `private final Detail detail;`
- Trường: `private final long retryAfterSeconds;`
- [AiUnavailableException.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L22) `public AiUnavailableException(Detail detail)`
- [AiUnavailableException.java:26](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L26) `public AiUnavailableException(Detail detail, long retryAfterSeconds)`
  - Lời gọi xuất hiện: `Math.max()`
- [AiUnavailableException.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L32) `public Detail detail()`
- [AiUnavailableException.java:36](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L36) `@Override public HttpStatus getHttpStatus()`
- [AiUnavailableException.java:41](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L41) `@Override public String getCode()`
- [AiUnavailableException.java:46](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L46) `@Override public Object getData()`
  - Lời gọi xuất hiện: `Map.of()`, `detail.name()`
- [AiUnavailableException.java:51](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L51) `@Override public Long getRetryAfterSeconds()`

### Detail — [AiUnavailableException.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/support/AiUnavailableException.java#L14)

Khai báo: `public enum Detail`

- Trường: `TIMEOUT`
- Trường: `PROVIDER_ERROR`
- Trường: `INVALID_RESPONSE`
- Trường: `DEADLINE_EXCEEDED`
- Trường: `CIRCUIT_OPEN`

## `pcbuild/internal/support/BuildMetrics.java`

### BuildMetrics — [BuildMetrics.java:13](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildMetrics.java#L13)

Khai báo: `public final class BuildMetrics`

- Trường: `private static final int CAPACITY_GB = 9;`
- Trường: `private static final int VRAM_GB = 12;`
- Trường: `private static final int CORE_COUNT = 22;`
- [BuildMetrics.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildMetrics.java#L19) `private BuildMetrics()`
- [BuildMetrics.java:22](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildMetrics.java#L22) `public static Map<MetricKind, Integer> of(List<BuildLine> lines, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `MetricKind.values()`, `values.put()`, `line.part()`, `snapshot.parsed()`, `line.quantity()`, `line.role()`, `role.equals()`, `values.merge()`, `specs.ints().getOrDefault()`, `specs.ints()`

## `pcbuild/internal/support/BuildRuleException.java`

### BuildRuleException — [BuildRuleException.java:7](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L7)

Khai báo: `public class BuildRuleException extends BusinessException`

- Trường: `private final String code;`
- Trường: `private final HttpStatus httpStatus;`
- Trường: `private final transient Object data;`
- Trường: `private final Long retryAfterSeconds;`
- [BuildRuleException.java:14](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L14) `public BuildRuleException(String code, HttpStatus httpStatus, String message, Object data)`
- [BuildRuleException.java:19](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L19) `public BuildRuleException(String code, HttpStatus httpStatus, String message, Object data, Long retryAfterSeconds)`
- [BuildRuleException.java:27](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L27) `@Override public Long getRetryAfterSeconds()`
- [BuildRuleException.java:32](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L32) `@Override public HttpStatus getHttpStatus()`
- [BuildRuleException.java:37](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L37) `@Override public String getCode()`
- [BuildRuleException.java:42](../../src/main/java/com/project/cybershop/pcbuild/internal/support/BuildRuleException.java#L42) `@Override public Object getData()`

## `pcbuild/internal/support/CandidatePoolBuilder.java`

### CandidatePoolBuilder — [CandidatePoolBuilder.java:28](../../src/main/java/com/project/cybershop/pcbuild/internal/support/CandidatePoolBuilder.java#L28)

Khai báo: `@Component public class CandidatePoolBuilder`

- Trường: `public static final List<String> BUILD_ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");`
- Trường: `private static final int CATEGORY_CASE = 7;`
- Trường: `private static final int CATEGORY_COOLER = 8;`
- Trường: `private static final int MAX_GPU_LENGTH_MM = 16;`
- Trường: `private static final int MAX_COOLER_HEIGHT_MM = 17;`
- Trường: `private static final int HEIGHT_MM = 19;`
- Trường: `private static final int COOLER_TYPE = 20;`
- Trường: `private final PartScorer partScorer;`
- Trường: `private final ProfilePresets profilePresets;`
- [CandidatePoolBuilder.java:43](../../src/main/java/com/project/cybershop/pcbuild/internal/support/CandidatePoolBuilder.java#L43) `public CandidatePoolBuilder(PartScorer partScorer, ProfilePresets profilePresets)`
- [CandidatePoolBuilder.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/support/CandidatePoolBuilder.java#L49) `public CandidatePool build(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget)`
  - Lời gọi xuất hiện: `profile.isExcluded()`, `profile.constraints().stream() .filter(constraint -> role.equals(constraint.getRole())).toList()`, `profile.constraints().stream().filter()`, `profile.constraints().stream()`, `profile.constraints()`, `role.equals()`, `constraint.getRole()`, `profile.profile().getBrands().getAvoid()`, `profile.profile().getBrands()`, `profile.profile()`, `snapshot.candidates()`, `role.equalsIgnoreCase()`, `candidate.getCategorySlug()`, `hasRequiredSpecs()`, `constraints.stream().allMatch()`, `constraints.stream()`, `ProfileMatcher.matches()`, `candidate.getBrandName()`, `avoided.stream().anyMatch()`, `avoided.stream()`, `candidate.getPrice()`, `candidate.getPrice().compareTo()`, `pool.add()`, `filtered.put()`, `filtered.forEach()`, `ranges.put()`, `rangesOf()`, `ScoreContext.of()`, `pool.forEach()`, `scores.put()`, `candidate.getVariantId()`, `partScorer.partScore()`, `profile.maxPrice()`, `Comparator.comparing(ComponentCandidateDTO::getPrice).reversed()`, `Comparator.comparing()`, `Comparator.comparing((ComponentCandidateDTO candidate) -> -scores.get(candidate.getVariantId())).thenComparing()`, `scores.get()`, `sorted.put()`, `pool.stream().sorted(order.thenComparing(ComponentCandidateDTO::getVariantId)).toList()`, `pool.stream().sorted()`, `pool.stream()`, `order.thenComparing()`
- [CandidatePoolBuilder.java:91](../../src/main/java/com/project/cybershop/pcbuild/internal/support/CandidatePoolBuilder.java#L91) `private static boolean hasRequiredSpecs(ComponentCandidateDTO candidate, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `snapshot.parsed()`, `candidate.getCategoryId()`, `snapshot.requiredSpecIds()`, `specs.has()`, `"Liquid".equalsIgnoreCase()`, `specs.raw().get()`, `specs.raw()`
- [CandidatePoolBuilder.java:105](../../src/main/java/com/project/cybershop/pcbuild/internal/support/CandidatePoolBuilder.java#L105) `private static Map<Integer, int[]> rangesOf(List<ComponentCandidateDTO> pool, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `snapshot.parsed(candidate).ints().keySet()`, `snapshot.parsed(candidate).ints()`, `snapshot.parsed()`, `specIds.add()`, `snapshot.effectiveInt()`, `ranges.merge()`, `Math.min()`, `Math.max()`

## `pcbuild/internal/support/PerfIndexEstimator.java`

### PerfIndexEstimator — [PerfIndexEstimator.java:18](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L18)

Khai báo: `@Slf4j public final class PerfIndexEstimator`

- Trường: `public static final int PERF_INDEX_SPEC_ID = 47;`
- Trường: `public static final int PERF_TIER_SPEC_ID = 48;`
- Trường: `private final TreeMap<Integer, Double> referenceByTier;`
- Trường: `private final Map<Integer, Integer> effectiveByVariantId;`
- [PerfIndexEstimator.java:27](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L27) `private PerfIndexEstimator(TreeMap<Integer, Double> referenceByTier, Map<Integer, Integer> effectiveByVariantId)`
  - Lời gọi xuất hiện: `Map.copyOf()`
- [PerfIndexEstimator.java:33](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L33) `public static PerfIndexEstimator build(List<ComponentCandidateDTO> gpus, Map<Integer, ParsedSpecs> parsedByVariantId)`
  - Lời gọi xuất hiện: `parsedByVariantId.getOrDefault()`, `gpu.getVariantId()`, `ParsedSpecs.of()`, `specs.ints().get()`, `specs.ints()`, `indexesByTier.computeIfAbsent(tier, key -> new ArrayList<>()).add()`, `indexesByTier.computeIfAbsent()`, `indexesByTier.forEach()`, `reference.put()`, `indexes.stream().mapToInt(Integer::intValue).average().orElse()`, `indexes.stream().mapToInt(Integer::intValue).average()`, `indexes.stream().mapToInt()`, `indexes.stream()`, `Map.of()`, `estimator.estimate()`, `effective.put()`, `log.warn()`
- [PerfIndexEstimator.java:61](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L61) `public Integer perfIndex(ComponentCandidateDTO gpu, ParsedSpecs specs)`
  - Lời gọi xuất hiện: `gpu.getVariantId()`, `effectiveByVariantId.get()`, `estimate()`
- [PerfIndexEstimator.java:66](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L66) `public Map<Integer, Double> referenceByTier()`
  - Lời gọi xuất hiện: `Map.copyOf()`
- [PerfIndexEstimator.java:70](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PerfIndexEstimator.java#L70) `private Integer estimate(ParsedSpecs specs)`
  - Lời gọi xuất hiện: `specs.ints().get()`, `specs.ints()`, `referenceByTier.isEmpty()`, `referenceByTier.get()`, `Math.round()`, `referenceByTier.lowerEntry()`, `referenceByTier.higherEntry()`, `higher.getValue()`, `higher.getKey()`, `lower.getValue()`, `lower.getKey()`

## `pcbuild/internal/support/PeripheralSelector.java`

### PeripheralSelector — [PeripheralSelector.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L20)

Khai báo: `public final class PeripheralSelector`

- Trường: `public static final String MONITOR = "monitor";`
- Trường: `public static final String KEYBOARD = "keyboard";`
- Trường: `public static final String MOUSE = "mouse";`
- Trường: `public static final List<String> ROLES = List.of(MONITOR, KEYBOARD, MOUSE);`
- Trường: `public static final int REFRESH_RATE_HZ = 70;`
- Trường: `private static final BigDecimal PC_SHARE = new BigDecimal("0.25");`
- Trường: `private static final BigDecimal MOUSE_SHARE = new BigDecimal("0.10");`
- Trường: `private static final BigDecimal KEYBOARD_SHARE = new BigDecimal("0.15");`
- Trường: `private static final int GAMING_MIN_REFRESH_HZ = 144;`
- Trường: `private static final Pattern DIGITS = Pattern.compile("\\d{1,4}");`
- [PeripheralSelector.java:41](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L41) `private PeripheralSelector()`
- [PeripheralSelector.java:49](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L49) `public static BudgetRange budgetRange(List<ComponentCandidateDTO> peripherals)`
  - Lời gọi xuất hiện: `optionsOf()`, `options.isEmpty()`, `min.add()`, `options.get(0).getPrice()`, `options.get()`, `rawMax.add()`, `options.get(options.size() - 1).getPrice()`, `options.size()`, `practicalMax(peripherals, rawMax, false).max()`, `practicalMax()`
- [PeripheralSelector.java:63](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L63) `private static BigDecimal practicalMax(List<ComponentCandidateDTO> peripherals, BigDecimal rawMax, boolean gaming)`
  - Lời gọi xuất hiện: `pick(peripherals, spend, gaming).total()`, `pick()`, `total.compareTo()`
- [PeripheralSelector.java:74](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L74) `public static List<ComponentCandidateDTO> optionsOf(List<ComponentCandidateDTO> peripherals, String role)`
  - Lời gọi xuất hiện: `peripherals.stream() .filter(item -> role.equalsIgnoreCase(item.getCategorySlug()) && item.getPrice() != null) .sorted(Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing(ComponentCandidateDTO::getVariantId)).toList()`, `peripherals.stream() .filter(item -> role.equalsIgnoreCase(item.getCategorySlug()) && item.getPrice() != null).sorted()`, `peripherals.stream().filter()`, `peripherals.stream()`, `role.equalsIgnoreCase()`, `item.getCategorySlug()`, `item.getPrice()`, `Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing()`, `Comparator.comparing()`
- [PeripheralSelector.java:85](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L85) `public static Selection select(List<ComponentCandidateDTO> peripherals, BigDecimal budget, BigDecimal pcTotal, String archetype)`
  - Lời gọi xuất hiện: `budgetRange()`, `budget.subtract()`, `available.compareTo()`, `range.min()`, `Map.of()`, `range.min().subtract()`, `available.min()`, `range.min().max()`, `pcTotal.multiply(PC_SHARE).setScale()`, `pcTotal.multiply()`, `pick()`, `Archetype.parse()`, `byRole.put()`, `picks.monitor()`, `picks.keyboard()`, `picks.mouse()`, `picks.total()`, `picks.gamingFallback()`
- [PeripheralSelector.java:115](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L115) `private static Picks pick(List<ComponentCandidateDTO> peripherals, BigDecimal spendLimit, boolean gaming)`
  - Lời gọi xuất hiện: `cheapest()`, `pickWithShare()`, `spendLimit.subtract(cheapestKeyboard).subtract()`, `spendLimit.subtract()`, `spendLimit.subtract(mouse.getPrice()).subtract()`, `mouse.getPrice()`, `keyboard.getPrice()`, `best(peripherals, MONITOR, monitorLimit).orElseThrow()`, `best()`, `peripherals.stream().filter(PeripheralSelector::isHighRefresh).toList()`, `peripherals.stream().filter()`, `peripherals.stream()`, `monitor.isPresent()`, `monitor.get()`, `roomy.isEmpty()`, `roomy.get().getPrice()`, `roomy.get()`, `rest.subtract()`, `cheaperKeyboard.getPrice()`
- [PeripheralSelector.java:145](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L145) `private static ComponentCandidateDTO pickWithShare(List<ComponentCandidateDTO> peripherals, String role, BigDecimal spendLimit, BigDecimal share, BigDecimal cap)`
  - Lời gọi xuất hiện: `spendLimit.multiply(share).setScale(0, RoundingMode.DOWN).max(cheapest(peripherals, role)).min()`, `spendLimit.multiply(share).setScale(0, RoundingMode.DOWN).max()`, `spendLimit.multiply(share).setScale()`, `spendLimit.multiply()`, `cheapest()`, `best(peripherals, role, limit).orElseThrow()`, `best()`
- [PeripheralSelector.java:152](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L152) `private static Optional<ComponentCandidateDTO> best(List<ComponentCandidateDTO> peripherals, String role, BigDecimal limit)`
  - Lời gọi xuất hiện: `optionsOf(peripherals, role).stream() .filter(item -> item.getPrice().compareTo(limit) <= 0).max()`, `optionsOf(peripherals, role).stream().filter()`, `optionsOf(peripherals, role).stream()`, `optionsOf()`, `item.getPrice().compareTo()`, `item.getPrice()`, `Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing()`, `Comparator.comparing()`, `Comparator.reverseOrder()`
- [PeripheralSelector.java:159](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L159) `private static BigDecimal cheapest(List<ComponentCandidateDTO> peripherals, String role)`
  - Lời gọi xuất hiện: `optionsOf(peripherals, role).get(0).getPrice()`, `optionsOf(peripherals, role).get()`, `optionsOf()`
- [PeripheralSelector.java:163](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L163) `private static boolean isHighRefresh(ComponentCandidateDTO item)`
  - Lời gọi xuất hiện: `item.getSpecById()`, `DIGITS.matcher(hz).matches()`, `DIGITS.matcher()`, `Integer.parseInt()`

### BudgetRange — [PeripheralSelector.java:34](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L34)

Khai báo: `public record BudgetRange(BigDecimal min, BigDecimal max)`


### Selection — [PeripheralSelector.java:37](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L37)

Khai báo: `public record Selection(BigDecimal available, BigDecimal spendLimit, BigDecimal peripheralTotal, Map<String, ComponentCandidateDTO> picks, boolean gamingFallback)`


### Picks — [PeripheralSelector.java:107](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L107)

Khai báo: `private record Picks(ComponentCandidateDTO monitor, ComponentCandidateDTO keyboard, ComponentCandidateDTO mouse, boolean gamingFallback)`

- [PeripheralSelector.java:109](../../src/main/java/com/project/cybershop/pcbuild/internal/support/PeripheralSelector.java#L109) `BigDecimal total()`
  - Lời gọi xuất hiện: `monitor.getPrice().add(keyboard.getPrice()).add()`, `monitor.getPrice().add()`, `monitor.getPrice()`, `keyboard.getPrice()`, `mouse.getPrice()`

## `pcbuild/internal/support/ProfileMatcher.java`

### ProfileMatcher — [ProfileMatcher.java:20](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L20)

Khai báo: `public final class ProfileMatcher`

- Trường: `public static final String BRAND_SPEC = "Brand";`
- Trường: `private static final Set<String> NUMERIC_OPS = Set.of("GTE", "LTE", "EQ");`
- [ProfileMatcher.java:27](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L27) `private ProfileMatcher()`
- [ProfileMatcher.java:31](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L31) `public static boolean matches(ProfileConstraintDto constraint, ComponentCandidateDTO part, CompatibilitySnapshot snapshot)`
  - Lời gọi xuất hiện: `BRAND_SPEC.equals()`, `constraint.getSpec()`, `part.getBrandName()`, `upper(constraint.getValues()).contains()`, `upper()`, `constraint.getValues()`, `part.getBrandName().toUpperCase()`, `"NOT_IN".equals()`, `constraint.getOp()`, `"IN".equals()`, `snapshot.specId()`, `NUMERIC_OPS.contains()`, `snapshot.effectiveInt()`, `constraint.getValue()`, `compare()`, `snapshot.parsed()`, `specs.raw().get()`, `specs.raw()`, `wanted.contains()`, `raw.toUpperCase()`, `specs.lists().getOrDefault(specId, Set.of()).containsAll()`, `specs.lists().getOrDefault()`, `specs.lists()`, `Set.of()`
- [ProfileMatcher.java:60](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L60) `public static boolean matches(ProfileMetricDto metric, Map<MetricKind, Integer> values)`
  - Lời gọi xuất hiện: `MetricKind.parse(metric.getMetric()).orElse()`, `MetricKind.parse()`, `metric.getMetric()`, `metric.getValue()`, `metric.getOp()`, `compare()`, `values.getOrDefault()`
- [ProfileMatcher.java:66](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L66) `public static boolean compare(String op, double actual, double expected)`
- [ProfileMatcher.java:79](../../src/main/java/com/project/cybershop/pcbuild/internal/support/ProfileMatcher.java#L79) `private static Set<String> upper(List<String> values)`
  - Lời gọi xuất hiện: `Set.of()`, `values.stream().map(value -> value.toUpperCase(Locale.ROOT)).collect()`, `values.stream().map()`, `values.stream()`, `value.toUpperCase()`, `Collectors.toSet()`

## `pcbuild/internal/support/RateLimiter.java`

### RateLimiter — [RateLimiter.java:21](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L21)

Khai báo: `@Component public class RateLimiter`

- Trường: `private static final Duration WINDOW = Duration.ofMinutes(1);`
- Trường: `private final Clock clock;`
- Trường: `private final int aiSuggestPerMinute;`
- Trường: `private final int checkPerMinute;`
- Trường: `private final Map<String, Deque<Instant>> hitsByClient = new ConcurrentHashMap<>();`
- Trường: `private volatile Instant nextEviction = Instant.MIN;`
- [RateLimiter.java:34](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L34) `public RateLimiter(Clock clock, @Value("${app.pcbuild.rate-limit-per-minute:6}") int aiSuggestPerMinute, @Value("${app.pcbuild.rate-limit-check-per-minute:30}") int checkPerMinute)`
- [RateLimiter.java:43](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L43) `public void acquire(Bucket bucket, String clientKey)`
  - Lời gọi xuất hiện: `clock.instant()`, `evictIdleClients()`, `now.minus()`, `hitsByClient.computeIfAbsent()`, `hits.isEmpty()`, `hits.peekFirst().isAfter()`, `hits.peekFirst()`, `hits.pollFirst()`, `hits.size()`, `limitOf()`, `hits.addLast()`, `secondsUntil()`, `hits.peekFirst().plus()`, `Map.of()`
- [RateLimiter.java:65](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L65) `int trackedClients()`
  - Lời gọi xuất hiện: `hitsByClient.size()`
- [RateLimiter.java:69](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L69) `private int limitOf(Bucket bucket)`
- [RateLimiter.java:77](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L77) `private void evictIdleClients(Instant now)`
  - Lời gọi xuất hiện: `now.isBefore()`, `now.plus()`, `now.minus()`, `hitsByClient.entrySet().removeIf()`, `hitsByClient.entrySet()`, `entry.getValue()`, `entry.getValue().peekLast()`, `lastHit.isAfter()`
- [RateLimiter.java:96](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L96) `private static long secondsUntil(Instant moment, Instant now)`
  - Lời gọi xuất hiện: `Duration.between(now, moment).toMillis()`, `Duration.between()`, `Math.max()`

### Bucket — [RateLimiter.java:24](../../src/main/java/com/project/cybershop/pcbuild/internal/support/RateLimiter.java#L24)

Khai báo: `public enum Bucket`

- Trường: `AI_SUGGEST`
- Trường: `VALIDATE`
- Trường: `REPLACEMENTS`
- Trường: `OPTIONS`
- Trường: `PERIPHERALS`

## `pcbuild/internal/support/TextNormalizer.java`

### TextNormalizer — [TextNormalizer.java:7](../../src/main/java/com/project/cybershop/pcbuild/internal/support/TextNormalizer.java#L7)

Khai báo: `public final class TextNormalizer`

- [TextNormalizer.java:9](../../src/main/java/com/project/cybershop/pcbuild/internal/support/TextNormalizer.java#L9) `private TextNormalizer()`
- [TextNormalizer.java:12](../../src/main/java/com/project/cybershop/pcbuild/internal/support/TextNormalizer.java#L12) `public static String normalize(String text)`
  - Lời gọi xuất hiện: `Normalizer.normalize()`, `text.replace('đ', 'd').replace()`, `text.replace()`, `decomposed.replaceAll("\\p{M}+", "") .toLowerCase(Locale.ROOT) .replaceAll("\\s+", " ").trim()`, `decomposed.replaceAll("\\p{M}+", "") .toLowerCase(Locale.ROOT).replaceAll()`, `decomposed.replaceAll("\\p{M}+", "").toLowerCase()`, `decomposed.replaceAll()`

