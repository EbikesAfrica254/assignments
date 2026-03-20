package com.ebikes.assignments.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.Inbox;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, String> {}
