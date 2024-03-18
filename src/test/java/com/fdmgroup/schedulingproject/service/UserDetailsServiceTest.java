package com.fdmgroup.schedulingproject.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.schedulingproject.exception.PasswordDoesNotMatchException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyExistsException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceTest {

	private UserDetailsService userService;

	@Mock
	UserRepository mockUserRepo;

	@Mock
	User mockUser;

	@BeforeEach
	void setUp() {
		userService = new UserDetailsService();
		userService.setUserRepository(mockUserRepo);
	}

	@Test
	@DisplayName("verifyCredentials with correct credentials does not throw an error")
	void testVerifyCredentials_WithCorrectCredentials() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		when(mockUser.getPassword()).thenReturn("password");
		assertDoesNotThrow(() -> userService.verifyCredentials("username", "password"));
	}

	@Test
	@DisplayName("verifyCredentials with incorrect username throws a UserNotFoundException")
	void testVerifyCredentials_WithIncorrectUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userService.verifyCredentials("username", "password"));
	}

	@Test
	@DisplayName("verifyCredentials with incorrect password throws a PasswordDoesNotMatchException")
	void testVerifyCredentials_WithIncorrectPassword() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		when(mockUser.getPassword()).thenReturn("password");
		assertThrows(PasswordDoesNotMatchException.class,
				() -> userService.verifyCredentials("username", "wrongPassword"));
	}

	@Test
	@DisplayName("createUser calls userRepo.save() with correct params if username does not exist")
	void testCreateUser_WithNewUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.empty());
		userService.createUser("username", "password");
		verify(mockUserRepo).findByUsername("username");
		verify(mockUserRepo).save(argThat(
				user -> user.getUsername().equals("username")
				&& user.getPassword().equals("password")
				&& user.getDisplayName().equals("username")));
	}

	@Test
	@DisplayName("createUser does not call userRepo.save() if username already exists and throws UserAlreadyExistsException")
	void testCreateUser_WithExistingUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		assertThrows(UserAlreadyExistsException.class, () -> userService.createUser("username", "password"));
		verifyNoMoreInteractions(mockUserRepo);
	}

	@Test
	@DisplayName("getUserInfo with incorrect username throws a UserNotFoundException")
	void getUserInfo_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userService.getUserInfo("username"));
	}

	@Test
	@DisplayName("getUserInfo with correct username returns User object")
	void getUserInfo_WithValidUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		User retrievedUser = userService.getUserInfo("username");
		assertEquals(mockUser, retrievedUser);
	}

	@Test
	@DisplayName("updateDisplayName with incorrect username throws a UserNotFoundException")
	void updateDisplayName_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userService.updateDisplayName("username", "newDisplayName"));
	}

	@Test
	@DisplayName("updateDisplayName with correct username calls userRepo with correct params")
	void updateDisplayName_WithValidUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		userService.updateDisplayName("username", "newDisplayName");
		verify(mockUser).setDisplayName("newDisplayName");
		verify(mockUserRepo).save(mockUser);
	}

	@Test
	@DisplayName("updatePassword with incorrect username throws a UserNotFoundException")
	void updatePassword_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userService.updateDisplayName("username", "newDisplayName"));
	}

	@Test
	@DisplayName("updatePassword with correct username but invalid password throws a PasswordDoesNotMatchException")
	void updatePassword_WithValidUsername_AndInvalidPassword() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		when(mockUser.getPassword()).thenReturn("password");
		assertThrows(PasswordDoesNotMatchException.class,
				() -> userService.updatePassword("username", "wrongPassword", "newPassword"));
	}

	@Test
	@DisplayName("updatePassword with correct username and password updates userRepo with new password")
	void updateDisplayName_WithValidUsername_AndValidPassword() {
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser));
		when(mockUser.getPassword()).thenReturn("password");
		userService.updatePassword("username", "password", "newPassword");
		verify(mockUser).setPassword("newPassword");
		verify(mockUserRepo).save(mockUser);
	}
}
