package io.zuzhi.reservationservice;

import java.util.Collection;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableDiscoveryClient
@IntegrationComponentScan
@EnableBinding(Sink.class)
@SpringBootApplication
public class ReservationServiceApplication {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Bean
  CommandLineRunner commandLineRunner(ReservationRepository reservationRepository) {
    return args -> Stream.of("Josh", "Pieter", "Tasha", "Eric", "Susie", "Max", "Chris")
        .forEach(n -> {
          Reservation r = new Reservation(n);
          reservationRepository.save(r);
          logger.info(r.toString());
        });
  }

  public static void main(String[] args) {
    SpringApplication.run(ReservationServiceApplication.class, args);
  }

}

@MessageEndpoint
class ReservationProcessor {

  private ReservationRepository reservationRepository;

  public ReservationProcessor(ReservationRepository reservationRepository) {
    this.reservationRepository = reservationRepository;
  }

  @ServiceActivator(inputChannel = Sink.INPUT)
  public void acceptNewReservations(String rn) {
    this.reservationRepository.save(new Reservation(rn));
  }
}

@RefreshScope
@RestController
class MessageRestController {

  private final String value;

  @Autowired
  public MessageRestController(@Value("${message}") String value) {
    this.value = value;
  }

  @RequestMapping("/message")
  String read() {
    return this.value;
  }
}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

  /**
   * Search "by-name", `/reservations/search/by-name?rn=Chris`
   *
   * @param rn the reservation name
   * @return reservation info list
   */
  @RestResource(path = "by-name")
  Collection<Reservation> findByReservationName(@Param("rn") String rn);
}

@Entity
@Data
@NoArgsConstructor
class Reservation {

  @Id
  @GeneratedValue
  private Long id;
  private String reservationName;

  public Reservation(String reservationName) {
    this.reservationName = reservationName;
  }
}
