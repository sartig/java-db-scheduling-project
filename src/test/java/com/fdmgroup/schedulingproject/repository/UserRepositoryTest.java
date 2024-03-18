package com.fdmgroup.schedulingproject.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.fdmgroup.schedulingproject.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class UserRepositoryTest {
	@Autowired
	private UserRepository userRepo;

	@Test
	@DisplayName("Verify findByUsername returns empty optional for invalid username")
	void testFindByUsername_WithInvalidUsername() {
		assertTrue(userRepo.findByUsername("invalid").isEmpty());
	}

	@Test
	@DisplayName("Verify findByUsername returns correct user")
	void testFindByUsername_ValidUsername() {
		User user = new User("username", "password", "displayName");
		userRepo.save(user);
		assertEquals(user, userRepo.findByUsername("username").get());
	}
}
