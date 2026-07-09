package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
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

import static com.milly.billing.application.usecase.builder.PaymentTestBuilder.aPayment;
import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static com.milly.order.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        CreatePaymentRequest request = walletPaymentRequest("50.00");

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

    @Test
    void processesPartialPaymentOnTopOfExistingPayments() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenExistingPayments(aPayment().withOrderId(orderId).withAmount(Money.of("25.00")).build());
        givenPaymentCanBeSaved();
        CreatePaymentRequest request = walletPaymentRequest("50.00");

        // Act
        ProcessPaymentResponse response = processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        assertThat(response.payment().amount()).isEqualByComparingTo("50.00");
        assertThat(response.bill().paidAmount()).isEqualByComparingTo("75.00"); // 25 + 50
        assertThat(response.bill().payments()).hasSize(2);
        verify(orderEventNotifier).paymentReceived(orderId, venueId, tableId);
    }

    @Test
    void processesPaymentThatFullyPaysOffRemainingBalance() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenExistingPayments(aPayment().withOrderId(orderId).withAmount(Money.of("25.00")).build());
        givenPaymentCanBeSaved();
        CreatePaymentRequest request = walletPaymentRequest("75.00"); // Remaining 75.00

        // Act
        ProcessPaymentResponse response = processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        assertThat(response.bill().paidAmount()).isEqualByComparingTo("100.00"); // 25 + 75
        assertThat(response.bill().payments()).hasSize(2);
    }
    @Test
    void savesCardPaymentDetailsInProviderMetadata() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        givenPaymentCanBeSaved();
        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(50.00),
                PaymentType.FULL,
                PaymentProvider.CARD,
                new CreatePaymentRequest.ProviderDetails("4242", "Visa", 12, 2025),
                null);

        // Act
        processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getProviderMetadata())
                .containsEntry("last4", "4242")
                .containsEntry("brand", "Visa")
                .containsEntry("expiryMonth", 12)
                .containsEntry("expiryYear", 2025);
    }

    @Test
    void savesSplitPeopleInProviderMetadata() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        givenPaymentCanBeSaved();
        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(50.00), PaymentType.SPLIT, PaymentProvider.APPLE, null, 3);

        // Act
        processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getProviderMetadata()).containsEntry("splitPeople", 3);
    }
    @Test
    void throwsResourceNotFoundWhenTableNotFound() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());
        CreatePaymentRequest request = walletPaymentRequest("50.00");

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(paymentRepository, orderEventNotifier);
    }

    @Test
    void throwsResourceNotFoundWhenTableIsNotActive() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId).withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(inactiveTable));
        CreatePaymentRequest request = walletPaymentRequest("50.00");

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(paymentRepository, orderEventNotifier);
    }

    @Test
    void throwsResourceNotFoundWhenOrderNotFound() {
        // Arrange
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));
        when(orderRepository.findByIdAndTableIdForUpdate(orderId, tableId)).thenReturn(Optional.empty());
        CreatePaymentRequest request = walletPaymentRequest("50.00");

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(paymentRepository, orderEventNotifier);
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

    private static CreatePaymentRequest walletPaymentRequest(String amount) {
        return new CreatePaymentRequest(new BigDecimal(amount), PaymentType.FULL, PaymentProvider.APPLE, null, null);
    }
}
