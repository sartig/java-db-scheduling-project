package com.fdmgroup.schedulingproject.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fdmgroup.schedulingproject.model.TimeslotTimeComparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Service class for managing events and related operations. The EventService
 * class provides various methods for handling events, such as accepting or
 * declining event invitations, finding suggested timeslots, creating events,
 * and retrieving events by ID.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Service
public class EventService {
	@Autowired
	private EventRepository eventRepo;
	@Autowired
	private UserRepository userRepo;

	private Logger logger = LogManager.getLogger(EventService.class);

	public void setEventRepository(EventRepository eventRepo) {
		this.eventRepo = eventRepo;
	}

	public void setUserRepository(UserRepository userRepo) {
		this.userRepo = userRepo;
	}

	/**
	 * Accepts an event invitation for a user.
	 *
	 * @param username the username of the user accepting the invitation
	 * @param eventId  the ID of the event to accept
	 * @throws EventNotFoundException          if the event is not found
	 * @throws UserNotFoundException           if the user is not found
	 * @throws UserNotInvitedException         if the user is not invited to the
	 *                                         event
	 * @throws EventAlreadyInCalendarException if the event is already in the user's
	 *                                         calendar
	 * @throws EventClashException             if accepting the event invitation
	 *                                         would cause a clash with existing
	 *                                         events
	 */
	@Transactional
	public void acceptEventInvite(String username, String eventId) throws EventNotFoundException, UserNotFoundException,
			UserNotInvitedException, EventAlreadyInCalendarException, EventClashException {
		try {
			long longId = Long.parseLong(eventId);
			Event event = eventRepo.findById(longId).orElseThrow(EventNotFoundException::new);

			User user = userRepo.findByUsername(username).orElseThrow(UserNotFoundException::new);
			if (user.getCalendar().contains(event)) {
				// event already accepted, remove invite
				user.removeEventInvite(event);
				throw new EventAlreadyInCalendarException();
			}
			if (!user.getEventInvites().contains(event)) {
				throw new UserNotInvitedException();
			}
			if (doesEventClashWithCalendar(user, event)) {
				throw new EventClashException();
			}
			user.removeEventInvite(event);
			user.addCalendarEvent(event);
			event.removeInvitee(user);
			event.addAttendee(user);

			userRepo.save(user);
			eventRepo.save(event);
		} catch (NumberFormatException nfe) {
			throw new EventNotFoundException();
		}
	}

	/**
	 * Declines an event invitation for a user.
	 *
	 * @param username the username of the user declining the invitation
	 * @param eventId  the ID of the event to decline
	 * @throws EventNotFoundException          if the event is not found
	 * @throws UserNotFoundException           if the user is not found
	 * @throws UserNotInvitedException         if the user is not invited to the
	 *                                         event
	 * @throws EventAlreadyInCalendarException if the event is already in the user's
	 *                                         calendar
	 */
	@Transactional
	public void declineEventInvite(String username, String eventId) throws EventNotFoundException,
			UserNotFoundException, UserNotInvitedException, EventAlreadyInCalendarException {
		try {
			long longId = Long.parseLong(eventId);
			Event event = eventRepo.findById(longId).orElseThrow(EventNotFoundException::new);

			User user = userRepo.findByUsername(username).orElseThrow(UserNotFoundException::new);
			if (user.getCalendar().contains(event)) {
				// event already accepted, remove invite
				user.removeEventInvite(event);
				throw new EventAlreadyInCalendarException();
			}
			if (!user.getEventInvites().contains(event)) {
				throw new UserNotInvitedException();
			}
			user.removeEventInvite(event);
			event.removeInvitee(user);

			userRepo.save(user);
			eventRepo.save(event);
		} catch (NumberFormatException nfe) {
			throw new EventNotFoundException();
		}
	}

	/**
	 * Checks if an event clashes with any existing events in a user's calendar.
	 *
	 * @param user  the user to check
	 * @param event the event to check for clashes
	 * @return true if the event clashes with any existing events, false otherwise
	 */
	private boolean doesEventClashWithCalendar(User user, Event event) {
		return user.getCalendar().stream().anyMatch(
				calendarEvent -> calendarEvent.doesEventClash(event.getStartTime(), event.getDurationMinutes()));
	}

	/**
	 * Finds suggested timeslots for an event based on the availability of users.
	 *
	 * @param organiser            the organiser of the event
	 * @param eventStartTime       the desired start time of the event
	 * @param eventDurationMinutes the duration of the event in minutes
	 * @param invitees             the list of users to invite to the event
	 * @return a list of suggested timeslots
	 */
	public List<Timeslot> findTimeslots(User organiser, LocalDateTime eventStartTime, int eventDurationMinutes,
			List<User> invitees) {
		List<Timeslot> suggestedTimes = new ArrayList<>();
		int offset = 0;
		while (suggestedTimes.size() < 3) {
			Timeslot testTimeslot = new Timeslot(eventStartTime.plusMinutes(offset), eventDurationMinutes);
			if (isTimeslotValidForAllUsers(testTimeslot, invitees, organiser)) {
				suggestedTimes.add(testTimeslot);
			}
			offset += Event.minIntervalMinutes;
		}
		offset = Event.minIntervalMinutes;
		long maxBackwardsOffset = Math
				.abs(Duration.between(suggestedTimes.get(0).getStart(), eventStartTime).toMinutes());
		if (maxBackwardsOffset == 0) {
			// don't need to sort since they were added in chronological order
			logger.trace("Found timeslot suggestions for users " + organiser.getUsername() + ","
					+ invitees.stream().map(User::getUsername).collect(Collectors.joining(", ")) + ": "
					+ "requested timeslot=" + eventStartTime + " - " + eventStartTime.plusMinutes(eventDurationMinutes)
					+ " | valid timeslots="
					+ suggestedTimes.stream().map(Timeslot::toString).collect(Collectors.joining(", ")));
			return suggestedTimes;
		}
		while (offset <= maxBackwardsOffset || suggestedTimes.size() < 4) {
			// show a slot before the requested time if it is closer to the suggested time
			// than the nearest later slot
			Timeslot testTimeslot = new Timeslot(eventStartTime.minusMinutes(offset), eventDurationMinutes);
			if (isTimeslotValidForAllUsers(testTimeslot, invitees, organiser)) {
				suggestedTimes.add(testTimeslot);
			}
			offset += Event.minIntervalMinutes;
		}
		suggestedTimes.sort(new TimeslotTimeComparator());
		logger.trace("Found timeslot suggestions for users " + organiser.getUsername() + ","
				+ invitees.stream().map(User::getUsername).collect(Collectors.joining(", ")) + ": "
				+ "requested timeslot=" + eventStartTime + " - " + eventStartTime.plusMinutes(eventDurationMinutes)
				+ " | valid timeslots="
				+ suggestedTimes.stream().map(Timeslot::toString).collect(Collectors.joining(", ")));
		return suggestedTimes;
	}

	/**
	 * Checks if a timeslot is valid for all users.
	 *
	 * @param timeslot  the timeslot to check
	 * @param users     the list of users to check for availability
	 * @param organizer the organizer of the event
	 * @return true if the timeslot is valid for all users, false otherwise
	 */
	private boolean isTimeslotValidForAllUsers(Timeslot timeslot, List<User> users, User organizer) {
		if (timeslot == null) {
			return false;
		}
		if (!organizer.isTimeslotAvailable(timeslot)) {
			return false;
		}
		for (User u : users) {
			if (!u.isTimeslotAvailable(timeslot)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates an event with the specified organizer, invitees, and event details.
	 * The method checks if the event timeslot is valid for all users and throws an
	 * exception if there is a clash. If the event creation is successful, it
	 * updates the organizer's calendar and invitees' event invites and saves the
	 * event.
	 *
	 * @param organiserUsername the username of the event organizer
	 * @param invitees          the list of users to invite to the event
	 * @param event             the event to create
	 * @throws UserNotFoundException if the organizer is not found
	 * @throws EventClashException   if the event timeslot clashes with existing
	 *                               events
	 */
	@Transactional
	public void createEvent(String organiserUsername, List<User> invitees, Event event)
			throws UserNotFoundException, EventClashException {
		User organiser = userRepo.findByUsername(organiserUsername).orElseThrow(UserNotFoundException::new);
		if (!isTimeslotValidForAllUsers(new Timeslot(event.getStartTime(), event.getDurationMinutes()), invitees,
				organiser)) {
			throw new EventClashException();
		}
		event.setOrganiser(organiser);
		organiser.addCalendarEvent(event);
		organiser.addCreatedEvent(event);
		for (User invitee : invitees) {
			event.addInvitee(invitee);
			invitee.addEventInvite(event);
		}
		eventRepo.save(event);
		userRepo.saveAll(invitees);
		userRepo.save(organiser);
	}

	/**
	 * Retrieves an event from its ID.
	 *
	 * @param id the ID of the event
	 * @return the event with the specified ID
	 * @throws EventNotFoundException if the event is not found
	 */
	public Event getEventFromId(String id) throws EventNotFoundException {
		try {
			long longId = Long.parseLong(id);
			return eventRepo.findById(longId).orElseThrow(EventNotFoundException::new);
		} catch (NumberFormatException e) {
			throw new EventNotFoundException();
		}
	}
}
