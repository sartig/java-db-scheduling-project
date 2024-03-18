package com.fdmgroup.schedulingproject.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeslotTest {

	private Timeslot timeslot;

	@Test
	@DisplayName("Timeslot(LocalDateTime, int) constructor sets end time properly")
	void testConstructor() {
		LocalDateTime now = LocalDateTime.now();
		timeslot = new Timeslot(now, 60);
		assertEquals(now.plusHours(1), timeslot.getEnd());
	}

	@Test
	@DisplayName("toString() override is formatted correctly")
	void testToString() {
		LocalDateTime time = LocalDateTime.of(2020, 5, 4, 3, 20);
		timeslot = new Timeslot(time, 20);
		String expected = time + " - " + LocalDateTime.of(2020, 5, 4, 3, 40);
		assertEquals(timeslot.toString(), expected);
	}
}
