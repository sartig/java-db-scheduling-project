package com.fdmgroup.schedulingproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdmgroup.schedulingproject.model.Event;

/**
 * Repository interface for managing Event entities. The EventRepository
 * interface extends the JpaRepository interface provided by Spring Data JPA,
 * allowing for CRUD (Create, Read, Update, Delete) operations on Event
 * entities. It provides methods for accessing and manipulating Event data in
 * the underlying database.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
