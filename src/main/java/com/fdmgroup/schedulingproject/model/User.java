package com.fdmgroup.schedulingproject.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Represents a user in the scheduling system. The User class stores information
 * about a user, including their username, password, display name, calendar
 * events, created events, event invites, contacts, and contact invites. It
 * provides methods for managing the user's calendar, events, and contacts.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Entity
public class User implements Cloneable {
	@Id
	@SequenceGenerator(name = "USER_SEQ_GEN", sequenceName = "user_seq")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ_GEN")
	private long id;

	@Column(unique = true)
	private String username;
	private String password;
	private String displayName;
	@ManyToMany
	@JoinTable(name = "user_calendar", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "event_id"))
	private List<Event> calendar = new ArrayList<>();
	@OneToMany(mappedBy = "organiser")
	private List<Event> createdEvents = new ArrayList<>();
	@OneToMany
	private List<Event> eventInvites = new ArrayList<>();
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_contacts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "contact_id"))
	private List<User> contacts = new ArrayList<>();
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "contact_requests_sent", joinColumns = @JoinColumn(name = "sender_id"), inverseJoinColumns = @JoinColumn(name = "recipient_id"))
	private List<User> sentContactInvites = new ArrayList<>();
	@ManyToMany(mappedBy = "sentContactInvites", fetch = FetchType.EAGER)
	private List<User> receivedContactInvites = new ArrayList<>();

	public User() {
		super();
	}

	/**
	 * Creates a new User instance with the specified username, password, and
	 * display name.
	 *
	 * @param username    the username of the user
	 * @param password    the password of the user
	 * @param displayName the display name of the user
	 */
	public User(String username, String password, String displayName) {
		this.username = username;
		this.password = password;
		this.displayName = displayName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public List<Event> getCalendar() {
		return calendar;
	}

	/**
	 * Retrieves events from the user's calendar that will occur in the future.
	 *
	 * @return the list of future calendar events
	 */
	public List<Event> getFutureCalendar() {
		return getFutureEvents(calendar);
	}

	public void setCalendar(List<Event> calendar) {
		this.calendar = calendar;
	}

	public void addCalendarEvent(Event event) {
		calendar.add(event);
	}

	public void removeCalendarEvent(Event event) {
		calendar.remove(event);
	}

	public List<Event> getCreatedEvents() {
		return createdEvents;
	}

	/**
	 * Retrieves events from the user's created events that will occur in the
	 * future.
	 *
	 * @return the list of future calendar events
	 */
	public List<Event> getFutureCreatedEvents() {
		return getFutureEvents(createdEvents);
	}

	public void addCreatedEvent(Event createdEvent) {
		createdEvents.add(createdEvent);
	}

	public void removeCreatedEvent(Event event) {
		createdEvents.remove(event);
	}

	public List<Event> getEventInvites() {
		return eventInvites;
	}

	/**
	 * Retrieves events from the user's events invites that will occur in the
	 * future.
	 *
	 * @return the list of future calendar events
	 */
	public List<Event> getFutureEventInvites() {
		return getFutureEvents(eventInvites);
	}

	public void setEventInvites(List<Event> eventInvites) {
		this.eventInvites = eventInvites;
	}

	public void addEventInvite(Event event) {
		eventInvites.add(event);
	}

	public void removeEventInvite(Event event) {
		eventInvites.remove(event);
	}

	public List<User> getContacts() {
		return contacts;
	}

	public void setContacts(List<User> contacts) {
		this.contacts = contacts;
	}

	public void addContact(User user) {
		contacts.add(user);
	}

	public void removeContact(User user) {
		contacts.remove(user);
	}

	public List<User> getSentContactInvites() {
		return sentContactInvites;
	}

	public void setSentContactInvites(List<User> sentContactInvites) {
		this.sentContactInvites = sentContactInvites;
	}

	public void addSentContactInvite(User user) {
		sentContactInvites.add(user);
	}

	public void removeSentContactInvite(User user) {
		sentContactInvites.remove(user);
	}

	public List<User> getReceivedContactInvites() {
		return receivedContactInvites;
	}

	public void setReceivedContactInvites(List<User> receivedContactInvites) {
		this.receivedContactInvites = receivedContactInvites;
	}

	public void addReceivedContactInvite(User user) {
		receivedContactInvites.add(user);
	}

	public void removeReceivedContactInvite(User user) {
		receivedContactInvites.remove(user);
	}

	@Override
	public Object clone() {
		User cloned = new User();
		cloned.setId(id);
		cloned.setPassword(password);
		cloned.setDisplayName(displayName);
		cloned.setUsername(username);
		cloned.calendar = new ArrayList<>(calendar);
		cloned.contacts = new ArrayList<>(contacts);
		cloned.createdEvents = new ArrayList<>(createdEvents);
		cloned.eventInvites = new ArrayList<>(eventInvites);
		cloned.receivedContactInvites = new ArrayList<>(receivedContactInvites);
		cloned.sentContactInvites = new ArrayList<>(sentContactInvites);
		return cloned;
	}

	/**
	 * Checks if the given timeslot is available for scheduling based on the user's
	 * calendar and pending event invites.
	 *
	 * @param timeslot the timeslot to check availability for
	 * @return {@code true} if the timeslot is available, {@code false} otherwise
	 */
	public boolean isTimeslotAvailable(Timeslot timeslot) {
		for (Event e : getCalendar()) {
			if (e.doesEventClash(timeslot)) {
				return false;
			}
		}
		for (Event e : getEventInvites()) {
			if (e.doesEventClash(timeslot)) {
				return false;
			}
		}
		return true;
	}

	private List<Event> getFutureEvents(List<Event> events) {
		return events.stream().filter(x -> x.getStartTime().isAfter(LocalDateTime.now())).toList();
	}

	@Override
	public int hashCode() {
		return Objects.hash(calendar, contacts, createdEvents, displayName, eventInvites, id, password,
				receivedContactInvites, sentContactInvites, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return Objects.equals(calendar, other.calendar) && Objects.equals(contacts, other.contacts)
				&& Objects.equals(createdEvents, other.createdEvents) && Objects.equals(displayName, other.displayName)
				&& Objects.equals(eventInvites, other.eventInvites) && id == other.id
				&& Objects.equals(password, other.password)
				&& Objects.equals(receivedContactInvites, other.receivedContactInvites)
				&& Objects.equals(sentContactInvites, other.sentContactInvites)
				&& Objects.equals(username, other.username);
	}

}
