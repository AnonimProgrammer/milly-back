package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.BillSummaryResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.billing.application.usecase.builder.PaymentTestBuilder.aPayment;
import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBillUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private PaymentJpaRepository paymentRepository;

    private GetBillUseCase getBillUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getBillUseCase = new GetBillUseCase(tableRepository, orderRepository, orderItemRepository, paymentRepository);
    }

    @Test
    void returnsBillSummaryForCompletedPayments() {
        // Arrange
        givenActiveTableWithOrder();
        List<OrderItemEntity> items = List.of(
                anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("50.00")).build(),
                anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("75.00")).build()); // Total 125.00
        PaymentEntity payment1 = aPayment().withOrderId(orderId).withAmount(Money.of("50.00")).build();
        PaymentEntity payment2 = aPayment().withOrderId(orderId).withAmount(Money.of("75.00")).build();
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(items);
        when(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(orderId, PaymentStatus.COMPLETED))
                .thenReturn(List.of(payment1, payment2));

        // Act
        BillSummaryResponse response = getBillUseCase.execute(tableId, orderId);

        // Assert
        assertThat(response.orderTotal()).isEqualByComparingTo("125.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("125.00");
        assertThat(response.totalTipAmount()).isZero();
        assertThat(response.payments()).extracting("id").containsExactly(payment1.getId(), payment2.getId());
    }

    @Test
    void returnsBillSummaryWithZeroPaidAmountWhenNoPayments() {
        // Arrange
        givenActiveTableWithOrder();
        when(orderItemRepository.findAllByOrderId(orderId))
                .thenReturn(List.of(anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("100.00")).build()));
        when(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(orderId, PaymentStatus.COMPLETED))
                .thenReturn(List.of());

        // Act
        BillSummaryResponse response = getBillUseCase.execute(tableId, orderId);

        // Assert
        assertThat(response.orderTotal()).isEqualByComparingTo("100.00");
        assertThat(response.paidAmount()).isZero();
        assertThat(response.payments()).isEmpty();
    }

    @Test
    void returnsBillSummaryWithPartialPaidAmount() {
        // Arrange
        givenActiveTableWithOrder();
        when(orderItemRepository.findAllByOrderId(orderId))
                .thenReturn(List.of(anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("100.00")).build()));
        PaymentEntity payment = aPayment().withOrderId(orderId).withAmount(Money.of("50.00")).build();
        when(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(orderId, PaymentStatus.COMPLETED))
                .thenReturn(List.of(payment));

        // Act
        BillSummaryResponse response = getBillUseCase.execute(tableId, orderId);

        // Assert
        assertThat(response.orderTotal()).isEqualByComparingTo("100.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("50.00");
        assertThat(response.payments()).extracting("id").containsExactly(payment.getId());
    }

    @Test
    void throwsResourceNotFoundWhenTableNotFound() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getBillUseCase.execute(tableId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsResourceNotFoundWhenTableIsNotActive() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId).withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(inactiveTable));

        // Act & Assert
        assertThatThrownBy(() -> getBillUseCase.execute(tableId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsResourceNotFoundWhenOrderNotFoundForTable() {
        // Arrange
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));
        when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getBillUseCase.execute(tableId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenActiveTableWithOrder() {
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        OrderEntity order = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));
        when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.of(order));
    }
}