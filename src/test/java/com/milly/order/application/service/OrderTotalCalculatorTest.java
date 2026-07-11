package com.milly.order.application.service;

import com.milly.common.domain.valueobject.Money;
import com.milly.order.domain.entity.OrderItemEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static org.assertj.core.api.Assertions.assertThat;

class OrderTotalCalculatorTest {

    @Test
    void sumsLineItemTotals() {
        // Arrange
        OrderItemEntity first = anOrderItem()
                .withQuantity(2)
                .withUnitPrice(Money.of("12.50"))
                .build();
        OrderItemEntity second = anOrderItem()
                .withQuantity(3)
                .withUnitPrice(Money.of("4.25"))
                .build();

        // Act
        BigDecimal total = OrderTotalCalculator.totalOf(List.of(first, second));

        // Assert
        assertThat(total).isEqualByComparingTo("37.75");
    }

    @Test
    void returnsZeroForEmptyOrder() {
        // Act & Assert
        assertThat(OrderTotalCalculator.totalOf(List.of()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
