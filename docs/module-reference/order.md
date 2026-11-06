# Tra cứu module order

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **POST `/api/orders/cart/items`** → `CartController.addToCart` [CartController.java:36](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L36)
  - `@PostMapping("/items") public ResponseEntity<ApiResponseDTO<Void>> addToCart( @Valid @RequestBody AddToCartRequest request )`
- **POST `/api/orders/cart/items/batch`** → `CartController.addBatchToCart` [CartController.java:56](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L56)
  - `@PostMapping("/items/batch") public ResponseEntity<ApiResponseDTO<Void>> addBatchToCart( @Valid @RequestBody BatchAddToCartRequest request )`
- **GET `/api/orders/cart`** → `CartController.getMyCart` [CartController.java:111](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L111)
  - `@GetMapping public ResponseEntity<ApiResponseDTO<CartResponse>> getMyCart()`
- **PUT `/api/orders/cart/items/{cartItemId}`** → `CartController.updateCartItem` [CartController.java:134](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L134)
  - `@PutMapping("/items/{cartItemId}") public ResponseEntity<ApiResponseDTO<Void>> updateCartItem( @PathVariable Integer cartItemId, @Valid @RequestBody UpdateCartItemRequest request )`
- **DELETE `/api/orders/cart/items/{cartItemId}`** → `CartController.removeCartItem` [CartController.java:163](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L163)
  - `@DeleteMapping("/items/{cartItemId}") public ResponseEntity<ApiResponseDTO<Void>> removeCartItem( @PathVariable Integer cartItemId )`
- **POST `/api/checkout/validate`** → `CheckoutController.validate` [CheckoutController.java:26](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutController.java#L26)
  - `@PostMapping("/validate") @ResponseStatus(HttpStatus.OK) public CheckoutPreviewResponse validate( Authentication authentication, @RequestBody(required = false) CheckoutValidateRequest request)`
- **POST `/api/checkout/details/preview`** → `CheckoutDetailsController.preview` [CheckoutDetailsController.java:23](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutDetailsController.java#L23)
  - `@PostMapping("/details/preview") public CheckoutDetailsResponse preview(@RequestBody CheckoutDetailsRequest request, Authentication authentication)`
- **POST `/api/orders`** → `OrderController.place` [OrderController.java:18](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L18)
  - `@PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderResponse place(Authentication auth, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PlaceOrderRequest request)`
- **GET `/api/orders`** → `OrderController.mine` [OrderController.java:25](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L25)
  - `@GetMapping public List<OrderResponse> mine(Authentication auth)`
- **GET `/api/orders/{code}`** → `OrderController.one` [OrderController.java:28](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L28)
  - `@GetMapping("/{code}") public OrderResponse one(Authentication auth, @PathVariable String code)`
- **POST `/api/orders/{code}/cancel`** → `OrderController.cancel` [OrderController.java:31](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L31)
  - `@PostMapping("/{code}/cancel") public OrderResponse cancel(Authentication auth, @PathVariable String code)`
- **GET `/api/operations/orders`** → `OrderOperationsController.list` [OrderOperationsController.java:21](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L21)
  - `@GetMapping public List<OrderResponse> list(Authentication auth, @RequestParam(required=false) String search, @RequestParam(required=false) String status)`
- **PATCH `/api/operations/orders/{code}/status`** → `OrderOperationsController.advance` [OrderOperationsController.java:29](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L29)
  - `@PatchMapping("/{code}/status") public OrderResponse advance(Authentication auth, @PathVariable String code, @Valid @RequestBody StatusRequest request)`
- **POST `/api/operations/orders/{code}/collect`** → `OrderOperationsController.collect` [OrderOperationsController.java:34](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L34)
  - `@PostMapping("/{code}/collect") public OrderResponse collect(Authentication auth, @PathVariable String code, @Valid @RequestBody CollectPaymentRequest request)`
- **POST `/api/operations/orders/{code}/handover`** → `OrderOperationsController.handover` [OrderOperationsController.java:39](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L39)
  - `@PostMapping("/{code}/handover") public OrderResponse handover(Authentication auth, @PathVariable String code, @Valid @RequestBody HandoverRequest request)`
- **POST `/api/operations/orders/{code}/cancel`** → `OrderOperationsController.cancel` [OrderOperationsController.java:44](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L44)
  - `@PostMapping("/{code}/cancel") public OrderResponse cancel(Authentication auth, @PathVariable String code)`

## Khai báo và lời gọi theo file

## `order/api/dto/request/AddToCartRequest.java`

### AddToCartRequest — [AddToCartRequest.java:7](../../src/main/java/com/project/cybershop/order/api/dto/request/AddToCartRequest.java#L7)

Khai báo: `public record AddToCartRequest( @NotNull(message = "Vui lòng chọn phiên bản sản phẩm") @Min(value = 1, message = "ID phiên bản phải lớn hơn 0") Integer productVariantId, @NotNull(message = "Vui lòng nhập số lượng") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity )`


## `order/api/dto/request/BatchAddToCartRequest.java`

### BatchAddToCartRequest — [BatchAddToCartRequest.java:7](../../src/main/java/com/project/cybershop/order/api/dto/request/BatchAddToCartRequest.java#L7)

Khai báo: `public record BatchAddToCartRequest( @NotEmpty(message = "Danh sách linh kiện không được để trống") List<@Valid AddToCartRequest> items )`


## `order/api/dto/request/CheckoutDetailsRequest.java`

### CheckoutDetailsRequest — [CheckoutDetailsRequest.java:6](../../src/main/java/com/project/cybershop/order/api/dto/request/CheckoutDetailsRequest.java#L6)

Khai báo: `public record CheckoutDetailsRequest( String pickupName, String pickupPhone, String note, String promotionCode, List<Integer> itemVariantIds )`

- [CheckoutDetailsRequest.java:13](../../src/main/java/com/project/cybershop/order/api/dto/request/CheckoutDetailsRequest.java#L13) `public CheckoutDetailsRequest(String pickupName, String pickupPhone, String note, String promotionCode)`

## `order/api/dto/request/CheckoutValidateRequest.java`

### CheckoutValidateRequest — [CheckoutValidateRequest.java:5](../../src/main/java/com/project/cybershop/order/api/dto/request/CheckoutValidateRequest.java#L5)

Khai báo: `public record CheckoutValidateRequest( List<Integer> itemVariantIds )`


## `order/api/dto/request/CollectPaymentRequest.java`

### CollectPaymentRequest — [CollectPaymentRequest.java:6](../../src/main/java/com/project/cybershop/order/api/dto/request/CollectPaymentRequest.java#L6)

Khai báo: `public record CollectPaymentRequest( @NotNull(message = "Vui lòng nhập số tiền đã nhận") @DecimalMin(value = "0", message = "Số tiền đã nhận không được âm") BigDecimal receivedAmount, @Size(max=255, message = "Mã tham chiếu tối đa 255 ký tự") String reference )`


## `order/api/dto/request/HandoverRequest.java`

### HandoverRequest — [HandoverRequest.java:7](../../src/main/java/com/project/cybershop/order/api/dto/request/HandoverRequest.java#L7)

Khai báo: `public record HandoverRequest( @NotBlank(message = "Vui lòng nhập số điện thoại người nhận") @Size(max=20, message = "Số điện thoại người nhận tối đa 20 ký tự") String recipientPhone, @AssertTrue(message = "Vui lòng xác nhận đã đối chiếu người nhận và thiết bị") boolean recipientVerified, @NotNull(message = "Thiếu danh sách thiết bị bàn giao") @Size(max=10000, message = "Danh sách thiết bị bàn giao quá dài") List<@NotNull(message = "Thông tin thiết bị không được để trống") @Valid Device> devices )`


### Device — [HandoverRequest.java:15](../../src/main/java/com/project/cybershop/order/api/dto/request/HandoverRequest.java#L15)

Khai báo: `public record Device( @NotNull(message = "Thiếu mã dòng đơn hàng của thiết bị") Integer orderItemId, @NotBlank(message = "Vui lòng nhập serial cho từng thiết bị") @Size(max=100, message = "Serial tối đa 100 ký tự") String serialNumber)`


## `order/api/dto/request/PlaceOrderRequest.java`

### PlaceOrderRequest — [PlaceOrderRequest.java:7](../../src/main/java/com/project/cybershop/order/api/dto/request/PlaceOrderRequest.java#L7)

Khai báo: `public record PlaceOrderRequest( @NotBlank(message = "Vui lòng nhập tên người nhận") @Size(max=100, message = "Tên người nhận tối đa 100 ký tự") String pickupName, @NotBlank(message = "Vui lòng nhập số điện thoại người nhận") @Size(max=20, message = "Số điện thoại người nhận tối đa 20 ký tự") String pickupPhone, @Size(max=2000, message = "Ghi chú tối đa 2000 ký tự") String note, @Size(max=50, message = "Mã khuyến mãi tối đa 50 ký tự") String promotionCode, @NotBlank(message = "Vui lòng chọn phương thức thanh toán") String method, Integer paymentMethodId, @NotNull(message = "Thiếu tổng tiền dự kiến") @DecimalMin(value = "0", message = "Tổng tiền dự kiến không được âm") BigDecimal expectedTotal, @NotNull(message = "Thiếu số tiền giảm dự kiến") @DecimalMin(value = "0", message = "Số tiền giảm dự kiến không được âm") BigDecimal expectedDiscount, List<Integer> itemVariantIds )`

- [PlaceOrderRequest.java:22](../../src/main/java/com/project/cybershop/order/api/dto/request/PlaceOrderRequest.java#L22) `public PlaceOrderRequest(String pickupName, String pickupPhone, String note, String promotionCode, String method, Integer paymentMethodId, BigDecimal expectedTotal, BigDecimal expectedDiscount)`

## `order/api/dto/request/UpdateCartItemRequest.java`

### UpdateCartItemRequest — [UpdateCartItemRequest.java:7](../../src/main/java/com/project/cybershop/order/api/dto/request/UpdateCartItemRequest.java#L7)

Khai báo: `public record UpdateCartItemRequest( @NotNull(message = "Vui lòng nhập số lượng") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity )`


## `order/api/dto/response/CartResponse.java`

### CartResponse — [CartResponse.java:7](../../src/main/java/com/project/cybershop/order/api/dto/response/CartResponse.java#L7)

Khai báo: `public record CartResponse( Integer cartId, Integer userId, List<CartItemResponse> items, Integer totalQuantity, BigDecimal totalAmount )`


### CartItemResponse — [CartResponse.java:22](../../src/main/java/com/project/cybershop/order/api/dto/response/CartResponse.java#L22)

Khai báo: `public record CartItemResponse( Integer cartItemId, Integer productVariantId, String productName, String variantName, String imageUrl, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal, Integer stockQuantity, boolean available )`


## `order/api/dto/response/CheckoutDetailsResponse.java`

### CheckoutDetailsResponse — [CheckoutDetailsResponse.java:6](../../src/main/java/com/project/cybershop/order/api/dto/response/CheckoutDetailsResponse.java#L6)

Khai báo: `public record CheckoutDetailsResponse( Integer cartId, String pickupName, String pickupPhone, String note, String promotionCode, List<CheckoutItemResponse> items, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount )`


## `order/api/dto/response/CheckoutItemResponse.java`

### CheckoutItemResponse — [CheckoutItemResponse.java:6](../../src/main/java/com/project/cybershop/order/api/dto/response/CheckoutItemResponse.java#L6)

Khai báo: `public record CheckoutItemResponse( Integer productVariantId, String productName, String variantName, String imageUrl, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal, Integer stockQuantity )`


## `order/api/dto/response/CheckoutPreviewResponse.java`

### CheckoutPreviewResponse — [CheckoutPreviewResponse.java:6](../../src/main/java/com/project/cybershop/order/api/dto/response/CheckoutPreviewResponse.java#L6)

Khai báo: `public record CheckoutPreviewResponse( Integer cartId, List<CheckoutItemResponse> items, BigDecimal totalAmount, boolean valid )`


## `order/api/dto/response/OrderResponse.java`

### OrderResponse — [OrderResponse.java:7](../../src/main/java/com/project/cybershop/order/api/dto/response/OrderResponse.java#L7)

Khai báo: `public record OrderResponse( Integer id, String orderCode, Integer userId, String status, String pickupName, String pickupPhone, String note, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount, String promotionCode, String paymentStatus, String methodType, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt, List<Item> items )`


### Item — [OrderResponse.java:15](../../src/main/java/com/project/cybershop/order/api/dto/response/OrderResponse.java#L15)

Khai báo: `public record Item(Integer id, Integer productVariantId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal, int warrantyPeriodMonths, Integer rating, String reviewComment)`


## `order/api/dto/response/ProductResponse.java`

### ProductResponse — [ProductResponse.java:5](../../src/main/java/com/project/cybershop/order/api/dto/response/ProductResponse.java#L5)

Khai báo: `public record ProductResponse( Integer variantId, Integer productId, String productName, String variantName, BigDecimal price, Integer stockQuantity, String imageUrl, Integer warrantyPeriodMonths )`


## `order/internal/controller/CartController.java`

### CartController — [CartController.java:25](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L25)

Khai báo: `@RestController @RequestMapping("/api/orders/cart") @RequiredArgsConstructor public class CartController`

- Trường: `private final CartService cartService;`
- Trường: `private final IdentityApi identityApi;`
- [CartController.java:36](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L36) `@PostMapping("/items") public ResponseEntity<ApiResponseDTO<Void>> addToCart( @Valid @RequestBody AddToCartRequest request )`
  - Lời gọi xuất hiện: `getCurrentUserId()`, `cartService.addToCart()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [CartController.java:56](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L56) `@PostMapping("/items/batch") public ResponseEntity<ApiResponseDTO<Void>> addBatchToCart( @Valid @RequestBody BatchAddToCartRequest request )`
  - Lời gọi xuất hiện: `getCurrentUserId()`, `cartService.addBatchToCart()`, `request.items()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [CartController.java:76](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L76) `private Integer getCurrentUserId()`
  - Lời gọi xuất hiện: `SecurityContextHolder .getContext().getAuthentication()`, `SecurityContextHolder.getContext()`, `auth.isAuthenticated()`, `auth.getName()`, `identityApi .getUserByEmail(email) .filter(u -> Boolean.TRUE.equals(u.getIsActive()) ).orElseThrow()`, `identityApi .getUserByEmail(email).filter()`, `identityApi.getUserByEmail()`, `Boolean.TRUE.equals()`, `u.getIsActive()`, `user.getId()`
- [CartController.java:111](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L111) `@GetMapping public ResponseEntity<ApiResponseDTO<CartResponse>> getMyCart()`
  - Lời gọi xuất hiện: `getCurrentUserId()`, `cartService.getMyCart()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [CartController.java:134](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L134) `@PutMapping("/items/{cartItemId}") public ResponseEntity<ApiResponseDTO<Void>> updateCartItem( @PathVariable Integer cartItemId, @Valid @RequestBody UpdateCartItemRequest request )`
  - Lời gọi xuất hiện: `getCurrentUserId()`, `cartService.updateCartItem()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [CartController.java:163](../../src/main/java/com/project/cybershop/order/internal/controller/CartController.java#L163) `@DeleteMapping("/items/{cartItemId}") public ResponseEntity<ApiResponseDTO<Void>> removeCartItem( @PathVariable Integer cartItemId )`
  - Lời gọi xuất hiện: `getCurrentUserId()`, `cartService.removeCartItem()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`

## `order/internal/controller/CheckoutController.java`

### CheckoutController — [CheckoutController.java:17](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutController.java#L17)

Khai báo: `@RestController @RequestMapping("/api/checkout") public class CheckoutController`

- Trường: `private final CheckoutService checkoutService;`
- [CheckoutController.java:22](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutController.java#L22) `public CheckoutController(CheckoutService checkoutService)`
- [CheckoutController.java:26](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutController.java#L26) `@PostMapping("/validate") @ResponseStatus(HttpStatus.OK) public CheckoutPreviewResponse validate( Authentication authentication, @RequestBody(required = false) CheckoutValidateRequest request)`
  - Lời gọi xuất hiện: `authentication.isAuthenticated()`, `"anonymousUser".equals()`, `authentication.getPrincipal()`, `request.itemVariantIds()`, `checkoutService.validate()`, `authentication.getName()`

## `order/internal/controller/CheckoutDetailsController.java`

### CheckoutDetailsController — [CheckoutDetailsController.java:14](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutDetailsController.java#L14)

Khai báo: `@RestController @RequestMapping("/api/checkout") public class CheckoutDetailsController`

- Trường: `private final CheckoutDetailsService service;`
- [CheckoutDetailsController.java:19](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutDetailsController.java#L19) `public CheckoutDetailsController(CheckoutDetailsService service)`
- [CheckoutDetailsController.java:23](../../src/main/java/com/project/cybershop/order/internal/controller/CheckoutDetailsController.java#L23) `@PostMapping("/details/preview") public CheckoutDetailsResponse preview(@RequestBody CheckoutDetailsRequest request, Authentication authentication)`
  - Lời gọi xuất hiện: `authentication.isAuthenticated()`, `"anonymousUser".equals()`, `authentication.getPrincipal()`, `service.preview()`, `authentication.getName()`

## `order/internal/controller/OrderController.java`

### OrderController — [OrderController.java:12](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L12)

Khai báo: `@RestController @RequestMapping("/api/orders") public class OrderController`

- Trường: `private final OrderService service;`
- [OrderController.java:16](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L16) `public OrderController(OrderService service)`
- [OrderController.java:18](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L18) `@PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderResponse place(Authentication auth, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PlaceOrderRequest request)`
  - Lời gọi xuất hiện: `service.place()`, `auth.getName()`
- [OrderController.java:25](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L25) `@GetMapping public List<OrderResponse> mine(Authentication auth)`
  - Lời gọi xuất hiện: `service.myOrders()`, `auth.getName()`
- [OrderController.java:28](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L28) `@GetMapping("/{code}") public OrderResponse one(Authentication auth, @PathVariable String code)`
  - Lời gọi xuất hiện: `service.myOrder()`, `auth.getName()`
- [OrderController.java:31](../../src/main/java/com/project/cybershop/order/internal/controller/OrderController.java#L31) `@PostMapping("/{code}/cancel") public OrderResponse cancel(Authentication auth, @PathVariable String code)`
  - Lời gọi xuất hiện: `service.cancel()`, `auth.getName()`

## `order/internal/controller/OrderOperationsController.java`

### OrderOperationsController — [OrderOperationsController.java:14](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L14)

Khai báo: `@RestController @RequestMapping("/api/operations/orders") @PreAuthorize("hasAnyRole('STAFF','ADMIN')") public class OrderOperationsController`

- Trường: `private final OrderService service;`
- [OrderOperationsController.java:19](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L19) `public OrderOperationsController(OrderService service)`
- [OrderOperationsController.java:21](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L21) `@GetMapping public List<OrderResponse> list(Authentication auth, @RequestParam(required=false) String search, @RequestParam(required=false) String status)`
  - Lời gọi xuất hiện: `service.operations()`, `auth.getName()`
- [OrderOperationsController.java:29](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L29) `@PatchMapping("/{code}/status") public OrderResponse advance(Authentication auth, @PathVariable String code, @Valid @RequestBody StatusRequest request)`
  - Lời gọi xuất hiện: `service.advance()`, `auth.getName()`, `request.status()`
- [OrderOperationsController.java:34](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L34) `@PostMapping("/{code}/collect") public OrderResponse collect(Authentication auth, @PathVariable String code, @Valid @RequestBody CollectPaymentRequest request)`
  - Lời gọi xuất hiện: `service.collect()`, `auth.getName()`
- [OrderOperationsController.java:39](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L39) `@PostMapping("/{code}/handover") public OrderResponse handover(Authentication auth, @PathVariable String code, @Valid @RequestBody HandoverRequest request)`
  - Lời gọi xuất hiện: `service.handover()`, `auth.getName()`
- [OrderOperationsController.java:44](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L44) `@PostMapping("/{code}/cancel") public OrderResponse cancel(Authentication auth, @PathVariable String code)`
  - Lời gọi xuất hiện: `service.cancel()`, `auth.getName()`

### StatusRequest — [OrderOperationsController.java:27](../../src/main/java/com/project/cybershop/order/internal/controller/OrderOperationsController.java#L27)

Khai báo: `public record StatusRequest(@NotBlank(message = "Vui lòng chọn trạng thái") @Size(max=30, message = "Trạng thái tối đa 30 ký tự") String status)`


## `order/internal/entity/Cart.java`

### Cart — [Cart.java:10](../../src/main/java/com/project/cybershop/order/internal/entity/Cart.java#L10)

Khai báo: `@Getter @Setter @Entity @Table(name = "carts") @NoArgsConstructor public class Cart`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "user_id", unique = true) private Integer userId;`
- Trường: `@Column(name = "updated_at") private LocalDateTime updatedAt;`

## `order/internal/entity/CartItem.java`

### CartItem — [CartItem.java:11](../../src/main/java/com/project/cybershop/order/internal/entity/CartItem.java#L11)

Khai báo: `@Getter @Setter @Entity @Table(name = "cart_items") @NoArgsConstructor public class CartItem`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "cart_id", nullable = false) private Integer cartId;`
- Trường: `@Column(name = "product_variant_id", nullable = false) private Integer productVariantId;`
- Trường: `@Column(name = "quantity", nullable = false) private Integer quantity;`
- Trường: `@Column(name = "created_at") private LocalDateTime createdAt;`

## `order/internal/entity/Order.java`

### Order — [Order.java:16](../../src/main/java/com/project/cybershop/order/internal/entity/Order.java#L16)

Khai báo: `@Entity @Table(name = "orders") @Getter @Setter @NoArgsConstructor public class Order`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "user_id", nullable = false) private Integer userId;`
- Trường: `@Column(name = "order_code", nullable = false, unique = true, length = 50) private String orderCode;`
- Trường: `@Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;`
- Trường: `@Column(name = "discount_amount", precision = 12, scale = 2) private BigDecimal discountAmount;`
- Trường: `@Column(name = "final_amount", nullable = false, precision = 12, scale = 2) private BigDecimal finalAmount;`
- Trường: `@Column(name = "status", columnDefinition = "varchar") private String status;`
- Trường: `@Column(name = "pickup_name", length = 100) private String pickupName;`
- Trường: `@Column(name = "pickup_phone", length = 20) private String pickupPhone;`
- Trường: `@Column(name = "note", columnDefinition = "text") private String note;`
- Trường: `@Column(name = "created_at") private LocalDateTime createdAt;`
- Trường: `@Column(name = "updated_at") private LocalDateTime updatedAt;`

## `order/internal/entity/OrderItem.java`

### OrderItem — [OrderItem.java:15](../../src/main/java/com/project/cybershop/order/internal/entity/OrderItem.java#L15)

Khai báo: `@Entity @Table(name = "order_items") @Getter @Setter @NoArgsConstructor public class OrderItem`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "order_id", nullable = false) private Integer orderId;`
- Trường: `@Column(name = "product_variant_id", nullable = false) private Integer productVariantId;`
- Trường: `@Column(name = "product_name", columnDefinition = "varchar") private String productName;`
- Trường: `@Column(name = "quantity", nullable = false) private Integer quantity;`
- Trường: `@Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;`
- Trường: `@Column(name = "subtotal", nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;`
- Trường: `@Column(name = "warranty_period_months", nullable = false) private Integer warrantyPeriodMonths;`

## `order/internal/mapper/CheckoutMapper.java`

### CheckoutMapper — [CheckoutMapper.java:10](../../src/main/java/com/project/cybershop/order/internal/mapper/CheckoutMapper.java#L10)

Khai báo: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR) public interface CheckoutMapper`

- [CheckoutMapper.java:13](../../src/main/java/com/project/cybershop/order/internal/mapper/CheckoutMapper.java#L13) `CheckoutItemResponse toItemResponse(CheckoutItemProjection item, BigDecimal subtotal)`

## `order/internal/mapper/OrderMapper.java`

### OrderMapper — [OrderMapper.java:18](../../src/main/java/com/project/cybershop/order/internal/mapper/OrderMapper.java#L18)

Khai báo: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR) public interface OrderMapper`

- [OrderMapper.java:21](../../src/main/java/com/project/cybershop/order/internal/mapper/OrderMapper.java#L21) `@Mapping(target = "id", ignore = true) @Mapping(target = "status", constant = "pending") @Mapping(target = "discountAmount", source = "discountAmount") @Mapping(target = "finalAmount", source = "finalAmount") @Mapping(target = "createdAt", source = "createdAt") @Mapping(target = "updatedAt", source = "createdAt") Order toOrder( CheckoutDetailsResponse checkout, Integer userId, String orderCode, BigDecimal discountAmount, BigDecimal finalAmount, LocalDateTime createdAt )`
- [OrderMapper.java:36](../../src/main/java/com/project/cybershop/order/internal/mapper/OrderMapper.java#L36) `@Mapping(target = "id", ignore = true) @Mapping(target = "productVariantId", source = "product.variantId") @Mapping(target = "unitPrice", source = "product.price") @Mapping(target = "productName", source = "productName") OrderItem toOrderItem( PurchaseVariantResponse product, Integer orderId, Integer quantity, String productName, BigDecimal subtotal )`
- [OrderMapper.java:48](../../src/main/java/com/project/cybershop/order/internal/mapper/OrderMapper.java#L48) `@Mapping(target = "status", source = "order.status") @Mapping(target = "paymentStatus", source = "payment.status") OrderResponse toResponse( Order order, PaymentApi.Payment payment, String promotionCode, List<OrderResponse.Item> items )`
- [OrderMapper.java:57](../../src/main/java/com/project/cybershop/order/internal/mapper/OrderMapper.java#L57) `OrderResponse.Item toItemResponse(OrderItemProjection item)`

## `order/internal/repository/CartItemRepository.java`

### CartItemRepository — [CartItemRepository.java:14](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L14)

Khai báo: `@Repository public interface CartItemRepository extends JpaRepository<CartItem, Integer>`

- [CartItemRepository.java:18](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L18) `List<CartItem> findByCartIdOrderByIdAsc(Integer cartId)`
- [CartItemRepository.java:20](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L20) `@Query(value = """ SELECT product_variant_id AS "productVariantId", quantity AS "quantity" FROM cart_items WHERE cart_id = :cartId ORDER BY product_variant_id, id FOR UPDATE """, nativeQuery = true) List<CartLineProjection> findForCheckout(@Param("cartId") Integer cartId)`
- [CartItemRepository.java:30](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L30) `Optional<CartItem> findByCartIdAndProductVariantId( Integer cartId, Integer productVariantId )`
- [CartItemRepository.java:36](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L36) `boolean existsByCartIdAndProductVariantId( Integer cartId, Integer productVariantId )`
- [CartItemRepository.java:42](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L42) `void deleteByCartId(Integer cartId)`
- [CartItemRepository.java:44](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L44) `@Modifying(flushAutomatically = true) @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId", nativeQuery = true) void clearAfterCheckout(@Param("cartId") Integer cartId)`
- [CartItemRepository.java:48](../../src/main/java/com/project/cybershop/order/internal/repository/CartItemRepository.java#L48) `@Modifying(flushAutomatically = true) @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId AND product_variant_id IN (:variantIds)", nativeQuery = true) void deleteByCartIdAndProductVariantIdIn(@Param("cartId") Integer cartId, @Param("variantIds") Collection<Integer> variantIds)`

## `order/internal/repository/CartLineProjection.java`

### CartLineProjection — [CartLineProjection.java:3](../../src/main/java/com/project/cybershop/order/internal/repository/CartLineProjection.java#L3)

Khai báo: `public interface CartLineProjection`

- [CartLineProjection.java:5](../../src/main/java/com/project/cybershop/order/internal/repository/CartLineProjection.java#L5) `Integer getProductVariantId()`
- [CartLineProjection.java:7](../../src/main/java/com/project/cybershop/order/internal/repository/CartLineProjection.java#L7) `Integer getQuantity()`

## `order/internal/repository/CartRepository.java`

### CartRepository — [CartRepository.java:12](../../src/main/java/com/project/cybershop/order/internal/repository/CartRepository.java#L12)

Khai báo: `@Repository public interface CartRepository extends JpaRepository<Cart, Integer>`

- [CartRepository.java:16](../../src/main/java/com/project/cybershop/order/internal/repository/CartRepository.java#L16) `Optional<Cart> findByUserId(Integer userId)`
- [CartRepository.java:19](../../src/main/java/com/project/cybershop/order/internal/repository/CartRepository.java#L19) `boolean existsByUserId(Integer userId)`
- [CartRepository.java:21](../../src/main/java/com/project/cybershop/order/internal/repository/CartRepository.java#L21) `@Query(value = "SELECT id FROM carts WHERE user_id = :userId FOR UPDATE", nativeQuery = true) Optional<Integer> findIdByUserIdForUpdate(@Param("userId") Integer userId)`

## `order/internal/repository/CheckoutItemProjection.java`

### CheckoutItemProjection — [CheckoutItemProjection.java:5](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L5)

Khai báo: `public interface CheckoutItemProjection`

- [CheckoutItemProjection.java:7](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L7) `Integer getProductVariantId()`
- [CheckoutItemProjection.java:8](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L8) `Integer getExistingVariantId()`
- [CheckoutItemProjection.java:9](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L9) `Integer getExistingProductId()`
- [CheckoutItemProjection.java:10](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L10) `Integer getQuantity()`
- [CheckoutItemProjection.java:11](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L11) `String getProductName()`
- [CheckoutItemProjection.java:12](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L12) `String getProductStatus()`
- [CheckoutItemProjection.java:13](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L13) `String getVariantName()`
- [CheckoutItemProjection.java:14](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L14) `String getImageUrl()`
- [CheckoutItemProjection.java:15](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L15) `BigDecimal getUnitPrice()`
- [CheckoutItemProjection.java:16](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutItemProjection.java#L16) `Integer getStockQuantity()`

## `order/internal/repository/CheckoutRepository.java`

### CheckoutRepository — [CheckoutRepository.java:11](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutRepository.java#L11)

Khai báo: `public interface CheckoutRepository extends Repository<Cart, Integer>`

- [CheckoutRepository.java:13](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutRepository.java#L13) `@Query(value = """ SELECT id FROM users WHERE lower(email) = lower(:email) AND is_active = true """, nativeQuery = true) Optional<Integer> findActiveUserId(@Param("email") String email)`
- [CheckoutRepository.java:20](../../src/main/java/com/project/cybershop/order/internal/repository/CheckoutRepository.java#L20) `@Query(value = """ SELECT ci.product_variant_id AS "productVariantId", ci.quantity AS "quantity", pv.id AS "existingVariantId", p.id AS "existingProductId", p.name AS "productName", p.status AS "productStatus", pv.variant_name AS "variantName", pv.image_url AS "imageUrl", pv.price AS "unitPrice", pv.stock_quantity AS "stockQuantity" FROM cart_items ci LEFT JOIN product_variants pv ON pv.id = ci.product_variant_id LEFT JOIN products p ON p.id = pv.product_id WHERE ci.cart_id = :cartId ORDER BY ci.id """, nativeQuery = true) List<CheckoutItemProjection> findItemsForPreview(@Param("cartId") Integer cartId)`

## `order/internal/repository/OrderItemProjection.java`

### OrderItemProjection — [OrderItemProjection.java:5](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L5)

Khai báo: `public interface OrderItemProjection`

- [OrderItemProjection.java:7](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L7) `Integer getId()`
- [OrderItemProjection.java:8](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L8) `Integer getProductVariantId()`
- [OrderItemProjection.java:9](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L9) `String getProductName()`
- [OrderItemProjection.java:10](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L10) `Integer getQuantity()`
- [OrderItemProjection.java:11](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L11) `BigDecimal getUnitPrice()`
- [OrderItemProjection.java:12](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L12) `BigDecimal getSubtotal()`
- [OrderItemProjection.java:13](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L13) `Integer getWarrantyPeriodMonths()`
- [OrderItemProjection.java:14](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L14) `Integer getRating()`
- [OrderItemProjection.java:15](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemProjection.java#L15) `String getReviewComment()`

## `order/internal/repository/OrderItemRepository.java`

### OrderItemRepository — [OrderItemRepository.java:11](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemRepository.java#L11)

Khai báo: `@Repository public interface OrderItemRepository extends JpaRepository<OrderItem, Integer>`

- [OrderItemRepository.java:14](../../src/main/java/com/project/cybershop/order/internal/repository/OrderItemRepository.java#L14) `@Query(value = """ SELECT oi.id AS "id", oi.product_variant_id AS "productVariantId", oi.product_name AS "productName", oi.quantity AS "quantity", oi.unit_price AS "unitPrice", oi.subtotal AS "subtotal", oi.warranty_period_months AS "warrantyPeriodMonths", r.rating AS "rating", r.comment AS "reviewComment" FROM order_items oi LEFT JOIN reviews r ON r.order_item_id = oi.id WHERE oi.order_id = :orderId ORDER BY oi.id """, nativeQuery = true) List<OrderItemProjection> findResponseItems(@Param("orderId") Integer orderId)`

## `order/internal/repository/OrderLockRepository.java`

### OrderLockRepository — [OrderLockRepository.java:7](../../src/main/java/com/project/cybershop/order/internal/repository/OrderLockRepository.java#L7)

Khai báo: `public interface OrderLockRepository`

- [OrderLockRepository.java:9](../../src/main/java/com/project/cybershop/order/internal/repository/OrderLockRepository.java#L9) `Optional<Order> findForUpdate(String orderCode, Integer userId)`

## `order/internal/repository/OrderLockRepositoryImpl.java`

### OrderLockRepositoryImpl — [OrderLockRepositoryImpl.java:11](../../src/main/java/com/project/cybershop/order/internal/repository/OrderLockRepositoryImpl.java#L11)

Khai báo: `public class OrderLockRepositoryImpl implements OrderLockRepository`

- Trường: `@PersistenceContext private EntityManager entityManager;`
- [OrderLockRepositoryImpl.java:16](../../src/main/java/com/project/cybershop/order/internal/repository/OrderLockRepositoryImpl.java#L16) `@Override public Optional<Order> findForUpdate(String orderCode, Integer userId)`
  - Lời gọi xuất hiện: `entityManager.createQuery(""" SELECT o FROM Order o WHERE o.orderCode = :orderCode AND (:userId IS NULL OR o.userId = :userId) """, Order.class) .setParameter("orderCode", orderCode) .setParameter("userId", userId) .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList()`, `entityManager.createQuery(""" SELECT o FROM Order o WHERE o.orderCode = :orderCode AND (:userId IS NULL OR o.userId = :userId) """, Order.class) .setParameter("orderCode", orderCode) .setParameter("userId", userId).setLockMode()`, `entityManager.createQuery(""" SELECT o FROM Order o WHERE o.orderCode = :orderCode AND (:userId IS NULL OR o.userId = :userId) """, Order.class) .setParameter("orderCode", orderCode).setParameter()`, `entityManager.createQuery(""" SELECT o FROM Order o WHERE o.orderCode = :orderCode AND (:userId IS NULL OR o.userId = :userId) """, Order.class).setParameter()`, `entityManager.createQuery()`, `orders.forEach()`, `entityManager.refresh()`, `orders.stream().findFirst()`, `orders.stream()`

## `order/internal/repository/OrderRepository.java`

### OrderRepository — [OrderRepository.java:13](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L13)

Khai báo: `@Repository public interface OrderRepository extends JpaRepository<Order, Integer>, OrderLockRepository`

- [OrderRepository.java:16](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L16) `Optional<Order> findByOrderCode(String orderCode)`
- [OrderRepository.java:18](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L18) `Optional<Order> findByOrderCodeAndUserId(String orderCode, Integer userId)`
- [OrderRepository.java:20](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L20) `List<Order> findByUserIdOrderByIdDesc(Integer userId)`
- [OrderRepository.java:22](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L22) `@Query(""" SELECT o FROM Order o WHERE (:status = '' OR o.status = :status) AND (lower(o.orderCode) LIKE :term OR lower(coalesce(o.pickupName, '')) LIKE :term OR coalesce(o.pickupPhone, '') LIKE :term) ORDER BY o.id DESC """) List<Order> findForOperations( @Param("status") String status, @Param("term") String term, Pageable pageable )`
- [OrderRepository.java:36](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L36) `@Query(value = """ SELECT p.code FROM promotion_usages u JOIN promotions p ON p.id = u.promotion_id WHERE u.order_id = :orderId """, nativeQuery = true) Optional<String> findPromotionCode(@Param("orderId") Integer orderId)`
- [OrderRepository.java:44](../../src/main/java/com/project/cybershop/order/internal/repository/OrderRepository.java#L44) `@Query(value = """ SELECT id FROM payment_methods WHERE id = :methodId AND user_id = :userId """, nativeQuery = true) Optional<Integer> findOwnedPaymentMethodId( @Param("methodId") Integer methodId, @Param("userId") Integer userId )`

## `order/internal/service/CartService.java`

### CartService — [CartService.java:9](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L9)

Khai báo: `public interface CartService`

- [CartService.java:10](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L10) `void addToCart(Integer userId, AddToCartRequest request)`
- [CartService.java:11](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L11) `void addBatchToCart(Integer userId, List<AddToCartRequest> items)`
- [CartService.java:12](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L12) `CartResponse getMyCart(Integer userId)`
- [CartService.java:13](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L13) `void updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request)`
- [CartService.java:14](../../src/main/java/com/project/cybershop/order/internal/service/CartService.java#L14) `void removeCartItem(Integer userId, Integer cartItemId)`

## `order/internal/service/CheckoutDetailsService.java`

### CheckoutDetailsService — [CheckoutDetailsService.java:6](../../src/main/java/com/project/cybershop/order/internal/service/CheckoutDetailsService.java#L6)

Khai báo: `public interface CheckoutDetailsService`

- [CheckoutDetailsService.java:7](../../src/main/java/com/project/cybershop/order/internal/service/CheckoutDetailsService.java#L7) `CheckoutDetailsResponse preview(String loginEmail, CheckoutDetailsRequest request)`

## `order/internal/service/CheckoutService.java`

### CheckoutService — [CheckoutService.java:6](../../src/main/java/com/project/cybershop/order/internal/service/CheckoutService.java#L6)

Khai báo: `public interface CheckoutService`

- [CheckoutService.java:7](../../src/main/java/com/project/cybershop/order/internal/service/CheckoutService.java#L7) `CheckoutPreviewResponse validate(String loginEmail)`
- [CheckoutService.java:8](../../src/main/java/com/project/cybershop/order/internal/service/CheckoutService.java#L8) `CheckoutPreviewResponse validate(String loginEmail, List<Integer> itemVariantIds)`

## `order/internal/service/impl/CartServiceImpl.java`

### CartServiceImpl — [CartServiceImpl.java:25](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L25)

Khai báo: `@Service @RequiredArgsConstructor public class CartServiceImpl implements CartService`

- Trường: `private final CartRepository cartRepository;`
- Trường: `private final CartItemRepository cartItemRepository;`
- Trường: `private final CatalogInternalApi catalogApi;`
- Trường: `private final IdentityApi identityApi;`
- [CartServiceImpl.java:34](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L34) `@Override @Transactional public void addToCart(Integer userId, AddToCartRequest request)`
  - Lời gọi xuất hiện: `validateUserId()`, `request.productVariantId()`, `request.quantity()`, `identityApi.lockActiveUser()`, `requireActiveVariant()`, `product.stockQuantity()`, `cartRepository.findByUserId(userId).orElseGet()`, `cartRepository.findByUserId()`, `createCart()`, `cartItemRepository .findByCartIdAndProductVariantId(cart.getId(), request.productVariantId()).orElse()`, `cartItemRepository.findByCartIdAndProductVariantId()`, `cart.getId()`, `validateQuantity()`, `cartItem.getQuantity()`, `validateStock()`, `cartItem.setCartId()`, `cartItem.setProductVariantId()`, `cartItem.setCreatedAt()`, `LocalDateTime.now()`, `cartItem.setQuantity()`, `cartItemRepository.save()`, `touchCart()`
- [CartServiceImpl.java:73](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L73) `@Override @Transactional public void addBatchToCart(Integer userId, List<AddToCartRequest> items)`
  - Lời gọi xuất hiện: `validateUserId()`, `items.isEmpty()`, `identityApi.lockActiveUser()`, `cartRepository.findByUserId(userId).orElseGet()`, `cartRepository.findByUserId()`, `createCart()`, `item.productVariantId()`, `item.quantity()`, `requireActiveVariant()`, `product.stockQuantity()`, `product.productName()`, `cartItemRepository .findByCartIdAndProductVariantId(cart.getId(), item.productVariantId()).orElse()`, `cartItemRepository.findByCartIdAndProductVariantId()`, `cart.getId()`, `validateQuantity()`, `cartItem.getQuantity()`, `validateStock()`, `cartItem.setCartId()`, `cartItem.setProductVariantId()`, `cartItem.setCreatedAt()`, `LocalDateTime.now()`, `cartItem.setQuantity()`, `cartItemRepository.save()`, `touchCart()`
- [CartServiceImpl.java:120](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L120) `@Override @Transactional(readOnly = true) public CartResponse getMyCart(Integer userId)`
  - Lời gọi xuất hiện: `validateUserId()`, `cartRepository.findByUserId(userId).orElse()`, `cartRepository.findByUserId()`, `List.of()`, `cartItemRepository.findByCartIdOrderByIdAsc()`, `cart.getId()`, `validateQuantity()`, `cartItem.getQuantity()`, `catalogApi.findActiveVariant(cartItem.getProductVariantId()).orElse()`, `catalogApi.findActiveVariant()`, `cartItem.getProductVariantId()`, `toItemResponse()`, `items.add()`, `Math.addExact()`, `totalAmount.add()`, `itemResponse.subtotal()`
- [CartServiceImpl.java:145](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L145) `@Override @Transactional public void updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request)`
  - Lời gọi xuất hiện: `validateUserId()`, `validateCartItemId()`, `request.quantity()`, `identityApi.lockActiveUser()`, `requireCart()`, `requireOwnedItem()`, `requireActiveVariant()`, `cartItem.getProductVariantId()`, `validateStock()`, `cartItem.setQuantity()`, `cartItemRepository.save()`, `touchCart()`
- [CartServiceImpl.java:165](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L165) `@Override @Transactional public void removeCartItem(Integer userId, Integer cartItemId)`
  - Lời gọi xuất hiện: `validateUserId()`, `validateCartItemId()`, `identityApi.lockActiveUser()`, `requireCart()`, `requireOwnedItem()`, `cartItemRepository.delete()`, `touchCart()`
- [CartServiceImpl.java:179](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L179) `private PurchaseVariantResponse requireActiveVariant(Integer variantId)`
  - Lời gọi xuất hiện: `catalogApi.findActiveVariant(variantId).orElseThrow()`, `catalogApi.findActiveVariant()`
- [CartServiceImpl.java:185](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L185) `private Cart createCart(Integer userId)`
  - Lời gọi xuất hiện: `cart.setUserId()`, `cart.setUpdatedAt()`, `LocalDateTime.now()`, `cartRepository.save()`
- [CartServiceImpl.java:192](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L192) `private Cart requireCart(Integer userId)`
  - Lời gọi xuất hiện: `cartRepository.findByUserId(userId).orElseThrow()`, `cartRepository.findByUserId()`
- [CartServiceImpl.java:197](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L197) `private CartItem requireOwnedItem(Cart cart, Integer cartItemId)`
  - Lời gọi xuất hiện: `cartItemRepository.findById(cartItemId) .filter(item -> cart.getId().equals(item.getCartId())).orElseThrow()`, `cartItemRepository.findById(cartItemId).filter()`, `cartItemRepository.findById()`, `cart.getId().equals()`, `cart.getId()`, `item.getCartId()`
- [CartServiceImpl.java:203](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L203) `private CartResponse.CartItemResponse toItemResponse(CartItem cartItem, PurchaseVariantResponse product)`
  - Lời gọi xuất hiện: `cartItem.getId()`, `cartItem.getProductVariantId()`, `cartItem.getQuantity()`, `validatePrice()`, `product.price().multiply()`, `product.price()`, `BigDecimal.valueOf()`, `product.productName()`, `product.variantName()`, `product.imageUrl()`, `product.stockQuantity()`
- [CartServiceImpl.java:217](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L217) `private void validateStock(PurchaseVariantResponse product, long quantity)`
  - Lời gọi xuất hiện: `validatePrice()`, `product.stockQuantity()`
- [CartServiceImpl.java:225](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L225) `private void validatePrice(PurchaseVariantResponse product)`
  - Lời gọi xuất hiện: `product.price()`, `product.price().signum()`, `product.productName()`
- [CartServiceImpl.java:231](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L231) `private void validateQuantity(Integer quantity)`
- [CartServiceImpl.java:237](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L237) `private void validateUserId(Integer userId)`
- [CartServiceImpl.java:243](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L243) `private void validateCartItemId(Integer cartItemId)`
- [CartServiceImpl.java:249](../../src/main/java/com/project/cybershop/order/internal/service/impl/CartServiceImpl.java#L249) `private void touchCart(Cart cart)`
  - Lời gọi xuất hiện: `cart.setUpdatedAt()`, `LocalDateTime.now()`, `cartRepository.save()`

## `order/internal/service/impl/CheckoutDetailsServiceImpl.java`

### CheckoutDetailsServiceImpl — [CheckoutDetailsServiceImpl.java:13](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutDetailsServiceImpl.java#L13)

Khai báo: `@Service public class CheckoutDetailsServiceImpl implements CheckoutDetailsService`

- Trường: `private final CheckoutService checkout;`
- Trường: `private final IdentityApi identity;`
- Trường: `private final PromotionApi promotions;`
- [CheckoutDetailsServiceImpl.java:19](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutDetailsServiceImpl.java#L19) `public CheckoutDetailsServiceImpl(CheckoutService checkout, IdentityApi identity, PromotionApi promotions)`
- [CheckoutDetailsServiceImpl.java:23](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutDetailsServiceImpl.java#L23) `@Override @Transactional(readOnly=true) public CheckoutDetailsResponse preview(String email, CheckoutDetailsRequest request)`
  - Lời gọi xuất hiện: `trim()`, `request.pickupName()`, `request.pickupPhone()`, `request.note()`, `request.promotionCode()`, `name.isEmpty()`, `name.length()`, `phone.isEmpty()`, `phone.replaceAll()`, `phone.length()`, `normalizedPhone.matches()`, `code.length()`, `note.length()`, `identity.requireActiveUser()`, `checkout.validate()`, `request.itemVariantIds()`, `promotions.quote()`, `user.getId()`, `cart.totalAmount()`, `cart.cartId()`, `quote.code()`, `cart.items()`, `quote.discount()`, `cart.totalAmount().subtract()`
- [CheckoutDetailsServiceImpl.java:54](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutDetailsServiceImpl.java#L54) `private static String trim(String value)`
  - Lời gọi xuất hiện: `value.trim()`

## `order/internal/service/impl/CheckoutServiceImpl.java`

### CheckoutServiceImpl — [CheckoutServiceImpl.java:21](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutServiceImpl.java#L21)

Khai báo: `@Service @RequiredArgsConstructor @Transactional(readOnly = true) public class CheckoutServiceImpl implements CheckoutService`

- Trường: `private final CheckoutRepository checkoutRepository;`
- Trường: `private final CartRepository cartRepository;`
- Trường: `private final CheckoutMapper checkoutMapper;`
- [CheckoutServiceImpl.java:30](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutServiceImpl.java#L30) `@Override public CheckoutPreviewResponse validate(String loginEmail)`
  - Lời gọi xuất hiện: `validate()`
- [CheckoutServiceImpl.java:36](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutServiceImpl.java#L36) `@Override public CheckoutPreviewResponse validate(String loginEmail, List<Integer> itemVariantIds)`
  - Lời gọi xuất hiện: `checkoutRepository.findActiveUserId(loginEmail).orElseThrow()`, `checkoutRepository.findActiveUserId()`, `cartRepository.findByUserId(userId).orElseThrow()`, `cartRepository.findByUserId()`, `checkoutRepository.findItemsForPreview()`, `cart.getId()`, `cartItems.isEmpty()`, `itemVariantIds.isEmpty()`, `cartItems.stream() .filter(item -> allowed.contains(item.getProductVariantId())).toList()`, `cartItems.stream().filter()`, `cartItems.stream()`, `allowed.contains()`, `item.getProductVariantId()`, `cartItems.stream() .map(this::validateAndMapItem).toList()`, `cartItems.stream().map()`, `items.stream() .map(CheckoutItemResponse::subtotal).reduce()`, `items.stream().map()`, `items.stream()`
- [CheckoutServiceImpl.java:70](../../src/main/java/com/project/cybershop/order/internal/service/impl/CheckoutServiceImpl.java#L70) `private CheckoutItemResponse validateAndMapItem(CheckoutItemProjection item)`
  - Lời gọi xuất hiện: `item.getExistingVariantId()`, `item.getExistingProductId()`, `item.getProductVariantId()`, `"active".equalsIgnoreCase()`, `item.getProductStatus()`, `item.getProductName()`, `item.getQuantity()`, `item.getStockQuantity()`, `item.getUnitPrice()`, `item.getUnitPrice().signum()`, `item.getUnitPrice().multiply()`, `BigDecimal.valueOf()`, `checkoutMapper.toItemResponse()`

## `order/internal/service/impl/OrderServiceImpl.java`

### OrderServiceImpl — [OrderServiceImpl.java:50](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L50)

Khai báo: `@Service @RequiredArgsConstructor @Transactional public class OrderServiceImpl implements OrderService`

- Trường: `private final OrderRepository orderRepository;`
- Trường: `private final OrderItemRepository orderItemRepository;`
- Trường: `private final CartRepository cartRepository;`
- Trường: `private final CartItemRepository cartItemRepository;`
- Trường: `private final OrderMapper orderMapper;`
- Trường: `private final IdentityApi identityApi;`
- Trường: `private final CatalogInternalApi catalogApi;`
- Trường: `private final CheckoutDetailsService checkoutDetailsService;`
- Trường: `private final PromotionApi promotionApi;`
- Trường: `private final PaymentApi paymentApi;`
- Trường: `private final PickupWarrantyApi pickupWarrantyApi;`
- Trường: `private final Clock clock;`
- [OrderServiceImpl.java:71](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L71) `@Override public OrderResponse place(String email, String idempotencyKey, PlaceOrderRequest request)`
  - Lời gọi xuất hiện: `requireActor()`, `"Customer".equalsIgnoreCase()`, `customer.getRole()`, `createOrderCode()`, `identityApi.lockActiveUser()`, `customer.getId()`, `orderRepository.findByOrderCode()`, `existingOrder.isPresent()`, `existingOrder.get()`, `order.getUserId().equals()`, `order.getUserId()`, `orderNotFound()`, `toResponse()`, `validatePaymentMethod()`, `cartRepository.findIdByUserIdForUpdate(customer.getId()).orElseThrow()`, `cartRepository.findIdByUserIdForUpdate()`, `cartItemRepository.findForCheckout()`, `request.itemVariantIds()`, `request.itemVariantIds().isEmpty()`, `cartItems.stream() .filter(i -> selectedSet.contains(i.getProductVariantId())).toList()`, `cartItems.stream().filter()`, `cartItems.stream()`, `selectedSet.contains()`, `i.getProductVariantId()`, `cartItems.isEmpty()`, `groupQuantities()`, `catalogApi.lockVariantsForPurchase()`, `quantities.keySet()`, `checkoutDetailsService.preview()`, `request.pickupName()`, `request.pickupPhone()`, `request.note()`, `request.promotionCode()`, `promotionApi.quote()`, `checkout.totalAmount()`, `validateExpectedAmounts()`, `checkout.totalAmount().subtract()`, `voucher.discount()`, `orderMapper.toOrder()`, `LocalDateTime.now()`, `orderRepository.saveAndFlush()`, `createOrderItems()`, `order.getId()`, `promotionApi.reserve()`, `finalAmount.signum()`, `paymentApi.createCounterPayment()`, `cartItemRepository.deleteByCartIdAndProductVariantIdIn()`, `clearPurchasedCart()`
- [OrderServiceImpl.java:144](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L144) `@Override @Transactional(readOnly = true) public List<OrderResponse> myOrders(String email)`
  - Lời gọi xuất hiện: `requireActor()`, `orderRepository.findByUserIdOrderByIdDesc(customer.getId()).stream() .map(this::toResponse).toList()`, `orderRepository.findByUserIdOrderByIdDesc(customer.getId()).stream().map()`, `orderRepository.findByUserIdOrderByIdDesc(customer.getId()).stream()`, `orderRepository.findByUserIdOrderByIdDesc()`, `customer.getId()`
- [OrderServiceImpl.java:153](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L153) `@Override @Transactional(readOnly = true) public OrderResponse myOrder(String email, String orderCode)`
  - Lời gọi xuất hiện: `requireActor()`, `orderRepository.findByOrderCodeAndUserId(orderCode, customer.getId()).orElseThrow()`, `orderRepository.findByOrderCodeAndUserId()`, `customer.getId()`, `toResponse()`
- [OrderServiceImpl.java:162](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L162) `@Override @Transactional(readOnly = true) public List<OrderResponse> operations(String email, String search, String status)`
  - Lời gọi xuất hiện: `requireActor()`, `"all".equals()`, `search.trim().toLowerCase()`, `search.trim()`, `orderRepository.findForOperations(selectedStatus, searchTerm, PageRequest.of(0, 200)).stream() .map(this::toResponse).toList()`, `orderRepository.findForOperations(selectedStatus, searchTerm, PageRequest.of(0, 200)).stream().map()`, `orderRepository.findForOperations(selectedStatus, searchTerm, PageRequest.of(0, 200)).stream()`, `orderRepository.findForOperations()`, `PageRequest.of()`
- [OrderServiceImpl.java:178](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L178) `@Override public OrderResponse advance(String email, String orderCode, String nextStatus)`
  - Lời gọi xuất hiện: `requireActor()`, `findOrderForUpdate()`, `order.getStatus().equals()`, `order.getStatus()`, `toResponse()`, `expectedStatus.equals()`, `updateStatus()`
- [OrderServiceImpl.java:205](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L205) `@Override public OrderResponse collect(String email, String orderCode, CollectPaymentRequest request)`
  - Lời gọi xuất hiện: `requireActor()`, `findOrderForUpdate()`, `Set.of("canceled", "completed").contains()`, `Set.of()`, `order.getStatus()`, `validatePaymentAmount()`, `paymentApi.collect()`, `order.getId()`, `request.receivedAmount()`, `request.reference()`, `promotionApi.markUsed()`, `order.setUpdatedAt()`, `LocalDateTime.now()`, `orderRepository.saveAndFlush()`, `toResponse()`
- [OrderServiceImpl.java:226](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L226) `@Override public OrderResponse cancel(String email, String orderCode, boolean staff)`
  - Lời gọi xuất hiện: `requireActor()`, `findOrderForUpdate()`, `actor.getId()`, `"canceled".equals()`, `order.getStatus()`, `toResponse()`, `Set.of("pending", "confirmed").contains()`, `Set.of()`, `"completed".equals()`, `paymentApi.cancel()`, `order.getId()`, `findOrderItems(order.getId()).stream() .sorted(Comparator.comparing(OrderResponse.Item::productVariantId)).toList()`, `findOrderItems(order.getId()).stream().sorted()`, `findOrderItems(order.getId()).stream()`, `findOrderItems()`, `Comparator.comparing()`, `catalogApi.restoreStock()`, `item.productVariantId()`, `item.quantity()`, `promotionApi.release()`, `updateStatus()`
- [OrderServiceImpl.java:258](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L258) `@Override public OrderResponse handover(String email, String orderCode, HandoverRequest request)`
  - Lời gọi xuất hiện: `requireActor()`, `findOrderForUpdate()`, `paymentApi.get()`, `order.getId()`, `"ready_for_pickup".equals()`, `order.getStatus()`, `"SUCCESS".equals()`, `payment.status()`, `validatePaymentAmount()`, `request.recipientVerified()`, `normalizePhone(order.getPickupPhone()).equals()`, `normalizePhone()`, `order.getPickupPhone()`, `request.recipientPhone()`, `findOrderItems()`, `validateWarrantyDevices()`, `pickupWarrantyApi.issue()`, `LocalDateTime.now()`, `updateStatus()`, `toResponse()`
- [OrderServiceImpl.java:282](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L282) `private String createOrderCode(String idempotencyKey)`
  - Lời gọi xuất hiện: `UUID.fromString(idempotencyKey).toString() .replace("-", "").toUpperCase()`, `UUID.fromString(idempotencyKey).toString().replace()`, `UUID.fromString(idempotencyKey).toString()`, `UUID.fromString()`
- [OrderServiceImpl.java:292](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L292) `private void validatePaymentMethod(Integer userId, PlaceOrderRequest request)`
  - Lời gọi xuất hiện: `"store".equals()`, `request.method()`, `request.paymentMethodId()`, `orderRepository.findOwnedPaymentMethodId(request.paymentMethodId(), userId).isEmpty()`, `orderRepository.findOwnedPaymentMethodId()`
- [OrderServiceImpl.java:307](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L307) `private Map<Integer, Integer> groupQuantities(List<CartLineProjection> cartItems)`
  - Lời gọi xuất hiện: `cartItems.isEmpty()`, `item.getQuantity()`, `quantities.merge()`, `item.getProductVariantId()`
- [OrderServiceImpl.java:322](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L322) `private void validateExpectedAmounts( PlaceOrderRequest request, CheckoutDetailsResponse checkout, PromotionApi.Quote voucher )`
  - Lời gọi xuất hiện: `checkout.totalAmount().compareTo()`, `checkout.totalAmount()`, `request.expectedTotal()`, `voucher.discount().compareTo()`, `voucher.discount()`, `request.expectedDiscount()`
- [OrderServiceImpl.java:333](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L333) `private void createOrderItems(Integer orderId, Map<Integer, Integer> quantities)`
  - Lời gọi xuất hiện: `quantities.entrySet()`, `catalogApi.getPurchaseVariant()`, `entry.getKey()`, `entry.getValue()`, `"active".equalsIgnoreCase()`, `product.status()`, `product.stockQuantity()`, `product.warrantyPeriodMonths()`, `product.productName()`, `catalogApi.deductStock()`, `product.variantId()`, `product.variantName()`, `product.variantName().isBlank()`, `product.price().multiply()`, `product.price()`, `BigDecimal.valueOf()`, `items.add()`, `orderMapper.toOrderItem()`, `orderItemRepository.saveAllAndFlush()`
- [OrderServiceImpl.java:362](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L362) `private void clearPurchasedCart(Integer cartId)`
  - Lời gọi xuất hiện: `cartItemRepository.clearAfterCheckout()`, `cartRepository.findById(cartId).orElseThrow()`, `cartRepository.findById()`, `cart.setUpdatedAt()`, `LocalDateTime.now()`, `cartRepository.saveAndFlush()`
- [OrderServiceImpl.java:370](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L370) `private void validatePaymentAmount(Order order)`
  - Lời gọi xuất hiện: `paymentApi.get()`, `order.getId()`, `payment.amount().compareTo()`, `payment.amount()`, `order.getFinalAmount()`
- [OrderServiceImpl.java:377](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L377) `private List<PickupWarrantyApi.Unit> validateWarrantyDevices( List<OrderResponse.Item> items, HandoverRequest request )`
  - Lời gọi xuất hiện: `request.devices()`, `items.stream() .filter(line -> line.id().equals(device.orderItemId()) && line.warrantyPeriodMonths() > 0) .findFirst().orElseThrow()`, `items.stream() .filter(line -> line.id().equals(device.orderItemId()) && line.warrantyPeriodMonths() > 0).findFirst()`, `items.stream().filter()`, `items.stream()`, `line.id().equals()`, `line.id()`, `device.orderItemId()`, `line.warrantyPeriodMonths()`, `device.serialNumber().trim().toUpperCase()`, `device.serialNumber().trim()`, `device.serialNumber()`, `serialNumber.isEmpty()`, `serialNumbers.add()`, `suppliedQuantities.merge()`, `item.id()`, `units.add()`, `item.warrantyPeriodMonths()`, `suppliedQuantities.getOrDefault()`, `item.quantity()`
- [OrderServiceImpl.java:412](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L412) `private UserDto requireActor(String email, boolean staff)`
  - Lời gọi xuất hiện: `identityApi.requireActiveUser()`, `Set.of("staff", "admin").contains()`, `Set.of()`, `user.getRole().toLowerCase()`, `user.getRole()`
- [OrderServiceImpl.java:420](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L420) `private Order findOrderForUpdate(String orderCode, Integer userId)`
  - Lời gọi xuất hiện: `orderRepository.findForUpdate(orderCode, userId).orElseThrow()`, `orderRepository.findForUpdate()`
- [OrderServiceImpl.java:425](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L425) `private void updateStatus(Order order, String status)`
  - Lời gọi xuất hiện: `order.setStatus()`, `order.setUpdatedAt()`, `LocalDateTime.now()`, `orderRepository.saveAndFlush()`
- [OrderServiceImpl.java:431](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L431) `private OrderResponse toResponse(Order order)`
  - Lời gọi xuất hiện: `paymentApi.get()`, `order.getId()`, `orderRepository.findPromotionCode(order.getId()).orElse()`, `orderRepository.findPromotionCode()`, `findOrderItems()`, `orderMapper.toResponse()`
- [OrderServiceImpl.java:438](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L438) `private List<OrderResponse.Item> findOrderItems(Integer orderId)`
  - Lời gọi xuất hiện: `orderItemRepository.findResponseItems(orderId).stream() .map(orderMapper::toItemResponse).toList()`, `orderItemRepository.findResponseItems(orderId).stream().map()`, `orderItemRepository.findResponseItems(orderId).stream()`, `orderItemRepository.findResponseItems()`
- [OrderServiceImpl.java:444](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L444) `private ResourceNotFoundException orderNotFound()`
- [OrderServiceImpl.java:448](../../src/main/java/com/project/cybershop/order/internal/service/impl/OrderServiceImpl.java#L448) `private String normalizePhone(String phone)`
  - Lời gọi xuất hiện: `phone.replaceAll()`

## `order/internal/service/OrderService.java`

### OrderService — [OrderService.java:10](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L10)

Khai báo: `public interface OrderService`

- [OrderService.java:13](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L13) `OrderResponse place(String email, String idempotencyKey, PlaceOrderRequest request)`
- [OrderService.java:15](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L15) `List<OrderResponse> myOrders(String email)`
- [OrderService.java:17](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L17) `OrderResponse myOrder(String email, String orderCode)`
- [OrderService.java:19](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L19) `OrderResponse cancel(String email, String orderCode, boolean staff)`
- [OrderService.java:21](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L21) `List<OrderResponse> operations(String email, String search, String status)`
- [OrderService.java:23](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L23) `OrderResponse advance(String email, String orderCode, String nextStatus)`
- [OrderService.java:25](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L25) `OrderResponse collect(String email, String orderCode, CollectPaymentRequest request)`
- [OrderService.java:27](../../src/main/java/com/project/cybershop/order/internal/service/OrderService.java#L27) `OrderResponse handover(String email, String orderCode, HandoverRequest request)`

