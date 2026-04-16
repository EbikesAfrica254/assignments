package com.ebikes.assignments.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.projections.AssignmentReference;
import com.ebikes.assignments.dtos.events.internal.OfferAcceptedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferExpiredTrigger;
import com.ebikes.assignments.dtos.events.outgoing.OfferCancelledEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferCreatedEvent;
import com.ebikes.assignments.dtos.responses.offers.OfferResponse;

@Mapper(componentModel = "spring")
public interface OfferMapper {

  @Mapping(source = "offer.agentId", target = "agentId")
  @Mapping(source = "assignment.id", target = "assignmentId")
  @Mapping(source = "offer.id", target = "offerId")
  OfferCancelledEvent toCancelledEvent(Offer offer);

  @Mapping(source = "offer.agentId", target = "agentId")
  @Mapping(source = "assignment.id", target = "assignmentId")
  @Mapping(source = "offer.expiresAt", target = "expiresAt")
  @Mapping(source = "offer.id", target = "offerId")
  OfferCreatedEvent toCreatedEvent(Offer offer);

  @Mapping(source = "agentId", target = "agentId")
  @Mapping(source = "assignment.id", target = "assignmentId")
  OfferAcceptedTrigger toAcceptedTrigger(Offer offer);

  @Mapping(source = "assignment.id", target = "assignmentId")
  @Mapping(source = "assignment.strategy", target = "strategy")
  OfferDeclinedTrigger toDeclinedTrigger(Offer offer);

  OfferExpiredTrigger toExpiredTrigger(AssignmentReference reference);

  @Mapping(source = "assignment.id", target = "assignmentId")
  OfferResponse toResponse(Offer offer);
}
