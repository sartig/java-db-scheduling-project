package com.fdmgroup.schedulingproject.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fdmgroup.schedulingproject.exception.PasswordDoesNotMatchException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyExistsException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.model.User;

/**
 * Service class for managing user details and related operations. The
 * UserDetailsService class provides various methods for handling user detail
 * interactions, such as verifying credentials, creating new users, updating
 * display names and passwords, and retrieving user info by username.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Service
public class UserDetailsService extends UserService {
	/**
	 * Verifies the credentials of a user by checking if the provided password
	 * matches the user's password.
	 *
	 * @param username the username of the user
	 * @param password the password to verify against the user's password
	 * @throws UserNotFoundException         if the user is not found
	 * @throws PasswordDoesNotMatchException if the provided password does not match
	 *                                       the user's password
	 */
	public void verifyCredentials(String username, String password)
			throws UserNotFoundException, PasswordDoesNotMatchException {
		User user = findUser(username);
		if (!user.getPassword().equals(password)) {
			throw new PasswordDoesNotMatchException();
		}
	}

	/**
	 * Creates a new user with the specified username and password.
	 *
	 * @param username the username of the user
	 * @param password the password of the user
	 * @throws UserAlreadyExistsException if a user with the specified username
	 *                                    already exists
	 */
	public void createUser(String username, String password) throws UserAlreadyExistsException {
		Optional<User> userOpt = userRepo.findByUsername(username);
		if (userOpt.isPresent()) {
			// Username already exists in database
			throw new UserAlreadyExistsException();
		}
		userRepo.save(new User(username, password, username));
	}

	/**
	 * Retrieves the user information for the specified username. The password data
	 * is removed for security reasons.
	 *
	 * @param username the username of the user
	 * @return the user information without password data
	 * @throws UserNotFoundException if the user is not found
	 */
	public User getUserInfo(String username) throws UserNotFoundException {
		User user = findUser(username);
		// remove password data
		return user;
	}

	/**
	 * Updates the display name of the user with the specified username.
	 *
	 * @param username    the username of the user
	 * @param displayName the new display name to set for the user
	 * @throws UserNotFoundException if the user is not found
	 */
	public void updateDisplayName(String username, String displayName) throws UserNotFoundException {
		User user = findUser(username);
		user.setDisplayName(displayName);
		userRepo.save(user);
	}

	/**
	 * Updates the password of the user with the specified username.
	 *
	 * @param username        the username of the user
	 * @param currentPassword the current password of the user
	 * @param newPassword     the new password to set for the user
	 * @throws UserNotFoundException         if the user is not found
	 * @throws PasswordDoesNotMatchException if the provided current password does
	 *                                       not match the user's password
	 */
	public void updatePassword(String username, String currentPassword, String newPassword)
			throws UserNotFoundException, PasswordDoesNotMatchException {
		User user = findUser(username);
		if (!currentPassword.equals(user.getPassword())) {
			throw new PasswordDoesNotMatchException();
		}
		user.setPassword(newPassword);
		userRepo.save(user);
	}
}
