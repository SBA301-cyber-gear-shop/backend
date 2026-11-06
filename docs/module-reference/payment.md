# Tra cứu module payment

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

Không tìm thấy HTTP mapping trực tiếp trong module này.

## Khai báo và lời gọi theo file

## `payment/api/PaymentApi.java`

### PaymentApi — [PaymentApi.java:6](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L6)

Khai báo: `public interface PaymentApi`

- [PaymentApi.java:8](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L8) `void createCounterPayment(Integer orderId, BigDecimal amount)`
- [PaymentApi.java:9](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L9) `Payment get(Integer orderId)`
- [PaymentApi.java:10](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L10) `void collect(Integer orderId, BigDecimal receivedAmount, String reference)`
- [PaymentApi.java:11](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L11) `void cancel(Integer orderId)`

### Payment — [PaymentApi.java:7](../../src/main/java/com/project/cybershop/payment/api/PaymentApi.java#L7)

Khai báo: `record Payment(String status, String methodType, BigDecimal amount, LocalDateTime paidAt)`


## `payment/internal/service/impl/PaymentServiceImpl.java`

### PaymentServiceImpl — [PaymentServiceImpl.java:14](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L14)

Khai báo: `@Service @Transactional public class PaymentServiceImpl implements PaymentService`

- Trường: `private final JdbcTemplate jdbc;`
- Trường: `private final Clock clock;`
- [PaymentServiceImpl.java:19](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L19) `public PaymentServiceImpl(JdbcTemplate jdbc, Clock clock)`
- [PaymentServiceImpl.java:21](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L21) `@Override public void createCounterPayment(Integer orderId, BigDecimal amount)`
  - Lời gọi xuất hiện: `amount.signum()`, `jdbc.update()`, `LocalDateTime.now()`
- [PaymentServiceImpl.java:30](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L30) `@Override @Transactional(readOnly = true) public Payment get(Integer orderId)`
  - Lời gọi xuất hiện: `jdbc.query()`, `rs.getString()`, `rs.getBigDecimal()`, `rs.getObject()`, `payments.isEmpty()`, `payments.get()`
- [PaymentServiceImpl.java:39](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L39) `@Override public void collect(Integer orderId, BigDecimal receivedAmount, String reference)`
  - Lời gọi xuất hiện: `jdbc.queryForList()`, `get()`, `receivedAmount.compareTo()`, `payment.amount()`, `"SUCCESS".equals()`, `payment.status()`, `"cash".equals()`, `payment.methodType()`, `"PENDING".equals()`, `jdbc.update()`, `LocalDateTime.now()`, `reference.isBlank()`, `reference.trim()`
- [PaymentServiceImpl.java:52](../../src/main/java/com/project/cybershop/payment/internal/service/impl/PaymentServiceImpl.java#L52) `@Override public void cancel(Integer orderId)`
  - Lời gọi xuất hiện: `get()`, `"SUCCESS".equals()`, `payment.status()`, `payment.amount().signum()`, `payment.amount()`, `jdbc.update()`

