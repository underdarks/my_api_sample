package com.example.my_api.service;

import com.example.my_api.dto.OrderCreateRequest;
import com.example.my_api.dto.OrderItemResponse;
import com.example.my_api.dto.OrderResponse;
import com.example.my_api.entity.Order;
import com.example.my_api.entity.OrderItem;
import com.example.my_api.entity.OrderStatus;
import com.example.my_api.entity.Product;
import com.example.my_api.exception.ParameterNotValidate;
import com.example.my_api.exception.ResourceNotFoundException;
import com.example.my_api.repository.MemberRepository;
import com.example.my_api.repository.OrderRepository;
import com.example.my_api.repository.ProductRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        var member = memberRepository.findById(request.memberId())
            .orElseThrow(
                () -> new ResourceNotFoundException("회원을 찾을 수 없습니다. id=" + request.memberId()));

        Product product = productRepository.findById(request.productId())
            .orElseThrow(
                () -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + request.productId()));

        if (product.getStock() < request.quantity()) {
            throw new ParameterNotValidate("재고가 부족합니다.");
        }

        Order order = Order.builder()
            .totalPrice(product.getPrice() * request.quantity())
            .status(OrderStatus.PENDING)
            .buyer(member)
            .build();

        OrderItem orderItem = OrderItem.builder()
            .product(product)
            .quantity(request.quantity())
            .price(product.getPrice())
            .build();

        order.addOrderItem(orderItem);

        product.decreaseQuantity(request.quantity());

        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. id=" + id));
        return toResponse(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("주문을 찾을 수 없습니다. id=" + id);
        }
        orderRepository.deleteById(id);
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new ParameterNotValidate(
                "유효하지 않은 주문 상태입니다. 사용 가능 값: PENDING, PAID, SHIPPED, CANCELED");
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice()
            ))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getBuyer().getId(),
            order.getBuyer().getEmail(),
            order.getTotalPrice(),
            order.getStatus(),
            items
        );
    }
}
