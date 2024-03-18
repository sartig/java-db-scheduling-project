package com.fdmgroup.schedulingproject.model;

import java.util.Comparator;

/**
 * Compares two Timeslot objects based on their start and end times. The
 * TimeslotTimeComparator class implements the Comparator interface, allowing
 * Timeslot objects to be compared and sorted based on their times.
 * 
 * @author Sam Artigolle
 * @version 1.0
 */
public class TimeslotTimeComparator implements Comparator<Timeslot> {

	/**
	 * Compares two Timeslot objects based on their start and end times.
	 *
	 * @param t1 the first Timeslot to compare
	 * @param t2 the second Timeslot to compare
	 * @return a negative integer if t1 comes before t2, a positive integer if t1
	 *         comes after t2, or zero if t1 and t2 have the same start and end
	 *         times
	 */
	public int compare(Timeslot t1, Timeslot t2) {
		if (t1.getStart().isAfter(t2.getStart())) {
			return 1;
		}
		if (t1.getStart().isBefore(t2.getStart())) {
			return -1;
		}
		if (t1.getEnd().isBefore(t2.getEnd())) {
			return -1;
		} else if (t1.getEnd().isAfter(t2.getEnd())) {
			return 1;
		}
		return 0;
	}
}
