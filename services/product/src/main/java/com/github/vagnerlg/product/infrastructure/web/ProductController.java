package com.github.vagnerlg.product.infrastructure.web;

import com.github.vagnerlg.product.application.CreateProduct;
import com.github.vagnerlg.product.application.ProductService;
import com.github.vagnerlg.product.application.UpdateProduct;
import com.github.vagnerlg.product.infrastructure.web.request.CreateProductRequest;
import com.github.vagnerlg.product.infrastructure.web.request.UpdateProductRequest;
import com.github.vagnerlg.product.infrastructure.web.response.ApiResponse;
import com.github.vagnerlg.product.infrastructure.web.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        var product = new CreateProduct(
                request.name(),
                request.description(),
                request.price(),
                request.category()
        );
        return ApiResponse.of(ProductResponse.from(productService.create(product)));
    }

    @GetMapping("/{id}")
    ApiResponse<ProductResponse> findById(@PathVariable String id) {
        return ApiResponse.of(ProductResponse.from(productService.findById(id)));
    }

    @PutMapping("/{id}")
    ApiResponse<ProductResponse> update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        var command = new UpdateProduct(id, request.name(), request.description(), request.price(), request.category());
        return ApiResponse.of(ProductResponse.from(productService.update(command)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        productService.delete(id);
    }
}
