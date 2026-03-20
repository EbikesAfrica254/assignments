package com.ebikes.assignments.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {

  @Mapping(source = "orderContext.orderId", target = "orderId")
  @Mapping(source = "orderContext.organizationId", target = "organizationId")
  AssignmentResponse toResponse(Assignment assignment);
}
