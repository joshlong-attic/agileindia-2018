package com.example.reservationservice;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public class ReservationTest {

	@Test
	public void create() throws Exception {
		Reservation reservation = new Reservation(1L, "Foo");
		Assert.assertEquals(reservation.getReservationName(), "Foo");
		Assert.assertThat(reservation.getReservationName(), Matchers.is("Foo"));
		Assertions.assertThat(reservation.getReservationName()).isEqualToIgnoringCase("Foo");
	}
}
