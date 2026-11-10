package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.aftersales.api.PickupWarrantyApi;
import com.project.cybershop.catalog.api.CatalogInternalApi;
import com.project.cybershop.catalog.api.dto.response.PurchaseVariantResponse;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.order.api.dto.request.CheckoutDetailsRequest;
import com.project.cybershop.order.api.dto.request.CollectPaymentRequest;
import com.project.cybershop.order.api.dto.request.HandoverRequest;
import com.project.cybershop.order.api.dto.request.PlaceOrderRequest;
import com.project.cybershop.order.api.dto.response.CashCollectionResponse;
import com.project.cybershop.order.api.dto.response.CheckoutDetailsResponse;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.entity.Order;
import com.project.cybershop.order.internal.entity.OrderItem;
import com.project.cybershop.order.internal.mapper.OrderMapper;
import com.project.cybershop.order.internal.repository.CartItemRepository;
import com.project.cybershop.order.internal.repository.CartLineProjection;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.order.internal.repository.OrderItemRepository;
import com.project.cybershop.order.internal.repository.OrderRepository;
import com.project.cybershop.order.internal.service.CheckoutDetailsService;
import com.project.cybershop.order.internal.service.OrderService;
import com.project.cybershop.payment.api.PaymentApi;
import com.project.cybershop.promotion.api.PromotionApi;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;
    private final IdentityApi identityApi;
    private final CatalogInternalApi catalogApi;
    private final CheckoutDetailsService checkoutDetailsService;
    private final PromotionApi promotionApi;
    private final PaymentApi paymentApi;
    private final PickupWarrantyApi pickupWarrantyApi;
    private final Clock clock;
    private final JdbcTemplate jdbc;

    // =========================================
    // ĐẶT HÀNG VÀ GIỮ TỒN KHO
    // =========================================
    @Override
    public OrderResponse place(String email, String idempotencyKey, PlaceOrderRequest request) {
        UserDto customer = requireActor(email, false);
        if (!"Customer".equalsIgnoreCase(customer.getRole())) {
            throw new AccessDeniedException("Chỉ tài khoản khách hàng được đặt hàng.");
        }

        String orderCode = createOrderCode(idempotencyKey);

        // Dùng chung khóa user với thao tác giỏ để giỏ không đổi giữa lúc checkout.
        identityApi.lockActiveUser(customer.getId());

        Optional<Order> existingOrder = orderRepository.findByOrderCode(orderCode);
        if (existingOrder.isPresent()) {
            Order order = existingOrder.get();
            if (!order.getUserId().equals(customer.getId())) {
                throw orderNotFound();
            }
            return toResponse(order);
        }

        validatePaymentMethod(customer.getId(), request);

        Integer cartId = cartRepository.findIdByUserIdForUpdate(customer.getId())
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống."));

        List<CartLineProjection> cartItems = cartItemRepository.findForCheckout(cartId);
        if (request.itemVariantIds() != null && !request.itemVariantIds().isEmpty()) {
            Set<Integer> selectedSet = new HashSet<>(request.itemVariantIds());
            cartItems = cartItems.stream()
                    .filter(i -> selectedSet.contains(i.getProductVariantId()))
                    .toList();
            if (cartItems.isEmpty()) {
                throw new BusinessException("Không có sản phẩm nào được chọn trong giỏ hàng.");
            }
        }
        Map<Integer, Integer> quantities = groupQuantities(cartItems);
        validatePcGroups(request.pcGroups(), quantities);
        catalogApi.lockVariantsForPurchase(new ArrayList<>(quantities.keySet()));
        promotionApi.syncProductPrices(new ArrayList<>(quantities.keySet()));

        CheckoutDetailsResponse checkout = checkoutDetailsService.preview(
                email,
                new CheckoutDetailsRequest(
                        request.pickupName(), request.pickupPhone(), request.note(), request.promotionCode(),
                        request.itemVariantIds()
                )
        );

        PromotionApi.Quote voucher = promotionApi.quote(
                customer.getId(), request.promotionCode(), checkout.totalAmount(), true
        );
        validateExpectedAmounts(request, checkout, voucher);

        BigDecimal finalAmount = checkout.totalAmount().subtract(voucher.discount());
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (request.deliveryType() != null && "shipping".equalsIgnoreCase(request.deliveryType())) {
            shippingFee = request.shippingFee() != null ? request.shippingFee() : BigDecimal.ZERO;
            finalAmount = finalAmount.add(shippingFee);
        }

        Order order = orderMapper.toOrder(
                checkout, customer.getId(), orderCode, voucher.discount(), finalAmount, LocalDateTime.now(clock)
        );
        if (request.deliveryType() != null && "shipping".equalsIgnoreCase(request.deliveryType())) {
            order.setDeliveryType("shipping");
            order.setShippingFee(shippingFee);
            order.setShippingAddress(request.shippingAddress());
            order.setToProvinceId(request.toProvinceId());
            order.setToDistrictId(request.toDistrictId());
            order.setToWardCode(request.toWardCode());
            order.setCarrier("GHN");
        } else {
            order.setDeliveryType("store");
            order.setShippingFee(BigDecimal.ZERO);
        }
        order = orderRepository.saveAndFlush(order);

        createOrderItems(order.getId(), customer.getId(), quantities, request.pcGroups());
        promotionApi.reserve(voucher, customer.getId(), order.getId(), finalAmount.signum() == 0);

        String paymentUrl = null;
        if ("vnpay".equalsIgnoreCase(request.method())) {
            paymentUrl = paymentApi.createVnPayPayment(order.getId(), order.getOrderCode(), finalAmount, "127.0.0.1");
        } else {
            paymentApi.createCounterPayment(order.getId(), finalAmount);
        }

        if (request.itemVariantIds() != null && !request.itemVariantIds().isEmpty()) {
            clearPurchasedItems(cartId, quantities, request.pcGroups());
        } else {
            clearPurchasedCart(cartId);
        }

        return toResponse(order, paymentUrl);
    }

    // =========================================
    // TRA CỨU ĐƠN HÀNG
    // =========================================
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String email) {
        UserDto customer = requireActor(email, false);
        return orderRepository.findByUserIdOrderByIdDesc(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse myOrder(String email, String orderCode) {
        UserDto customer = requireActor(email, false);
        Order order = orderRepository.findByOrderCodeAndUserId(orderCode, customer.getId())
                .orElseThrow(this::orderNotFound);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> operations(String email, String search, String status) {
        requireActor(email, true);

        String selectedStatus = status == null || "all".equals(status) ? "" : status;
        String searchTerm = "%" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT)) + "%";

        return orderRepository.findForOperations(selectedStatus, searchTerm, PageRequest.of(0, 200)).stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================
    // XÁC NHẬN VÀ CHUẨN BỊ HÀNG
    // =========================================
    @Override
    public OrderResponse advance(String email, String orderCode, String nextStatus) {
        requireActor(email, true);
        Order order = findOrderForUpdate(orderCode, null);

        if (order.getStatus().equals(nextStatus)) {
            return toResponse(order);
        }

        String expectedStatus = switch (order.getStatus()) {
            case "pending" -> "confirmed";
            case "confirmed" -> "preparing";
            case "preparing" -> "ready_for_pickup";
            case "ready_for_pickup" -> "shipping".equalsIgnoreCase(order.getDeliveryType()) ? "delivering" : "";
            case "delivering" -> "completed";
            default -> "";
        };

        if (!expectedStatus.equals(nextStatus)) {
            throw new BusinessException("Không được bỏ qua hoặc quay lại bước xử lý đơn.");
        }

        updateStatus(order, nextStatus);

        if ("completed".equals(nextStatus)) {
            try {
                PaymentApi.Payment payment = paymentApi.get(order.getId());
                if (payment != null && !"SUCCESS".equalsIgnoreCase(payment.status())) {
                    paymentApi.collect(order.getId(), order.getFinalAmount(), "COD_GHN");
                }
            } catch (Exception ignored) {}
            try {
                promotionApi.markUsed(order.getId());
            } catch (Exception ignored) {}
        }
        return toResponse(order);
    }

    // =========================================
    // THU TIỀN TẠI QUẦY
    // =========================================
    @Override
    public OrderResponse collect(String email, String orderCode, CollectPaymentRequest request) {
        UserDto collector = requireActor(email, true);
        Order order = findOrderForUpdate(orderCode, null);

        if (!"store".equalsIgnoreCase(order.getDeliveryType())) {
            throw new BusinessException("Đơn giao tận nơi không thu tiền tại quầy.");
        }
        if (Set.of("canceled", "completed").contains(order.getStatus())) {
            throw new BusinessException("Đơn không còn được thu tiền.");
        }

        PaymentApi.Payment payment = validatePaymentAmount(order);
        if (!"cash".equals(payment.methodType())) {
            throw new BusinessException("Chỉ xác nhận thu tiền mặt tại quầy.");
        }
        if ("SUCCESS".equals(payment.status())) {
            return toResponse(order);
        }

        String reference = request.reference() == null || request.reference().isBlank()
                ? null : request.reference().trim();
        paymentApi.collect(order.getId(), request.receivedAmount(), reference);
        LocalDateTime paidAt = paymentApi.get(order.getId()).paidAt();
        jdbc.update("""
                INSERT INTO order_cash_collections
                    (order_id, collector_user_id, collector_name, collected_at, amount_due, received_amount, reference)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, order.getId(), collector.getId(), collector.getFullName(), paidAt,
                payment.amount(), request.receivedAmount(), reference);
        promotionApi.markUsed(order.getId());

        order.setUpdatedAt(LocalDateTime.now(clock));
        orderRepository.saveAndFlush(order);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashCollectionResponse> cashCollection(String email, String orderCode) {
        requireActor(email, true);
        Order order = orderRepository.findByOrderCode(orderCode).orElseThrow(this::orderNotFound);
        return jdbc.query("""
                SELECT collector_user_id, collector_name, collected_at, received_amount,
                       received_amount - amount_due AS change_amount, reference
                FROM order_cash_collections WHERE order_id = ?
                """, (rs, rowNum) -> new CashCollectionResponse(
                rs.getInt("collector_user_id"), rs.getString("collector_name"),
                rs.getObject("collected_at", LocalDateTime.class), rs.getBigDecimal("received_amount"),
                rs.getBigDecimal("change_amount"), rs.getString("reference")), order.getId())
                .stream().findFirst();
    }

    // =========================================
    // HỦY ĐƠN VÀ HOÀN TỒN KHO
    // =========================================
    @Override
    public OrderResponse cancel(String email, String orderCode, boolean staff, String reason) {
        UserDto actor = requireActor(email, staff);
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException("Vui lòng nhập lý do hủy đơn từ 1 đến 500 ký tự.");
        }
        Order order = findOrderForUpdate(orderCode, staff ? null : actor.getId());

        if ("canceled".equals(order.getStatus())) {
            return toResponse(order);
        }

        boolean customerCanCancel = Set.of("pending", "confirmed").contains(order.getStatus());
        if ("completed".equals(order.getStatus()) || (!staff && !customerCanCancel)) {
            throw new BusinessException("Đơn không còn được hủy tại bước này.");
        }

        paymentApi.cancel(order.getId());

        List<OrderResponse.Item> items = findOrderItems(order.getId()).stream()
                .sorted(Comparator.comparing(OrderResponse.Item::productVariantId))
                .toList();

        for (OrderResponse.Item item : items) {
            catalogApi.restoreStock(item.productVariantId(), item.quantity());
        }

        promotionApi.release(order.getId());
        order.setCancelReason(reason.trim());
        updateStatus(order, "canceled");
        return toResponse(order);
    }

    // =========================================
    // BÀN GIAO VÀ KÍCH HOẠT BẢO HÀNH
    // =========================================
    @Override
    public OrderResponse handover(String email, String orderCode, HandoverRequest request) {
        requireActor(email, true);
        Order order = findOrderForUpdate(orderCode, null);
        PaymentApi.Payment payment = paymentApi.get(order.getId());

        if (!"ready_for_pickup".equals(order.getStatus()) || !"SUCCESS".equals(payment.status())) {
            throw new BusinessException("Đơn phải sẵn sàng nhận và đã thanh toán đủ trước khi bàn giao.");
        }

        validatePaymentAmount(order);
        if (!request.recipientVerified()
                || !normalizePhone(order.getPickupPhone()).equals(normalizePhone(request.recipientPhone()))) {
            throw new BusinessException("Thông tin người nhận chưa được đối chiếu chính xác.");
        }

        List<OrderResponse.Item> items = findOrderItems(order.getId());
        List<PickupWarrantyApi.Unit> units = validateWarrantyDevices(items, request.devices());

        pickupWarrantyApi.issue(units, LocalDateTime.now(clock));
        updateStatus(order, "completed");
        return toResponse(order);
    }

    private String createOrderCode(String idempotencyKey) {
        try {
            return "TZ-" + UUID.fromString(idempotencyKey).toString()
                    .replace("-", "")
                    .toUpperCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            throw new BusinessException("Idempotency-Key phải là UUID hợp lệ.");
        }
    }

    private void validatePaymentMethod(Integer userId, PlaceOrderRequest request) {
        if (!"store".equalsIgnoreCase(request.method()) && !"vnpay".equalsIgnoreCase(request.method()) && !"cod".equalsIgnoreCase(request.method())) {
            throw new BusinessException(
                    "Phương thức thanh toán không hợp lệ. Vui lòng chọn thanh toán tại cửa hàng, COD hoặc VNPAY."
            );
        }

        if (request.paymentMethodId() != null) {
            if (orderRepository.findOwnedPaymentMethodId(request.paymentMethodId(), userId).isEmpty()) {
                throw new BusinessException("Phương thức thanh toán không thuộc tài khoản của bạn.");
            }
        }
    }

    private Map<Integer, Integer> groupQuantities(List<CartLineProjection> cartItems) {
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống.");
        }

        Map<Integer, Integer> quantities = new TreeMap<>();
        for (CartLineProjection item : cartItems) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng trong giỏ hàng không hợp lệ.");
            }
            quantities.merge(item.getProductVariantId(), item.getQuantity(), Math::addExact);
        }
        return quantities;
    }

    private void validateExpectedAmounts(
            PlaceOrderRequest request,
            CheckoutDetailsResponse checkout,
            PromotionApi.Quote voucher
    ) {
        if (checkout.totalAmount().compareTo(request.expectedTotal()) != 0
                || voucher.discount().compareTo(request.expectedDiscount()) != 0) {
            throw new BusinessException("Giá hoặc ưu đãi đã thay đổi. Vui lòng kiểm tra lại trước khi xác nhận.");
        }
    }

    static void validatePcGroups(List<PlaceOrderRequest.PcGroup> groups, Map<Integer, Integer> quantities) {
        if (groups == null || groups.isEmpty()) return;
        Map<Integer, Integer> remaining = new HashMap<>(quantities);
        for (var group : groups) {
            if (group == null || group.parts() == null || group.parts().size() < 2)
                throw new BusinessException("Bộ PC không đầy đủ. Vui lòng kiểm tra giỏ hàng.");
            Set<Integer> groupParts = new HashSet<>();
            for (var part : group.parts()) {
                if (part == null || part.variantId() == null || part.quantity() == null || part.quantity() <= 0
                        || part.role() == null || part.role().isBlank() || part.role().length() > 50) {
                    throw new BusinessException("Thông tin linh kiện bộ PC không hợp lệ.");
                }
                if (!groupParts.add(part.variantId())) {
                    throw new BusinessException("Linh kiện bị trùng trong cùng một bộ PC.");
                }
                Integer available = remaining.get(part.variantId());
                if (available == null || available < part.quantity()) {
                    throw new BusinessException("Linh kiện bộ PC không đủ số lượng trong giỏ hàng. Vui lòng kiểm tra lại.");
                }
                remaining.put(part.variantId(), available - part.quantity());
            }
        }
    }

    private void createOrderItems(Integer orderId, Integer userId, Map<Integer, Integer> quantities, List<PlaceOrderRequest.PcGroup> groups) {
        List<OrderItem> items = new ArrayList<>();
        Map<Integer, Integer> remainingQuantities = new HashMap<>(quantities);

        // 1. Tạo các OrderItem cho từng bộ PC
        if (groups != null) {
            for (var group : groups) {
                var keys = new GeneratedKeyHolder();
                jdbc.update(connection -> {
                    var statement = connection.prepareStatement("INSERT INTO pc_customs (order_id, user_id, total_price) VALUES (?, ?, 0)", new String[]{"id"});
                    statement.setInt(1, orderId);
                    statement.setInt(2, userId);
                    return statement;
                }, keys);
                Integer pcId = java.util.Objects.requireNonNull(keys.getKey()).intValue();

                BigDecimal groupTotal = BigDecimal.ZERO;
                for (var part : group.parts()) {
                    PurchaseVariantResponse product = catalogApi.getPurchaseVariant(part.variantId());
                    Integer qty = part.quantity();

                    if (!"active".equalsIgnoreCase(product.status())
                            || product.stockQuantity() < qty
                            || product.warrantyPeriodMonths() < 0) {
                        throw new BusinessException(
                                "Sản phẩm không còn đủ điều kiện đặt mua: " + product.productName()
                        );
                    }

                    catalogApi.deductStock(product.variantId(), qty);

                    String productName = product.productName();
                    if (product.variantName() != null && !product.variantName().isBlank()) {
                        productName += " · " + product.variantName();
                    }

                    BigDecimal partSubtotal = product.price().multiply(BigDecimal.valueOf(qty));
                    OrderItem item = orderMapper.toOrderItem(product, orderId, qty, productName, partSubtotal);
                    item.setPcCustomId(pcId);
                    items.add(item);

                    jdbc.update("INSERT INTO pc_custom_items (pc_custom_id, product_variant_id, quantity, unit_price, component_role) VALUES (?, ?, ?, ?, ?)",
                            pcId, part.variantId(), qty, product.price(), part.role());
                    groupTotal = groupTotal.add(partSubtotal);

                    remainingQuantities.put(part.variantId(), remainingQuantities.get(part.variantId()) - qty);
                }

                jdbc.update("UPDATE pc_customs SET total_price = ? WHERE id = ?", groupTotal, pcId);
            }
        }

        // 2. Tạo các OrderItem cho các linh kiện mua lẻ còn lại
        for (Map.Entry<Integer, Integer> entry : remainingQuantities.entrySet()) {
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) continue;

            PurchaseVariantResponse product = catalogApi.getPurchaseVariant(entry.getKey());
            if (!"active".equalsIgnoreCase(product.status())
                    || product.stockQuantity() < quantity
                    || product.warrantyPeriodMonths() < 0) {
                throw new BusinessException(
                        "Sản phẩm không còn đủ điều kiện đặt mua: " + product.productName()
                );
            }

            catalogApi.deductStock(product.variantId(), quantity);

            String productName = product.productName();
            if (product.variantName() != null && !product.variantName().isBlank()) {
                productName += " · " + product.variantName();
            }

            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(quantity));
            OrderItem item = orderMapper.toOrderItem(product, orderId, quantity, productName, subtotal);
            item.setPcCustomId(null);
            items.add(item);
        }

        // Ghi xong dòng hàng trước khi payment/bảo hành đọc qua API module khác.
        orderItemRepository.saveAllAndFlush(items);
    }

    private void clearPurchasedItems(Integer cartId, Map<Integer, Integer> purchasedQuantities, List<PlaceOrderRequest.PcGroup> groups) {
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        if (cartItems.isEmpty()) return;

        Map<Integer, Integer> remainingToDeduct = new HashMap<>(purchasedQuantities);

        // 1. Nếu có nhóm PC, ưu tiên trừ/xóa các dòng cartItem thuộc bộ PC trước
        if (groups != null && !groups.isEmpty()) {
            Set<Integer> groupVariantIds = groups.stream()
                    .filter(Objects::nonNull)
                    .filter(g -> g.parts() != null)
                    .flatMap(g -> g.parts().stream())
                    .filter(Objects::nonNull)
                    .map(PlaceOrderRequest.PcGroupPart::variantId)
                    .collect(Collectors.toSet());

            for (CartItem ci : cartItems) {
                if (ci.getPcCustomId() != null && groupVariantIds.contains(ci.getProductVariantId())) {
                    Integer needed = remainingToDeduct.get(ci.getProductVariantId());
                    if (needed != null && needed > 0) {
                        int deduct = Math.min(ci.getQuantity(), needed);
                        ci.setQuantity(ci.getQuantity() - deduct);
                        remainingToDeduct.put(ci.getProductVariantId(), needed - deduct);
                        if (ci.getQuantity() <= 0) {
                            cartItemRepository.delete(ci);
                        } else {
                            cartItemRepository.save(ci);
                        }
                    }
                }
            }
        }

        // 2. Trừ các món hàng mua lẻ còn lại
        for (CartItem ci : cartItems) {
            Integer needed = remainingToDeduct.get(ci.getProductVariantId());
            if (needed != null && needed > 0 && ci.getQuantity() > 0) {
                int deduct = Math.min(ci.getQuantity(), needed);
                ci.setQuantity(ci.getQuantity() - deduct);
                remainingToDeduct.put(ci.getProductVariantId(), needed - deduct);
                if (ci.getQuantity() <= 0) {
                    cartItemRepository.delete(ci);
                } else {
                    cartItemRepository.save(ci);
                }
            }
        }

        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart != null) {
            cart.setUpdatedAt(LocalDateTime.now(clock));
            cartRepository.saveAndFlush(cart);
        }
    }

    private void clearPurchasedCart(Integer cartId) {
        cartItemRepository.clearAfterCheckout(cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có giỏ hàng."));
        cart.setUpdatedAt(LocalDateTime.now(clock));
        cartRepository.saveAndFlush(cart);
    }

    private PaymentApi.Payment validatePaymentAmount(Order order) {
        PaymentApi.Payment payment = paymentApi.get(order.getId());
        if (payment.amount().compareTo(order.getFinalAmount()) != 0) {
            throw new BusinessException("Khoản thanh toán không khớp giá trị đơn hàng.");
        }
        return payment;
    }

    private List<PickupWarrantyApi.Unit> validateWarrantyDevices(
            List<OrderResponse.Item> items,
            List<HandoverRequest.Device> devices
    ) {
        Map<Integer, Long> suppliedQuantities = new HashMap<>();
        Set<String> serialNumbers = new HashSet<>();
        List<PickupWarrantyApi.Unit> units = new ArrayList<>();

        for (HandoverRequest.Device device : devices) {
            OrderResponse.Item item = items.stream()
                    .filter(line -> line.id().equals(device.orderItemId()) && line.warrantyPeriodMonths() > 0)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Serial không thuộc dòng hàng cần bảo hành của đơn này."
                    ));

            String serialNumber = device.serialNumber().trim().toUpperCase(Locale.ROOT);
            if (serialNumber.isEmpty() || !serialNumbers.add(serialNumber)) {
                throw new BusinessException("Serial từng thiết bị phải đầy đủ và không trùng nhau.");
            }

            suppliedQuantities.merge(item.id(), 1L, Long::sum);
            units.add(new PickupWarrantyApi.Unit(item.id(), serialNumber, item.warrantyPeriodMonths()));
        }

        for (OrderResponse.Item item : items) {
            if (item.warrantyPeriodMonths() > 0
                    && suppliedQuantities.getOrDefault(item.id(), 0L) != item.quantity()) {
                throw new BusinessException("Cần nhập đủ serial cho từng thiết bị được bảo hành.");
            }
        }

        return units;
    }

    private UserDto requireActor(String email, boolean staff) {
        UserDto user = identityApi.requireActiveUser(email);
        if (staff && !Set.of("staff", "admin").contains(user.getRole().toLowerCase(Locale.ROOT))) {
            throw new AccessDeniedException("Chỉ nhân viên bán hàng hoặc quản trị viên được xử lý đơn.");
        }
        return user;
    }

    private Order findOrderForUpdate(String orderCode, Integer userId) {
        return orderRepository.findForUpdate(orderCode, userId)
                .orElseThrow(this::orderNotFound);
    }

    private void updateStatus(Order order, String status) {
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now(clock));
        orderRepository.saveAndFlush(order);
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    private OrderResponse toResponse(Order order, String paymentUrl) {
        PaymentApi.Payment payment = paymentApi.get(order.getId());
        String promotionCode = orderRepository.findPromotionCode(order.getId()).orElse(null);
        List<OrderResponse.Item> items = findOrderItems(order.getId());
        return orderMapper.toResponse(order, payment, promotionCode, items, paymentUrl);
    }

    private List<OrderResponse.Item> findOrderItems(Integer orderId) {
        return orderItemRepository.findResponseItems(orderId).stream()
                .map(orderMapper::toItemResponse)
                .toList();
    }

    private ResourceNotFoundException orderNotFound() {
        return new ResourceNotFoundException("Không tìm thấy đơn hàng.");
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[ .()-]", "");
        return normalized.matches("\\+84[0-9]{9}")
                ? "0" + normalized.substring(3) : normalized;
    }

    @Override
    public OrderResponse registerShippingSerials(String email, String orderCode, List<HandoverRequest.Device> devices) {
        requireActor(email, true);
        Order order = findOrderForUpdate(orderCode, null);
        if (!"shipping".equalsIgnoreCase(order.getDeliveryType()) || !"completed".equals(order.getStatus())) {
            throw new BusinessException("Chỉ ghi nhận serial cho đơn giao hàng đã hoàn tất.");
        }
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM warranty_cards wc JOIN order_items oi ON oi.id=wc.order_item_id WHERE oi.order_id=?", Integer.class, order.getId());
        if (existing != null && existing > 0) {
            throw new BusinessException("Đơn đã có thẻ bảo hành; cần đối chiếu dữ liệu cũ trước khi cập nhật serial.");
        }
        var units = validateWarrantyDevices(findOrderItems(order.getId()), devices);
        pickupWarrantyApi.issue(units, order.getUpdatedAt() == null ? LocalDateTime.now(clock) : order.getUpdatedAt());
        return toResponse(order);
    }
}
