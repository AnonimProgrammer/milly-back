package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.domain.valueobject.Money;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static com.milly.order.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private PaymentJpaRepository paymentRepository;

    @Mock
    private OrderEventNotifier orderEventNotifier;

    @Captor
    private ArgumentCaptor<PaymentEntity> paymentCaptor;

    private ProcessPaymentUseCase processPaymentUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processPaymentUseCase = new ProcessPaymentUseCase(
                tableRepository, orderRepository, orderItemRepository, paymentRepository, orderEventNotifier);
    }

    @Test
    void processesFullPaymentAndNotifiesOrderEvent() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        givenPaymentCanBeSaved();
        CreatePaymentRequest request = walletPaymentRequest();

        // Act
        ProcessPaymentResponse response = processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        assertThat(response.payment().amount()).isEqualByComparingTo("50.00");
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.bill().orderTotal()).isEqualByComparingTo("100.00");
        assertThat(response.bill().paidAmount()).isEqualByComparingTo("50.00");
        assertThat(response.bill().payments()).hasSize(1);

        verify(paymentRepository).save(paymentCaptor.capture());
        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount().amount()).isEqualByComparingTo("50.00");
        assertThat(savedPayment.getProvider()).isEqualTo(PaymentProvider.APPLE);
        assertThat(savedPayment.getProviderReference()).startsWith("pay_");
        verify(orderEventNotifier).paymentReceived(orderId, venueId, tableId);
    }


    private void givenApprovedOrderWithTotal() {
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        OrderEntity approvedOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                .withStatus(OrderStatus.APPROVED).build();
        List<OrderItemEntity> orderItems = List.of(anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("100.00")).build());
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));
        when(orderRepository.findByIdAndTableIdForUpdate(orderId, tableId)).thenReturn(Optional.of(approvedOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(orderItems);
    }

    private void givenNoExistingPayments() {
        givenExistingPayments();
    }

    private void givenExistingPayments(PaymentEntity... payments) {
        when(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(orderId, PaymentStatus.COMPLETED))
                .thenReturn(List.of(payments));
    }

    private void givenPaymentCanBeSaved() {
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity savedPayment = invocation.getArgument(0);
            savedPayment.setId(UUID.randomUUID());
            return savedPayment;
        });
    }

    private static CreatePaymentRequest walletPaymentRequest() {
        return new CreatePaymentRequest(new BigDecimal("50.00"), PaymentType.FULL, PaymentProvider.APPLE, null, null);
    }
}
