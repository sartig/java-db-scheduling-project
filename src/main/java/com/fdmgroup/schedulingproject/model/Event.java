package com.fdmgroup.schedulingproject.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Transient;

import jakarta.persistence.*;

/**
 * Represents an event in the scheduling system. The Event class stores
 * information about an event, including its title, description, location, start
 * time, end time, duration, organizer, attendees, and invitees. It provides
 * methods for managing event details and checking for scheduling conflicts.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
@Entity
public class Event {
	@Id
	@SequenceGenerator(name = "EVENT_SEQ_GEN", sequenceName = "event_seq")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EVENT_SEQ_GEN")
	private long id;
	private String title;
	private String description;
	private String location;
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime startTime;
	@Transient
	private LocalDateTime endTime;
	private int durationMinutes = 30;
	@ManyToOne
	@JoinColumn(name = "organiser_id")
	private User organiser;
	@ManyToMany
	@JoinTable(name = "event_attendees", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private List<User> attendees = new ArrayList<>();
	@ManyToMany
	@JoinTable(name = "event_invitees", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private List<User> invitees = new ArrayList<>();

	public static int minIntervalMinutes = 15;

	public Event() {
		super();
	}

	/**
	 * Creates a new Event instance with the specified title, description, location,
	 * start time, and duration.
	 * 
	 * @param title           the title of the event
	 * @param description     the description of the event
	 * @param location        the location of the event
	 * @param startTime       the start time of the event
	 * @param durationMinutes the duration of the event in minutes
	 */
	public Event(String title, String description, String location, LocalDateTime startTime, int durationMinutes) {
		this.title = title;
		this.description = description;
		this.location = location;
		this.startTime = startTime;
		this.durationMinutes = durationMinutes;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return getStartTime().plusMinutes(getDurationMinutes());
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(int durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public User getOrganiser() {
		return organiser;
	}

	public void setOrganiser(User organiser) {
		this.organiser = organiser;
	}

	public void addAttendee(User attendee) {
		attendees.add(attendee);
	}

	public void removeAttendee(User attendee) {
		attendees.remove(attendee);
	}

	public List<User> getAttendees() {
		return attendees;
	}

	public void addInvitee(User invitee) {
		invitees.add(invitee);
	}

	public void removeInvitee(User invitee) {
		invitees.remove(invitee);
	}

	public List<User> getInvitees() {
		return invitees;
	}

	/**
	 * Checks if this event clashes with another event given their start time and
	 * duration.
	 *
	 * @param compareStartTime       the start time of the other event to compare
	 * @param compareDurationMinutes the duration of the other event to compare in
	 *                               minutes
	 * @return {@code true} if there is a clash, {@code false} otherwise
	 */
	public boolean doesEventClash(LocalDateTime compareStartTime, int compareDurationMinutes) {
		LocalDateTime thisEndTime = getStartTime().plusMinutes(getDurationMinutes());
		LocalDateTime compareEndTime = compareStartTime.plusMinutes(compareDurationMinutes);
		return (((compareStartTime.isBefore(getStartTime()) || compareStartTime.isEqual(getStartTime()))
				&& compareEndTime.isAfter(getStartTime()))
				|| (compareStartTime.isBefore(thisEndTime)
						&& (compareEndTime.isAfter(thisEndTime) || compareEndTime.isEqual(thisEndTime)))
				|| (compareStartTime.isEqual(getStartTime()) || compareStartTime.isAfter(getStartTime()))
						&& (compareEndTime.isEqual(thisEndTime) || compareEndTime.isBefore(thisEndTime)));
	}

	/**
	 * Checks if this event clashes with the given timeslot.
	 *
	 * @param timeslot the timeslot to compare for clash
	 * @return {@code true} if there is a clash, {@code false} otherwise
	 */
	public boolean doesEventClash(Timeslot timeslot) {
		LocalDateTime eventStartTime = timeslot.getStart();
		int durationMinutes = (int) Math.abs(Duration.between(eventStartTime, timeslot.getEnd()).toMinutes());
		return doesEventClash(eventStartTime, durationMinutes);
	}
}
