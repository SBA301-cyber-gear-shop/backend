# Quy ước package backend

Cấu trúc áp dụng cho identity, catalogue, giỏ/checkout/đơn hàng, payment, promotion, bảo hành và review:

```text
<module>/
├── api/
│   ├── <Module>Api.java
│   └── dto/
│       ├── request/
│       └── response/
└── internal/
    ├── controller/
    ├── entity/
    ├── mapper/
    ├── repository/
    └── service/
        ├── <Feature>Service.java          # interface
        └── impl/
            └── <Feature>ServiceImpl.java  # implements interface
```

- Controller inject service interface. Các service cũng phụ thuộc interface khi gọi nhau.
- Interface khai báo contract; không đặt `@Service` trên interface.
- Implementation đặt trong `service.impl`, có `@Service`, `@Override` và các annotation transaction phù hợp.
- Mapper MapStruct đặt trong `internal.mapper`. Lớp khởi tạo dữ liệu hiện nằm trong `service.impl` và đã triển khai `CommandLineRunner`; không tạo thêm service interface cho runner.
- DTO request/response được tách theo package. Việc đổi package không đổi HTTP route hoặc JSON contract.

Ví dụ flow checkout:

```text
CheckoutDetailsController
  → CheckoutDetailsService (interface)
    → CheckoutDetailsServiceImpl
      → CheckoutService (interface)
        → CheckoutServiceImpl
```

## Mapping database

Đối chiếu với `SBA301-scriptDB.sql` do người dùng cung cấp. Bảng được khai báo bằng tên chữ thường: `users`, `roles`, `carts`, `cart_items`, `products`, `product_variants`, `promotions`, `promotion_usages`, `warranty_cards`…

Entity phải khai báo `@Table(name = "tên_bảng_trong_SQL")` chính xác. Ví dụ lớp `CartItem` map `cart_items`; không map `"Cart_Items"`. Không đổi tên bảng để giống tên lớp Java. SQL chỉ được đọc để đối chiếu, không tự chạy script tạo lại schema.

## Kiểm tra sau khi di chuyển package

Chạy Java 21 và `.\mvnw.cmd clean test` để loại bỏ class/MapStruct output từ package cũ. Test dùng schema H2 riêng trong `src/test/resources`, không dùng Neon. Các test kiểm tra catalogue, JWT, quyền sở hữu, checkout, tạo đơn lặp, promotion, thu tiền, bàn giao, review và rollback/cạnh tranh tồn/voucher.

## Điều phối flow nhận tại cửa hàng

`OrderServiceImpl` điều phối transaction đặt/hủy/thu tiền/bàn giao. Các chức năng liên module gọi qua `IdentityApi`, `PromotionApi`, `PaymentApi`, `PickupWarrantyApi`; controller luôn inject service interface.

Promotion và review dùng Spring Data JPA: `Controller → Service interface → ServiceImpl → JpaRepository → Entity`. MapStruct mapper chuyển DTO/entity; service xử lý nghiệp vụ và transaction, không chứa SQL hoặc gọi `JdbcTemplate`. Entity map đúng bảng `promotions`, `promotion_usages`, `banners`, `reviews` đã có trong SQL.

Promotion dùng repository fragment JPA với `PESSIMISTIC_WRITE` và refresh sau khi khóa để kiểm tra lại dữ liệu hiện tại khi đặt hàng/sửa hạn mức. Preview dùng projection không đưa entity vào persistence context trước khi checkout lấy khóa. Review có hai native query đặt trong `ReviewRepository` để khóa đơn thuộc module order và đọc đánh giá công khai qua các bảng hiện có; CRUD đánh giá dùng `JpaRepository`. Không tạo entity trùng cho bảng thuộc module khác.

Khóa user dùng chung với thay đổi giỏ; khóa phiên bản theo thứ tự ID và khóa promotion trước khi giữ lượt. Tất cả ghi đơn, dòng hàng, tồn, voucher, payment và dọn giỏ thuộc cùng transaction. Trạng thái đơn được khóa khi xử lý, serial và thẻ bảo hành được tạo trong transaction bàn giao. Review khóa đơn trước khi tạo/cập nhật để không sinh nhiều đánh giá từ các request đồng thời.

## Checkout và đơn hàng

Giỏ hàng đọc sản phẩm qua `CartController → CartService → CartServiceImpl → CatalogInternalApi → ProductVariantRepository`. Module catalog dùng `products`/`product_variants` hiện có; không tạo lại `ProductLookup` hoặc entity sản phẩm trong module order. `findActiveVariant` trả giá, tên, ảnh và tồn kho hiện tại của phiên bản có sản phẩm đang `ACTIVE`; phiên bản không tồn tại hoặc sản phẩm ngừng bán trả về rỗng.

Thêm/cập nhật giỏ kiểm tra sản phẩm và tồn kho; xem giỏ giữ dòng sản phẩm ngừng bán với `available=false` để khách xóa được. Thay đổi giỏ khóa user qua `IdentityApi.lockActiveUser`, dùng chung cơ chế với đặt hàng. Giỏ không giữ/trừ tồn; khi đặt hàng, `OrderServiceImpl` khóa phiên bản, đọc lại giá/tồn qua catalog rồi trừ tồn trong transaction tạo đơn. HTTP route và cấu trúc `CartResponse` giữ nguyên.

`CheckoutServiceImpl` và `OrderServiceImpl` dùng cùng mẫu `Service interface → ServiceImpl → Repository → Entity`. Service chỉ kiểm tra nghiệp vụ, điều phối API giữa các module và quản lý transaction; không chứa SQL hoặc `JdbcTemplate`.

- `Order` và `OrderItem` map đúng hai bảng `orders`, `order_items`; CRUD qua `OrderRepository`, `OrderItemRepository`.
- `CheckoutMapper` và `OrderMapper` dùng MapStruct để chuyển projection/entity thành DTO, giữ nguyên HTTP route và JSON contract.
- Các truy vấn đọc liên bảng cho checkout, đánh giá và mã voucher nằm ở repository dưới dạng projection; không tạo entity trùng cho bảng của module khác.
- Khóa user đi qua `IdentityApi`; khóa/trừ/hoàn tồn đi qua `CatalogInternalApi`. Trừ tồn dùng cập nhật có điều kiện ở repository để không bán vượt tồn.
- `OrderLockRepository` lấy `PESSIMISTIC_WRITE` rồi refresh entity trước khi kiểm tra trạng thái. Dòng giỏ và giá/tồn hiện tại được đọc bằng projection để không dùng entity cũ trong persistence context.
- Ghi đơn/dòng hàng được flush trước khi payment, promotion hoặc bảo hành đọc bằng kết nối trong cùng transaction. Thất bại ở một bước phải rollback toàn bộ flow.

Định dạng service: import tường minh, dependency `final` với `@RequiredArgsConstructor`, tên biến đầy đủ, mỗi câu lệnh một dòng, điều kiện có dấu ngoặc và tách các bước kiểm tra/tạo DTO thành hàm có tên rõ nghĩa. API public hiện tại được giữ nguyên để frontend tiếp tục sử dụng.
