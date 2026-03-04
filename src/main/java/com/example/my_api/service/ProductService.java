package com.example.my_api.service;

import com.example.my_api.dto.ProductCreateRequest;
import com.example.my_api.dto.ProductResponse;
import com.example.my_api.entity.Product;
import com.example.my_api.exception.ParameterNotValidate;
import com.example.my_api.exception.ResourceNotFoundException;
import com.example.my_api.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (request.name().isBlank()) {
            throw new ParameterNotValidate("상품명은 비어 있을 수 없습니다.");
        }
        Product product = Product.builder()
            .name(request.name().trim())
            .description(request.description())
            .price(request.price())
            .stock(request.stock())
            .build();
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }


    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock()
        );
    }
}
