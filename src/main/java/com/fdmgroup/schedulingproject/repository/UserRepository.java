package com.fdmgroup.schedulingproject.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdmgroup.schedulingproject.model.User;

/**
 * Repository interface for managing User entities. The UserRepository interface
 * extends the JpaRepository interface provided by Spring Data JPA, allowing for
 * CRUD (Create, Read, Update, Delete) operations on User entities. It provides
 * methods for accessing and manipulating User data in the underlying database,
 * including a custom method findByUsername to retrieve a user by username.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	/**
	 * Retrieves an optional User entity by its username.
	 *
	 * @param username the username of the user to retrieve
	 * @return an Optional containing the User entity, or an empty Optional if not
	 *         found
	 */
	Optional<User> findByUsername(String username);
}
