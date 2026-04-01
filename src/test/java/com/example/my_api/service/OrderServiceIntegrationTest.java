package com.example.my_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.my_api.dto.OrderCreateRequest;
import com.example.my_api.dto.OrderResponse;
import com.example.my_api.entity.Member;
import com.example.my_api.entity.MemberFixture;
import com.example.my_api.entity.Product;
import com.example.my_api.entity.ProductFixture;
import com.example.my_api.exception.ResourceNotFoundException;
import com.example.my_api.repository.MemberRepository;
import com.example.my_api.repository.OrderItemRepository;
import com.example.my_api.repository.OrderRepository;
import com.example.my_api.repository.ProductRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * OrderService 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceIntegrationTest.class);
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;


    private Member saveMember() {
        return memberRepository.save(MemberFixture.defaultMember().build());
    }

    private Product saveProduct(int stock) {
        return productRepository.save(ProductFixture.defaultProduct()
            .stock(stock)
            .build());
    }

    @BeforeEach
    public void setUp() {
        orderItemRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원이 없으면 ResourceNotFoundException 이 발생한다")
    void createOrder_memberNotFound() {
        //given
        OrderCreateRequest request = new OrderCreateRequest(999L, 1L, 1);

        //when, then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("회원을 찾을 수 없습니다. id=999");
    }

    @Test
    @DisplayName("상품이 없으면 ResourceNotFoundException 이 발생한다")
    void createOrder_productNotFound() {
        //given
        Member member = saveMember();
        OrderCreateRequest request = new OrderCreateRequest(member.getId(), 999L, 1);

        //when, then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("상품을 찾을 수 없습니다. id=999");
    }


    //Nested를 통해 테스트 케이스를 그룹하여 테스트 수행할 수 있다.
    @Nested()
    @DisplayName("주문 생성 테스트")
    class createOrderTest {

        @Test
        @DisplayName("주문 생성 시 DB에 저장되고 응답값이 정상 반환된다")
        void createOrderPersistsAndReturns() {
            //given
            Member member = saveMember();
            Product product = saveProduct(10);

            OrderCreateRequest request = new OrderCreateRequest(member.getId(), product.getId(), 2);

            //when
            OrderResponse response = orderService.createOrder(request);

            //then
            assertThat(response.id()).isNotNull();
            assertThat(orderRepository.findById(response.id()).isPresent()).isTrue();
        }

        @Test
        @DisplayName("주문 생성 시 DB에 저장되고 재고값이 정상 차감된다")
        void createOrderRollbackStockWhenOrderSaveFails() {
            //given
            Member member = saveMember();
            Product product = saveProduct(10);
            int initialStock = product.getStock();

            OrderCreateRequest request = new OrderCreateRequest(member.getId(), product.getId(), 2);

            //when
            OrderResponse response = orderService.createOrder(request);

            //then
            Product saveProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(initialStock - request.quantity()).isEqualTo(saveProduct.getStock());
        }

        /**
         * @concurrencyOrderUsers : 동시 요청 유저 수
         * @stock : 재고 수
         * @quantity : 주문 개수
         */
        @ParameterizedTest
        @CsvSource({
            "1, 1, 1",
            "2, 2, 1",
            "5, 5, 1",
            "5, 3, 2",
            "10, 8, 2",
            "20, 10, 2",
            "50, 25, 3"
        })
        @DisplayName("상품 재고 동시성 테스트(의도적인 race condition 상황 생성)")
        void createOrderConcurrencyTest(int concurrencyOrderUsers, int stock, int quantity)
            throws Exception {
            //given
            Member member = saveMember();
            Product product = saveProduct(stock);

            OrderCreateRequest request = new OrderCreateRequest(member.getId(), product.getId(),
                quantity);

            ExecutorService executor = Executors.newFixedThreadPool(concurrencyOrderUsers);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(concurrencyOrderUsers);
            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            for (int i = 0; i < concurrencyOrderUsers; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); //모든 스레드 여기서 대기 후
                        orderService.createOrderWithXLock(request); //로직 실행(race-condition 발생)
                        successCount.incrementAndGet(); //주문 성공 카운트
                    } catch (Exception e) {
                        failCount.incrementAndGet(); //실패 처리 카운트
                    } finally {
                        doneLatch.countDown(); //완료 처리
                    }
                });
            }

            //when
            startLatch.countDown(); //동시 실행 시작
            doneLatch.await(); // 모든 작업 종료 대기
            executor.shutdown(); //executor 종료

            //then
            Product savedProduct = productRepository.findById(product.getId())
                .orElseThrow();
            long orderCount = orderRepository.count();

            //기대값은 expectedSuccess = min(concurrencyOrderUsers, stock / quantity) 기준으로 계산
            //재고는 expectedRemaining = stock - (expectedSuccess * quantity)로 검증
            int expectedSuccess = Math.min(concurrencyOrderUsers, stock / quantity);
            int expectedRemaining = stock - (expectedSuccess * quantity);

            log.info("동시 주문 요청하는 유저 수 = {}", concurrencyOrderUsers);
            log.info("요청 재고 = {}", stock);
            log.info("요청 주문 수량 = {}", quantity);
            log.info("예상 주문 성공 수 = {}", expectedSuccess);
            log.info("예상 남은 재고 = {}", expectedRemaining);

            assertThat(savedProduct.getStock()).isEqualTo(expectedRemaining); //재고 수 검증
            assertThat(orderCount).isEqualTo(expectedSuccess); //주문 수 검증
        }
    }


}
