package com.example.reservationclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureJsonTesters
@AutoConfigureStubRunner(ids = "com.example:reservation-service:+:8080",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
//@AutoConfigureWireMock(port = 8080)
public class ReservationClientApplicationTests {

//	@Autowired
//	private ObjectMapper objectMapper;

	@Autowired
	private ReservationClient client;

	@Test
	public void contextLoads() throws Exception {
	/*	String json = this.objectMapper.writeValueAsString(
				Arrays.asList(new Reservation(1L, "Jane"), new Reservation(2L, "John")));

		WireMock.stubFor(
				WireMock.get("/reservations")
						.willReturn(WireMock.aResponse()
								.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
								.withStatus(HttpStatus.OK.value())
								.withBody(json)));
	*/

		Collection<Reservation> res = this.client.getAllReservations();
		Assertions.assertThat(res).contains(new Reservation(1L, "Jane"), new Reservation(2L, "John"));
	}

}
