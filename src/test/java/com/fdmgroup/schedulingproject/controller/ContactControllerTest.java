package com.fdmgroup.schedulingproject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;

import com.fdmgroup.schedulingproject.exception.CannotInviteSelfException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInContactsException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInvitedException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.*;

@WebMvcTest
@ExtendWith(MockitoExtension.class)
public class ContactControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	UserDetailsService mockUserDetailsService;
	@MockBean
	UserContactService mockUserContactService;
	@MockBean
	EventService mockEventService;

	@Mock
	User mockUser1;

	@Test
	@DisplayName("Test GET request to \"/contacts\" redirects to index.html if user not logged in")
	void testGetContacts_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/contacts")).andExpectAll(
				MockMvcResultMatchers.status().is3xxRedirection(), MockMvcResultMatchers.redirectedUrl("/"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts\" redirects to index.html for invalid username")
	void testGetContacts_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).getUserInfo("invalid");

		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/home").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts\" for valid user populates correct params")
	void testGetContacts_HasCorrectAttributes_ForLoggedInUser() throws Exception {
		when(mockUserDetailsService.getUserInfo("valid")).thenReturn(mockUser1);
		ArrayList<User> mockContacts = new ArrayList<>();
		ArrayList<User> mockReceivedContactInvites = new ArrayList<>();
		ArrayList<User> mockSentContactInvites = new ArrayList<>();
		when(mockUser1.getContacts()).thenReturn(mockContacts);
		when(mockUser1.getReceivedContactInvites()).thenReturn(mockReceivedContactInvites);
		when(mockUser1.getSentContactInvites()).thenReturn(mockSentContactInvites);
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/contacts").sessionAttr("current_user", "valid"))
				.andExpect(MockMvcResultMatchers.view().name("contacts")).andReturn();

		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		Map<?, ?> model = modelAndView.getModel();
		assertEquals(mockContacts, model.get("contacts"));
		assertEquals(mockReceivedContactInvites, model.get("receivedInvites"));
		assertEquals(mockSentContactInvites, model.get("sentInvites"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for user not logged in redirects to index.html")
	void testPostContactInvite_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for invalid contact username redirects to contacts.html")
	void testPostContactInvite_RedirectsToContacts_IfInviteUsernameIsInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserContactService).sendContactInvite("username", "invitee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User could not be found", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for invite to user already a contact redirects to contacts.html")
	void testPostContactInvite_RedirectsToContacts_IfInvitedUsername_IsAlreadyAContact() throws Exception {
		doThrow(new UserAlreadyInContactsException()).when(mockUserContactService).sendContactInvite("username",
				"invitee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User invitee already in contacts", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for invite to user with a pending invite to contacts.html")
	void testPostContactInvite_RedirectsToContacts_IfInvitedUsername_IsAlreadyInvited() throws Exception {
		doThrow(new UserAlreadyInvitedException()).when(mockUserContactService).sendContactInvite("username",
				"invitee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User invitee already invited", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for invite to self to contacts.html")
	void testPostContactInvite_RedirectsToContacts_IfInvitedOwnUsername() throws Exception {
		doThrow(new CannotInviteSelfException()).when(mockUserContactService).sendContactInvite("username", "invitee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Cannot send contact invite to self", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/contacts/invite\" for valid invite to contacts.html")
	void testPostContactInvite_RedirectsToContacts_ForValidInvite() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/contacts/invite")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", "invitee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		verify(mockUserContactService).sendContactInvite("username", "invitee");
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Sent invite to invitee", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/remove\" for user not logged in redirects to index.html")
	void testGetContactRemove_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/contacts/remove").param("username", "invitee"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/remove\" for invalid provided user to remove redirects to contacts.html")
	void testGetContactRemove_RedirectsToContacts_IfUsernameToRemove_IsInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserContactService).removeFromContacts("username", "removee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/remove").param("username", "removee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User could not be found", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/remove\" for valid removal to contacts.html")
	void testGetContactRemove_RedirectsToContacts_ForValidRemoval() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/remove").param("username", "removee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		verify(mockUserContactService).removeFromContacts("username", "removee");
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User removee removed from contacts", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/accept\" for user not logged in redirects to index.html")
	void testGetContactAccept_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/contacts/accept").param("username", "acceptee"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/accept\" for invalid provided user to remove redirects to contacts.html")
	void testGetContactAccept_RedirectsToContacts_IfUsernameToRemove_IsInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserContactService).acceptContact("username", "acceptee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/accept").param("username", "acceptee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User could not be found", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/accept\" for user without invitation to contacts.html")
	void testGetContactAccept_RedirectsToContacts_IfUsernameToAccept_DidNotSendInvite() throws Exception {
		doThrow(new UserNotInvitedException()).when(mockUserContactService).acceptContact("username", "acceptee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/accept").param("username", "acceptee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find invite from user acceptee", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/accept\" for invite to self to contacts.html")
	void testGetContactAccept_RedirectsToContacts_IfAcceptingOwnUsername() throws Exception {
		doThrow(new CannotInviteSelfException()).when(mockUserContactService).acceptContact("username", "acceptee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/accept").param("username", "acceptee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Cannot accept invitation from self", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/accept\" for valid removal to contacts.html")
	void testGetContactAccept_RedirectsToContacts_ForValidRemoval() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/accept").param("username", "acceptee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		verify(mockUserContactService).acceptContact("username", "acceptee");
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User acceptee added to contacts", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/cancel\" for user not logged in redirects to index.html")
	void testGetContactCancel_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/contacts/cancel").param("username", "cancelee"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/cancel\" for invalid provided user to remove redirects to contacts.html")
	void testGetContactCancel_RedirectsToContacts_IfInviteToCancel_IsInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserContactService).cancelContactInvite("username", "cancelee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/cancel").param("username", "cancelee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("User could not be found", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/cancel\" for invite to self to contacts.html")
	void testGetContactCancel_RedirectsToContacts_IfCancelledOwnUsername() throws Exception {
		doThrow(new CannotInviteSelfException()).when(mockUserContactService).cancelContactInvite("username",
				"cancelee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/cancel").param("username", "cancelee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Cannot cancel invitation from self", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/cancel\" for user without invitation to contacts.html")
	void testGetContactCancel_RedirectsToContacts_IfUsernameToCancel_DidNotHaveInvite() throws Exception {
		doThrow(new UserNotInvitedException()).when(mockUserContactService).cancelContactInvite("username", "cancelee");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/cancel").param("username", "cancelee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find invite to user cancelee", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/contacts/cancel\" for valid removal to contacts.html")
	void testGetContactCancel_RedirectsToContacts_ForValidRemoval() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/contacts/cancel").param("username", "cancelee")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/contacts"))
				.andReturn();
		verify(mockUserContactService).cancelContactInvite("username", "cancelee");
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Invitation to user cancelee removed", flashMap.get("message"));
	}
}
