package com.fdmgroup.schedulingproject.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventTest {

	private Event event;
	@Mock
	private User mockUser;
	@Mock
	private Timeslot mockTimeslot;

	@BeforeEach
	void setUp() {
		event = new Event();
	}

	@Test
	@DisplayName("addAttendee() adds a given User object to the attendees List<User>")
	void testAddAttendee() {
		event.addAttendee(mockUser);
		
		assertTrue(event.getAttendees().contains(mockUser));
	}

	@Test
	@DisplayName("removeAttendee() removes a given User object from the attendees List<User>")
	void testRemoveAttendee() {
		event.addAttendee(mockUser);
		event.removeAttendee(mockUser);
		
		assertFalse(event.getAttendees().contains(mockUser));
	}

	@Test
	@DisplayName("addInvitee() adds a given User object to the invitees List<User>")
	void testAddInvitee() {
		event.addInvitee(mockUser);
		
		assertTrue(event.getInvitees().contains(mockUser));
	}

	@Test
	@DisplayName("removeInvitee() removes a given User object from the invitees List<User>")
	void testRemoveInvitee() {
		event.addInvitee(mockUser);
		event.removeInvitee(mockUser);
		
		assertFalse(event.getInvitees().contains(mockUser));
	}

	@Test
	@DisplayName("doesEventClash() returns true if passed a start time and duration that overlaps it entirely")
	void testDoesEventClash_WithWhollyClashingEvent() {
		LocalDateTime startTime = LocalDateTime.now().minusHours(1);
		int durationMinutes = 120;
		event.setStartTime(LocalDateTime.now());
		event.setDurationMinutes(30);

		assertTrue(event.doesEventClash(startTime, durationMinutes));
	}


	@Test
	@DisplayName("doesEventClash() returns true if passed a start time and duration that overlaps its beginning")
	void testDoesEventClash_WithStartingClashingEvent() {
		LocalDateTime startTime = LocalDateTime.now().minusHours(1);
		int durationMinutes = 120;
		event.setStartTime(LocalDateTime.now());
		event.setDurationMinutes(120);

		assertTrue(event.doesEventClash(startTime, durationMinutes));
	}

	@Test
	@DisplayName("doesEventClash() returns true if passed a start time and duration that overlaps its end")
	void testDoesEventClash_WithEndingClashingEvent() {
		LocalDateTime startTime = LocalDateTime.now().plusHours(1);
		int durationMinutes = 60;
		event.setStartTime(LocalDateTime.now());
		event.setDurationMinutes(90);

		assertTrue(event.doesEventClash(startTime, durationMinutes));
	}

	@Test
	@DisplayName("doesEventClash() returns false if passed a start time and duration that does not clash with it")
	void testDoesEventClash_WithNonClashingEvent() {
		LocalDateTime startTime = LocalDateTime.now().minusHours(1);
		int durationMinutes = 45;
		event.setStartTime(LocalDateTime.now());
		event.setDurationMinutes(30);
		
		assertFalse(event.doesEventClash(startTime, durationMinutes));
	}

	@Test
	@DisplayName("doesEventClash() returns false if passed a start time that matches its end time")
	void testDoesEventClash_WithSequentialEvent() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startTime = now.plusHours(1);
		int durationMinutes = 60;
		event.setStartTime(now);
		event.setDurationMinutes(60);

		assertFalse(event.doesEventClash(startTime, durationMinutes));
	}

	@Test
	@DisplayName("doesEventClash() returns true if passed a timeslot that entirely overlaps with it")
	void testDoesEventClash_WithWhollyClashingTimeslot() {
		LocalDateTime startTime = LocalDateTime.now().minusHours(1);
		int durationMinutes = 120;
		event.setStartTime(LocalDateTime.now());
		event.setDurationMinutes(60);
		when(mockTimeslot.getStart()).thenReturn(startTime);
		when(mockTimeslot.getEnd()).thenReturn(startTime.plusMinutes(durationMinutes));

		assertTrue(event.doesEventClash(mockTimeslot));
	}

}
