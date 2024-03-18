package com.fdmgroup.schedulingproject.model;

import java.time.LocalDateTime;

/**
 * Represents a time slot in the scheduling system. The Timeslot class stores
 * information about a specific time period, including its start and end times.
 * It provides methods to retrieve the start and end times, as well as a string
 * representation of the timeslot.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
public class Timeslot {

	private final LocalDateTime start;
	private final LocalDateTime end;

	/**
	 * Constructs a new Timeslot instance with the given start time and duration in
	 * minutes.
	 *
	 * @param startTime       the start time of the timeslot
	 * @param durationMinutes the duration of the timeslot in minutes
	 */
	public Timeslot(LocalDateTime startTime, int durationMinutes) {
		start = startTime;
		end = startTime.plusMinutes(durationMinutes);
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	/**
	 * Returns a string representation of the timeslot in the format "start - end".
	 * The start and end times are represented as strings using their respective
	 * {@code toString()} methods.
	 *
	 * @return a string representation of the timeslot
	 */
	@Override
	public String toString() {
		return start.toString() + " - " + end.toString();
	}
}
