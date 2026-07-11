package com.milly.order.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderPreparationAiResult;
import com.milly.order.application.dto.OrderPreparationEstimateResponse;
import com.milly.order.application.service.OrderPreparationEstimator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstimateOrderPreparationTimeUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;
    @Mock
    private OrderJpaRepository orderRepository;
    @Mock
    private OrderItemJpaRepository orderItemRepository;
    @Mock
    private OrderPreparationEstimator orderPreparationEstimator;

    private EstimateOrderPreparationTimeUseCase useCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new EstimateOrderPreparationTimeUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, orderPreparationEstimator);
    }

    @Test
    void returnsPreparationEstimateForOrder() {
        // Arrange
        OrderEntity order = anOrder().withId(orderId).withVenueId(venueId).build();
        List<OrderItemEntity> items = List.of(anOrderItem().withOrderId(orderId).build());
        OrderPreparationAiResult estimate = new OrderPreparationAiResult(25, "25 minutes");
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(items);
        when(orderPreparationEstimator.estimate(venueId, orderId, items)).thenReturn(estimate);

        // Act
        OrderPreparationEstimateResponse response = useCase.execute(venueId, userId, orderId);

        // Assert
        assertThat(response).isEqualTo(new OrderPreparationEstimateResponse(orderId, 25, "25 minutes"));
        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void throwsNotFoundWhenOrderDoesNotBelongToVenue() {
        // Arrange
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(venueId, userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderItemRepository, orderPreparationEstimator);
    }

    @Test
    void stopsWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(venueId, userId, orderId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderItemRepository, orderPreparationEstimator);
    }
}
