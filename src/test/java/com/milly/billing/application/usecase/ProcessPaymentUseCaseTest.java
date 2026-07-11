package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.service.PaymentReceiptService;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.PaymentValidationException;
import com.milly.common.application.exception.ResourceNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.milly.billing.application.usecase.builder.PaymentTestBuilder.aPayment;
import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
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

    @Mock
    private PaymentReceiptService paymentReceiptService;

    @Captor
    private ArgumentCaptor<PaymentEntity> paymentCaptor;

    private ProcessPaymentUseCase processPaymentUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processPaymentUseCase = new ProcessPaymentUseCase(
                tableRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                paymentReceiptService,
                orderEventNotifier);
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
        assertThat(response.payment().tipAmount()).isZero();
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.bill().orderTotal()).isEqualByComparingTo("100.00");
        assertThat(response.bill().paidAmount()).isEqualByComparingTo("50.00");
        assertThat(response.bill().payments()).hasSize(1);
        assertThat(response.payment().receiptUrl()).isEqualTo("https://storage.local/receipt.pdf");

        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        List<PaymentEntity> savedPayments = paymentCaptor.getAllValues();
        PaymentEntity savedPayment = savedPayments.getFirst();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount().amount()).isEqualByComparingTo("50.00");
        assertThat(savedPayment.getTipAmount().amount()).isZero();
        assertThat(savedPayment.getProvider()).isEqualTo(PaymentProvider.APPLE);
        assertThat(savedPayment.getProviderReference()).startsWith("pay_");
        assertThat(savedPayments.get(1).getReceiptUrl()).isEqualTo("https://storage.local/receipt.pdf");
        verify(paymentReceiptService).generateAndStore(savedPayments.getFirst(), venueId);
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
                null,
                null);

        // Act
        processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getAllValues().getFirst().getProviderMetadata())
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
                BigDecimal.valueOf(50.00), PaymentType.SPLIT, PaymentProvider.APPLE, null, 3, null);

        // Act
        processPaymentUseCase.execute(tableId, orderId, request);

        // Assert
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getAllValues().getFirst().getProviderMetadata()).containsEntry("splitPeople", 3);
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

    @Test
    void throwsPaymentValidationExceptionWhenOrderIsNotApproved() {
        // Arrange
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        OrderEntity pendingOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                .withStatus(OrderStatus.PENDING).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));
        when(orderRepository.findByIdAndTableIdForUpdate(orderId, tableId)).thenReturn(Optional.of(pendingOrder));
        CreatePaymentRequest request = walletPaymentRequest("50.00");

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("Order is not open for payment.");
        verifyNoInteractions(paymentRepository, orderEventNotifier);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void throwsPaymentValidationExceptionForNonPositiveAmount(BigDecimal amount) {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        CreatePaymentRequest request = new CreatePaymentRequest(amount, PaymentType.FULL, PaymentProvider.APPLE, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("Amount must be greater than zero.");
        verify(paymentRepository, never()).save(any());
        verify(orderEventNotifier, never()).paymentReceived(any(), any(), any());
    }

    private static Stream<BigDecimal> invalidAmounts() {
        return Stream.of(BigDecimal.ZERO, BigDecimal.valueOf(-10.00));
    }

    @Test
    void throwsPaymentValidationExceptionWhenAmountExceedsRemainingBalance() {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        CreatePaymentRequest request = walletPaymentRequest("101.00");

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("Amount exceeds the remaining balance.");
        verify(paymentRepository, never()).save(any());
        verify(orderEventNotifier, never()).paymentReceived(any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("invalidCardDetails")
    void throwsPaymentValidationExceptionForInvalidCardDetails(CreatePaymentRequest.ProviderDetails details) {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(50.00), PaymentType.FULL, PaymentProvider.CARD, details, null, null);

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("Card payments require a 4-digit last4 and a brand.");
        verify(paymentRepository, never()).save(any());
        verify(orderEventNotifier, never()).paymentReceived(any(), any(), any());
    }

    private static Stream<CreatePaymentRequest.ProviderDetails> invalidCardDetails() {
        return Stream.of(
                null,
                new CreatePaymentRequest.ProviderDetails(null, "Visa", null, null),
                new CreatePaymentRequest.ProviderDetails("123", "Visa", null, null),
                new CreatePaymentRequest.ProviderDetails("1234", null, null, null));
    }

    @ParameterizedTest
    @MethodSource("invalidSplitPeople")
    void throwsPaymentValidationExceptionForInvalidSplitPeople(Integer splitPeople) {
        // Arrange
        givenApprovedOrderWithTotal();
        givenNoExistingPayments();
        CreatePaymentRequest request = new CreatePaymentRequest(
                BigDecimal.valueOf(50.00), PaymentType.SPLIT, PaymentProvider.APPLE, null, splitPeople, null);

        // Act & Assert
        assertThatThrownBy(() -> processPaymentUseCase.execute(tableId, orderId, request))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessage("splitPeople is required and must be at least 2 for split payments.");
        verify(paymentRepository, never()).save(any());
        verify(orderEventNotifier, never()).paymentReceived(any(), any(), any());
    }

    private static Stream<Integer> invalidSplitPeople() {
        return Stream.of(null, 1);
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
        when(paymentReceiptService.generateAndStore(any(PaymentEntity.class), any(UUID.class)))
                .thenReturn("https://storage.local/receipt.pdf");
    }

    private static CreatePaymentRequest walletPaymentRequest(String amount) {
        return new CreatePaymentRequest(new BigDecimal(amount), PaymentType.FULL, PaymentProvider.APPLE, null, null, null);
    }
}
