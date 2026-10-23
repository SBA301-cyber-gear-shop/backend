Danh sách module

Quy ước package: xem [ARCHITECTURE.md](docs/ARCHITECTURE.md). Service nghiệp vụ có interface trong `internal.service` và implementation trong `internal.service.impl`; controller/DTO/mapper đã được tổ chức tương ứng.

Promotion/review dùng Spring Data JPA: entity ánh xạ bảng hiện tại, `JpaRepository` truy cập dữ liệu, MapStruct mapper chuyển DTO/entity. Service không chứa SQL hoặc `JdbcTemplate`.

Flow mua hàng hiện hỗ trợ đặt hàng và thanh toán tại cửa hàng, promotion, bàn giao/bảo hành từng thiết bị và đánh giá sau mua. Hướng dẫn xuyên suốt nằm trong `D:\frontend\docs\PICKUP_API_INTEGRATION.md`.

| API | Quyền truy cập |
|---|---|
| `GET /api/products` | Công khai; dữ liệu theo từng phiên bản và thông số |
| `GET /api/orders/cart` | JWT; giỏ hàng của tài khoản hiện tại |
| `POST /api/orders/cart/items` | JWT; thêm `{productVariantId, quantity}` |
| `PUT /api/orders/cart/items/{cartItemId}` | JWT; sửa `{quantity}` |
| `DELETE /api/orders/cart/items/{cartItemId}` | JWT; xóa dòng thuộc giỏ hiện tại |
| `POST /api/checkout/validate` | JWT; kiểm tra toàn bộ giỏ, không giữ tồn |
| `POST /api/checkout/details/preview` | JWT; tính thử `{pickupName, pickupPhone, note, promotionCode}` |
| `POST /api/orders` | Customer; header UUID `Idempotency-Key`, tạo đơn và giữ tồn/voucher trong transaction |
| `GET /api/orders`, `GET /api/orders/{orderCode}` | Chủ đơn |
| `POST /api/orders/{orderCode}/cancel` | Chủ đơn; pending/confirmed, chưa thu tiền dương |
| `GET /api/operations/orders` | Staff/Admin; tìm và lọc đơn |
| `PATCH /api/operations/orders/{orderCode}/status` | Staff/Admin; confirmed → preparing → ready_for_pickup |
| `POST /api/operations/orders/{orderCode}/collect` | Staff/Admin; nhập tiền thực nhận, xác nhận đủ |
| `POST /api/operations/orders/{orderCode}/handover` | Staff/Admin; đủ tiền, đối chiếu người nhận và serial |
| `POST /api/operations/orders/{orderCode}/cancel` | Staff/Admin; trước completed và chưa thu tiền dương |
| `GET /api/me/warranties` | Thẻ bảo hành thuộc khách hiện tại |
| `GET /api/promotions` | Banner promotion hợp lệ công khai |
| `GET/POST /api/admin/promotions`, `PUT /api/admin/promotions/{id}` | Admin; quản lý promotion và banner |
| `PUT /api/order-items/{id}/review` | Chủ đơn completed; tạo/sửa đánh giá |
| `GET /api/products/{productVariantId}/reviews` | Đánh giá công khai từ đơn completed |

Code dùng bảng chữ thường đúng schema và khóa ngoại hiện tại. Các bảng chữ hoa cũ không được tự di chuyển/xóa. Nếu tài khoản chỉ tồn tại ở `"Users"`, cần chuyển dữ liệu vào `users` với ánh xạ ID phù hợp.

Preview không tạo đơn, trừ tồn hay ghi lượt dùng voucher. Tạo đơn khóa tồn/voucher, chụp giá/tên/bảo hành, tạo payment và xóa giỏ trong một transaction. Retry cùng key trả cùng đơn. Hủy hoàn tồn và trả lượt chung một lần; user không dùng lại cùng voucher theo ràng buộc SQL hiện tại. Thu đủ tiền đổi voucher RESERVED → USED. Chỉ bàn giao thành công mới kích hoạt bảo hành; đủ serial cho từng thiết bị có bảo hành. Review chỉ sau completed, một đánh giá mỗi dòng hàng.

Hiện hỗ trợ tiền mặt tại cửa hàng; thanh toán online và hoàn tiền chưa tích hợp. Đơn đã thu tiền dương bị chặn hủy đến khi có quy trình hoàn tiền. Thời gian nghiệp vụ dùng Asia/Bangkok (UTC+07:00).

Test `.\mvnw.cmd test` dùng H2 cục bộ thay vì Neon, gồm kiểm thử cạnh tranh tồn/voucher và rollback nếu tạo payment hoặc serial thất bại. Chưa thay thế kiểm thử tích hợp PostgreSQL thực tế. Không tự chạy script SQL gốc vì script có lệnh xóa schema.

| Module | Thực thể sở hữu | Trách nhiệm |
|---|---|---|
| `identity` | `users`, `roles` | Tài khoản, xác thực, vai trò, trạng thái hoạt động |
| `catalog` | `brands`, `categories`, `products`, `product_variants`, `specifications`, `category_specifications`, `specification_values` | Danh mục sản phẩm, giá, thông số, tìm kiếm, lọc và cập nhật tồn trong schema hiện tại |
| `order` | `orders`, `order_items` | Checkout, tính tiền đơn, điều phối mua hàng, trạng thái đơn, hủy và bàn giao |
| `payment` | `payment_methods`, `payments` | Phương thức thanh toán, thanh toán online/tại quầy, xác minh kết quả |
| `promotion` | `promotions`, `promotion_usages`, `banners` | Voucher, hạn mức, lượt sử dụng và banner |
| `pcbuild` | `pc_customs`, `pc_custom_items` | Build thủ công, kiểm tra tương thích, gợi ý AI và quản lý cấu hình |
| `aftersales` | `warranty_cards`, `service_requests` | Thẻ bảo hành và xử lý bảo hành |
| `review` | `reviews` | Đánh giá sau mua |
