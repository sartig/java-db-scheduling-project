package com.fdmgroup.schedulingproject.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fdmgroup.schedulingproject.exception.CannotInviteSelfException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInContactsException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInvitedException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.User;

/**
 * Service class for managing user contacts and related operations. The
 * UserContactService class provides various methods for handling user contact
 * interactions, such as sending, cancelling, accepting or declining contact
 * requests, and retrieving a list of contacts by username.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Service
public class UserContactService extends UserService {
	/**
	 * Sends a contact invite from the sender to the receiver. Checks various
	 * conditions and throws exceptions if necessary.
	 *
	 * @param sender   the username of the sender
	 * @param receiver the username of the receiver
	 * @throws UserNotFoundException          if the sender or receiver is not found
	 * @throws UserAlreadyInContactsException if the sender or receiver is already
	 *                                        in each other's contacts
	 * @throws UserAlreadyInvitedException    if the receiver has already been
	 *                                        invited by the sender
	 * @throws CannotInviteSelfException      if the sender and receiver are the
	 *                                        same user
	 */
	public void sendContactInvite(String sender, String receiver) throws UserNotFoundException,
			UserAlreadyInContactsException, UserAlreadyInvitedException, CannotInviteSelfException {
		if (sender.equals(receiver)) {
			throw new CannotInviteSelfException();
		}
		User sendingUser = findUser(sender);
		User receivingUser = findUser(receiver);
		if (sendingUser.getContacts().contains(receivingUser) || receivingUser.getContacts().contains(sendingUser)) {
			throw new UserAlreadyInContactsException();
		}
		if (sendingUser.getSentContactInvites().contains(receivingUser)) {
			throw new UserAlreadyInvitedException();
		}
		if (receivingUser.getSentContactInvites().contains(sendingUser)) {
			// receiver also sent sender a request, so directly add to contacts for both
			receivingUser.removeSentContactInvite(sendingUser);
			sendingUser.addContact(receivingUser);
			receivingUser.addContact(sendingUser);
		} else {
			// otherwise send invite
			receivingUser.addReceivedContactInvite(sendingUser);
			sendingUser.addSentContactInvite(receivingUser);
		}
		saveUserPair(sendingUser, receivingUser);

	}

	/**
	 * Removes a user from the contacts of another user.
	 *
	 * @param myUsername the username of the user removing the contact
	 * @param username   the username of the contact to be removed
	 * @throws UserNotFoundException if the current user or the contact to be
	 *                               removed is not found
	 */
	public void removeFromContacts(String myUsername, String username) throws UserNotFoundException {
		User user1 = findUser(myUsername);
		User user2 = findUser(username);
		user1.removeContact(user2);
		user2.removeContact(user1);
		saveUserPair(user1, user2);
	}

	private void saveUserPair(User user1, User user2) {
		List<User> userList = List.of(user1, user2);
		userRepo.saveAll(userList);
	}

	/**
	 * Accepts a contact invite from another user.
	 *
	 * @param myUsername the username of the user accepting the contact invite
	 * @param username   the username of the user who sent the contact invite
	 * @throws UserNotFoundException     if the current user or the inviting user is
	 *                                   not found
	 * @throws UserNotInvitedException   if the current user has not been invited by
	 *                                   the inviting user
	 * @throws CannotInviteSelfException if the current user and the inviting user
	 *                                   are the same user
	 */
	public void acceptContact(String myUsername, String username)
			throws UserNotFoundException, UserNotInvitedException, CannotInviteSelfException {
		if (myUsername.equals(username)) {
			throw new CannotInviteSelfException();
		}
		User user1 = findUser(myUsername);
		User user2 = findUser(username);
		if (!user1.getReceivedContactInvites().contains(user2) || !user2.getSentContactInvites().contains(user1)) {
			// remove from both in case of asymmetry
			user1.removeReceivedContactInvite(user2);
			user2.removeSentContactInvite(user1);
			throw new UserNotInvitedException();
		}
		user1.removeReceivedContactInvite(user2);
		user2.removeSentContactInvite(user1);
		user1.addContact(user2);
		user2.addContact(user1);
		saveUserPair(user1, user2);
	}

	/**
	 * Cancels a contact invite sent by the current user to another user.
	 *
	 * @param myUsername the username of the user canceling the contact invite
	 * @param username   the username of the user to whom the contact invite was
	 *                   sent
	 * @throws UserNotFoundException     if the current user or the invited user is
	 *                                   not found
	 * @throws UserNotInvitedException   if the invited user has not been invited by
	 *                                   the current user
	 * @throws CannotInviteSelfException if the current user and the invited user
	 *                                   are the same user
	 */
	public void cancelContactInvite(String myUsername, String username)
			throws UserNotFoundException, UserNotInvitedException, CannotInviteSelfException {
		if (myUsername.equals(username)) {
			throw new CannotInviteSelfException();
		}
		User user1 = findUser(myUsername);
		User user2 = findUser(username);
		if (!user1.getSentContactInvites().contains(user2) || !user2.getReceivedContactInvites().contains(user1)) {
			// user2 was not invited by user1
			throw new UserNotInvitedException();
		}
		user1.removeSentContactInvite(user2);
		user2.removeReceivedContactInvite(user1);
		saveUserPair(user1, user2);
	}

	/**
	 * Retrieves the list of contacts for the specified user.
	 *
	 * @param username the username of the user
	 * @return the list of contacts for the user
	 * @throws UserNotFoundException if the user is not found
	 */
	public List<User> getContacts(String username) throws UserNotFoundException {
		User user = findUser(username);
		return user.getContacts();
	}
}
