package com.github.vagnerlg.search.infrastructure.web;

import com.github.vagnerlg.search.application.ProductSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
class ProductSearchController {

    private final ProductSearchService service;

    ProductSearchController(ProductSearchService service) {
        this.service = service;
    }

    @GetMapping
    Page<ProductSearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return service.search(q, category, pageable).map(ProductSearchResponse::from);
    }
}
