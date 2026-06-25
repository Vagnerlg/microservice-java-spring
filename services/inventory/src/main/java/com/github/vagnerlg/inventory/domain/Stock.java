package com.github.vagnerlg.inventory.domain;

import java.util.UUID;

public record Stock(UUID id, String productId, int totalQuantity, int reservedQuantity) {

    public int availableQuantity() {
        return totalQuantity - reservedQuantity;
    }
}
