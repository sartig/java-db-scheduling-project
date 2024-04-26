package com.fdmgroup.schedulingproject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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

import com.fdmgroup.schedulingproject.exception.PasswordDoesNotMatchException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.model.Event;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.*;

import jakarta.servlet.http.HttpSession;

@WebMvcTest
@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	UserDetailsService mockUserDetailsService;
	@MockBean
	UserContactService mockUserContactService;
	@MockBean
	EventService mockEventService;

	@Mock
	User mockUser;

	@Test
	@DisplayName("Test get request to \"/\" returns index.html")
	void testIndex_ReturnsCorrectView() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/")).andExpect(MockMvcResultMatchers.view().name("index"));
	}

	@Test
	@DisplayName("Test POST request to \"/login\" redirects to index.html for invalid username")
	void testPostLogin_RedirectsToIndex_WithInvalidUsername() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).verifyCredentials("invalid", "password");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "invalid").param("password", "password"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Username or password is incorrect", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/login\" redirects to index.html for invalid password")
	void testPostLogin_RedirectsToIndex_WithIncorrectPassword() throws Exception {
		doThrow(new PasswordDoesNotMatchException()).when(mockUserDetailsService).verifyCredentials("invalid",
				"password");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "invalid").param("password", "password"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Username or password is incorrect", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/login\" redirects to home.html for valid credentials and saves username to session")
	void testPostLogin_RedirectsToHome_WithValidCredentials() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "valid").param("password", "password"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/home"))
				.andReturn();

		HttpSession session = result.getRequest().getSession();
		assertNotNull(session);
		assertEquals("valid", session.getAttribute("current_user"));
	}

	@Test
	@DisplayName("Test GET request to \"/home\" redirects to index.html if user not logged in")
	void testGetHome_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/logout")).andExpectAll(
				MockMvcResultMatchers.status().is3xxRedirection(), MockMvcResultMatchers.redirectedUrl("/"));
	}

	@Test
	@DisplayName("Test GET request to \"/home\" redirects to index.html for invalid username")
	void testGetHome_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).verifyCredentials("invalid", "password");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/home").param("username", "invalid").param("password", "password"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/home\" for valid user populates correct params")
	void testGetHome_HasCorrectAttributes_ForLoggedInUser() throws Exception {
		when(mockUserDetailsService.getUserInfo("valid")).thenReturn(mockUser);
		when(mockUser.getDisplayName()).thenReturn("display");
		when(mockUser.getReceivedContactInvites()).thenReturn(new ArrayList<>());
		when(mockUser.getFutureEventInvites()).thenReturn(new ArrayList<>());
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/home").sessionAttr("current_user", "valid"))
				.andExpect(MockMvcResultMatchers.view().name("home")).andReturn();

		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		Map<?, ?> model = modelAndView.getModel();
		assertEquals("display", model.get("user"));
		assertEquals(0, model.get("pending"));
		assertEquals(0, model.get("eventInvites"));
	}

	@Test
	@DisplayName("Test GET request to \"/logout\" redirects to index.html and removes username from session")
	void testGetLogout_RedirectsToIndex_AndClearsCredentials() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/logout").sessionAttr("current_user", "valid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();

		HttpSession session = result.getRequest().getSession();
		assertNotNull(session);
		assertNull(session.getAttribute("current_user"));
	}

	@Test
	@DisplayName("Test GET request to \"/profile\" redirects to index.html if user not logged in")
	void testGetProfile_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/profile")).andExpectAll(
				MockMvcResultMatchers.status().is3xxRedirection(), MockMvcResultMatchers.redirectedUrl("/"));
	}

	@Test
	@DisplayName("Test GET request to \"/profile\" redirects to index.html if session user is invalid")
	void testGetProfile_RedirectsToIndex_IfSessionInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).getUserInfo("invalid");
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/profile").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();

		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/profile\" loads profile.html and passes user to model")
	void testGetProfile_WithValidUsername() throws Exception {
		User realUser = new User();
		when(mockUserDetailsService.getUserInfo("username")).thenReturn(realUser);
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/profile").sessionAttr("current_user", "username"))
				.andExpect(MockMvcResultMatchers.view().name("profile")).andReturn();

		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		assertEquals(realUser, modelAndView.getModel().get("user"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-display-name\" redirects to index.html for not logged in user")
	void testPostUpdateDisplayName_RedirectsToIndex_WithNoSessionUsername() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-display-name")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("displayName", "newDisplayName"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-display-name\" redirects to index.html for invalid username")
	void testPostUpdateDisplayName_RedirectsToIndex_WithInvalidUsername() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).updateDisplayName("username",
				"newDisplayName");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-display-name")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("displayName", "newDisplayName")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-display-name\" redirects to profile.html for valid credentials and updates user's display name")
	void testPostUpdateDisplayName_RedirectsToProfileAndUpdatesRepo_WithValidCredentials() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/profile/update-display-name")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("displayName", "newDisplayName")
				.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/profile"));
		verify(mockUserDetailsService).updateDisplayName("username", "newDisplayName");
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-password\" redirects to index.html for not logged in user")
	void testPostUpdatePassword_RedirectsToIndex_WithNoSessionUsername() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-password")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("currentPassword", "currentPassword")
						.param("newPassword", "newPassword").param("newPasswordConfirmation", "newPassword"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		verifyNoInteractions(mockUserDetailsService);
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-password\" redirects to index.html for invalid username")
	void testPostUpdatePassword_RedirectsToIndex_WithInvalidUsername() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).updatePassword("username", "currentPassword",
				"newPassword");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-password")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("currentPassword", "currentPassword")
						.param("newPassword", "newPassword").param("newPasswordConfirmation", "newPassword")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-password\" redirects to profile.html for invalid current password")
	void testPostUpdatePassword_RedirectsToProfile_WithInvalidPassword() throws Exception {
		doThrow(new PasswordDoesNotMatchException()).when(mockUserDetailsService).updatePassword("username",
				"currentPassword", "newPassword");
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-password")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("currentPassword", "currentPassword")
						.param("newPassword", "newPassword").param("newPasswordConfirmation", "newPassword")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/profile"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		verify(mockUserDetailsService).updatePassword("username", "currentPassword", "newPassword");
		assertEquals("Current password did not match", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-password\" redirects to profile.html for invalid new password")
	void testPostUpdatePassword_RedirectsToProfile_WithInvalidNewPassword() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-password")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("currentPassword", "currentPassword")
						.param("newPassword", "newPassword").param("newPasswordConfirmation", "wrongNewPassword")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/profile"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		verifyNoInteractions(mockUserDetailsService);
		assertEquals("New password did not match", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/profile/update-password\" redirects to profile.html for valid credentials and updates user's password")
	void testPostUpdatePassword_RedirectsToProfileAndUpdatesRepo_WithNewPassword() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/profile/update-password")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("currentPassword", "currentPassword")
						.param("newPassword", "newPassword").param("newPasswordConfirmation", "newPassword")
						.sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/profile"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		verify(mockUserDetailsService).updatePassword("username", "currentPassword", "newPassword");
		assertEquals("Password successfully updated", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/calendar\" redirects to index.html if user not logged in")
	void testGetCalendar_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/calendar"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/calendar\" redirects to index.html if session user is invalid")
	void testGetCalendar_RedirectsToIndex_IfSessionInvalid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).getUserInfo("invalid");
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/calendar").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();

		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/calendar\" loads calendar.html and passes user's calendar info to model")
	void testGetCalendar_WithValidUsername() throws Exception {
		when(mockUserDetailsService.getUserInfo("username")).thenReturn(mockUser);
		List<Event> mockCalendar = new ArrayList<>();
		List<Event> mockInvites = new ArrayList<>();
		when(mockUser.getFutureCalendar()).thenReturn(mockCalendar);
		when(mockUser.getFutureEventInvites()).thenReturn(mockInvites);
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/calendar").sessionAttr("current_user", "username"))
				.andExpect(MockMvcResultMatchers.view().name("calendar")).andReturn();

		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		assertEquals(mockCalendar, modelAndView.getModel().get("calendar"));
		assertEquals(mockInvites, modelAndView.getModel().get("calendarInvites"));
	}
}
