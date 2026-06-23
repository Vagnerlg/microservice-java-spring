package com.github.vagnerlg.order.infrastructure.web;

import com.github.vagnerlg.order.application.OrderService;
import com.github.vagnerlg.order.infrastructure.web.response.ApiResponse;
import com.github.vagnerlg.order.infrastructure.web.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    ApiResponse<Page<OrderResponse>> list(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        var page = orderService.listByUser(jwt.getSubject(), pageable)
                .map(OrderResponse::from);
        return ApiResponse.of(page);
    }

    @GetMapping("/{id}")
    ApiResponse<OrderResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(OrderResponse.from(orderService.findById(id, jwt.getSubject())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        orderService.cancelByUser(id, jwt.getSubject());
    }
}
