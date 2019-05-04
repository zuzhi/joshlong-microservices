# joshlong-microservices

**A microservices demo project**

This is a demo application, which demonstrates [Microservices Architecture Pattern](https://martinfowler.com/microservices/) using Spring Boot and Spring Cloud.

## Infrastructure services

### Config service (config-server)

`application.yml`

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: ${HOME}/Desktop/config
server:
  port: 8888
```

- `git.uri`: Specify the path to the config files using a file-based git repository(`zuzhi/joshlong-microservices-config.git`).

#### zuzhi/joshlong-microservices-config.git

https://github.com/zuzhi/joshlong-microservices-config

```properties
# application.properties
```

```properties
# eureka-service.properties
server.port=8761
```

```properties
# reservation-service.properties
server.port=${PORT:8000}
message=Hello World
spring.cloud.stream.bindings.input.destination=reservations
```

```properties
# reservation-client.properties
server.port=9999
spring.cloud.stream.bindings.output.destination=reservations
```

```properties
# hystrix-dashboard.properties
server.port=8010
```

### Service discovery (eureka-service)

`bootstrap.yml`

```yaml
spring:
  application:
    name: eureka-service
  cloud:
    config:
      uri: localhost:8888
      fail-fast: true
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

- `fail-fast`: It means that Spring Boot application will fail startup immediately, if it cannot connect to the Config Service.
- `register-with-eureka`: If we make this property as true then while the server starts the inbuilt client will try to register itself with the Eureka server.
- `fetch-registry`: The inbuilt client will try to fetch the Eureka registry if we configure this property as true.

### Monitor dashboard (hystrix-dashboard)

`bootstrap.yml`

```yaml
spring:
  application:
    name: hystrix-dashboard
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
```

`http://localhost:9999/actuator/hystrix.stream`

### Distributed tracing (zipkin-server)

- [Spring Cloud Sleuth](https://cloud.spring.io/spring-cloud-sleuth/)
- Using docker to boot up a zipkin server

### API Gateway (reservation-client)

`bootstrap.yml`

```yaml
spring:
  application:
    name: reservation-client
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    sender:
      type: web
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    hystrix:
      stream:
        enabled: true
```


## Functional services

### reservation-service

- `config-server` need to be running.
- A `rabbitmq` instance is needed.
- A `zipkin` instance is needed.

`bootstrap.yml`

```yaml
spring:
  application:
    name: reservation-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    sender:
      type: web
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    env:
      enabled: true
    refresh:
      enabled: true
```

## HOWTO

### How to run all the things?

#### Config Server

check health

http://localhost:8888/actuator/health

check config

http://localhost:8888/reservation-service/master


#### Eureka Service

http://localhost:8761/


#### Reservation Service

http://localhost:8000/reservations


#### Reservation Client

proxy

GET http://localhost:9999/reservation-service/reservations

service to service call

http://localhost:9999/reservations/names

message queue

POST http://localhost:9999/reservations


#### Hystrix Dashboard

http://localhost:8010/hystrix

stream

http://localhost:9999/actuator/hystrix.stream


#### Zipkin Server

http://localhost:9411


#### RabbitMQ Mangement UI

Enable `rabbitmq_management` plugin

```bash
# connect to docker
$ docker exec -it rabbitmq /bin/bash

# enable rabbitmq_management plugin
$ rabbitmq-plugins enable rabbitmq_management
```

Expose port 15672

Go to http://localhost:15672


## TODO

### More on config


### More on Infrastructure services

- Auth service
- Log analysis
- Load balancer, Circuit breaker and Http client
	- Ribbon
	- Feign
	- Turbine

### Infrastructure automation
