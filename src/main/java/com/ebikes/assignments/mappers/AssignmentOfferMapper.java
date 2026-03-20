package com.ebikes.assignments.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.dtos.responses.offers.AssignmentOfferResponse;

@Mapper(componentModel = "spring")
public interface AssignmentOfferMapper {

  @Mapping(source = "assignment.id", target = "assignmentId")
  AssignmentOfferResponse toResponse(AssignmentOffer offer);
}
