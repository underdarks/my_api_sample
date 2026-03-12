package com.example.my_api.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.my_api.dto.OrderCreateRequest;
import com.example.my_api.dto.OrderResponse;
import com.example.my_api.entity.Member;
import com.example.my_api.entity.MemberFixture;
import com.example.my_api.entity.Order;
import com.example.my_api.entity.Product;
import com.example.my_api.entity.ProductFixture;
import com.example.my_api.exception.ParameterNotValidate;
import com.example.my_api.repository.MemberRepository;
import com.example.my_api.repository.OrderRepository;
import com.example.my_api.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * OrderService 단위 테스트
 */
@ExtendWith(MockitoExtension.class) //Mockito 활성화
class OrderServiceUnitTest {

    @Mock //가짜 객체
    ProductRepository productRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    OrderRepository orderRepository;


    @InjectMocks //실제 테스트할 대상 클래스(Mock을 자동 주입받는 클래스)
    OrderService orderService;


    @Test
    @DisplayName("주문 요청이 정상 작동한다")
    public void createOrder() {
        //given
        Product product = ProductFixture.defaultProduct()
            .stock(2)
            .build();

        Member member = MemberFixture.defaultMember().build();

        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(1L, 1L, 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        //save(order)호출 되면 그 order 그대로 반환되게 모킹
        when(orderRepository.save(any())).thenAnswer(
            invocation -> invocation.getArgument(0));

        //when
        OrderResponse res = orderService.createOrder(orderCreateRequest);

        //then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture()); //save가 호출되는지 확인

        assertThat(res.memberEmail()).isNotNull();
    }


    @Test
    @DisplayName("주문 시 재고 부족시 예외 처리가 된다.")
    public void notEnoughStockQuantity() {
        //given
        Product product = Product.builder()
            .stock(1)
            .name("상품1")
            .price(10000L)
            .description("상품1 입니다.")
            .build();

        Member member = Member.builder()
            .email("test1@gmail.com")
            .password("1234")
            .build();

        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(1L, 1L, 2);

        //when
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        //then
        assertThatThrownBy(
            () -> orderService.createOrder(orderCreateRequest))
            .isInstanceOf(ParameterNotValidate.class)
            .hasMessage("재고가 부족합니다.");
    }

}