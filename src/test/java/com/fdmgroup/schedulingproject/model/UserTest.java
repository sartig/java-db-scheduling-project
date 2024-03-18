package com.fdmgroup.schedulingproject.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTest {

	private User user;
	@Mock
	private Event pastEvent, futureEvent;
	
	@Mock
	private User mockUser;
	
	@Mock
	private Timeslot mockTimeslot;

	@BeforeEach
	void setUp() {
		user = new User();
	}
	
	@Test
	@DisplayName("addCalendarEvent() adds a given Event object to the calendar List<Event>")
	void testAddCalendarEvent() {
		user.addCalendarEvent(pastEvent);
		
		assertTrue(user.getCalendar().contains(pastEvent));
	}
	
	@Test
	@DisplayName("removeCalendarEvent() removes a given Event object from the calendar List<Event>")
	void testRemoveCalendarEvent() {
		user.addCalendarEvent(pastEvent);
		user.removeCalendarEvent(pastEvent);
		
		assertFalse(user.getCalendar().contains(pastEvent));
	}

	@Test
	@DisplayName("getFutureCalendar correctly filters out events prior to current time")
	void testGetFutureCalendar() {
		user.addCalendarEvent(pastEvent);
		user.addCalendarEvent(futureEvent);
		when(pastEvent.getStartTime()).thenReturn(LocalDateTime.now().minusHours(2));
		when(futureEvent.getStartTime()).thenReturn(LocalDateTime.now().plusHours(2));
		List<Event> futureCalendar = user.getFutureCalendar();
		assertEquals(1, futureCalendar.size());
		assertTrue(futureCalendar.contains(futureEvent));
        assertFalse(futureCalendar.contains(pastEvent));
	}
	
	@Test
	@DisplayName("addCreatedEvent() adds a given Event object to the createdEvent List<Event>")
	void testAddCreatedEvent() {
		user.addCreatedEvent(pastEvent);
		
		assertTrue(user.getCreatedEvents().contains(pastEvent));
	}
	
	@Test
	@DisplayName("removeCreatedEvent() removes a given Event object from the createdEvent List<Event>")
	void testRemoveCreatedEvent() {
		user.addCreatedEvent(pastEvent);
		user.removeCreatedEvent(pastEvent);
		
		assertFalse(user.getCreatedEvents().contains(pastEvent));
	}

	@Test
	@DisplayName("getFutureCreatedEvents correctly filters out events prior to current time")
	void testGetFutureCreatedEvents() {
		user.addCreatedEvent(pastEvent);
		user.addCreatedEvent(futureEvent);
		when(pastEvent.getStartTime()).thenReturn(LocalDateTime.now().minusHours(2));
		when(futureEvent.getStartTime()).thenReturn(LocalDateTime.now().plusHours(2));
		List<Event> futureCreatedEvents = user.getFutureCreatedEvents();
		assertEquals(1, futureCreatedEvents.size());
		assertTrue(futureCreatedEvents.contains(futureEvent));
        assertFalse(futureCreatedEvents.contains(pastEvent));
	}
	
	@Test
	@DisplayName("addEventInvite() adds a given Event object to the eventInvites List<Event>")
	void testAddEventInvite() {
		user.addEventInvite(pastEvent);
		
		assertTrue(user.getEventInvites().contains(pastEvent));
	}
	
	@Test
	@DisplayName("removeEventInvite() removes a given Event object from the eventInvites List<Event>")
	void testRemoveEventInvite() {
		user.addEventInvite(pastEvent);
		user.removeEventInvite(pastEvent);
		
		assertFalse(user.getEventInvites().contains(pastEvent));
	}

	@Test
	@DisplayName("getFutureEventInvites correctly filters out events prior to current time")
	void testGetFutureEventInvites() {
		user.addEventInvite(pastEvent);
		user.addEventInvite(futureEvent);
		when(pastEvent.getStartTime()).thenReturn(LocalDateTime.now().minusHours(2));
		when(futureEvent.getStartTime()).thenReturn(LocalDateTime.now().plusHours(2));
		List<Event> futureEventInvites = user.getFutureEventInvites();
		assertEquals(1, futureEventInvites.size());
		assertTrue(futureEventInvites.contains(futureEvent));
        assertFalse(futureEventInvites.contains(pastEvent));
	}
	
	@Test
	@DisplayName("addContact() adds a given User object to the contacts List<User>")
	void testAddContact() {
		user.addContact(mockUser);
		
		assertTrue(user.getContacts().contains(mockUser));
	}
	
	@Test
	@DisplayName("removeContact() removes a given User object from the contacts List<User>")
	void testRemoveContact() {
		user.addContact(mockUser);
		user.removeContact(mockUser);
		
		assertFalse(user.getContacts().contains(mockUser));
	}
	
	@Test
	@DisplayName("addSentContactInvite() adds a given User object to the sentContactInvites List<User>")
	void testAddSentContactInvite() {
		user.addSentContactInvite(mockUser);
		
		assertTrue(user.getSentContactInvites().contains(mockUser));
	}
	
	@Test
	@DisplayName("removeSentContactInvite() removes a given User object from the sentContactInvites List<User>")
	void testRemoveSentContactInvite() {
		user.addSentContactInvite(mockUser);
		user.removeSentContactInvite(mockUser);
		
		assertFalse(user.getSentContactInvites().contains(mockUser));
	}
	
	@Test
	@DisplayName("addReceivedContactInvite() adds a given User object to the receivedContactInvites List<User>")
	void testAddReceivedContactInvite() {
		user.addReceivedContactInvite(mockUser);
		
		assertTrue(user.getReceivedContactInvites().contains(mockUser));
	}
	
	@Test
	@DisplayName("removeReceivedContactInvite() removes a given User object from the receivedContactInvites List<User>")
	void testRemoveReceivedContactInvite() {
		user.addReceivedContactInvite(mockUser);
		user.removeReceivedContactInvite(mockUser);
		
		assertFalse(user.getReceivedContactInvites().contains(mockUser));
	}
	
	@Test
	@DisplayName("isTimeslotAvailable() checks events in calendar for clash")
	void testIsTimeslotAvailable_CheckEventsInCalendar() {
		user.addCalendarEvent(futureEvent);
		when(futureEvent.doesEventClash(mockTimeslot)).thenReturn(false);
		assertTrue(user.isTimeslotAvailable(mockTimeslot));
	}
	
	@Test
	@DisplayName("isTimeslotAvailable() checks events in eventInvites for clash")
	void testIsTimeslotAvailable_CheckEventsInEventInvites() {
		user.addEventInvite(futureEvent);
		when(futureEvent.doesEventClash(mockTimeslot)).thenReturn(true);
		assertFalse(user.isTimeslotAvailable(mockTimeslot));
	}

}
