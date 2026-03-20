package com.ebikes.assignments.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.ShortlistCandidate;

@Repository
public interface ShortlistCandidateRepository extends JpaRepository<ShortlistCandidate, UUID> {

  List<ShortlistCandidate> findByAssignmentIdOrderByRankAsc(UUID assignmentId);
}
