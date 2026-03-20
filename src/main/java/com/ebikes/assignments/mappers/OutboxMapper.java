package com.ebikes.assignments.mappers;

import org.mapstruct.Mapper;

import com.ebikes.assignments.database.entities.Outbox;
import com.ebikes.assignments.dtos.responses.outbox.OutboxResponse;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
