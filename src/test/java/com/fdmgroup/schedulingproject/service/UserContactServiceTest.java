package com.fdmgroup.schedulingproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.schedulingproject.exception.CannotInviteSelfException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInContactsException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInvitedException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserContactServiceTest {

	private UserContactService userContactService;

	@Mock
	UserRepository mockUserRepo;

	@Mock
	User mockUser1, mockUser2;

	List<User> listContainingUser1, listContainingUser2;

	@BeforeEach
	void setUp() {
		userContactService = new UserContactService();
		userContactService.setUserRepository(mockUserRepo);
		listContainingUser1 = new ArrayList<>();
		listContainingUser1.add(mockUser1);
		listContainingUser2 = new ArrayList<>();
		listContainingUser2.add(mockUser2);
	}

	@Test
	@DisplayName("sendContactInvite to self throws CannotInviteSelfException")
	void testSendContactInvite_ToSelf() {
		assertThrows(CannotInviteSelfException.class,
				() -> userContactService.sendContactInvite("username", "username"));
	}

	@Test
	@DisplayName("sendContactInvite with invalid username throws UserNotFoundException")
	void testSendContactInvite_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userContactService.sendContactInvite("user1", "user2"));
	}

	@Test
	@DisplayName("sendContactInvite to user already in contacts throws UserAlreadyInContactsException")
	void testSendContactInvite_ToContact() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getContacts()).thenReturn(listContainingUser2);
		assertThrows(UserAlreadyInContactsException.class,
				() -> userContactService.sendContactInvite("user1", "user2"));
	}

	@Test
	@DisplayName("sendContactInvite to user already invited throws UserAlreadyInvitedException")
	void testSendContactInvite_ToAlreadyInvitedContact() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getSentContactInvites()).thenReturn(listContainingUser2);
		assertThrows(UserAlreadyInvitedException.class, () -> userContactService.sendContactInvite("user1", "user2"));
	}

	@Test
	@DisplayName("sendContactInvite to user who also sent an invite directly adds to contacts")
	void testSendContactInvite_ToContactWithPendingInvite() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getContacts()).thenReturn(new ArrayList<>());
		when(mockUser2.getContacts()).thenReturn(new ArrayList<>());
		when(mockUser1.getSentContactInvites()).thenReturn(new ArrayList<>());
		when(mockUser2.getSentContactInvites()).thenReturn(listContainingUser1);
		userContactService.sendContactInvite("user1", "user2");
		verify(mockUser2).removeSentContactInvite(mockUser1);
		verify(mockUser1).addContact(mockUser2);
		verify(mockUser2).addContact(mockUser1);
		verify(mockUserRepo).saveAll(List.of(mockUser1, mockUser2));
	}

	@Test
	@DisplayName("sendContactInvite to user modifies both users correctly")
	void testSendContactInvite_ToNewContact() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getSentContactInvites()).thenReturn(new ArrayList<>());
		when(mockUser2.getSentContactInvites()).thenReturn(new ArrayList<>());
		userContactService.sendContactInvite("user1", "user2");
		verify(mockUser1).addSentContactInvite(mockUser2);
		verify(mockUser2).addReceivedContactInvite(mockUser1);
		verify(mockUserRepo).saveAll(List.of(mockUser1, mockUser2));
	}

	@Test
	@DisplayName("removeFromContacts throws UserNotFoundException for invalid username")
	void testRemoveFromContacts_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userContactService.removeFromContacts("user1", "user2"));
	}

	@Test
	@DisplayName("removeFromContacts affects both users' contact lists")
	void testRemoveFromContacts_WithValidUsernames() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		userContactService.removeFromContacts("user1", "user2");
		verify(mockUser1).removeContact(mockUser2);
		verify(mockUser2).removeContact(mockUser1);
		verify(mockUserRepo).saveAll(List.of(mockUser1, mockUser2));
	}

	@Test
	@DisplayName("acceptContact throws UserNotFoundException for invalid username")
	void testAcceptContact_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userContactService.acceptContact("user1", "user2"));
	}

	@Test
	@DisplayName("acceptContact throws UserNotInvitedException if user not invited")
	void testAcceptContact_WithInvalidInvite() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		assertThrows(UserNotInvitedException.class, () -> userContactService.acceptContact("user1", "user2"));
	}

	@Test
	@DisplayName("acceptContact affects both users' contact lists")
	void testAcceptContact_WithValidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getReceivedContactInvites()).thenReturn(listContainingUser2);
		when(mockUser2.getSentContactInvites()).thenReturn(listContainingUser1);
		userContactService.acceptContact("user1", "user2");
		verify(mockUser1).addContact(mockUser2);
		verify(mockUser2).addContact(mockUser1);
		verify(mockUser1).removeReceivedContactInvite(mockUser2);
		verify(mockUser2).removeSentContactInvite(mockUser1);
		verify(mockUserRepo).saveAll(List.of(mockUser1, mockUser2));
	}

	@Test
	@DisplayName("cancelContactInvite throws UserNotFoundException for invalid username")
	void testCancelContactInvite_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userContactService.cancelContactInvite("user1", "user2"));
	}

	@Test
	@DisplayName("cancelContactInvite throws UserNotInvitedException if user not invited")
	void testCancelContactInvite_WithInvalidInvite() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		assertThrows(UserNotInvitedException.class, () -> userContactService.cancelContactInvite("user1", "user2"));
	}

	@Test
	@DisplayName("cancelContactInvite modifies both users' contact invite lists correctly")
	void testCancelContactInvite_WithValidInvite() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUserRepo.findByUsername("user2")).thenReturn(Optional.ofNullable(mockUser2));
		when(mockUser1.getSentContactInvites()).thenReturn(listContainingUser2);
		when(mockUser2.getReceivedContactInvites()).thenReturn(listContainingUser1);
		userContactService.cancelContactInvite("user1", "user2");
		verify(mockUser1).removeSentContactInvite(mockUser2);
		verify(mockUser2).removeReceivedContactInvite(mockUser1);
		verify(mockUserRepo).saveAll(List.of(mockUser1, mockUser2));
	}

	@Test
	@DisplayName("getContacts throws UserNotFoundException for invalid username")
	void testGetContacts_WithInvalidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.empty());
		assertThrows(UserNotFoundException.class, () -> userContactService.getContacts("user1"));
	}

	@Test
	@DisplayName("getContacts returns correct list for valid username")
	void testGetContacts_WithIValidUsername() {
		when(mockUserRepo.findByUsername("user1")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUser1.getContacts()).thenReturn(listContainingUser2);
		assertEquals(listContainingUser2, userContactService.getContacts("user1"));
	}
}
