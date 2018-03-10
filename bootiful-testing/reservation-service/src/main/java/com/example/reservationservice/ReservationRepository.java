package com.example.reservationservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {


	Collection<Reservation> findByReservationName(String rn);
}
