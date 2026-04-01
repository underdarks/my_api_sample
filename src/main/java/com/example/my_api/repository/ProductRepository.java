package com.example.my_api.repository;

import com.example.my_api.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true) //영속성 컨텍스트 sync 문제로 인해 강제로 비우기
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity where p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    //비관적락
    @Lock(LockModeType.PESSIMISTIC_WRITE) //X-Lock(배타락)
    @Query("SELECT p FROM Product p WHERE p.id =:id ")
    Optional<Product> findByIdWithLock(@Param("id") Long id);


}
