package com.fdmgroup.schedulingproject.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeslotTimeComparatorTest {

    private TimeslotTimeComparator comparator;

	@BeforeEach
	void setUp() {
		comparator = new TimeslotTimeComparator();
	}

	@Test
	@DisplayName("Timeslot comparator sorts two events correctly")
	void testComparator() {
		LocalDateTime now = LocalDateTime.now();
        Timeslot timeslot1 = new Timeslot(now, 60);
        Timeslot timeslot2 = new Timeslot(now.plusMinutes(10), 60);
        Timeslot timeslot3 = new Timeslot(now, 60);
        Timeslot timeslot4 = new Timeslot(now, 10);
		assertEquals(-1, comparator.compare(timeslot1, timeslot2));
		assertEquals(0, comparator.compare(timeslot1, timeslot3));
		assertEquals(1, comparator.compare(timeslot2, timeslot3));
		assertEquals(-1, comparator.compare(timeslot4, timeslot1));
	}
}
