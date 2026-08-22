package com.producttagger.backend.shared.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Relay poll: oldest unpublished rows first, bounded batch
    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByIdAsc();
}
