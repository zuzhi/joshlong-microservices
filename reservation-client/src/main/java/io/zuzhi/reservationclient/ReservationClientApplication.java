package io.zuzhi.reservationclient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@EnableCircuitBreaker
@EnableBinding(Source.class)
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReservationClientApplication.class, args);
  }

}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Bean
  private RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Autowired
  private RestTemplate restTemplate;

  public Collection<String> getReservationNamesFallback() {
    return new ArrayList<>();
  }

  @Autowired
  private Source source;

  @RequestMapping(method = RequestMethod.POST)
  public void writeReservation(@RequestBody Reservation r) {
    Message<String> msg = MessageBuilder.withPayload(r.getReservationName()).build();
    this.source.output().send(msg);
  }

  @HystrixCommand(fallbackMethod = "getReservationNamesFallback")
  @RequestMapping(method = RequestMethod.GET, value = "/names")
  public Collection<String> getReservationNames() {

    ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
    };

    ResponseEntity<Resources<Reservation>> entity = this.restTemplate
        .exchange("http://localhost:8000/reservations", HttpMethod.GET, null, ptr);
        // .exchange("http://reservation-service/reservations", HttpMethod.GET, null, ptr);

    return entity
        .getBody()
        .getContent()
        .stream()
        .map(Reservation::getReservationName)
        .collect(Collectors.toList());
  }
}


class Reservation {

  private String reservationName;

  public String getReservationName() {
    return reservationName;
  }
}
