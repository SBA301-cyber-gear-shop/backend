package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogInternalApi;
import com.project.cybershop.catalog.api.dto.response.PurchaseVariantResponse;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.order.api.dto.request.AddToCartRequest;
import com.project.cybershop.order.api.dto.request.UpdateCartItemRequest;
import com.project.cybershop.order.api.dto.response.CartResponse;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.entity.CartItem;
import com.project.cybershop.order.internal.repository.CartItemRepository;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.order.internal.service.CartService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogInternalApi catalogApi;
    private final IdentityApi identityApi;
    private final com.project.cybershop.promotion.api.PromotionApi promotions;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Override
    @Transactional
    public void addToCart(Integer userId, AddToCartRequest request) {
        validateUserId(userId);
        if (request == null || request.productVariantId() == null || request.productVariantId() <= 0
                || request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("Thông tin sản phẩm hoặc số lượng không hợp lệ");
        }

        // Dùng cùng khóa user với đặt hàng để tránh thay đổi giỏ trong lúc checkout.
        identityApi.lockActiveUser(userId);
        PurchaseVariantResponse product = requireActiveVariant(request.productVariantId());
        if (product.stockQuantity() <= 0) {
            throw new BusinessException("Sản phẩm đã hết hàng");
        }

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductVariantIdAndPcCustomId(cart.getId(), request.productVariantId(), request.pcCustomId())
                .orElse(null);

        long newQuantity = request.quantity();
        if (cartItem != null) {
            validateQuantity(cartItem.getQuantity());
            newQuantity += cartItem.getQuantity();
        }
        validateStock(product, newQuantity);

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCartId(cart.getId());
            cartItem.setProductVariantId(request.productVariantId());
            cartItem.setPcCustomId(request.pcCustomId());
            cartItem.setCreatedAt(LocalDateTime.now());
        }
        cartItem.setQuantity((int) newQuantity);
        cartItemRepository.save(cartItem);
        touchCart(cart);
    }

    @Override
    @Transactional
    public void addBatchToCart(Integer userId, List<AddToCartRequest> items) {
        addBatchToCart(userId, items, null);
    }

    @Override
    @Transactional
    public void addBatchToCart(Integer userId, List<AddToCartRequest> items, Integer pcCustomId) {
        validateUserId(userId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Danh sách linh kiện thêm vào giỏ không được để trống");
        }

        // Dùng cùng khóa user với đặt hàng để tránh thay đổi giỏ trong lúc checkout.
        identityApi.lockActiveUser(userId);
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));

        for (AddToCartRequest item : items) {
            if (item == null || item.productVariantId() == null || item.productVariantId() <= 0
                    || item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException("Thông tin linh kiện hoặc số lượng không hợp lệ");
            }

            PurchaseVariantResponse product = requireActiveVariant(item.productVariantId());
            if (product.stockQuantity() <= 0) {
                throw new BusinessException("Linh kiện '" + product.productName() + "' đã hết hàng");
            }

            Integer effectivePcCustomId = item.pcCustomId() != null ? item.pcCustomId() : pcCustomId;
            CartItem cartItem = cartItemRepository
                    .findByCartIdAndProductVariantIdAndPcCustomId(cart.getId(), item.productVariantId(), effectivePcCustomId)
                    .orElse(null);

            long newQuantity = item.quantity();
            if (cartItem != null) {
                validateQuantity(cartItem.getQuantity());
                newQuantity += cartItem.getQuantity();
            }
            validateStock(product, newQuantity);

            if (cartItem == null) {
                cartItem = new CartItem();
                cartItem.setCartId(cart.getId());
                cartItem.setProductVariantId(item.productVariantId());
                cartItem.setPcCustomId(effectivePcCustomId);
                cartItem.setCreatedAt(LocalDateTime.now());
            }
            cartItem.setQuantity((int) newQuantity);
            cartItemRepository.save(cartItem);
        }

        touchCart(cart);
    }

    @Override
    @Transactional
    public CartResponse getMyCart(Integer userId) {
        validateUserId(userId);
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return new CartResponse(null, userId, List.of(), 0, BigDecimal.ZERO);
        }

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        promotions.syncProductPrices(cartItems.stream()
                .map(CartItem::getProductVariantId).toList());

        Set<Integer> pcCustomIds = cartItems.stream()
                .map(CartItem::getPcCustomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> pcCustomNames = new HashMap<>();
        if (!pcCustomIds.isEmpty()) {
            String inSql = String.join(",", Collections.nCopies(pcCustomIds.size(), "?"));
            jdbc.query("SELECT id, name FROM pc_customs WHERE id IN (" + inSql + ")",
                    rs -> {
                        pcCustomNames.put(rs.getInt("id"), rs.getString("name"));
                    },
                    pcCustomIds.toArray());
        }

        List<CartResponse.CartItemResponse> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            validateQuantity(cartItem.getQuantity());
            PurchaseVariantResponse product = catalogApi.findActiveVariant(cartItem.getProductVariantId())
                    .orElse(null);
            String pcName = cartItem.getPcCustomId() != null ? pcCustomNames.get(cartItem.getPcCustomId()) : null;
            CartResponse.CartItemResponse itemResponse = toItemResponse(cartItem, product, pcName);
            items.add(itemResponse);
            totalQuantity = Math.addExact(totalQuantity, cartItem.getQuantity());
            totalAmount = totalAmount.add(itemResponse.subtotal());
        }

        return new CartResponse(cart.getId(), userId, items, totalQuantity, totalAmount);
    }

    @Override
    @Transactional
    public void removePcSet(Integer userId, Integer pcCustomId) {
        validateUserId(userId);
        if (pcCustomId == null || pcCustomId <= 0) {
            throw new BusinessException("ID bộ PC không hợp lệ");
        }
        identityApi.lockActiveUser(userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy giỏ hàng"));
        cartItemRepository.deleteByCartIdAndPcCustomId(cart.getId(), pcCustomId);
        touchCart(cart);
    }

    @Override
    @Transactional
    public void detachPcSet(Integer userId, Integer pcCustomId) {
        validateUserId(userId);
        if (pcCustomId == null || pcCustomId <= 0) {
            throw new BusinessException("ID bộ PC không hợp lệ");
        }
        identityApi.lockActiveUser(userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy giỏ hàng"));
        List<CartItem> setItems = cartItemRepository.findByCartIdAndPcCustomId(cart.getId(), pcCustomId);
        if (setItems.isEmpty()) {
            return;
        }
        for (CartItem setItem : setItems) {
            Optional<CartItem> existingLoose = cartItemRepository
                    .findByCartIdAndProductVariantIdAndPcCustomId(cart.getId(), setItem.getProductVariantId(), null);
            if (existingLoose.isPresent()) {
                CartItem looseItem = existingLoose.get();
                looseItem.setQuantity(looseItem.getQuantity() + setItem.getQuantity());
                cartItemRepository.save(looseItem);
                cartItemRepository.delete(setItem);
            } else {
                setItem.setPcCustomId(null);
                cartItemRepository.save(setItem);
            }
        }
        touchCart(cart);
    }

    @Override
    @Transactional
    public void updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request) {
        validateUserId(userId);
        validateCartItemId(cartItemId);
        if (request == null || request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0");
        }

        identityApi.lockActiveUser(userId);
        Cart cart = requireCart(userId);
        CartItem cartItem = requireOwnedItem(cart, cartItemId);
        PurchaseVariantResponse product = requireActiveVariant(cartItem.getProductVariantId());
        validateStock(product, request.quantity());

        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);
        touchCart(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(Integer userId, Integer cartItemId) {
        validateUserId(userId);
        validateCartItemId(cartItemId);
        identityApi.lockActiveUser(userId);
        Cart cart = requireCart(userId);
        CartItem cartItem = requireOwnedItem(cart, cartItemId);

        // Cho phép xóa cả dòng có sản phẩm đã ngừng bán.
        cartItemRepository.delete(cartItem);
        touchCart(cart);
    }

    private PurchaseVariantResponse requireActiveVariant(Integer variantId) {
        return catalogApi.findActiveVariant(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm hoặc sản phẩm đã ngừng kinh doanh"));
    }

    private Cart createCart(Integer userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    private Cart requireCart(Integer userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
    }

    private CartItem requireOwnedItem(Cart cart, Integer cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .filter(item -> cart.getId().equals(item.getCartId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));
    }

    private CartResponse.CartItemResponse toItemResponse(CartItem cartItem, PurchaseVariantResponse product, String pcCustomName) {
        if (product == null) {
            return new CartResponse.CartItemResponse(
                    cartItem.getId(), cartItem.getProductVariantId(), "Sản phẩm không còn kinh doanh",
                    "", null, BigDecimal.ZERO, cartItem.getQuantity(), BigDecimal.ZERO, 0, false,
                    cartItem.getPcCustomId(), pcCustomName);
        }

        validatePrice(product);
        BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return new CartResponse.CartItemResponse(
                cartItem.getId(), cartItem.getProductVariantId(), product.productName(), product.variantName(),
                product.imageUrl(), product.price(), cartItem.getQuantity(), subtotal, product.stockQuantity(), true,
                cartItem.getPcCustomId(), pcCustomName);
    }

    private void validateStock(PurchaseVariantResponse product, long quantity) {
        validatePrice(product);
        if (quantity > product.stockQuantity()) {
            throw new BusinessException(
                    "Số lượng vượt quá tồn kho. Hiện còn " + product.stockQuantity() + " sản phẩm");
        }
    }

    private void validatePrice(PurchaseVariantResponse product) {
        if (product.price() == null || product.price().signum() < 0) {
            throw new BusinessException("Giá sản phẩm không hợp lệ: " + product.productName());
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Số lượng trong giỏ hàng không hợp lệ");
        }
    }

    private void validateUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("Người dùng không hợp lệ");
        }
    }

    private void validateCartItemId(Integer cartItemId) {
        if (cartItemId == null || cartItemId <= 0) {
            throw new BusinessException("ID sản phẩm trong giỏ không hợp lệ");
        }
    }

    private void touchCart(Cart cart) {
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
}
