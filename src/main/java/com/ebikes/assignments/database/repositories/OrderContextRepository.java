package com.ebikes.assignments.database.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.OrderContext;

@Repository
public interface OrderContextRepository extends JpaRepository<OrderContext, UUID> {

  Optional<OrderContext> findByOrderId(UUID orderId);
}
