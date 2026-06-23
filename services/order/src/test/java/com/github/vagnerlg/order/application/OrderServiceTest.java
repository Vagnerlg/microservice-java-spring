package com.github.vagnerlg.order.application;

import com.github.vagnerlg.order.domain.CreateOrderItem;
import com.github.vagnerlg.order.domain.Order;
import com.github.vagnerlg.order.domain.OrderEventPublisher;
import com.github.vagnerlg.order.domain.OrderItem;
import com.github.vagnerlg.order.domain.OrderRepository;
import com.github.vagnerlg.order.domain.OrderStatus;
import com.github.vagnerlg.order.domain.exception.OrderAccessDeniedException;
import com.github.vagnerlg.order.domain.exception.OrderCancellationException;
import com.github.vagnerlg.order.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createFromCheckout_shouldSaveOrderAndPublishCreatedEvent() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(new CreateOrderItem("p-1", "Widget", new BigDecimal("10.00"), 2));
        var order = orderService.createFromCheckout("user-1", items);

        assertThat(order.userId()).isEqualTo("user-1");
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalPrice()).isEqualByComparingTo("20.00");
        assertThat(order.items()).hasSize(1);
        verify(orderEventPublisher).publishCreated(any(Order.class));
    }

    @Test
    void listByUser_shouldReturnPageForUser() {
        var pageable = PageRequest.of(0, 10);
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findAllByUserId("user-1", pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        var result = orderService.listByUser("user-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo("user-1");
    }

    @Test
    void findById_shouldReturnOrder_whenOwner() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        var result = orderService.findById(order.id(), "user-1");

        assertThat(result.id()).isEqualTo(order.id());
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        var id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id, "user-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findById_shouldThrow403_whenNotOwner() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.findById(order.id(), "user-2"))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void confirm_shouldUpdateStatusToConfirmed() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.confirm(order.id());

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelBySystem_shouldUpdateStatusAndPublishCancelledEvent() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelBySystem(order.id(), "Out of stock");

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(captor.getValue().cancellationReason()).isEqualTo("Out of stock");
        verify(orderEventPublisher).publishCancelled(any(Order.class));
    }

    @Test
    void cancelByUser_shouldCancelPendingOrder() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelByUser(order.id(), "user-1");

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPublisher).publishCancelled(any(Order.class));
    }

    @Test
    void cancelByUser_shouldThrow403_whenNotOwner() {
        var order = buildOrder("user-1", OrderStatus.PENDING);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelByUser(order.id(), "user-2"))
                .isInstanceOf(OrderAccessDeniedException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelByUser_shouldThrow422_whenOrderNotPending() {
        var order = buildOrder("user-1", OrderStatus.CONFIRMED);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelByUser(order.id(), "user-1"))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("CONFIRMED");
        verify(orderRepository, never()).save(any());
    }

    private Order buildOrder(String userId, OrderStatus status) {
        var now = Instant.now();
        var item = new OrderItem(UUID.randomUUID(), "p-1", "Widget", new BigDecimal("10.00"), 2);
        return new Order(UUID.randomUUID(), userId, List.of(item), status,
                new BigDecimal("20.00"), null, now, now);
    }
}
