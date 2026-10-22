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
