Distributed tracing
===

Centralized log aggregation plus correlation id is fundamental: http://cloud.spring.io/spring-cloud-sleuth/spring-cloud-sleuth.html#_only_sleuth_log_correlation
here we demonstrate it via cf logs and also using kibana in Solera if available.


https://github.com/spring-cloud/spring-cloud-sleuth/tree/master/spring-cloud-sleuth-samples

# Log aggregation and correlation id out of the box on edge services in PCF

1. checkout the branch `add-distributed-tracing`
2. add some logging statements to flight-availability's `FlightAvailabilityController`:
	```java
		@RequestMapping("/")
		Collection<Flight> search(@RequestParam String origin, @RequestParam String destination) {
			if (origin == null) {
				throw new IllegalArgumentException("missing origin");
			}
			if (destination == null) {
				throw new IllegalArgumentException("missing destination");
			}
			log.info("Searching flights for {}/{}", origin, destination);

			return flightService.find(origin, destination);
		}
	```
3. add some logging statements to fare-service's `FareController`:
	```java
	@PostMapping("/")
	public String[] applyFares(@RequestBody Flight[] flights) {
		log.info("Calculating fares for {} flights", flights.length);
		return Arrays.stream(flights).map(f -> Double.toString(random.nextDouble())).toArray(String[]::new);
	}
	```

4. restart flight-availability and fare-service
5. test logging statement by running the request `curl 'localhost:8080?origin=MAD&destination=FRA'` and checking the standard output:
```
2017-05-11 18:42:55.171  INFO 26718 --- [nio-8080-exec-1] c.e.web.FlightAvailabilityController     : Searching flights for MAD/FRA
```


# Distributed tracing and log correlation


1. add sleuth dependency to our applications: `flight-availability`, `fare-service`
```xml
	<properties>
		...
		<spring-cloud.version>Dalston.RELEASE</spring-cloud.version>
	</properties>
	...
	<dependencies>
	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-sleuth</artifactId>
		</dependency>
	...
	</dependencies>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
	....
```
2. make sure both applications have `spring.application.name` property configured
3. call the flight-availability : `curl 'localhost:8080?origin=MAD&destination=FRA'` shall produce these log statements. See that *trace-id* `24b1abab1aa3c28c` is common across both logs:
*flight-availability* logs:
```
2017-05-11 19:01:51.711  INFO [flight-availability,24b1abab1aa3c28c,24b1abab1aa3c28c,false] 26794 --- [nio-8080-exec-5] c.e.web.FlightAvailabilityController     : Searching fared flights for MAD/FRA
```
*fare-service* logs:
```
2017-05-11 19:01:51.869  INFO [fare-service,24b1abab1aa3c28c,3bccd9836a0287e3,false] 26930 --- [nio-8081-exec-1] com.example.web.FareController           : Calculating fares for 1 flights
```
4. we need log aggregation not only across all instances of a given application or service but also across all services that make up our solution/architecture. Something like ELK or Spunk. This is outside of the scope of this workshop unless PCF is draining logs to ELK. Future versions of this workshop might provision a local ELK stack.

# Distributed tracing with Zipkin

Zipkin is a server that receives tracing information (i.e. spans and trace-ids)  from multiple applications and it is also UI capable of displaying complex distributed call chains.

## Why do we need Zipkin if we have correlated and aggregated logging? what value does it add?
- It makes it easy to understand the call chains. In a complex call chain where lots of services are involved, looking at the logs may not be as intuitive as looking at user interface that properly displays them. So, one value is to help us visualize the dependencies and the call chain.
- It also facilitates latency analysis because Zipkin automatically calculates the overall latency and the latency breakdown.
- And finally, logging tends to be more verbose and hence it requires more network bandwidth to distribute it. However, be careful with the number of spans we want to capture with Sleuth/Zipkin because that could also have a negative impact on network bandwidth.

It is important to notice the difference between distributed logging and distributed tracing. With distributed logging our applications produce as many logging statements as we wish. If we aggregated the logs from our 2 applications we can infer from the timestamp the chronology and what happened on each stage.  
*aggregated logs for flight-availability and fare-service* logs:
```
2017-05-11 19:01:51.711  INFO [flight-availability,24b1abab1aa3c28c,24b1abab1aa3c28c,false] 26794 --- [nio-8080-exec-5] c.e.web.FlightAvailabilityController     : Searching fared flights for MAD/FRA
2017-05-11 19:01:51.869  INFO [fare-service,24b1abab1aa3c28c,3bccd9836a0287e3,false] 26930 --- [nio-8081-exec-1] com.example.web.FareController           : Calculating fares for 1 flights
```

With distributed tracing and in particular with Zipkin, the application only sends *spans* not logging statements. As we already know, *span* is a unit of work within a transaction or request, in simple words, it is a snapshot/timestamp taken from a call chain. See the diagram below of the `/fare` request:
![Distributed transaction](assets/zipkin-6.png)

From left to right:
- a request comes without any tracing information.
- flight-availability receives the request and creates a trace-id (`X`) and span (`A`) and annotate the span with `serverReceived` timestamp. Zipkin/Sleuth creates a span for each incoming and/or outgoing request. It does not create, at least by default, spans within an application logic only at the edges.
- flight-availability sends a request out to the fare-service carrying the same trace-id. As we said earlier, Zipkin/Sleuth creates a brand new span (`B`) for that outgoing request and annotates it with `clientSent` timestamp.
- unlike flight-availability, fare-service receives a request which carries a trace-id (`X`). fare-service creates a span (`C`) for the incoming request and annotates with `serverReceived` timestamp.
- fare-service replies and completes the request and annotates the current span (`C`) with `serverSent` timestamp
- flight-availability receives the response and annotates the current span (`B`) with `clientReceived` timestamp
- flight-availability replies and completes the request and annotates the current span (`A`) with `serverSent` timestamp

As we can see we have 3 spans, `A`, `B` and `C`. However, Zipkin collapses `B` and `C` into a single span which combines `clientSent` from flight-availability, `serverReceived` from fare-service, `serverSent` from fare-service and `clientReceived` from flight-availability. So the total number of spans are 2.

## Sampling

In a high volume distributed system we have to be cautious with the amount instrumentation data we generate, distributed tracing is not an exception. It is for this reason that Zipkin will trace some of the requests but not all. We can control which percentage we want to trace by using this property `spring.sleuth.sampler.percentage`. To capture all the requests we set it to `1` which is not the default value.

If we use logging in addition to Zipkin to trace requests, the logging statements can tells us whether the current request was sent to Zipkin by looking at the boolean value. For instance below was not sent to Zipkin.
`2017-05-11 19:01:51.869  INFO [fare-service,24b1abab1aa3c28c,3bccd9836a0287e3,false] 26930 --- [nio-8081-exec-1] com.example.web.FareController           : Calculating fares for 1 flights`

## How and when does Sleuth create spans

Sleuth adds a *HTTP Filter* that intercepts all incoming requests. When request arrives it creates a span and closes it when we send back a http response.
It also supports async rest controller, i.e.  *Callable* or *WebAsyncTask*.

Sleuth also adds a *RestTemplate* interceptor that ensure all the tracing information is propagated in the outgoing http requests. A span is created when we make a call and it is closed when we receive a response.

Sleuth also supports *Feign*, *Hystrix*, *Zuul* and *Spring Integration*.

## Bootstrap a Zipkin server

This workshop assumes we dont have a Zipkin server already running in our infrastructure otherwise we would point our applications to it.

First we are going to create our Zipkin server as a Spring boot application with these dependencies:
```xml
	...
	<dependencies>
		<dependency>
			<groupId>io.zipkin.java</groupId>
			<artifactId>zipkin-autoconfigure-ui</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
      <groupId>io.zipkin.java</groupId>
      <artifactId>zipkin-server</artifactId>
  	</dependency>
	</dependencies>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```
We configure it to run on port 9411 :
```
server.port: 9411
```

We need to turn this spring boot app into ZipKin server.
```java
@SpringBootApplication
@EnableZipkinServer // added this annotation
public class ZipkinServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZipkinServerApplication.class, args);
	}
}
```

Finally we launch it : `mvn spring-boot:run` and check the dashboard on `http://localhost:9411`.

## Configure applications to forward tracing to Zipkin

So far we only have ZipKin running, we need to configure our applications to feed the tracing information to Zipkin. Lets add Zipkin client to our applications.

1. Add this dependency to flight-availability and fare-service:
```xml
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
```
2. Restart both applications


## Trace HTTP distributed call chain of a successful request

1. Send a request that calls 2 services: flight-availability and fare-service: `curl 'localhost:8080/fares?origin=MAD&destination=FRA'`  

2. In zipkin dashboard, select the service `flight-availability`, select `all` as the type of request, enter the appropriate time frame and hit the button `Find Traces`.
	![Zipkin Trace](assets/zipkin-2.png)

 	Our request took 437msec out of which 298msec was spent under the fare-service.
	![Zipkin Trace](assets/zipkin-0.png)

## Trace HTTP distributed call chains of an unsuccessful request

1. Shut down fare-service

2. Send the request: `curl 'localhost:8080/fares?origin=MAD&destination=FRA'`  

3. Zipkin visualizes failed requests with red color. In the search view we can quickly identify which requests failed, see below:
	![Zipkin Trace](assets/zipkin-3.png)

	To find out what exactly happened, we click on the transaction on red font. We can see that the span with darker red color was the origin of the failure.
	![Zipkin Trace](assets/zipkin-4.png)

	To find out the exactly happened we click on that span which opens a dialog box with all span details which include the failure reason: Connection Refused.
	![Zipkin Trace](assets/zipkin-5.png)


## Trace HTTP requests via Hystrix
TODO

## Trace message interaction
TODO

## Discover dependencies

Zipkin is also useful to detect all the dependencies and their direction based on the tracing information sent from our application. In the Zipking dashboard click on the link `Dependencies` both on the top bar.
![Zipkin Dependencies](assets/zipkin-1.png)
