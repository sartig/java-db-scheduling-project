package com.fdmgroup.schedulingproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.schedulingproject.exception.EventAlreadyInCalendarException;
import com.fdmgroup.schedulingproject.exception.EventClashException;
import com.fdmgroup.schedulingproject.exception.EventNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.Event;
import com.fdmgroup.schedulingproject.model.Timeslot;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.repository.EventRepository;
import com.fdmgroup.schedulingproject.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

	private EventService eventService;

	@Mock
	EventRepository mockEventRepo;
	@Mock
	UserRepository mockUserRepo;

	@Mock
	Event mockEvent1, mockEvent2;
	@Mock
	User mockUser1, mockUser2;

	List<Event> listContainingMockEvent1, listContainingMockEvent2;

	List<User> listContainingMockUser2;

	@BeforeEach
	void setUp() {
		eventService = new EventService();
		eventService.setUserRepository(mockUserRepo);
		eventService.setEventRepository(mockEventRepo);
		listContainingMockEvent1 = new ArrayList<>();
		listContainingMockEvent1.add(mockEvent1);
		listContainingMockEvent2 = new ArrayList<>();
		listContainingMockEvent2.add(mockEvent2);
		listContainingMockUser2 = new ArrayList<>();
		listContainingMockUser2.add(mockUser2);
	}

	@Test
	@DisplayName("acceptEventInvite throws EventNotFoundException for invalid event format")
	void testAcceptEventInvite_WithInvalidEventIdFormat() {
		assertThrows(EventNotFoundException.class, () -> eventService.acceptEventInvite("username", "invalid"));
	}

	@Test
	@DisplayName("acceptEventInvite throws EventNotFoundException for invalid event id")
	void testAcceptEventInvite_WithInvalidEventId() {
		assertThrows(EventNotFoundException.class, () -> eventService.acceptEventInvite("username", "5"));
	}

	@Test
	@DisplayName("acceptEventInvite throws UserNotFoundException for invalid username")
	void testAcceptEventInvite_WithInvalidUsername() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		assertThrows(UserNotFoundException.class, () -> eventService.acceptEventInvite("username", "5"));
	}

	@Test
	@DisplayName("acceptEventInvite throws EventAlreadyInCalendarException for event already in calendar")
	void testAcceptEventInvite_WithEventAlreadyInCalendar() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUser1.getCalendar()).thenReturn(listContainingMockEvent1);
		assertThrows(EventAlreadyInCalendarException.class, () -> eventService.acceptEventInvite("username", "5"));
	}

	@Test
	@DisplayName("acceptEventInvite throws UserNotInvitedException for event where user does not have an invitation")
	void testAcceptEventInvite_WithEventWithoutInvite() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser1));
		assertThrows(UserNotInvitedException.class, () -> eventService.acceptEventInvite("username", "5"));
	}

	@Test
	@DisplayName("acceptEventInvite throws EventClashException for event where user has clashing event in calendar")
	void testAcceptEventInvite_WithClashingEvent() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUser1.getEventInvites()).thenReturn(listContainingMockEvent1);
		when(mockUser1.getCalendar()).thenReturn(listContainingMockEvent2);
		when(mockEvent2.doesEventClash(mockEvent1.getStartTime(), mockEvent1.getDurationMinutes())).thenReturn(true);
		assertThrows(EventClashException.class, () -> eventService.acceptEventInvite("username", "5"));
	}

	@Test
	@DisplayName("acceptEventInvite processes valid event correctly")
	void testAcceptEventInvite_WithValidEvent() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUser1.getEventInvites()).thenReturn(listContainingMockEvent1);
		eventService.acceptEventInvite("username", "5");
		verify(mockUser1).removeEventInvite(mockEvent1);
		verify(mockUser1).addCalendarEvent(mockEvent1);
		verify(mockEvent1).addAttendee(mockUser1);
		verify(mockEvent1).removeInvitee(mockUser1);
	}

	@Test
	@DisplayName("acceptEventInvite processes valid event correctly with non-clashing events in calendar")
	void testAcceptEventInvite_WithValidNonClashingEvent() {
		when(mockEventRepo.findById((long) 5)).thenReturn(Optional.ofNullable(mockEvent1));
		when(mockUserRepo.findByUsername("username")).thenReturn(Optional.ofNullable(mockUser1));
		when(mockUser1.getEventInvites()).thenReturn(listContainingMockEvent1);
		when(mockUser1.getCalendar()).thenReturn(listContainingMockEvent2);
		when(mockEvent2.doesEventClash(mockEvent1.getStartTime(), mockEvent1.getDurationMinutes())).thenReturn(false);
		eventService.acceptEventInvite("username", "5");
		verify(mockUser1).removeEventInvite(mockEvent1);
		verify(mockUser1).addCalendarEvent(mockEvent1);
		verify(mockEvent1).addAttendee(mockUser1);
		verify(mockEvent1).removeInvitee(mockUser1);
	}

	@Test
	@DisplayName("findTimeslots provides correct correct selection for attendees with no other events in calendar")
	void testFindTimeslots_WithNoOtherEvents() {
		LocalDateTime now = LocalDateTime.now();
		Timeslot timeslot1 = new Timeslot(now, 30);
		Timeslot timeslot2 = new Timeslot(now.plusMinutes(15), 30);
		Timeslot timeslot3 = new Timeslot(now.plusMinutes(30), 30);
		List<Timeslot> expectedTimeslots = new ArrayList<>();
		expectedTimeslots.add(timeslot1);
		expectedTimeslots.add(timeslot2);
		expectedTimeslots.add(timeslot3);

		when(mockUser1.isTimeslotAvailable(any())).thenReturn(true);
		when(mockUser2.isTimeslotAvailable(any())).thenReturn(true);
		List<Timeslot> retrievedTimeslots = eventService.findTimeslots(mockUser1, now, 30, listContainingMockUser2);
		assertEquals(expectedTimeslots.size(), retrievedTimeslots.size());
		for (int i = 0; i < expectedTimeslots.size(); i++) {
			// timeslots are different instances so can't use assertIterableEquals()
			assertEquals(expectedTimeslots.get(i).getStart(), retrievedTimeslots.get(i).getStart());
			assertEquals(expectedTimeslots.get(i).getEnd(), retrievedTimeslots.get(i).getEnd());
		}
	}

	@Test
	@DisplayName("findTimeslots provides correct correct selection for attendees with clash")
	void testFindTimeslots_WithClashingEvents() {
		LocalDateTime now = LocalDateTime.now();
		Timeslot timeslot1 = new Timeslot(now.plusMinutes(30), 30);
		Timeslot timeslot2 = new Timeslot(now.plusMinutes(45), 30);
		Timeslot timeslot3 = new Timeslot(now.plusMinutes(60), 30);
		Timeslot timeslot4 = new Timeslot(now.minusMinutes(30), 30);
		List<Timeslot> expectedTimeslots = new ArrayList<>();
		expectedTimeslots.add(timeslot4);
		expectedTimeslots.add(timeslot1);
		expectedTimeslots.add(timeslot2);
		expectedTimeslots.add(timeslot3);
		
		lenient().when(mockUser1.isTimeslotAvailable(argThat(ts -> ts != null && ts.getStart().isAfter(now.plusMinutes(29))))).thenReturn(true);
		lenient().when(mockUser1.isTimeslotAvailable(argThat(ts -> ts != null && ts.getStart().isBefore(now.minusMinutes(29))))).thenReturn(true);
		List<Timeslot> retrievedTimeslots = eventService.findTimeslots(mockUser1, now, 30, new ArrayList<>());
		assertEquals(expectedTimeslots.size(), retrievedTimeslots.size());
		for (int i = 0; i < expectedTimeslots.size(); i++) {
			// timeslots are different instances so can't use assertIterableEquals()
			assertEquals(expectedTimeslots.get(i).getStart(), retrievedTimeslots.get(i).getStart());
			assertEquals(expectedTimeslots.get(i).getEnd(), retrievedTimeslots.get(i).getEnd());
		}
	}
}
