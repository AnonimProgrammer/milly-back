package com.milly.order.application.usecase;

import com.milly.common.web.PageResponse;
import com.milly.common.web.PaginationMeta;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListVenueOrdersUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final PaymentSummaryPort paymentSummaryPort;

    @Transactional(readOnly = true)
    public PageResponse<StaffOrderResponse> execute(
            UUID venueId,
            UUID userId,
            OrderStatus status,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit) {
        venueAuthorizationService.requireMember(userId, venueId);

        int safeLimit = Math.max(1, limit);
        int page = parseCursor(cursor);
        Pageable pageable = PageRequest.of(page, safeLimit);
        OffsetDateTime fromDateTime = from != null ? from : todayStart();
        OffsetDateTime toDateTime = to != null ? to : todayEnd();

        Page<OrderEntity> orders = status != null
                ? orderRepository.findAllByVenueIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                venueId, status, fromDateTime, toDateTime, pageable)
                : orderRepository.findAllByVenueIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                venueId, fromDateTime, toDateTime, pageable);

        List<OrderEntity> orderContent = orders.getContent();
        List<UUID> orderIds = orderContent.stream().map(OrderEntity::getId).toList();
        Map<UUID, List<OrderItemEntity>> itemsByOrder = orderIds.isEmpty()
                ? Collections.emptyMap()
                : orderItemRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        Map<UUID, BigDecimal> paidAmountsByOrder = orderIds.isEmpty()
                ? Collections.emptyMap()
                : paymentSummaryPort.paidAmountsFor(orderIds);

        List<StaffOrderResponse> response = orderContent.stream()
                .map(order -> StaffOrderResponse.of(
                        order,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        paidAmountsByOrder.getOrDefault(order.getId(), BigDecimal.ZERO)))
                .toList();

        return new PageResponse<>(response, new PaginationMeta(
                nextCursor(orders),
                previousCursor(orders),
                orders.hasNext(),
                orders.hasPrevious(),
                safeLimit));
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cursor must be a non-negative integer page token.", ex);
        }
    }

    private String nextCursor(Page<OrderEntity> orders) {
        return orders.hasNext() ? Integer.toString(orders.getNumber() + 1) : null;
    }

    private String previousCursor(Page<OrderEntity> orders) {
        return orders.hasPrevious() ? Integer.toString(orders.getNumber() - 1) : null;
    }

    private OffsetDateTime todayStart() {
        ZoneId zone = ZoneId.systemDefault();
        return LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
    }

    private OffsetDateTime todayEnd() {
        ZoneId zone = ZoneId.systemDefault();
        return LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone).toOffsetDateTime();
    }
}