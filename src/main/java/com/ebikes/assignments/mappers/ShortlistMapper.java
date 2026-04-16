package com.ebikes.assignments.mappers;

import org.mapstruct.Mapping;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.dtos.internal.Candidate;

public interface ShortlistMapper {

  @Mapping(target = "assignment", source = "assignment")
  Shortlist toEntry(Candidate candidate, Assignment assignment);
}
