package com.example.my_api.repository;

import com.example.my_api.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = {"member", "orderItems", "orderItems.product"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"member", "orderItems", "orderItems.product"})
    Optional<Order> findById(Long id);
}
