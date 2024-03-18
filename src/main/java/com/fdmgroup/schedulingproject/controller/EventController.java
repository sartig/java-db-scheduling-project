package com.fdmgroup.schedulingproject.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fdmgroup.schedulingproject.exception.EventAlreadyInCalendarException;
import com.fdmgroup.schedulingproject.exception.EventClashException;
import com.fdmgroup.schedulingproject.exception.EventNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.Event;
import com.fdmgroup.schedulingproject.model.Timeslot;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.EventService;
import com.fdmgroup.schedulingproject.service.UserContactService;
import com.fdmgroup.schedulingproject.service.UserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class EventController {

	@Autowired
	private EventService eventService;
	@Autowired
	private UserDetailsService userDetailsService;
	@Autowired
	private UserContactService userContactService;

	private Logger logger = LogManager.getLogger(EventController.class);

	@GetMapping("/event/create")
	public String createEventPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /event/accept without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			List<User> contacts = userContactService.getContacts(username);
			model.addAttribute("contacts", contacts);
			logger.trace("User with username " + username + " loaded /event/create page");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		return "create-event";
	}

	@PostMapping("/event/create-schedule")
	public String selectEventTime(HttpServletRequest req, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /event/accept without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}

		try {
			User user = userDetailsService.getUserInfo(username);
			List<User> potentialAttendingUsers = getInvitedUsersFromPostRequestParams(req);
			model.addAttribute("invited", potentialAttendingUsers);
			Event event = createEventFromPostRequestParams(req);
			model.addAttribute("event", event);

			List<Timeslot> suggestedTimeslots = eventService.findTimeslots(user, event.getStartTime(),
					event.getDurationMinutes(), potentialAttendingUsers);
			model.addAttribute("suggestedTimeslots", suggestedTimeslots);
			logger.trace("User with username " + username + " loaded /event/create-schedule page");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		return "create-event-schedule";
	}

	@PostMapping("/event/create-final")
	public String createEvent(HttpServletRequest req, HttpSession session, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /event/create-final without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}

		// if event successfully created, navigate to that event's details page
		// otherwise go to create event page with whatever error
		Event createdEvent = createEventFromPostRequestParams(req);
		try {
			eventService.createEvent(username, getInvitedUsersFromPostRequestParams(req), createdEvent);
			logger.info("User " + username + " created new event with title " + createdEvent.getTitle());
			logger.trace("New event created with parameters (title=" + createdEvent.getTitle() + ", description="
					+ createdEvent.getDescription() + ", location=" + createdEvent.getLocation() + ", startTime="
					+ createdEvent.getStartTime() + ", durationMinutes=" + createdEvent.getDurationMinutes()
					+ ", organiserUsername=" + username + ")");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		} catch (EventClashException e) {
			redirectAttributes.addFlashAttribute("message", "Event clashes with other events in calendar");
			return "redirect:/event/create";
		}
		return "redirect:/event/" + createdEvent.getId();
	}

	private Event createEventFromPostRequestParams(HttpServletRequest req) {
		String eventTitle = req.getParameter("title");
		String eventDescription = req.getParameter("description");
		String eventLocation = req.getParameter("location");
		LocalDateTime eventStartTime = LocalDateTime.parse(req.getParameter("startTime"));
		int eventDurationMinutes = Integer.parseInt(req.getParameter("durationMinutes"));
		return new Event(eventTitle, eventDescription, eventLocation, eventStartTime, eventDurationMinutes);
	}

	private List<User> getInvitedUsersFromPostRequestParams(HttpServletRequest req) throws UserNotFoundException {
		List<User> invitedUsers = new ArrayList<>();
		String[] invitedContacts = req.getParameterValues("selectedContacts");
		if (invitedContacts != null) {
			for (String contact : invitedContacts) {
				invitedUsers.add(userDetailsService.getUserInfo(contact));
			}
		}
		return invitedUsers;
	}

	@GetMapping("/event/{id}")
	public String eventDetailsPage(@PathVariable String id, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			Event event = eventService.getEventFromId(id);
			String username = (String) session.getAttribute("current_user");
			if (username == null) {
				// if user navigates manually to /event/accept without logging in
				redirectAttributes.addFlashAttribute("message", "Please log in");
				return "redirect:/";
			}
			User user = userDetailsService.getUserInfo(username);
			if (!(event.getOrganiser().getId() == user.getId()) && !event.getAttendees().contains(user)
					&& !event.getInvitees().contains(user)) {
				// user not involved with this event so prevent them from viewing it
				logger.error("User " + username + " attempted to access event without invitation");
				redirectAttributes.addFlashAttribute("message", "No access to this event");
				return "redirect:/home";
			}

			// TODO: additional controls for organiser (modify event, remove invites)
			if (event.getOrganiser().getId() == user.getId()) {

			}
			model.addAttribute("event", event);
			logger.trace("User with username " + username + " loaded /event page for event with id " + id);
		} catch (EventNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find event");
			return "redirect:/home";
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		return "event";
	}

	@GetMapping("/event/accept/{id}")
	public String acceptEventInvite(@PathVariable String id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /event/accept without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			eventService.acceptEventInvite(username, id);
			logger.info("User with username" + username + " accepted invite to event with id " + id);
		} catch (EventNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find event");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		} catch (UserNotInvitedException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find invite to this event");
		} catch (EventAlreadyInCalendarException e) {
			redirectAttributes.addFlashAttribute("message", "Event already in calendar");
		} catch (EventClashException e) {
			redirectAttributes.addFlashAttribute("message", "Event clashes with other events in calendar");
		}
		return "redirect:/calendar";
	}

	@GetMapping("/event/decline/{id}")
	public String declineEventInvite(@PathVariable String id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /event/accept without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			eventService.declineEventInvite(username, id);
			logger.info("User with username" + username + " declined invite to event with id " + id);
		} catch (EventNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find event");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		} catch (UserNotInvitedException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find invite to this event");
		} catch (EventAlreadyInCalendarException e) {
			redirectAttributes.addFlashAttribute("message", "Event already in calendar");
		}
		return "redirect:/calendar";
	}

	// TODO: cancel event invite!
}
