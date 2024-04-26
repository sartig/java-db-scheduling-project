package com.fdmgroup.schedulingproject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.fdmgroup.schedulingproject.exception.EventAlreadyInCalendarException;
import com.fdmgroup.schedulingproject.exception.EventClashException;
import com.fdmgroup.schedulingproject.exception.EventNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.Event;
import com.fdmgroup.schedulingproject.model.Timeslot;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.*;

@WebMvcTest
@ExtendWith(MockitoExtension.class)
public class EventControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	UserDetailsService mockUserDetailsService;
	@MockBean
	UserContactService mockUserContactService;
	@MockBean
	EventService mockEventService;

	@Mock
	User mockUser1, mockUser2;

	@Mock
	Timeslot mockTimeslot1, mockTimeslot2, mockTimeslot3;

	@Test
	@DisplayName("Test GET request to \"/event/create\" redirects to index.html if user not logged in")
	void testGetEventCreation_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/event/create"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/create\" returns to index.html for invalid username")
	void testGetEventCreation_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserContactService).getContacts("invalid");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/create").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/create\" for valid user populates correct params")
	void testGetEventCreation_HasCorrectAttributes_ForLoggedInUser() throws Exception {
		List<User> mockContactsList = new ArrayList<>();
		mockContactsList.add(mockUser2);
		when(mockUserContactService.getContacts("valid")).thenReturn(mockContactsList);
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/event/create").sessionAttr("current_user", "valid"))
				.andExpect(MockMvcResultMatchers.view().name("create-event")).andReturn();
		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		Map<?, ?> model = modelAndView.getModel();
		assertEquals(mockContactsList, model.get("contacts"));
	}

	@Test
	@DisplayName("Test POST request to \"/event/create-schedule\" redirects to index.html for non-logged in user")
	void testPostEventCreateSchedule_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/event/create-schedule")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/event/create-schedule\" redirects to index.html for invalid username")
	void testPostEventCreateSchedule_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockUserDetailsService).getUserInfo("invalid");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/event/create-schedule")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED).sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("Test POST request to \"/event/create-schedule\" for valid username populates correct params")
	void testPostEventCreateSchedule_HasCorrectAttributes_ForValidUser() throws Exception {
		when(mockUserDetailsService.getUserInfo("username")).thenReturn(mockUser1);
		when(mockUserDetailsService.getUserInfo("invitee1")).thenReturn(mockUser2);
		LocalDateTime now = LocalDateTime.now();
		List<Timeslot> mockTimeslots = new ArrayList<>();
		mockTimeslots.add(mockTimeslot1);
		mockTimeslots.add(mockTimeslot2);
		mockTimeslots.add(mockTimeslot3);
		when(mockEventService.findTimeslots(eq(mockUser1), eq(now), eq(30), argThat(x -> x.contains(mockUser2))))
				.thenReturn(mockTimeslots);
		MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/event/create-schedule")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("selectedContacts", "invitee1").param("title", "title")
				.param("description", "description").param("location", "location").param("startTime", now.toString())
				.param("durationMinutes", "30").sessionAttr("current_user", "username"))
				.andExpect(MockMvcResultMatchers.view().name("create-event-schedule")).andReturn();

		ModelAndView modelAndView = result.getModelAndView();
		assertNotNull(modelAndView);
		Map<?, ?> model = modelAndView.getModel();
		List<User> invitedUsers = (List<User>) model.get("invited");
		Event createdEvent = (Event) model.get("event");
		assertEquals(mockUser2, invitedUsers.get(0));
		assertEquals("title", createdEvent.getTitle());
		assertEquals("description", createdEvent.getDescription());
		assertEquals("location", createdEvent.getLocation());
		assertEquals(now, createdEvent.getStartTime());
		assertEquals(30, createdEvent.getDurationMinutes());
		assertEquals(mockTimeslots, model.get("suggestedTimeslots"));
	}

	@Test
	@DisplayName("Test POST request to \"/event/create-final\" redirects to index.html for non-logged in user")
	void testPostEventCreateFinal_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.post("/event/create-final")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/event/create-final\" redirects to index.html for invalid username")
	void testPostEventCreateFinal_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockEventService).createEvent(eq("invalid"), any(), any());
		LocalDateTime now = LocalDateTime.now();
		when(mockUserDetailsService.getUserInfo("invitee1")).thenReturn(mockUser2);

		MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/event/create-final")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).sessionAttr("current_user", "invalid")
				.param("selectedContacts", "invitee1").param("title", "title")
				.param("description", "description").param("location", "location").param("startTime", now.toString())
				.param("durationMinutes", "30").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test POST request to \"/event/create-final\" redirects to specific event.html for valid user")
	void testPostEventCreateFinal_RedirectsToEvent_ForValidUser() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		when(mockUserDetailsService.getUserInfo("invitee1")).thenReturn(mockUser2);

		mvc.perform(MockMvcRequestBuilders.post("/event/create-final")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).sessionAttr("current_user", "username")
				.param("selectedContacts", "invitee1").param("title", "title")
				.param("description", "description").param("location", "location").param("startTime", now.toString())
				.param("durationMinutes", "30").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrlPattern("/event/*"))
				.andReturn();
		verify(mockEventService).createEvent(eq("username"), argThat(x -> x.contains(mockUser2)),
				argThat(e -> e.getTitle().equals("title") && e.getDescription().equals("description")
						&& e.getLocation().equals("location") && e.getStartTime().equals(now)
						&& e.getDurationMinutes() == 30));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id} redirects to index.html for non-logged in user")
	void testGetAcceptEvent_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id}\" redirects to index.html for invalid username")
	void testGetAcceptEvent_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockEventService).acceptEventInvite("invalid", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id}\" redirects to calendar.html for invalid event")
	void testGetAcceptEvent_RedirectsToCalendar_IfEventNotValid() throws Exception {
		doThrow(new EventNotFoundException()).when(mockEventService).acceptEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find event", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id}\" redirects to calendar.html if user not invited")
	void testGetAcceptEvent_RedirectsToCalendar_IfUserNotInvited() throws Exception {
		doThrow(new UserNotInvitedException()).when(mockEventService).acceptEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find invite to this event", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id}\" redirects to calendar.html if event already in user calendar")
	void testGetAcceptEvent_RedirectsToCalendar_IfEventInUserCalendar() throws Exception {
		doThrow(new EventAlreadyInCalendarException()).when(mockEventService).acceptEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Event already in calendar", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/accept/{event-id}\" redirects to calendar.html if event clashes with user calendar")
	void testGetAcceptEvent_RedirectsToCalendar_IfEventClashesWithUserCalendar() throws Exception {
		doThrow(new EventClashException()).when(mockEventService).acceptEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/accept/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Event clashes with other events in calendar", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/decline/{event-id} redirects to index.html for non-logged in user")
	void testGetDeclineEvent_RedirectsToIndex_IfNotLoggedIn() throws Exception {
		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/decline/4321"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/decline/{event-id}\" redirects to index.html for invalid username")
	void testGetDeclineEvent_RedirectsToIndex_IfSessionNotValid() throws Exception {
		doThrow(new UserNotFoundException()).when(mockEventService).declineEventInvite("invalid", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/decline/4321").sessionAttr("current_user", "invalid"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Please log in", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/decline/{event-id}\" redirects to calendar.html for invalid event")
	void testGetDeclineEvent_RedirectsToCalendar_IfEventNotValid() throws Exception {
		doThrow(new EventNotFoundException()).when(mockEventService).declineEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/decline/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find event", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/decline/{event-id}\" redirects to calendar.html if user not invited")
	void testGetDeclineEvent_RedirectsToCalendar_IfUserNotInvited() throws Exception {
		doThrow(new UserNotInvitedException()).when(mockEventService).declineEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/decline/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Could not find invite to this event", flashMap.get("message"));
	}

	@Test
	@DisplayName("Test GET request to \"/event/decline/{event-id}\" redirects to calendar.html if event already in user calendar")
	void testGetDeclineEvent_RedirectsToCalendar_IfEventInUserCalendar() throws Exception {
		doThrow(new EventAlreadyInCalendarException()).when(mockEventService).declineEventInvite("username", "4321");

		MvcResult result = mvc
				.perform(MockMvcRequestBuilders.get("/event/decline/4321").sessionAttr("current_user", "username"))
				.andExpectAll(MockMvcResultMatchers.status().is3xxRedirection(),
						MockMvcResultMatchers.redirectedUrl("/calendar"))
				.andReturn();
		FlashMap flashMap = result.getFlashMap();
		assertEquals("Event already in calendar", flashMap.get("message"));
	}
}
