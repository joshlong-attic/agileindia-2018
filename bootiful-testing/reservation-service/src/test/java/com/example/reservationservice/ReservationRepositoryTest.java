package com.example.reservationservice;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@DataJpaTest
@RunWith(SpringRunner.class)
public class ReservationRepositoryTest {

	@Autowired
	private TestEntityManager testEntityManager;

	@Autowired
	private ReservationRepository reservationRepository;

	@Test
	public void findByReservationNmae() throws Exception {
		Reservation saved = this.testEntityManager
				.persistFlushFind(new Reservation(null, "John"));
		Long id = this.reservationRepository.findByReservationName("John").iterator().next().getId();
		Assertions.assertThat(id).isEqualTo(saved.getId());
	}
}
