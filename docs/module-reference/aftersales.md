# Tra cứu module aftersales

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **GET `/api/my/warranty-cards`** → `MyWarrantyController.myCards` [MyWarrantyController.java:30](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L30)
  - `@GetMapping("/warranty-cards") @Operation(summary = "Thẻ bảo hành và tiến trình phiếu của tôi, chỉ gồm nội dung công khai") public ResponseEntity<ApiResponseDTO<List<MyWarrantyCardDto>>> myCards()`
- **GET `/api/my/service-intakes`** → `MyWarrantyController.myIntakes` [MyWarrantyController.java:38](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L38)
  - `@GetMapping("/service-intakes") @Operation(summary = "Các lần gửi bảo hành của tôi: trạng thái chung của bộ máy và kết quả từng linh kiện") public ResponseEntity<ApiResponseDTO<List<MyServiceIntakeDto>>> myIntakes()`
- **GET `/api/me/warranties`** → `PickupWarrantyController.mine` [PickupWarrantyController.java:16](../../src/main/java/com/project/cybershop/aftersales/internal/controller/PickupWarrantyController.java#L16)
  - `@GetMapping public List<PickupWarrantyApi.Card> mine(Authentication auth)`
- **POST `/api/service-intakes`** → `ServiceIntakeController.create` [ServiceIntakeController.java:32](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L32)
  - `@PostMapping @Operation(summary = "Tiếp nhận một hoặc nhiều linh kiện cùng đơn hàng, mỗi linh kiện một phiếu con") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> create(@Valid @RequestBody IntakeCreateRequest request)`
- **GET `/api/service-intakes/{id}`** → `ServiceIntakeController.get` [ServiceIntakeController.java:40](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L40)
  - `@GetMapping("/{id}") @Operation(summary = "Chi tiết lần tiếp nhận: các linh kiện, trạng thái chung và thao tác được phép") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> get(@PathVariable("id") Integer id)`
- **POST `/api/service-intakes/{id}/return`** → `ServiceIntakeController.returnAll` [ServiceIntakeController.java:47](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L47)
  - `@PostMapping("/{id}/return") @Operation(summary = "Trả cả bộ máy cho khách khi mọi linh kiện đã xử lý xong") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> returnAll(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
- **POST `/api/service-intakes/{id}/cancel`** → `ServiceIntakeController.cancel` [ServiceIntakeController.java:55](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L55)
  - `@PostMapping("/{id}/cancel") @Operation(summary = "Hủy cả lần tiếp nhận khi chưa linh kiện nào được kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> cancel(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
- **POST `/api/service-intakes/{id}/requests`** → `ServiceIntakeController.addComponent` [ServiceIntakeController.java:63](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L63)
  - `@PostMapping("/{id}/requests") @Operation(summary = "Thêm một linh kiện cùng đơn vào lần tiếp nhận khi kiểm tra thấy linh kiện đó cũng lỗi") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> addComponent(@PathVariable("id") Integer id, @Valid @RequestBody RelinkCardRequest request)`
- **POST `/api/service-intakes/{id}/no-fault-rest`** → `ServiceIntakeController.noFaultRest` [ServiceIntakeController.java:72](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L72)
  - `@PostMapping("/{id}/no-fault-rest") @Operation(summary = "Kết luận không tìm thấy lỗi cho các linh kiện còn chờ hoặc đang kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> noFaultRest(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
- **GET `/api/service-requests`** → `ServiceRequestController.list` [ServiceRequestController.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L35)
  - `@GetMapping @Operation(summary = "Danh sách phiếu, lọc theo trạng thái nếu có") public ResponseEntity<ApiResponseDTO<List<ServiceRequestSummaryDto>>> list( @RequestParam(value = "status", required = false) String status)`
- **GET `/api/service-requests/{id}`** → `ServiceRequestController.get` [ServiceRequestController.java:43](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L43)
  - `@GetMapping("/{id}") @Operation(summary = "Chi tiết phiếu, lịch sử và các bước người gọi được làm tiếp") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> get(@PathVariable("id") Integer id)`
- **POST `/api/service-requests/{id}/transitions`** → `ServiceRequestController.transition` [ServiceRequestController.java:50](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L50)
  - `@PostMapping("/{id}/transitions") @Operation(summary = "Chuyển trạng thái phiếu theo bảng chuyển trạng thái của từng vai trò") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> transition(@PathVariable("id") Integer id, @Valid @RequestBody TransitionRequest request)`
- **POST `/api/service-requests/{id}/notes`** → `ServiceRequestController.addNote` [ServiceRequestController.java:59](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L59)
  - `@PostMapping("/{id}/notes") @Operation(summary = "Thêm ghi chú mà không đổi trạng thái") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> addNote(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
- **PUT `/api/service-requests/{id}/warranty-card`** → `ServiceRequestController.relinkCard` [ServiceRequestController.java:67](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L67)
  - `@PutMapping("/{id}/warranty-card") @Operation(summary = "Đổi thẻ gắn với phiếu sang thẻ khác cùng đơn hàng khi đang kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> relinkCard(@PathVariable("id") Integer id, @Valid @RequestBody RelinkCardRequest request)`
- **POST `/api/service-requests/{id}/replacement`** → `ServiceRequestController.replaceDevice` [ServiceRequestController.java:75](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L75)
  - `@PostMapping("/{id}/replacement") @Operation(summary = "Đổi thiết bị sang serial khác và chuyển phiếu sang chờ trả") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> replaceDevice(@PathVariable("id") Integer id, @Valid @RequestBody ReplacementRequest request)`
- **GET `/api/warranty-cards`** → `WarrantyCardController.search` [WarrantyCardController.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/controller/WarrantyCardController.java#L31)
  - `@GetMapping @Operation(summary = "Tra cứu thẻ theo serial, mã đơn, số điện thoại hoặc mã phiếu (so khớp chính xác)") public ResponseEntity<ApiResponseDTO<List<WarrantyCardDto>>> search(@RequestParam("q") String query)`

## Khai báo và lời gọi theo file

## `aftersales/api/dto/request/IntakeCreateRequest.java`

### IntakeCreateRequest — [IntakeCreateRequest.java:16](../../src/main/java/com/project/cybershop/aftersales/api/dto/request/IntakeCreateRequest.java#L16)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class IntakeCreateRequest`

- Trường: `@NotEmpty(message = "Chưa chọn linh kiện nào để tiếp nhận") private List<Integer> warrantyCardIds;`
- Trường: `private List<Integer> outOfWarrantyCardIds;`
- Trường: `@NotBlank(message = "Mô tả lỗi không được để trống") private String issueDescription;`
- Trường: `@NotBlank(message = "Tình trạng ngoại quan không được để trống") private String conditionNote;`
- Trường: `@NotBlank(message = "Phụ kiện nhận giữ không được để trống") private String accessoriesNote;`
- Trường: `@NotBlank(message = "Tên người mang máy không được để trống") @Size(max = 100, message = "Tên người mang máy tối đa 100 ký tự") private String contactName;`
- Trường: `@NotBlank(message = "Số điện thoại liên hệ không được để trống") @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0") private String contactPhone;`
- Trường: `@NotNull(message = "Ngày hẹn trả không được để trống") private LocalDate expectedReturnDate;`

## `aftersales/api/dto/request/NoteRequest.java`

### NoteRequest — [NoteRequest.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/request/NoteRequest.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class NoteRequest`

- Trường: `@NotBlank(message = "Ghi chú nội bộ không được để trống") private String internalNote;`
- Trường: `private String publicNote;`
- Trường: `private LocalDate expectedReturnDate;`

## `aftersales/api/dto/request/RelinkCardRequest.java`

### RelinkCardRequest — [RelinkCardRequest.java:10](../../src/main/java/com/project/cybershop/aftersales/api/dto/request/RelinkCardRequest.java#L10)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class RelinkCardRequest`

- Trường: `@NotNull(message = "Chưa chọn thẻ bảo hành") private Integer warrantyCardId;`
- Trường: `@NotBlank(message = "Ghi chú nội bộ không được để trống") private String internalNote;`

## `aftersales/api/dto/request/ReplacementRequest.java`

### ReplacementRequest — [ReplacementRequest.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/request/ReplacementRequest.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ReplacementRequest`

- Trường: `@NotBlank(message = "Serial thiết bị thay thế không được để trống") @Size(max = 100, message = "Serial tối đa 100 ký tự") private String newSerialNumber;`
- Trường: `@NotBlank(message = "Chưa chọn nguồn thiết bị thay thế") @Pattern(regexp = "stock|supplier", message = "Nguồn thiết bị phải là stock hoặc supplier") private String source;`
- Trường: `@NotBlank(message = "Ghi chú nội bộ không được để trống") private String internalNote;`
- Trường: `@NotBlank(message = "Nội dung thông báo cho khách không được để trống") private String publicNote;`

## `aftersales/api/dto/request/TransitionRequest.java`

### TransitionRequest — [TransitionRequest.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/request/TransitionRequest.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class TransitionRequest`

- Trường: `@NotBlank(message = "Chưa chọn trạng thái mới") private String toStatus;`
- Trường: `private String resolution;`
- Trường: `@NotBlank(message = "Ghi chú nội bộ không được để trống") private String internalNote;`
- Trường: `private String publicNote;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private Long version;`

## `aftersales/api/dto/response/AllowedTransitionDto.java`

### AllowedTransitionDto — [AllowedTransitionDto.java:13](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/AllowedTransitionDto.java#L13)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class AllowedTransitionDto`

- Trường: `private String toStatus;`
- Trường: `private List<String> resolutions;`

## `aftersales/api/dto/response/MyServiceIntakeDto.java`

### MyServiceIntakeDto — [MyServiceIntakeDto.java:16](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/MyServiceIntakeDto.java#L16)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class MyServiceIntakeDto`

- Trường: `private String intakeCode;`
- Trường: `private String orderCode;`
- Trường: `private String status;`
- Trường: `private String issueDescription;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private LocalDateTime createdAt;`
- Trường: `private LocalDateTime returnedAt;`
- Trường: `private List<MyServiceRequestDto> components;`

## `aftersales/api/dto/response/MyServiceRequestDto.java`

### MyServiceRequestDto — [MyServiceRequestDto.java:15](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/MyServiceRequestDto.java#L15)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class MyServiceRequestDto`

- Trường: `private Integer id;`
- Trường: `private String requestCode;`
- Trường: `private String intakeCode;`
- Trường: `private String productName;`
- Trường: `private String serialNumber;`
- Trường: `private String status;`
- Trường: `private String resolution;`
- Trường: `private String issueDescription;`
- Trường: `private String publicNote;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private LocalDateTime createdAt;`
- Trường: `private LocalDateTime completedAt;`
- Trường: `private List<MyTimelineEntryDto> timeline;`

## `aftersales/api/dto/response/MyTimelineEntryDto.java`

### MyTimelineEntryDto — [MyTimelineEntryDto.java:10](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/MyTimelineEntryDto.java#L10)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class MyTimelineEntryDto`

- Trường: `private String status;`
- Trường: `private String publicNote;`
- Trường: `private LocalDateTime createdAt;`

## `aftersales/api/dto/response/MyWarrantyCardDto.java`

### MyWarrantyCardDto — [MyWarrantyCardDto.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/MyWarrantyCardDto.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class MyWarrantyCardDto`

- Trường: `private Integer id;`
- Trường: `private String serialNumber;`
- Trường: `private LocalDateTime startDate;`
- Trường: `private LocalDateTime endDate;`
- Trường: `private String status;`
- Trường: `private boolean valid;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private String orderCode;`
- Trường: `private String replacedBySerialNumber;`
- Trường: `private List<MyServiceRequestDto> requests;`

## `aftersales/api/dto/response/ServiceIntakeDto.java`

### ServiceIntakeDto — [ServiceIntakeDto.java:16](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/ServiceIntakeDto.java#L16)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ServiceIntakeDto`

- Trường: `private Integer id;`
- Trường: `private String intakeCode;`
- Trường: `private Integer orderId;`
- Trường: `private String orderCode;`
- Trường: `private String customerName;`
- Trường: `private String status;`
- Trường: `private String contactName;`
- Trường: `private String contactPhone;`
- Trường: `private String issueDescription;`
- Trường: `private String conditionNote;`
- Trường: `private String accessoriesNote;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private LocalDateTime createdAt;`
- Trường: `private LocalDateTime returnedAt;`
- Trường: `private Long version;`
- Trường: `private List<ServiceRequestSummaryDto> requests;`
- Trường: `private boolean canReturn;`
- Trường: `private boolean canCancel;`
- Trường: `private boolean canAddComponent;`

## `aftersales/api/dto/response/ServiceRequestDto.java`

### ServiceRequestDto — [ServiceRequestDto.java:12](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/ServiceRequestDto.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ServiceRequestDto`

- Trường: `private Integer id;`
- Trường: `private String requestCode;`
- Trường: `private Integer intakeId;`
- Trường: `private String intakeCode;`
- Trường: `private String status;`
- Trường: `private String resolution;`
- Trường: `private String issueDescription;`
- Trường: `private String conditionNote;`
- Trường: `private String accessoriesNote;`
- Trường: `private String contactName;`
- Trường: `private String contactPhone;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private String technicianNote;`
- Trường: `private String publicNote;`
- Trường: `private Integer assignedTechnicianId;`
- Trường: `private String assignedTechnicianName;`
- Trường: `private LocalDateTime createdAt;`
- Trường: `private LocalDateTime completedAt;`
- Trường: `private Long version;`
- Trường: `private WarrantyCardDto card;`
- Trường: `private List<ServiceRequestEventDto> events;`
- Trường: `private List<AllowedTransitionDto> allowedTransitions;`
- Trường: `private boolean canAddNote;`
- Trường: `private boolean canRelinkCard;`
- Trường: `private boolean canReplaceDevice;`
- Trường: `private boolean replacementInStock;`

## `aftersales/api/dto/response/ServiceRequestEventDto.java`

### ServiceRequestEventDto — [ServiceRequestEventDto.java:10](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/ServiceRequestEventDto.java#L10)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ServiceRequestEventDto`

- Trường: `private Integer id;`
- Trường: `private String fromStatus;`
- Trường: `private String toStatus;`
- Trường: `private Integer actorId;`
- Trường: `private String actorName;`
- Trường: `private String internalNote;`
- Trường: `private String publicNote;`
- Trường: `private LocalDateTime createdAt;`

## `aftersales/api/dto/response/ServiceRequestSummaryDto.java`

### ServiceRequestSummaryDto — [ServiceRequestSummaryDto.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/ServiceRequestSummaryDto.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ServiceRequestSummaryDto`

- Trường: `private Integer id;`
- Trường: `private String requestCode;`
- Trường: `private Integer intakeId;`
- Trường: `private String intakeCode;`
- Trường: `private String status;`
- Trường: `private String resolution;`
- Trường: `private String issueDescription;`
- Trường: `private String contactName;`
- Trường: `private String contactPhone;`
- Trường: `private LocalDate expectedReturnDate;`
- Trường: `private LocalDateTime createdAt;`
- Trường: `private LocalDateTime completedAt;`
- Trường: `private Integer warrantyCardId;`
- Trường: `private String serialNumber;`
- Trường: `private String productName;`
- Trường: `private String orderCode;`

## `aftersales/api/dto/response/WarrantyCardDto.java`

### WarrantyCardDto — [WarrantyCardDto.java:11](../../src/main/java/com/project/cybershop/aftersales/api/dto/response/WarrantyCardDto.java#L11)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class WarrantyCardDto`

- Trường: `private Integer id;`
- Trường: `private String serialNumber;`
- Trường: `private LocalDateTime startDate;`
- Trường: `private LocalDateTime endDate;`
- Trường: `private String status;`
- Trường: `private boolean valid;`
- Trường: `private String productName;`
- Trường: `private String variantName;`
- Trường: `private String sku;`
- Trường: `private String orderCode;`
- Trường: `private Integer orderId;`
- Trường: `private Integer pcCustomId;`
- Trường: `private boolean matched;`
- Trường: `private String customerName;`
- Trường: `private String customerPhone;`
- Trường: `private String pickupName;`
- Trường: `private String pickupPhone;`
- Trường: `private Integer replacesCardId;`
- Trường: `private Integer replacedByCardId;`
- Trường: `private String replacedBySerialNumber;`
- Trường: `private long repairedCount;`
- Trường: `private ServiceRequestSummaryDto openRequest;`
- Trường: `private List<ServiceRequestSummaryDto> requests;`

## `aftersales/api/PickupWarrantyApi.java`

### PickupWarrantyApi — [PickupWarrantyApi.java:6](../../src/main/java/com/project/cybershop/aftersales/api/PickupWarrantyApi.java#L6)

Khai báo: `public interface PickupWarrantyApi`

- [PickupWarrantyApi.java:10](../../src/main/java/com/project/cybershop/aftersales/api/PickupWarrantyApi.java#L10) `void issue(List<Unit> units, LocalDateTime start)`
- [PickupWarrantyApi.java:11](../../src/main/java/com/project/cybershop/aftersales/api/PickupWarrantyApi.java#L11) `List<Card> forCustomer(Integer userId)`

### Unit — [PickupWarrantyApi.java:7](../../src/main/java/com/project/cybershop/aftersales/api/PickupWarrantyApi.java#L7)

Khai báo: `record Unit(Integer orderItemId, String serialNumber, int warrantyMonths)`


### Card — [PickupWarrantyApi.java:8](../../src/main/java/com/project/cybershop/aftersales/api/PickupWarrantyApi.java#L8)

Khai báo: `record Card(String id, String orderId, Integer userId, String productId, String productName, String serial, LocalDateTime start, LocalDateTime end, String status)`


## `aftersales/internal/controller/MyWarrantyController.java`

### MyWarrantyController — [MyWarrantyController.java:17](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L17)

Khai báo: `@RestController @RequestMapping("/api/my") @Tag(name = "My Warranty", description = "API bảo hành của khách hàng đang đăng nhập") public class MyWarrantyController`

- Trường: `private final WarrantyCardService cardService;`
- Trường: `private final CurrentUser currentUser;`
- [MyWarrantyController.java:25](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L25) `public MyWarrantyController(WarrantyCardService cardService, CurrentUser currentUser)`
- [MyWarrantyController.java:30](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L30) `@GetMapping("/warranty-cards") @Operation(summary = "Thẻ bảo hành và tiến trình phiếu của tôi, chỉ gồm nội dung công khai") public ResponseEntity<ApiResponseDTO<List<MyWarrantyCardDto>>> myCards()`
  - Lời gọi xuất hiện: `currentUser.require().id()`, `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `cardService.myCards()`
- [MyWarrantyController.java:38](../../src/main/java/com/project/cybershop/aftersales/internal/controller/MyWarrantyController.java#L38) `@GetMapping("/service-intakes") @Operation(summary = "Các lần gửi bảo hành của tôi: trạng thái chung của bộ máy và kết quả từng linh kiện") public ResponseEntity<ApiResponseDTO<List<MyServiceIntakeDto>>> myIntakes()`
  - Lời gọi xuất hiện: `currentUser.require().id()`, `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `cardService.myIntakes()`

## `aftersales/internal/controller/PickupWarrantyController.java`

### PickupWarrantyController — [PickupWarrantyController.java:9](../../src/main/java/com/project/cybershop/aftersales/internal/controller/PickupWarrantyController.java#L9)

Khai báo: `@RestController @RequestMapping("/api/me/warranties") public class PickupWarrantyController`

- Trường: `private final PickupWarrantyApi warranties;`
- Trường: `private final IdentityApi identity;`
- [PickupWarrantyController.java:14](../../src/main/java/com/project/cybershop/aftersales/internal/controller/PickupWarrantyController.java#L14) `public PickupWarrantyController(PickupWarrantyApi warranties, IdentityApi identity)`
- [PickupWarrantyController.java:16](../../src/main/java/com/project/cybershop/aftersales/internal/controller/PickupWarrantyController.java#L16) `@GetMapping public List<PickupWarrantyApi.Card> mine(Authentication auth)`
  - Lời gọi xuất hiện: `warranties.forCustomer()`, `identity.requireActiveUser(auth.getName()).getId()`, `identity.requireActiveUser()`, `auth.getName()`

## `aftersales/internal/controller/ServiceIntakeController.java`

### ServiceIntakeController — [ServiceIntakeController.java:19](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L19)

Khai báo: `@RestController @RequestMapping("/api/service-intakes") @Tag(name = "Service Intakes", description = "API lần tiếp nhận bảo hành: cả bộ máy khách mang tới, mỗi linh kiện một phiếu con") public class ServiceIntakeController`

- Trường: `private final ServiceIntakeService service;`
- Trường: `private final CurrentUser currentUser;`
- [ServiceIntakeController.java:27](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L27) `public ServiceIntakeController(ServiceIntakeService service, CurrentUser currentUser)`
- [ServiceIntakeController.java:32](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L32) `@PostMapping @Operation(summary = "Tiếp nhận một hoặc nhiều linh kiện cùng đơn hàng, mỗi linh kiện một phiếu con") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> create(@Valid @RequestBody IntakeCreateRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.success()`, `service.create()`
- [ServiceIntakeController.java:40](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L40) `@GetMapping("/{id}") @Operation(summary = "Chi tiết lần tiếp nhận: các linh kiện, trạng thái chung và thao tác được phép") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> get(@PathVariable("id") Integer id)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.get()`
- [ServiceIntakeController.java:47](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L47) `@PostMapping("/{id}/return") @Operation(summary = "Trả cả bộ máy cho khách khi mọi linh kiện đã xử lý xong") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> returnAll(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.returnAll()`
- [ServiceIntakeController.java:55](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L55) `@PostMapping("/{id}/cancel") @Operation(summary = "Hủy cả lần tiếp nhận khi chưa linh kiện nào được kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> cancel(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.cancel()`
- [ServiceIntakeController.java:63](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L63) `@PostMapping("/{id}/requests") @Operation(summary = "Thêm một linh kiện cùng đơn vào lần tiếp nhận khi kiểm tra thấy linh kiện đó cũng lỗi") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> addComponent(@PathVariable("id") Integer id, @Valid @RequestBody RelinkCardRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.addComponent()`
- [ServiceIntakeController.java:72](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceIntakeController.java#L72) `@PostMapping("/{id}/no-fault-rest") @Operation(summary = "Kết luận không tìm thấy lỗi cho các linh kiện còn chờ hoặc đang kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> noFaultRest(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.noFaultRest()`

## `aftersales/internal/controller/ServiceRequestController.java`

### ServiceRequestController — [ServiceRequestController.java:22](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L22)

Khai báo: `@RestController @RequestMapping("/api/service-requests") @Tag(name = "Service Requests", description = "API phiếu bảo hành: tiếp nhận, xử lý kỹ thuật, trả máy") public class ServiceRequestController`

- Trường: `private final ServiceRequestService service;`
- Trường: `private final CurrentUser currentUser;`
- [ServiceRequestController.java:30](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L30) `public ServiceRequestController(ServiceRequestService service, CurrentUser currentUser)`
- [ServiceRequestController.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L35) `@GetMapping @Operation(summary = "Danh sách phiếu, lọc theo trạng thái nếu có") public ResponseEntity<ApiResponseDTO<List<ServiceRequestSummaryDto>>> list( @RequestParam(value = "status", required = false) String status)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.list()`
- [ServiceRequestController.java:43](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L43) `@GetMapping("/{id}") @Operation(summary = "Chi tiết phiếu, lịch sử và các bước người gọi được làm tiếp") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> get(@PathVariable("id") Integer id)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.get()`
- [ServiceRequestController.java:50](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L50) `@PostMapping("/{id}/transitions") @Operation(summary = "Chuyển trạng thái phiếu theo bảng chuyển trạng thái của từng vai trò") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> transition(@PathVariable("id") Integer id, @Valid @RequestBody TransitionRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.transition()`
- [ServiceRequestController.java:59](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L59) `@PostMapping("/{id}/notes") @Operation(summary = "Thêm ghi chú mà không đổi trạng thái") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> addNote(@PathVariable("id") Integer id, @Valid @RequestBody NoteRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.addNote()`
- [ServiceRequestController.java:67](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L67) `@PutMapping("/{id}/warranty-card") @Operation(summary = "Đổi thẻ gắn với phiếu sang thẻ khác cùng đơn hàng khi đang kiểm tra") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> relinkCard(@PathVariable("id") Integer id, @Valid @RequestBody RelinkCardRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.relinkCard()`
- [ServiceRequestController.java:75](../../src/main/java/com/project/cybershop/aftersales/internal/controller/ServiceRequestController.java#L75) `@PostMapping("/{id}/replacement") @Operation(summary = "Đổi thiết bị sang serial khác và chuyển phiếu sang chờ trả") public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> replaceDevice(@PathVariable("id") Integer id, @Valid @RequestBody ReplacementRequest request)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `service.replaceDevice()`

## `aftersales/internal/controller/WarrantyCardController.java`

### WarrantyCardController — [WarrantyCardController.java:18](../../src/main/java/com/project/cybershop/aftersales/internal/controller/WarrantyCardController.java#L18)

Khai báo: `@RestController @RequestMapping("/api/warranty-cards") @Tag(name = "Warranty Cards", description = "API tra cứu thẻ bảo hành cho nhân viên") public class WarrantyCardController`

- Trường: `private final WarrantyCardService cardService;`
- Trường: `private final CurrentUser currentUser;`
- [WarrantyCardController.java:26](../../src/main/java/com/project/cybershop/aftersales/internal/controller/WarrantyCardController.java#L26) `public WarrantyCardController(WarrantyCardService cardService, CurrentUser currentUser)`
- [WarrantyCardController.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/controller/WarrantyCardController.java#L31) `@GetMapping @Operation(summary = "Tra cứu thẻ theo serial, mã đơn, số điện thoại hoặc mã phiếu (so khớp chính xác)") public ResponseEntity<ApiResponseDTO<List<WarrantyCardDto>>> search(@RequestParam("q") String query)`
  - Lời gọi xuất hiện: `currentUser.require()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`, `cardService.search()`

## `aftersales/internal/entity/Resolution.java`

### Resolution — [Resolution.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/entity/Resolution.java#L13)

Khai báo: `public enum Resolution`

- Trường: `REPAIRED`
- Trường: `REPLACED`
- Trường: `REJECTED`
- Trường: `NO_FAULT_FOUND`
- Trường: `WITHDRAWN`

### JpaConverter — [Resolution.java:32](../../src/main/java/com/project/cybershop/aftersales/internal/entity/Resolution.java#L32)

Khai báo: `@Converter public static class JpaConverter implements AttributeConverter<Resolution, String>`

- [Resolution.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/entity/Resolution.java#L35) `@Override public String convertToDatabaseColumn(Resolution resolution)`
  - Lời gọi xuất hiện: `resolution.code()`
- [Resolution.java:40](../../src/main/java/com/project/cybershop/aftersales/internal/entity/Resolution.java#L40) `@Override public Resolution convertToEntityAttribute(String value)`
  - Lời gọi xuất hiện: `valueOf()`, `value.toUpperCase()`

## `aftersales/internal/entity/ServiceIntake.java`

### ServiceIntake — [ServiceIntake.java:14](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceIntake.java#L14)

Khai báo: `@Entity @Table(name = "service_intakes") @Getter @Setter @NoArgsConstructor public class ServiceIntake`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "intake_code", nullable = false, length = 50, unique = true) private String intakeCode;`
- Trường: `@Column(name = "order_id", nullable = false) private Integer orderId;`
- Trường: `@Column(name = "user_id", nullable = false) private Integer userId;`
- Trường: `@Column(name = "created_by", nullable = false) private Integer createdBy;`
- Trường: `@Column(name = "created_at", nullable = false) private LocalDateTime createdAt;`
- Trường: `@Column(name = "returned_at") private LocalDateTime returnedAt;`
- Trường: `@Version @Column(name = "version", nullable = false) private Long version;`

## `aftersales/internal/entity/ServiceRequest.java`

### ServiceRequest — [ServiceRequest.java:11](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequest.java#L11)

Khai báo: `@Entity @Table(name = "service_requests") @Getter @Setter @NoArgsConstructor public class ServiceRequest`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "request_code", nullable = false, length = 30) private String requestCode;`
- Trường: `@Column(name = "user_id", nullable = false) private Integer userId;`
- Trường: `@Column(name = "warranty_card_id", nullable = false) private Integer warrantyCardId;`
- Trường: `@Column(name = "intake_id", nullable = false) private Integer intakeId;`
- Trường: `@Column(name = "contact_name", length = 100) private String contactName;`
- Trường: `@Column(name = "contact_phone", length = 20) private String contactPhone;`
- Trường: `@Column(name = "issue_description", nullable = false, columnDefinition = "TEXT") private String issueDescription;`
- Trường: `@Column(name = "condition_note", columnDefinition = "TEXT") private String conditionNote;`
- Trường: `@Column(name = "accessories_note", columnDefinition = "TEXT") private String accessoriesNote;`
- Trường: `@Column(name = "expected_return_date") private LocalDate expectedReturnDate;`
- Trường: `@Convert(converter = ServiceRequestStatus.JpaConverter.class) @Column(name = "status", nullable = false, length = 50) private ServiceRequestStatus status;`
- Trường: `@Convert(converter = Resolution.JpaConverter.class) @Column(name = "resolution", length = 30) private Resolution resolution;`
- Trường: `@Column(name = "assigned_technician_id") private Integer assignedTechnicianId;`
- Trường: `@Column(name = "technician_note", columnDefinition = "TEXT") private String technicianNote;`
- Trường: `@Column(name = "public_note", columnDefinition = "TEXT") private String publicNote;`
- Trường: `@Column(name = "created_at") private LocalDateTime createdAt;`
- Trường: `@Column(name = "completed_at") private LocalDateTime completedAt;`
- Trường: `@Version @Column(name = "version", nullable = false) private Long version;`

## `aftersales/internal/entity/ServiceRequestEvent.java`

### ServiceRequestEvent — [ServiceRequestEvent.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequestEvent.java#L13)

Khai báo: `@Entity @Table(name = "service_request_events") @Getter @Setter @NoArgsConstructor public class ServiceRequestEvent`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "service_request_id", nullable = false) private Integer serviceRequestId;`
- Trường: `@Column(name = "actor_id", nullable = false) private Integer actorId;`
- Trường: `@Column(name = "event_type", nullable = false, length = 50) private String eventType;`
- Trường: `@Convert(converter = ServiceRequestStatus.JpaConverter.class) @Column(name = "from_status", length = 50) private ServiceRequestStatus fromStatus;`
- Trường: `@Convert(converter = ServiceRequestStatus.JpaConverter.class) @Column(name = "to_status", nullable = false, length = 50) private ServiceRequestStatus toStatus;`
- Trường: `@Column(name = "internal_note", columnDefinition = "TEXT") private String internalNote;`
- Trường: `@Column(name = "public_note", columnDefinition = "TEXT") private String publicNote;`
- Trường: `@Column(name = "created_at", nullable = false) private LocalDateTime createdAt;`

## `aftersales/internal/entity/ServiceRequestStatus.java`

### ServiceRequestStatus — [ServiceRequestStatus.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequestStatus.java#L13)

Khai báo: `public enum ServiceRequestStatus`

- Trường: `RECEIVED`
- Trường: `INSPECTING`
- Trường: `REPAIRING`
- Trường: `WAITING_FOR_PARTS`
- Trường: `READY_FOR_RETURN`
- Trường: `RETURNED`
- Trường: `CANCELED`

### JpaConverter — [ServiceRequestStatus.java:33](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequestStatus.java#L33)

Khai báo: `@Converter public static class JpaConverter implements AttributeConverter<ServiceRequestStatus, String>`

- [ServiceRequestStatus.java:36](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequestStatus.java#L36) `@Override public String convertToDatabaseColumn(ServiceRequestStatus status)`
  - Lời gọi xuất hiện: `status.code()`
- [ServiceRequestStatus.java:41](../../src/main/java/com/project/cybershop/aftersales/internal/entity/ServiceRequestStatus.java#L41) `@Override public ServiceRequestStatus convertToEntityAttribute(String value)`
  - Lời gọi xuất hiện: `valueOf()`, `value.toUpperCase()`

## `aftersales/internal/entity/WarrantyCard.java`

### WarrantyCard — [WarrantyCard.java:10](../../src/main/java/com/project/cybershop/aftersales/internal/entity/WarrantyCard.java#L10)

Khai báo: `@Entity @Table(name = "warranty_cards") @Getter @Setter @NoArgsConstructor public class WarrantyCard`

- Trường: `public static final String ACTIVE = "active";`
- Trường: `public static final String VOID = "void";`
- Trường: `public static final String REPLACED = "replaced";`
- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "order_item_id", nullable = false) private Integer orderItemId;`
- Trường: `@Column(name = "serial_number", nullable = false, length = 100) private String serialNumber;`
- Trường: `@Column(name = "start_date", nullable = false) private LocalDateTime startDate;`
- Trường: `@Column(name = "end_date", nullable = false) private LocalDateTime endDate;`
- Trường: `@Column(name = "status", length = 50) private String status = ACTIVE;`
- Trường: `@Column(name = "replaces_card_id") private Integer replacesCardId;`

## `aftersales/internal/mapper/AftersalesMapper.java`

### AftersalesMapper — [AftersalesMapper.java:17](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L17)

Khai báo: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE) public interface AftersalesMapper`

- [AftersalesMapper.java:20](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L20) `ServiceRequestSummaryDto toSummary(ServiceRequest request)`
- [AftersalesMapper.java:22](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L22) `ServiceRequestDto toDetail(ServiceRequest request)`
- [AftersalesMapper.java:24](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L24) `ServiceRequestEventDto toDto(ServiceRequestEvent event)`
- [AftersalesMapper.java:26](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L26) `MyServiceRequestDto toMine(ServiceRequest request)`
- [AftersalesMapper.java:28](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L28) `@Mapping(target = "status", source = "toStatus") MyTimelineEntryDto toTimeline(ServiceRequestEvent event)`
- [AftersalesMapper.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L31) `default String code(ServiceRequestStatus status)`
  - Lời gọi xuất hiện: `status.code()`
- [AftersalesMapper.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/mapper/AftersalesMapper.java#L35) `default String code(Resolution resolution)`
  - Lời gọi xuất hiện: `resolution.code()`

## `aftersales/internal/repository/OrderLookup.java`

### OrderLookup — [OrderLookup.java:15](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L15)

Khai báo: `@Repository public class OrderLookup`

- Trường: `private static final String FROM = """ FROM warranty_cards wc JOIN order_items oi ON oi.id = wc.order_item_id JOIN product_variants pv ON pv.id = oi.product_variant_id JOIN orders o ON o.id = oi.order_id JOIN users u ON u.id = o.user_id """;`
- Trường: `private final EntityManager em;`
- [OrderLookup.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L35) `public OrderLookup(EntityManager em)`
- [OrderLookup.java:39](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L39) `public Map<Integer, OrderInfo> byCardIds(Collection<Integer> cardIds)`
  - Lời gọi xuất hiện: `cardIds.isEmpty()`, `em.createNativeQuery(""" SELECT wc.id AS card_id, oi.id AS order_item_id, oi.product_name, pv.variant_name, pv.serial_code, oi.warranty_period_months, o.id AS order_id, o.order_code, o.pickup_name, o.pickup_phone, u.id AS user_id, u.full_name, u.phone, oi.pc_custom_id, pv.id AS variant_id """ + FROM + "WHERE wc.id IN (:ids)") .setParameter("ids", cardIds).getResultList()`, `em.createNativeQuery(""" SELECT wc.id AS card_id, oi.id AS order_item_id, oi.product_name, pv.variant_name, pv.serial_code, oi.warranty_period_months, o.id AS order_id, o.order_code, o.pickup_name, o.pickup_phone, u.id AS user_id, u.full_name, u.phone, oi.pc_custom_id, pv.id AS variant_id """ + FROM + "WHERE wc.id IN (:ids)").setParameter()`, `em.createNativeQuery()`, `number()`, `result.put()`, `info.cardId()`
- [OrderLookup.java:62](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L62) `public List<Integer> searchCardIds(String query)`
  - Lời gọi xuất hiện: `ids()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + """ WHERE upper(wc.serial_number) = :q OR upper(o.order_code) = :q OR u.phone = :q OR o.pickup_phone = :q OR EXISTS (SELECT 1 FROM service_requests sr WHERE sr.warranty_card_id = wc.id AND upper(sr.request_code) = :q) OR EXISTS (SELECT 1 FROM service_requests sr JOIN service_intakes si ON si.id = sr.intake_id WHERE sr.warranty_card_id = wc.id AND upper(si.intake_code) = :q) ORDER BY wc.id """) .setParameter("q", query.trim().toUpperCase(Locale.ROOT)).getResultList()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + """ WHERE upper(wc.serial_number) = :q OR upper(o.order_code) = :q OR u.phone = :q OR o.pickup_phone = :q OR EXISTS (SELECT 1 FROM service_requests sr WHERE sr.warranty_card_id = wc.id AND upper(sr.request_code) = :q) OR EXISTS (SELECT 1 FROM service_requests sr JOIN service_intakes si ON si.id = sr.intake_id WHERE sr.warranty_card_id = wc.id AND upper(si.intake_code) = :q) ORDER BY wc.id """).setParameter()`, `em.createNativeQuery()`, `query.trim().toUpperCase()`, `query.trim()`
- [OrderLookup.java:77](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L77) `public List<Integer> cardIdsOfOrders(Collection<Integer> orderIds)`
  - Lời gọi xuất hiện: `orderIds.isEmpty()`, `List.of()`, `ids()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + "WHERE o.id IN (:ids) ORDER BY wc.id") .setParameter("ids", orderIds).getResultList()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + "WHERE o.id IN (:ids) ORDER BY wc.id").setParameter()`, `em.createNativeQuery()`
- [OrderLookup.java:86](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L86) `public List<Integer> cardIdsOfUser(Integer userId)`
  - Lời gọi xuất hiện: `ids()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + "WHERE o.user_id = :userId ORDER BY wc.id DESC") .setParameter("userId", userId).getResultList()`, `em.createNativeQuery("SELECT wc.id AS card_id " + FROM + "WHERE o.user_id = :userId ORDER BY wc.id DESC").setParameter()`, `em.createNativeQuery()`
- [OrderLookup.java:92](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L92) `private static List<Integer> ids(List<?> rows)`
  - Lời gọi xuất hiện: `rows.stream().map(OrderLookup::number).toList()`, `rows.stream().map()`, `rows.stream()`
- [OrderLookup.java:96](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L96) `private static Integer number(Object value)`
  - Lời gọi xuất hiện: `((Number) value).intValue()`

### OrderInfo — [OrderLookup.java:18](../../src/main/java/com/project/cybershop/aftersales/internal/repository/OrderLookup.java#L18)

Khai báo: `public record OrderInfo(Integer cardId, Integer orderItemId, String productName, String variantName, String sku, Integer warrantyPeriodMonths, Integer orderId, String orderCode, String pickupName, String pickupPhone, Integer userId, String customerName, String customerPhone, Integer pcCustomId, Integer variantId)`


## `aftersales/internal/repository/ServiceIntakeRepository.java`

### ServiceIntakeRepository — [ServiceIntakeRepository.java:9](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceIntakeRepository.java#L9)

Khai báo: `@Repository public interface ServiceIntakeRepository extends JpaRepository<ServiceIntake, Integer>`

- [ServiceIntakeRepository.java:12](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceIntakeRepository.java#L12) `List<ServiceIntake> findByUserIdOrderByCreatedAtDescIdDesc(Integer userId)`

## `aftersales/internal/repository/ServiceRequestEventRepository.java`

### ServiceRequestEventRepository — [ServiceRequestEventRepository.java:10](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestEventRepository.java#L10)

Khai báo: `@Repository public interface ServiceRequestEventRepository extends JpaRepository<ServiceRequestEvent, Integer>`

- [ServiceRequestEventRepository.java:12](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestEventRepository.java#L12) `List<ServiceRequestEvent> findByServiceRequestIdOrderByCreatedAtAscIdAsc(Integer serviceRequestId)`
- [ServiceRequestEventRepository.java:14](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestEventRepository.java#L14) `List<ServiceRequestEvent> findByServiceRequestIdInOrderByCreatedAtAscIdAsc(Collection<Integer> serviceRequestIds)`

## `aftersales/internal/repository/ServiceRequestRepository.java`

### ServiceRequestRepository — [ServiceRequestRepository.java:11](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L11)

Khai báo: `@Repository public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer>`

- [ServiceRequestRepository.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L13) `List<ServiceRequest> findAllByOrderByCreatedAtDescIdDesc()`
- [ServiceRequestRepository.java:15](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L15) `List<ServiceRequest> findByStatusOrderByCreatedAtDescIdDesc(ServiceRequestStatus status)`
- [ServiceRequestRepository.java:17](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L17) `List<ServiceRequest> findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(Collection<Integer> cardIds)`
- [ServiceRequestRepository.java:19](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L19) `boolean existsByWarrantyCardIdAndStatusNotIn(Integer cardId, Collection<ServiceRequestStatus> statuses)`
- [ServiceRequestRepository.java:21](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L21) `List<ServiceRequest> findByIntakeIdOrderByIdAsc(Integer intakeId)`
- [ServiceRequestRepository.java:23](../../src/main/java/com/project/cybershop/aftersales/internal/repository/ServiceRequestRepository.java#L23) `List<ServiceRequest> findByIntakeIdInOrderByIdAsc(Collection<Integer> intakeIds)`

## `aftersales/internal/repository/WarrantyCardRepository.java`

### WarrantyCardRepository — [WarrantyCardRepository.java:10](../../src/main/java/com/project/cybershop/aftersales/internal/repository/WarrantyCardRepository.java#L10)

Khai báo: `@Repository public interface WarrantyCardRepository extends JpaRepository<WarrantyCard, Integer>`

- [WarrantyCardRepository.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/repository/WarrantyCardRepository.java#L13) `List<WarrantyCard> findByReplacesCardIdIn(Collection<Integer> cardIds)`
- [WarrantyCardRepository.java:15](../../src/main/java/com/project/cybershop/aftersales/internal/repository/WarrantyCardRepository.java#L15) `boolean existsBySerialNumberIgnoreCase(String serialNumber)`

## `aftersales/internal/service/Actor.java`

### Actor — [Actor.java:4](../../src/main/java/com/project/cybershop/aftersales/internal/service/Actor.java#L4)

Khai báo: `public enum Actor`

- Trường: `STAFF`
- Trường: `TECHNICIAN`
- Trường: `ADMIN`
- Trường: `CUSTOMER`

## `aftersales/internal/service/CurrentUser.java`

### CurrentUser — [CurrentUser.java:18](../../src/main/java/com/project/cybershop/aftersales/internal/service/CurrentUser.java#L18)

Khai báo: `@Component public class CurrentUser`

- Trường: `private final IdentityApi identityApi;`
- [CurrentUser.java:26](../../src/main/java/com/project/cybershop/aftersales/internal/service/CurrentUser.java#L26) `public CurrentUser(IdentityApi identityApi)`
- [CurrentUser.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/service/CurrentUser.java#L31) `public Principal require(Actor... allowed)`
  - Lời gọi xuất hiện: `SecurityContextHolder.getContext().getAuthentication()`, `SecurityContextHolder.getContext()`, `auth.isAuthenticated()`, `identityApi.getUserByEmail(auth.getName()) .filter(u -> Boolean.TRUE.equals(u.getIsActive())).orElseThrow()`, `identityApi.getUserByEmail(auth.getName()).filter()`, `identityApi.getUserByEmail()`, `auth.getName()`, `Boolean.TRUE.equals()`, `u.getIsActive()`, `actorOf()`, `user.getRole()`, `List.of(allowed).contains()`, `List.of()`, `user.getId()`, `user.getFullName()`
- [CurrentUser.java:46](../../src/main/java/com/project/cybershop/aftersales/internal/service/CurrentUser.java#L46) `private static Actor actorOf(String role)`
  - Lời gọi xuất hiện: `Actor.valueOf()`, `role.toUpperCase()`

### Principal — [CurrentUser.java:21](../../src/main/java/com/project/cybershop/aftersales/internal/service/CurrentUser.java#L21)

Khai báo: `public record Principal(Integer id, String name, Actor actor)`


## `aftersales/internal/service/impl/PickupWarrantyServiceImpl.java`

### PickupWarrantyServiceImpl — [PickupWarrantyServiceImpl.java:13](../../src/main/java/com/project/cybershop/aftersales/internal/service/impl/PickupWarrantyServiceImpl.java#L13)

Khai báo: `@Service @Transactional public class PickupWarrantyServiceImpl implements PickupWarrantyService`

- Trường: `private final JdbcTemplate jdbc;`
- [PickupWarrantyServiceImpl.java:17](../../src/main/java/com/project/cybershop/aftersales/internal/service/impl/PickupWarrantyServiceImpl.java#L17) `public PickupWarrantyServiceImpl(JdbcTemplate jdbc)`
- [PickupWarrantyServiceImpl.java:19](../../src/main/java/com/project/cybershop/aftersales/internal/service/impl/PickupWarrantyServiceImpl.java#L19) `@Override public void issue(List<Unit> units, LocalDateTime start)`
  - Lời gọi xuất hiện: `jdbc.queryForObject()`, `unit.serialNumber()`, `jdbc.update()`, `unit.orderItemId()`, `start.plusMonths()`, `unit.warrantyMonths()`
- [PickupWarrantyServiceImpl.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/service/impl/PickupWarrantyServiceImpl.java#L31) `@Override @Transactional(readOnly = true) public List<Card> forCustomer(Integer userId)`
  - Lời gọi xuất hiện: `jdbc.query()`, `rs.getString()`, `rs.getInt()`, `rs.getObject()`

## `aftersales/internal/service/ServiceIntakeService.java`

### ServiceIntakeService — [ServiceIntakeService.java:38](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L38)

Khai báo: `@Service public class ServiceIntakeService`

- Trường: `private static final SecureRandom RANDOM = new SecureRandom();`
- Trường: `private static final String NO_FAULT_NOTE = "Linh kiện hoạt động bình thường, không phát hiện lỗi.";`
- Trường: `private final ServiceIntakeRepository intakes;`
- Trường: `private final ServiceRequestRepository requests;`
- Trường: `private final ServiceRequestService requestService;`
- Trường: `private final WarrantyCardService cardService;`
- Trường: `private final OrderLookup orderLookup;`
- [ServiceIntakeService.java:50](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L50) `public ServiceIntakeService(ServiceIntakeRepository intakes, ServiceRequestRepository requests, ServiceRequestService requestService, WarrantyCardService cardService, OrderLookup orderLookup)`
- [ServiceIntakeService.java:62](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L62) `@Transactional public ServiceIntakeDto create(IntakeCreateRequest req, Principal actor)`
  - Lời gọi xuất hiện: `WarrantyRules.now()`, `now.toLocalDate()`, `req.getExpectedReturnDate().isBefore()`, `req.getExpectedReturnDate()`, `req.getWarrantyCardIds().stream().distinct().toList()`, `req.getWarrantyCardIds().stream().distinct()`, `req.getWarrantyCardIds().stream()`, `req.getWarrantyCardIds()`, `req.getOutOfWarrantyCardIds()`, `List.of()`, `req.getOutOfWarrantyCardIds().stream().distinct().filter(id -> !cardIds.contains(id)).toList()`, `req.getOutOfWarrantyCardIds().stream().distinct().filter()`, `req.getOutOfWarrantyCardIds().stream().distinct()`, `req.getOutOfWarrantyCardIds().stream()`, `cardIds.contains()`, `Stream.concat(cardIds.stream(), extraIds.stream()).toList()`, `Stream.concat()`, `cardIds.stream()`, `extraIds.stream()`, `orderLookup.byCardIds()`, `allIds.stream().map(id -> { OrderInfo info = orders.get(id); if (info == null) { throw new BusinessException("Thẻ bảo hành chưa gắn với đơn hàng hợp lệ."); } return info.orderId(); }).collect()`, `allIds.stream().map()`, `allIds.stream()`, `orders.get()`, `info.orderId()`, `Collectors.toSet()`, `orderIds.size()`, `cardIds.stream().map(requestService::card).toList()`, `cardIds.stream().map()`, `WarrantyRules.isValidOn()`, `card.getSerialNumber()`, `req.getAccessoriesNote().trim()`, `req.getAccessoriesNote()`, `extraIds.isEmpty()`, `extraIds.stream().map(requestService::card) .map(WarrantyCard::getSerialNumber).collect()`, `extraIds.stream().map(requestService::card).map()`, `extraIds.stream().map()`, `Collectors.joining()`, `cardIds.get()`, `intake.setIntakeCode()`, `WarrantyRules.newRequestCode()`, `intake.setOrderId()`, `order.orderId()`, `intake.setUserId()`, `order.userId()`, `intake.setCreatedBy()`, `actor.id()`, `intake.setCreatedAt()`, `save()`, `child()`, `request.setContactName()`, `req.getContactName().trim()`, `req.getContactName()`, `request.setContactPhone()`, `req.getContactPhone().trim()`, `req.getContactPhone()`, `request.setIssueDescription()`, `req.getIssueDescription().trim()`, `req.getIssueDescription()`, `request.setConditionNote()`, `req.getConditionNote().trim()`, `req.getConditionNote()`, `request.setAccessoriesNote()`, `request.setExpectedReturnDate()`, `request.setStatus()`, `request.setCreatedAt()`, `requestService.open()`, `intake.getIntakeCode()`, `detail()`
- [ServiceIntakeService.java:123](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L123) `@Transactional(readOnly = true) public ServiceIntakeDto get(Integer id, Principal actor)`
  - Lời gọi xuất hiện: `detail()`, `load()`
- [ServiceIntakeService.java:129](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L129) `@Transactional public ServiceIntakeDto returnAll(Integer id, NoteRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `parts()`, `WarrantyRules.canReturn()`, `statuses()`, `part.getStatus()`, `requestService.move()`, `req.getInternalNote()`, `req.getPublicNote()`, `intake.setReturnedAt()`, `WarrantyRules.now()`, `save()`, `detail()`
- [ServiceIntakeService.java:147](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L147) `@Transactional public ServiceIntakeDto cancel(Integer id, NoteRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `parts()`, `statuses()`, `canCancel()`, `part.getStatus()`, `requestService.move()`, `req.getInternalNote()`, `req.getPublicNote()`, `detail()`
- [ServiceIntakeService.java:167](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L167) `@Transactional public ServiceIntakeDto addComponent(Integer id, RelinkCardRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `parts()`, `intake.getReturnedAt()`, `parts.stream().noneMatch()`, `parts.stream()`, `p.getStatus().isOpen()`, `p.getStatus()`, `requestService.card()`, `req.getWarrantyCardId()`, `requestService.orderInfo(card).orderId().equals()`, `requestService.orderInfo(card).orderId()`, `requestService.orderInfo()`, `intake.getOrderId()`, `WarrantyRules.isValidOn()`, `intake.getCreatedAt().toLocalDate()`, `intake.getCreatedAt()`, `parts.get()`, `child()`, `parts.size()`, `request.setContactName()`, `first.getContactName()`, `request.setContactPhone()`, `first.getContactPhone()`, `request.setIssueDescription()`, `first.getIssueDescription()`, `request.setConditionNote()`, `first.getConditionNote()`, `request.setAccessoriesNote()`, `first.getAccessoriesNote()`, `request.setExpectedReturnDate()`, `first.getExpectedReturnDate()`, `request.setCreatedAt()`, `request.setStatus()`, `request.setAssignedTechnicianId()`, `actor.id()`, `requestService.open()`, `card.getSerialNumber()`, `req.getInternalNote().trim()`, `req.getInternalNote()`, `WarrantyRules.now()`, `detail()`
- [ServiceIntakeService.java:199](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L199) `@Transactional public ServiceIntakeDto noFaultRest(Integer id, NoteRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `parts(intake).stream() .filter(p -> p.getStatus() == RECEIVED || p.getStatus() == INSPECTING).toList()`, `parts(intake).stream().filter()`, `parts(intake).stream()`, `parts()`, `p.getStatus()`, `rest.isEmpty()`, `req.getPublicNote()`, `req.getPublicNote().isBlank()`, `part.getStatus()`, `requestService.move()`, `req.getInternalNote()`, `detail()`
- [ServiceIntakeService.java:220](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L220) `private ServiceIntake load(Integer id)`
  - Lời gọi xuất hiện: `intakes.findById(id).orElseThrow()`, `intakes.findById()`
- [ServiceIntakeService.java:225](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L225) `private List<ServiceRequest> parts(ServiceIntake intake)`
  - Lời gọi xuất hiện: `requests.findByIntakeIdOrderByIdAsc()`, `intake.getId()`
- [ServiceIntakeService.java:229](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L229) `private static List<ServiceRequestStatus> statuses(List<ServiceRequest> parts)`
  - Lời gọi xuất hiện: `parts.stream().map(ServiceRequest::getStatus).toList()`, `parts.stream().map()`, `parts.stream()`
- [ServiceIntakeService.java:233](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L233) `private static boolean canCancel(List<ServiceRequestStatus> statuses)`
  - Lời gọi xuất hiện: `statuses.contains()`, `statuses.stream().allMatch()`, `statuses.stream()`
- [ServiceIntakeService.java:237](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L237) `private static ServiceRequest child(ServiceIntake intake, WarrantyCard card, int index)`
  - Lời gọi xuất hiện: `request.setRequestCode()`, `WarrantyRules.childCode()`, `intake.getIntakeCode()`, `request.setIntakeId()`, `intake.getId()`, `request.setUserId()`, `intake.getUserId()`, `request.setWarrantyCardId()`, `card.getId()`, `request.setPublicNote()`
- [ServiceIntakeService.java:247](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L247) `private void save(ServiceIntake intake)`
  - Lời gọi xuất hiện: `intakes.saveAndFlush()`
- [ServiceIntakeService.java:255](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceIntakeService.java#L255) `private ServiceIntakeDto detail(ServiceIntake intake, Principal actor)`
  - Lời gọi xuất hiện: `parts()`, `statuses()`, `parts.get()`, `orderLookup.byCardIds(List.of(first.getWarrantyCardId())).get()`, `orderLookup.byCardIds()`, `List.of()`, `first.getWarrantyCardId()`, `actor.actor()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .version(intake.getVersion()) .requests(cardService.summaries(parts)) .canReturn(counter && WarrantyRules.canReturn(statuses)) .canCancel(counter && canCancel(statuses)) .canAddComponent(technical && intake.getReturnedAt() == null && statuses.stream().anyMatch(ServiceRequestStatus::isOpen)).build()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .version(intake.getVersion()) .requests(cardService.summaries(parts)) .canReturn(counter && WarrantyRules.canReturn(statuses)) .canCancel(counter && canCancel(statuses)).canAddComponent()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .version(intake.getVersion()) .requests(cardService.summaries(parts)) .canReturn(counter && WarrantyRules.canReturn(statuses)).canCancel()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .version(intake.getVersion()) .requests(cardService.summaries(parts)).canReturn()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .version(intake.getVersion()).requests()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()).version()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()) .createdAt(intake.getCreatedAt()).returnedAt()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()) .expectedReturnDate(first.getExpectedReturnDate()).createdAt()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()) .accessoriesNote(first.getAccessoriesNote()).expectedReturnDate()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()) .conditionNote(first.getConditionNote()).accessoriesNote()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()) .issueDescription(first.getIssueDescription()).conditionNote()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()) .contactPhone(first.getContactPhone()).issueDescription()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()) .contactName(first.getContactName()).contactPhone()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()) .status(WarrantyRules.intakeStatus(statuses).code()).contactName()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()) .customerName(order == null ? null : order.customerName()).status()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()) .orderCode(order == null ? null : order.orderCode()).customerName()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()) .orderId(intake.getOrderId()).orderCode()`, `ServiceIntakeDto.builder() .id(intake.getId()) .intakeCode(intake.getIntakeCode()).orderId()`, `ServiceIntakeDto.builder() .id(intake.getId()).intakeCode()`, `ServiceIntakeDto.builder().id()`, `ServiceIntakeDto.builder()`, `intake.getId()`, `intake.getIntakeCode()`, `intake.getOrderId()`, `order.orderCode()`, `order.customerName()`, `WarrantyRules.intakeStatus(statuses).code()`, `WarrantyRules.intakeStatus()`, `first.getContactName()`, `first.getContactPhone()`, `first.getIssueDescription()`, `first.getConditionNote()`, `first.getAccessoriesNote()`, `first.getExpectedReturnDate()`, `intake.getCreatedAt()`, `intake.getReturnedAt()`, `intake.getVersion()`, `cardService.summaries()`, `WarrantyRules.canReturn()`, `canCancel()`, `statuses.stream().anyMatch()`, `statuses.stream()`

## `aftersales/internal/service/ServiceRequestService.java`

### ServiceRequestService — [ServiceRequestService.java:49](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L49)

Khai báo: `@Service public class ServiceRequestService`

- Trường: `private static final List<ServiceRequestStatus> CLOSED = List.of(RETURNED, CANCELED);`
- Trường: `private static final Set<ServiceRequestStatus> IN_PROGRESS = EnumSet.of(INSPECTING, REPAIRING, WAITING_FOR_PARTS);`
- Trường: `private static final String OPEN_EXISTS = "Thiết bị đã có phiếu chưa đóng.";`
- Trường: `static final String STALE = "Phiếu đã được người khác cập nhật, vui lòng tải lại.";`
- Trường: `private static final String RETURN_BY_INTAKE = "Trả máy theo cả lần tiếp nhận để giao đủ bộ máy cho khách.";`
- Trường: `private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");`
- Trường: `private static final SecureRandom RANDOM = new SecureRandom();`
- Trường: `private final ServiceRequestRepository requests;`
- Trường: `private final ServiceRequestEventRepository events;`
- Trường: `private final WarrantyCardRepository cards;`
- Trường: `private final OrderLookup orderLookup;`
- Trường: `private final WarrantyCardService cardService;`
- Trường: `private final AftersalesMapper mapper;`
- Trường: `private final IdentityApi identityApi;`
- Trường: `private final ServiceIntakeRepository intakes;`
- Trường: `private final CatalogInternalApi catalogApi;`
- [ServiceRequestService.java:70](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L70) `public ServiceRequestService(ServiceRequestRepository requests, ServiceRequestEventRepository events, WarrantyCardRepository cards, OrderLookup orderLookup, WarrantyCardService cardService, AftersalesMapper mapper, IdentityApi identityApi, ServiceIntakeRepository intakes, CatalogInternalApi catalogApi)`
- [ServiceRequestService.java:90](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L90) `@Transactional(readOnly = true) public List<ServiceRequestSummaryDto> list(String status)`
  - Lời gọi xuất hiện: `status.isBlank()`, `requests.findAllByOrderByCreatedAtDescIdDesc()`, `requests.findByStatusOrderByCreatedAtDescIdDesc()`, `ServiceRequestStatus.fromCode()`, `cardService.summaries()`
- [ServiceRequestService.java:99](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L99) `@Transactional(readOnly = true) public ServiceRequestDto get(Integer id, Principal actor)`
  - Lời gọi xuất hiện: `detail()`, `load()`
- [ServiceRequestService.java:104](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L104) `@Transactional public ServiceRequestDto transition(Integer id, TransitionRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `req.getVersion()`, `req.getVersion().equals()`, `request.getVersion()`, `ServiceRequestStatus.fromCode()`, `req.getToStatus()`, `move()`, `Resolution.fromCodeOrNull()`, `req.getResolution()`, `req.getInternalNote()`, `req.getPublicNote()`, `req.getExpectedReturnDate()`, `detail()`
- [ServiceRequestService.java:120](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L120) `void move(ServiceRequest request, ServiceRequestStatus to, Resolution resolution, String internalNoteText, String publicNoteText, LocalDate expectedReturnDate, Principal actor)`
  - Lời gọi xuất hiện: `request.getStatus()`, `TransitionPolicy.check()`, `actor.actor()`, `card()`, `request.getWarrantyCardId()`, `WarrantyRules.isValidOn()`, `receivedDay()`, `reinspectsReplaced()`, `internalNoteText.trim()`, `blankToNull()`, `WarrantyRules.now()`, `request.setAssignedTechnicianId()`, `actor.id()`, `request.setResolution()`, `request.setCompletedAt()`, `request.getResolution()`, `WarrantyRules.extensionDays()`, `request.getCreatedAt()`, `request.getCompletedAt()`, `card.getEndDate()`, `card.setEndDate()`, `oldEnd.plusDays()`, `oldEnd.format()`, `card.getEndDate().format()`, `request.setStatus()`, `apply()`, `save()`, `log()`
- [ServiceRequestService.java:169](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L169) `void open(ServiceRequest request, Principal actor, String internalNote, LocalDateTime now)`
  - Lời gọi xuất hiện: `requests.existsByWarrantyCardIdAndStatusNotIn()`, `request.getWarrantyCardId()`, `save()`, `log()`, `request.getPublicNote()`
- [ServiceRequestService.java:177](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L177) `@Transactional public ServiceRequestDto addNote(Integer id, NoteRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `IN_PROGRESS.contains()`, `request.getStatus()`, `req.getInternalNote().trim()`, `req.getInternalNote()`, `blankToNull()`, `req.getPublicNote()`, `apply()`, `req.getExpectedReturnDate()`, `save()`, `log()`, `WarrantyRules.now()`, `detail()`
- [ServiceRequestService.java:192](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L192) `@Transactional public ServiceRequestDto relinkCard(Integer id, RelinkCardRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `request.getStatus()`, `card()`, `request.getWarrantyCardId()`, `req.getWarrantyCardId()`, `target.getId().equals()`, `target.getId()`, `current.getId()`, `WarrantyCard.ACTIVE.equals()`, `target.getStatus()`, `orderLookup.byCardIds()`, `List.of()`, `orders.get()`, `currentOrder.orderId().equals()`, `currentOrder.orderId()`, `targetOrder.orderId()`, `requests.existsByWarrantyCardIdAndStatusNotIn()`, `current.getSerialNumber()`, `target.getSerialNumber()`, `req.getInternalNote().trim()`, `req.getInternalNote()`, `request.setWarrantyCardId()`, `apply()`, `save()`, `log()`, `WarrantyRules.now()`, `detail()`
- [ServiceRequestService.java:226](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L226) `@Transactional public ServiceRequestDto replaceDevice(Integer id, ReplacementRequest req, Principal actor)`
  - Lời gọi xuất hiện: `load()`, `request.getStatus()`, `card()`, `request.getWarrantyCardId()`, `WarrantyRules.isValidOn()`, `receivedDay()`, `WarrantyRules.normalizeSerial()`, `req.getNewSerialNumber()`, `cards.existsBySerialNumberIgnoreCase()`, `orderInfo()`, `order.warrantyPeriodMonths()`, `WarrantyRules.now()`, `replacement.setOrderItemId()`, `old.getOrderItemId()`, `replacement.setSerialNumber()`, `replacement.setStartDate()`, `replacement.setEndDate()`, `now.plusMonths()`, `replacement.setReplacesCardId()`, `old.getId()`, `cards.saveAndFlush()`, `"stock".equals()`, `req.getSource()`, `catalogApi.deductStock()`, `order.variantId()`, `old.setStatus()`, `old.getSerialNumber()`, `req.getInternalNote().trim()`, `req.getInternalNote()`, `request.setStatus()`, `request.setResolution()`, `request.setCompletedAt()`, `apply()`, `req.getPublicNote().trim()`, `req.getPublicNote()`, `save()`, `log()`, `request.getPublicNote()`, `detail()`
- [ServiceRequestService.java:280](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L280) `private ServiceRequest load(Integer id)`
  - Lời gọi xuất hiện: `requests.findById(id).orElseThrow()`, `requests.findById()`
- [ServiceRequestService.java:285](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L285) `WarrantyCard card(Integer id)`
  - Lời gọi xuất hiện: `cards.findById(id).orElseThrow()`, `cards.findById()`
- [ServiceRequestService.java:290](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L290) `OrderInfo orderInfo(WarrantyCard card)`
  - Lời gọi xuất hiện: `orderLookup.byCardIds(List.of(card.getId())).get()`, `orderLookup.byCardIds()`, `List.of()`, `card.getId()`
- [ServiceRequestService.java:298](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L298) `static LocalDate receivedDay(ServiceRequest request)`
  - Lời gọi xuất hiện: `(request.getCreatedAt() == null ? WarrantyRules.now() : request.getCreatedAt()).toLocalDate()`, `request.getCreatedAt()`, `WarrantyRules.now()`
- [ServiceRequestService.java:302](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L302) `private static String blankToNull(String text)`
  - Lời gọi xuất hiện: `text.isBlank()`, `text.trim()`
- [ServiceRequestService.java:307](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L307) `private static boolean reinspectsReplaced(ServiceRequest request, ServiceRequestStatus to)`
  - Lời gọi xuất hiện: `request.getStatus()`, `request.getResolution()`
- [ServiceRequestService.java:313](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L313) `private static void apply(ServiceRequest request, String internalNote, String publicNote, LocalDate expectedReturnDate)`
  - Lời gọi xuất hiện: `request.setTechnicianNote()`, `request.setPublicNote()`, `expectedReturnDate.equals()`, `request.getExpectedReturnDate()`, `expectedReturnDate.isBefore()`, `WarrantyRules.now().toLocalDate()`, `WarrantyRules.now()`, `request.setExpectedReturnDate()`
- [ServiceRequestService.java:327](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L327) `private void save(ServiceRequest request)`
  - Lời gọi xuất hiện: `requests.saveAndFlush()`, `String.valueOf(e.getMostSpecificCause().getMessage()).contains()`, `String.valueOf()`, `e.getMostSpecificCause().getMessage()`, `e.getMostSpecificCause()`
- [ServiceRequestService.java:345](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L345) `private void log(ServiceRequest request, Principal actor, ServiceRequestStatus from, String internalNote, String publicNote, LocalDateTime at)`
  - Lời gọi xuất hiện: `event.setServiceRequestId()`, `request.getId()`, `event.setActorId()`, `actor.id()`, `event.setEventType()`, `request.getStatus()`, `event.setFromStatus()`, `event.setToStatus()`, `event.setInternalNote()`, `event.setPublicNote()`, `event.setCreatedAt()`, `events.save()`
- [ServiceRequestService.java:359](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L359) `private ServiceRequestDto detail(ServiceRequest request, Principal actor)`
  - Lời gọi xuất hiện: `mapper.toDetail()`, `intakes.findById(request.getIntakeId()).ifPresent()`, `intakes.findById()`, `request.getIntakeId()`, `dto.setIntakeCode()`, `intake.getIntakeCode()`, `dto.setCard()`, `cardService.toDtos(List.of(card(request.getWarrantyCardId()))).get()`, `cardService.toDtos()`, `List.of()`, `card()`, `request.getWarrantyCardId()`, `events.findByServiceRequestIdOrderByCreatedAtAscIdAsc()`, `request.getId()`, `dto.setEvents()`, `history.stream().map(event -> { ServiceRequestEventDto eventDto = mapper.toDto(event); eventDto.setActorName(names.computeIfAbsent(event.getActorId(), this::userName)); return eventDto; }).toList()`, `history.stream().map()`, `history.stream()`, `mapper.toDto()`, `eventDto.setActorName()`, `names.computeIfAbsent()`, `event.getActorId()`, `request.getAssignedTechnicianId()`, `dto.setAssignedTechnicianName()`, `request.getStatus()`, `dto.setAllowedTransitions()`, `TransitionPolicy.allowedFor(status, actor.actor()).stream() // Trả máy làm ở cấp cả lần tiếp nhận (ServiceIntakeService.returnAll). .filter(rule -> rule.to() != RETURNED && !reinspectsReplaced(request, rule.to())) .map(rule -> new AllowedTransitionDto(rule.to().code(), rule.resolutions().stream().map(Resolution::code).toList())).toList()`, `TransitionPolicy.allowedFor(status, actor.actor()).stream() // Trả máy làm ở cấp cả lần tiếp nhận (ServiceIntakeService.returnAll). .filter(rule -> rule.to() != RETURNED && !reinspectsReplaced(request, rule.to())).map()`, `TransitionPolicy.allowedFor(status, actor.actor()).stream().filter()`, `TransitionPolicy.allowedFor(status, actor.actor()).stream()`, `TransitionPolicy.allowedFor()`, `actor.actor()`, `rule.to()`, `reinspectsReplaced()`, `rule.to().code()`, `rule.resolutions().stream().map(Resolution::code).toList()`, `rule.resolutions().stream().map()`, `rule.resolutions().stream()`, `rule.resolutions()`, `dto.setCanAddNote()`, `IN_PROGRESS.contains()`, `dto.setCanRelinkCard()`, `dto.setCanReplaceDevice()`, `dto.isCanReplaceDevice()`, `dto.setReplacementInStock()`, `catalogApi.isVariantInStock()`, `orderInfo(card(request.getWarrantyCardId())).variantId()`, `orderInfo()`
- [ServiceRequestService.java:393](../../src/main/java/com/project/cybershop/aftersales/internal/service/ServiceRequestService.java#L393) `private String userName(Integer userId)`
  - Lời gọi xuất hiện: `identityApi.getUserById(userId).map(UserDto::getFullName).orElse()`, `identityApi.getUserById(userId).map()`, `identityApi.getUserById()`

## `aftersales/internal/service/TransitionPolicy.java`

### TransitionPolicy — [TransitionPolicy.java:19](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L19)

Khai báo: `public final class TransitionPolicy`

- Trường: `private static final List<Rule> RULES = List.of( rule(RECEIVED, INSPECTING, TECHNICIAN), rule(RECEIVED, CANCELED, STAFF), rule(INSPECTING, REPAIRING, TECHNICIAN), rule(INSPECTING, READY_FOR_RETURN, TECHNICIAN, REJECTED, NO_FAULT_FOUND, WITHDRAWN), rule(INSPECTING, CANCELED, TECHNICIAN), rule(REPAIRING, READY_FOR_RETURN, TECHNICIAN, REPAIRED, WIT...`
- [TransitionPolicy.java:41](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L41) `private TransitionPolicy()`
- [TransitionPolicy.java:44](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L44) `private static Rule rule(ServiceRequestStatus from, ServiceRequestStatus to, Actor actor, Resolution... resolutions)`
  - Lời gọi xuất hiện: `List.of()`
- [TransitionPolicy.java:49](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L49) `public static List<Rule> allowedFor(ServiceRequestStatus from, Actor actor)`
  - Lời gọi xuất hiện: `RULES.stream().filter(r -> r.from() == from && r.allows(actor)).toList()`, `RULES.stream().filter()`, `RULES.stream()`, `r.from()`, `r.allows()`
- [TransitionPolicy.java:53](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L53) `public static void check(ServiceRequestStatus from, ServiceRequestStatus to, Resolution resolution, Actor actor)`
  - Lời gọi xuất hiện: `RULES.stream() .filter(r -> r.from() == from && r.to() == to) .findFirst().orElseThrow()`, `RULES.stream() .filter(r -> r.from() == from && r.to() == to).findFirst()`, `RULES.stream().filter()`, `RULES.stream()`, `r.from()`, `r.to()`, `rule.allows()`, `rule.resolutions().isEmpty()`, `rule.resolutions()`, `rule.resolutions().contains()`

### Rule — [TransitionPolicy.java:21](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L21)

Khai báo: `public record Rule(ServiceRequestStatus from, ServiceRequestStatus to, Actor actor, List<Resolution> resolutions)`

- [TransitionPolicy.java:22](../../src/main/java/com/project/cybershop/aftersales/internal/service/TransitionPolicy.java#L22) `boolean allows(Actor who)`

## `aftersales/internal/service/WarrantyCardService.java`

### WarrantyCardService — [WarrantyCardService.java:35](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L35)

Khai báo: `@Service @Transactional(readOnly = true) public class WarrantyCardService`

- Trường: `private final WarrantyCardRepository cards;`
- Trường: `private final ServiceRequestRepository requests;`
- Trường: `private final ServiceRequestEventRepository events;`
- Trường: `private final ServiceIntakeRepository intakes;`
- Trường: `private final OrderLookup orderLookup;`
- Trường: `private final AftersalesMapper mapper;`
- [WarrantyCardService.java:46](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L46) `public WarrantyCardService(WarrantyCardRepository cards, ServiceRequestRepository requests, ServiceRequestEventRepository events, ServiceIntakeRepository intakes, OrderLookup orderLookup, AftersalesMapper mapper)`
- [WarrantyCardService.java:61](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L61) `public List<WarrantyCardDto> search(String query)`
  - Lời gọi xuất hiện: `query.isBlank()`, `orderLookup.searchCardIds()`, `orderLookup.byCardIds(matched).values().stream() .map(OrderInfo::orderId).collect()`, `orderLookup.byCardIds(matched).values().stream().map()`, `orderLookup.byCardIds(matched).values().stream()`, `orderLookup.byCardIds(matched).values()`, `orderLookup.byCardIds()`, `Collectors.toSet()`, `cards.findAllById()`, `orderLookup.cardIdsOfOrders()`, `toDtos()`, `found.stream().sorted(Comparator.comparing(WarrantyCard::getId)).toList()`, `found.stream().sorted()`, `found.stream()`, `Comparator.comparing()`
- [WarrantyCardService.java:72](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L72) `public List<WarrantyCardDto> toDtos(List<WarrantyCard> list)`
  - Lời gọi xuất hiện: `toDtos()`, `Set.of()`
- [WarrantyCardService.java:77](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L77) `private List<WarrantyCardDto> toDtos(List<WarrantyCard> list, Set<Integer> matched)`
  - Lời gọi xuất hiện: `list.isEmpty()`, `List.of()`, `list.stream().map(WarrantyCard::getId).toList()`, `list.stream().map()`, `list.stream()`, `orderLookup.byCardIds()`, `requestsByCard()`, `intakeCodes()`, `byCard.values().stream().flatMap(Collection::stream).toList()`, `byCard.values().stream().flatMap()`, `byCard.values().stream()`, `byCard.values()`, `replacedBy()`, `WarrantyRules.now().toLocalDate()`, `WarrantyRules.now()`, `list.stream().map(card -> { OrderInfo order = orders.get(card.getId()); List<ServiceRequest> history = byCard.getOrDefault(card.getId(), List.of()); WarrantyCard next = replacedBy.get(card.getId()); return WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count()) .openRequest(history.stream().filter(t -> t.getStatus().isOpen()).findFirst() .map(t -> summary(t, card, order, codes)).orElse(null)) .requests(history.stream().map(t -> summary(t, card, order, codes)).toList()) .build(); }).toList()`, `orders.get()`, `card.getId()`, `byCard.getOrDefault()`, `replacedBy.get()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count()) .openRequest(history.stream().filter(t -> t.getStatus().isOpen()).findFirst() .map(t -> summary(t, card, order, codes)).orElse(null)) .requests(history.stream().map(t -> summary(t, card, order, codes)).toList()).build()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count()) .openRequest(history.stream().filter(t -> t.getStatus().isOpen()).findFirst() .map(t -> summary(t, card, order, codes)).orElse(null)).requests()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count()).openRequest()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()).repairedCount()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()) .replacedByCardId(next == null ? null : next.getId()).replacedBySerialNumber()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()) .replacesCardId(card.getReplacesCardId()).replacedByCardId()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()) .pickupPhone(order == null ? null : order.pickupPhone()).replacesCardId()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()) .pickupName(order == null ? null : order.pickupName()).pickupPhone()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()) .customerPhone(order == null ? null : order.customerPhone()).pickupName()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())) .customerName(order == null ? null : order.customerName()).customerPhone()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()) .matched(matched.contains(card.getId())).customerName()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()) .pcCustomId(order == null ? null : order.pcCustomId()).matched()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()) .orderId(order == null ? null : order.orderId()).pcCustomId()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()) .orderCode(order == null ? null : order.orderCode()).orderId()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .sku(order == null ? null : order.sku()).orderCode()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()).sku()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()).variantName()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)).productName()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()).valid()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()).status()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()).endDate()`, `WarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()).startDate()`, `WarrantyCardDto.builder() .id(card.getId()).serialNumber()`, `WarrantyCardDto.builder().id()`, `WarrantyCardDto.builder()`, `card.getSerialNumber()`, `card.getStartDate()`, `card.getEndDate()`, `card.getStatus()`, `WarrantyRules.isValidOn()`, `order.productName()`, `order.variantName()`, `order.sku()`, `order.orderCode()`, `order.orderId()`, `order.pcCustomId()`, `matched.contains()`, `order.customerName()`, `order.customerPhone()`, `order.pickupName()`, `order.pickupPhone()`, `card.getReplacesCardId()`, `next.getId()`, `next.getSerialNumber()`, `history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count()`, `history.stream().filter()`, `history.stream()`, `t.getResolution()`, `history.stream().filter(t -> t.getStatus().isOpen()).findFirst() .map(t -> summary(t, card, order, codes)).orElse()`, `history.stream().filter(t -> t.getStatus().isOpen()).findFirst().map()`, `history.stream().filter(t -> t.getStatus().isOpen()).findFirst()`, `t.getStatus().isOpen()`, `t.getStatus()`, `summary()`, `history.stream().map(t -> summary(t, card, order, codes)).toList()`, `history.stream().map()`
- [WarrantyCardService.java:122](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L122) `public List<ServiceRequestSummaryDto> summaries(List<ServiceRequest> list)`
  - Lời gọi xuất hiện: `list.isEmpty()`, `List.of()`, `list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()`, `list.stream().map(ServiceRequest::getWarrantyCardId).distinct()`, `list.stream().map()`, `list.stream()`, `cards.findAllById(cardIds).stream().collect()`, `cards.findAllById(cardIds).stream()`, `cards.findAllById()`, `Collectors.toMap()`, `Function.identity()`, `orderLookup.byCardIds()`, `intakeCodes()`, `list.stream() .map(t -> summary(t, cardById.get(t.getWarrantyCardId()), orders.get(t.getWarrantyCardId()), codes)).toList()`, `summary()`, `cardById.get()`, `t.getWarrantyCardId()`, `orders.get()`
- [WarrantyCardService.java:137](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L137) `public List<MyWarrantyCardDto> myCards(Integer userId)`
  - Lời gọi xuất hiện: `cards.findAllById(orderLookup.cardIdsOfUser(userId)).stream() .sorted(Comparator.comparing(WarrantyCard::getId).reversed()).toList()`, `cards.findAllById(orderLookup.cardIdsOfUser(userId)).stream().sorted()`, `cards.findAllById(orderLookup.cardIdsOfUser(userId)).stream()`, `cards.findAllById()`, `orderLookup.cardIdsOfUser()`, `Comparator.comparing(WarrantyCard::getId).reversed()`, `Comparator.comparing()`, `mine.isEmpty()`, `List.of()`, `mine.stream().map(WarrantyCard::getId).toList()`, `mine.stream().map()`, `mine.stream()`, `orderLookup.byCardIds()`, `requestsByCard()`, `replacedBy()`, `myRequests()`, `byCard.values().stream().flatMap(Collection::stream) .filter(t -> Objects.equals(t.getUserId(), userId)).toList()`, `byCard.values().stream().flatMap(Collection::stream).filter()`, `byCard.values().stream().flatMap()`, `byCard.values().stream()`, `byCard.values()`, `Objects.equals()`, `t.getUserId()`, `WarrantyRules.now().toLocalDate()`, `WarrantyRules.now()`, `mine.stream().map(card -> { OrderInfo order = orders.get(card.getId()); WarrantyCard next = replacedBy.get(card.getId()); return MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .orderCode(order == null ? null : order.orderCode()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .requests(byCard.getOrDefault(card.getId(), List.of()).stream() .map(t -> mineRequests.get(t.getId())).filter(Objects::nonNull).toList()) .build(); }).toList()`, `orders.get()`, `card.getId()`, `replacedBy.get()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .orderCode(order == null ? null : order.orderCode()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()) .requests(byCard.getOrDefault(card.getId(), List.of()).stream() .map(t -> mineRequests.get(t.getId())).filter(Objects::nonNull).toList()).build()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .orderCode(order == null ? null : order.orderCode()) .replacedBySerialNumber(next == null ? null : next.getSerialNumber()).requests()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()) .orderCode(order == null ? null : order.orderCode()).replacedBySerialNumber()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()) .variantName(order == null ? null : order.variantName()).orderCode()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)) .productName(order == null ? null : order.productName()).variantName()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()) .valid(WarrantyRules.isValidOn(card, today)).productName()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()) .status(card.getStatus()).valid()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()) .endDate(card.getEndDate()).status()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()) .startDate(card.getStartDate()).endDate()`, `MyWarrantyCardDto.builder() .id(card.getId()) .serialNumber(card.getSerialNumber()).startDate()`, `MyWarrantyCardDto.builder() .id(card.getId()).serialNumber()`, `MyWarrantyCardDto.builder().id()`, `MyWarrantyCardDto.builder()`, `card.getSerialNumber()`, `card.getStartDate()`, `card.getEndDate()`, `card.getStatus()`, `WarrantyRules.isValidOn()`, `order.productName()`, `order.variantName()`, `order.orderCode()`, `next.getSerialNumber()`, `byCard.getOrDefault(card.getId(), List.of()).stream() .map(t -> mineRequests.get(t.getId())).filter(Objects::nonNull).toList()`, `byCard.getOrDefault(card.getId(), List.of()).stream() .map(t -> mineRequests.get(t.getId())).filter()`, `byCard.getOrDefault(card.getId(), List.of()).stream().map()`, `byCard.getOrDefault(card.getId(), List.of()).stream()`, `byCard.getOrDefault()`, `mineRequests.get()`, `t.getId()`
- [WarrantyCardService.java:174](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L174) `public List<MyServiceIntakeDto> myIntakes(Integer userId)`
  - Lời gọi xuất hiện: `intakes.findByUserIdOrderByCreatedAtDescIdDesc()`, `mine.isEmpty()`, `List.of()`, `requests .findByIntakeIdInOrderByIdAsc(mine.stream().map(ServiceIntake::getId).toList()).stream().collect()`, `requests .findByIntakeIdInOrderByIdAsc(mine.stream().map(ServiceIntake::getId).toList()).stream()`, `requests.findByIntakeIdInOrderByIdAsc()`, `mine.stream().map(ServiceIntake::getId).toList()`, `mine.stream().map()`, `mine.stream()`, `Collectors.groupingBy()`, `byIntake.values().stream().flatMap(Collection::stream).toList()`, `byIntake.values().stream().flatMap()`, `byIntake.values().stream()`, `byIntake.values()`, `orderLookup.byCardIds()`, `all.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()`, `all.stream().map(ServiceRequest::getWarrantyCardId).distinct()`, `all.stream().map()`, `all.stream()`, `myRequests()`, `mine.stream().filter(i -> byIntake.containsKey(i.getId())).map(intake -> { List<ServiceRequest> parts = byIntake.get(intake.getId()); ServiceRequest first = parts.get(0); OrderInfo order = orders.get(first.getWarrantyCardId()); return MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()) .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .components(parts.stream().map(t -> dtos.get(t.getId())).toList()) .build(); }).toList()`, `mine.stream().filter(i -> byIntake.containsKey(i.getId())).map()`, `mine.stream().filter()`, `byIntake.containsKey()`, `i.getId()`, `byIntake.get()`, `intake.getId()`, `parts.get()`, `orders.get()`, `first.getWarrantyCardId()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()) .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()) .components(parts.stream().map(t -> dtos.get(t.getId())).toList()).build()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()) .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)) .createdAt(intake.getCreatedAt()) .returnedAt(intake.getReturnedAt()).components()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()) .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)) .createdAt(intake.getCreatedAt()).returnedAt()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()) .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)).createdAt()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()) .issueDescription(first.getIssueDescription()).expectedReturnDate()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()) .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()).issueDescription()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()) .orderCode(order == null ? null : order.orderCode()).status()`, `MyServiceIntakeDto.builder() .intakeCode(intake.getIntakeCode()).orderCode()`, `MyServiceIntakeDto.builder().intakeCode()`, `MyServiceIntakeDto.builder()`, `intake.getIntakeCode()`, `order.orderCode()`, `WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code()`, `WarrantyRules.intakeStatus()`, `parts.stream().map(ServiceRequest::getStatus).toList()`, `parts.stream().map()`, `parts.stream()`, `first.getIssueDescription()`, `parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse()`, `parts.stream().map(ServiceRequest::getExpectedReturnDate) .filter(Objects::nonNull).max()`, `parts.stream().map(ServiceRequest::getExpectedReturnDate).filter()`, `Comparator.naturalOrder()`, `intake.getCreatedAt()`, `intake.getReturnedAt()`, `parts.stream().map(t -> dtos.get(t.getId())).toList()`, `dtos.get()`, `t.getId()`
- [WarrantyCardService.java:206](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L206) `private Map<Integer, MyServiceRequestDto> myRequests(List<ServiceRequest> list, Map<Integer, OrderInfo> orders)`
  - Lời gọi xuất hiện: `list.isEmpty()`, `Map.of()`, `events .findByServiceRequestIdInOrderByCreatedAtAscIdAsc(list.stream().map(ServiceRequest::getId).toList()) .stream().collect()`, `events .findByServiceRequestIdInOrderByCreatedAtAscIdAsc(list.stream().map(ServiceRequest::getId).toList()).stream()`, `events.findByServiceRequestIdInOrderByCreatedAtAscIdAsc()`, `list.stream().map(ServiceRequest::getId).toList()`, `list.stream().map()`, `list.stream()`, `Collectors.groupingBy()`, `cards.findAllById( list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()).stream().collect()`, `cards.findAllById( list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()).stream()`, `cards.findAllById()`, `list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()`, `list.stream().map(ServiceRequest::getWarrantyCardId).distinct()`, `Collectors.toMap()`, `Function.identity()`, `intakeCodes()`, `list.stream().collect()`, `mapper.toMine()`, `orders.get()`, `t.getWarrantyCardId()`, `cardById.get()`, `dto.setIntakeCode()`, `codes.get()`, `t.getIntakeId()`, `dto.setProductName()`, `order.productName()`, `dto.setSerialNumber()`, `card.getSerialNumber()`, `dto.setTimeline()`, `timeline.getOrDefault(t.getId(), List.of()).stream() .filter(e -> e.getFromStatus() != e.getToStatus() || e.getPublicNote() != null) .map(mapper::toTimeline).toList()`, `timeline.getOrDefault(t.getId(), List.of()).stream() .filter(e -> e.getFromStatus() != e.getToStatus() || e.getPublicNote() != null).map()`, `timeline.getOrDefault(t.getId(), List.of()).stream().filter()`, `timeline.getOrDefault(t.getId(), List.of()).stream()`, `timeline.getOrDefault()`, `t.getId()`, `List.of()`, `e.getFromStatus()`, `e.getToStatus()`, `e.getPublicNote()`
- [WarrantyCardService.java:233](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L233) `private Map<Integer, List<ServiceRequest>> requestsByCard(List<Integer> cardIds)`
  - Lời gọi xuất hiện: `requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(cardIds).stream().collect()`, `requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(cardIds).stream()`, `requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc()`, `Collectors.groupingBy()`
- [WarrantyCardService.java:238](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L238) `private Map<Integer, WarrantyCard> replacedBy(List<Integer> cardIds)`
  - Lời gọi xuất hiện: `cards.findByReplacesCardIdIn(cardIds).stream().collect()`, `cards.findByReplacesCardIdIn(cardIds).stream()`, `cards.findByReplacesCardIdIn()`, `Collectors.toMap()`, `Function.identity()`
- [WarrantyCardService.java:243](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L243) `private Map<Integer, String> intakeCodes(Collection<ServiceRequest> list)`
  - Lời gọi xuất hiện: `list.stream().map(ServiceRequest::getIntakeId).filter(Objects::nonNull).distinct().toList()`, `list.stream().map(ServiceRequest::getIntakeId).filter(Objects::nonNull).distinct()`, `list.stream().map(ServiceRequest::getIntakeId).filter()`, `list.stream().map()`, `list.stream()`, `ids.isEmpty()`, `Map.of()`, `intakes.findAllById(ids).stream().collect()`, `intakes.findAllById(ids).stream()`, `intakes.findAllById()`, `Collectors.toMap()`
- [WarrantyCardService.java:249](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyCardService.java#L249) `private ServiceRequestSummaryDto summary(ServiceRequest request, WarrantyCard card, OrderInfo order, Map<Integer, String> intakeCodes)`
  - Lời gọi xuất hiện: `mapper.toSummary()`, `dto.setIntakeCode()`, `intakeCodes.get()`, `request.getIntakeId()`, `dto.setSerialNumber()`, `card.getSerialNumber()`, `dto.setProductName()`, `order.productName()`, `dto.setOrderCode()`, `order.orderCode()`

## `aftersales/internal/service/WarrantyRules.java`

### WarrantyRules — [WarrantyRules.java:19](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L19)

Khai báo: `public final class WarrantyRules`

- Trường: `public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");`
- Trường: `private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.ofPattern("yyMMdd");`
- Trường: `private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";`
- [WarrantyRules.java:28](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L28) `private WarrantyRules()`
- [WarrantyRules.java:31](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L31) `public static LocalDateTime now()`
  - Lời gọi xuất hiện: `LocalDateTime.now()`
- [WarrantyRules.java:36](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L36) `public static boolean isValidOn(WarrantyCard card, LocalDate day)`
  - Lời gọi xuất hiện: `WarrantyCard.ACTIVE.equals()`, `card.getStatus()`, `day.isBefore()`, `card.getStartDate().toLocalDate()`, `card.getStartDate()`, `day.isAfter()`, `card.getEndDate().toLocalDate()`, `card.getEndDate()`
- [WarrantyRules.java:43](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L43) `public static long extensionDays(LocalDateTime receivedAt, LocalDateTime completedAt)`
  - Lời gọi xuất hiện: `Math.max()`, `ChronoUnit.DAYS.between()`, `receivedAt.toLocalDate()`, `completedAt.toLocalDate()`
- [WarrantyRules.java:51](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L51) `public static String newRequestCode(LocalDate day, RandomGenerator random)`
  - Lời gọi xuất hiện: `new StringBuilder("BH-").append(day.format(CODE_DAY)).append()`, `new StringBuilder("BH-").append()`, `day.format()`, `code.append()`, `CODE_ALPHABET.charAt()`, `random.nextInt()`, `CODE_ALPHABET.length()`, `code.toString()`
- [WarrantyRules.java:60](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L60) `public static String childCode(String intakeCode, int index)`
  - Lời gọi xuất hiện: `String.format()`
- [WarrantyRules.java:68](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L68) `public static ServiceRequestStatus intakeStatus(Collection<ServiceRequestStatus> components)`
  - Lời gọi xuất hiện: `components.stream() .filter(s -> s != ServiceRequestStatus.CANCELED) .min(Comparator.naturalOrder()).orElse()`, `components.stream() .filter(s -> s != ServiceRequestStatus.CANCELED).min()`, `components.stream().filter()`, `components.stream()`, `Comparator.naturalOrder()`
- [WarrantyRules.java:76](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L76) `public static boolean canReturn(Collection<ServiceRequestStatus> components)`
  - Lời gọi xuất hiện: `components.contains()`, `components.stream().allMatch()`, `components.stream()`
- [WarrantyRules.java:82](../../src/main/java/com/project/cybershop/aftersales/internal/service/WarrantyRules.java#L82) `public static String normalizeSerial(String serial)`
  - Lời gọi xuất hiện: `serial.trim().toUpperCase()`, `serial.trim()`

