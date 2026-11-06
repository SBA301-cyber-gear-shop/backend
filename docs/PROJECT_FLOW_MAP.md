# Bản đồ luồng backend CyberShop

Ngày khảo sát: 2026-11-05. Đây là ghi chú đọc source tĩnh, không phải xác nhận mọi nhánh chạy thành công. Kiểm tra lại source khi thay đổi. Phạm vi: backend; chưa khảo sát FE và DB độc lập.

## Luồng tổng quát

`CybershopApplication` khởi động Spring Boot. Request đi qua `shared/config/SecurityConfig` và `JwtAuthenticationFilter`, rồi controller → service → repository/JPA → database. API liên module thường nằm trong `<module>/api`. Xem `docs/ARCHITECTURE.md` để biết quy ước và chi tiết transaction.

## Các module

- **identity**: `AuthController` (`/api/auth`) → `AuthServiceImpl`; đăng ký, đăng nhập, thông tin tài khoản. `IdentityApi` cung cấp tài khoản/khóa tài khoản cho các module khác; JWT nằm trong `shared/config`.
- **catalog**: `CatalogController`, `StorefrontCatalogController` và các `Admin*Controller` → service → repository; quản lý sản phẩm, phiên bản, danh mục, thương hiệu, thông số. `CatalogInternalApi` cung cấp tra cứu/khóa/cập nhật tồn cho order; `CatalogPublicApi` cung cấp ứng viên và thông số cho pcbuild. `CatalogServiceImpl` dùng `ReviewApi`.
- **order**: `CartController` → `CartServiceImpl` dùng identity và catalog; `CheckoutController`/`CheckoutDetailsController` kiểm tra và preview. `OrderController` và `OrderOperationsController` → `OrderServiceImpl`, điều phối đặt đơn, tiến trình, thu tiền, hủy và bàn giao.
- **promotion**: `PromotionController` → `PromotionServiceImpl` → repository; công khai promotion và quản trị. `PromotionApi` được order dùng để báo giá, giữ lượt, xác nhận sử dụng hoặc trả lượt khi hủy.
- **payment**: `PaymentServiceImpl` cung cấp `PaymentApi` cho order tạo thanh toán tại quầy, thu tiền, tra cứu và hủy. `OrderServiceImpl.validatePaymentMethod` chỉ chấp nhận phương thức tại cửa hàng; chưa cấu hình online.
- **aftersales**: bàn giao qua `PickupWarrantyApi` tạo bảo hành; `PickupWarrantyController` cho khách xem thẻ. `ServiceIntakeController`, `ServiceRequestController`, `WarrantyCardController`, `MyWarrantyController` xử lý tiếp nhận, yêu cầu, lịch sử và thẻ; `ServiceRequestService` dùng identity và catalog.
- **review**: `ReviewController` → `ReviewServiceImpl` → repository; ghi đánh giá theo dòng đơn và đọc đánh giá sản phẩm. Có điều kiện đơn hoàn tất và quyền sở hữu; catalog dùng `ReviewApi`.
- **pcbuild**: `PcBuildController` (`/api/v1/pc-build`) → `PcCustomServiceImpl` hoặc `AiPcBuilderServiceImpl`. Đọc linh kiện qua `CatalogPublicApi`; hỗ trợ kiểm tra, thay thế, lưu và gợi ý cấu hình.
- **shared**: bảo mật JWT, cấu hình và thành phần dùng chung; không phải module nghiệp vụ độc lập.

## Đặt hàng nhận tại cửa hàng

1. Giỏ hàng kiểm tra sản phẩm/tồn; giỏ chưa giữ tồn. Checkout validate/preview tính thử.
2. `OrderServiceImpl.place`: xác thực Customer, kiểm tra UUID idempotency, khóa user, tìm đơn đã tạo, kiểm tra phương thức; đọc giỏ và khóa variants; preview, báo giá voucher, kiểm tra số tiền kỳ vọng; lưu đơn/dòng hàng, giữ voucher, tạo payment và xóa các dòng đã mua. Các thao tác thuộc transaction của service.
3. `advance`: `pending → confirmed → preparing → ready_for_pickup`. `collect` thu tiền và đánh dấu voucher đã dùng.
4. `handover`: cần ready_for_pickup và payment SUCCESS, đối chiếu người nhận và serial, cấp bảo hành rồi chuyển completed.
5. `cancel`: kiểm tra quyền/trạng thái, yêu cầu payment hủy, hoàn tồn và trả voucher. Khách chỉ hủy pending/confirmed; điều kiện thanh toán được kiểm tra trong payment.
6. Review cho đơn hoàn tất; bảo hành và yêu cầu hậu mãi tiếp tục qua aftersales.

Chi tiết và điều kiện cần xác minh trong `order/internal/service/impl/OrderServiceImpl.java`, `payment/internal/service/impl/PaymentServiceImpl.java` và `docs/ARCHITECTURE.md`.

## Gợi ý PC bằng AI

`PcBuildController.aiSuggest` → `AiPcBuilderServiceImpl.processAiBuild`: lấy một snapshot ứng viên, validate request/ngân sách; lấy profile người dùng hoặc trích xuất profile; bổ sung preset và validate; tạo candidate pool → solver chọn bộ linh kiện → AI giải thích → compatibility engine đánh giá lại → response. Source có deadline chung cho AI và xử lý lỗi giải thích; các rule chi tiết nằm trong `RuleBasedCompatibilityEngine` và solver.

## Tình trạng kiểm tra

- Graphify: đã tạo `graphify-out/graph.json`, `graph.html` và `GRAPH_REPORT.md` bằng môi trường riêng. Kiểm tra tính toàn vẹn báo 1.490 cạnh có đầu mút thiếu và các cạnh bị gộp; cần đối chiếu source trước khi kết luận. Graph được tạo trước lần cập nhật ghi chú này.
- Maven: `mvn -o '-Dmaven.repo.local=C:\Users\dat\.m2\repository' test -q` thất bại lúc compile do lỗi liên quan file `commons-lang3-3.20.0.jar`; chưa chạy được test. Lệnh mặc định cũng không tạo được local repository tại `C:\.m2\repository`.
- Có test hiện hữu cho purchase, pickup, promotion, catalog, aftersales và pcbuild trong `src/test`; sự tồn tại của test không chứng minh chúng đang pass.
- Chưa kiểm tra exhaustively từng method, từng nhánh, bảo mật, schema PostgreSQL thực tế hoặc tích hợp FE. Không khẳng định không có lỗi.
