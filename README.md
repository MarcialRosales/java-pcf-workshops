PCF Developers workshop
==

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Introduction](#Introduction)
- [Pivotal Cloud Foundry Technical Overview](#pivotal-cloud-foundry-technical-overview)
	- [Lab - Run Spring boot app](#run-spring-boot-app)
	- [Lab - Run web site](#run-web-site)
- [Deploying simple apps](#deploying-simple-apps)
  - [Lab - Deploy Spring boot app](#deploy-spring-boot-app)
  - [Lab - Deploy web site](#Deploy-web-site)
- [Cloud Foundry services](#cloud-foundry-services)
  - [Lab - Load flights from an in-memory database](#load-flights-from-an-in-memory-database)
  - [Lab - Load flights from a database](#load-flights-from-a-provisioned-database)  
  - [Lab - Load flights' fares from a 3rd-party application](#load-flights-fares-from-an-external-application)
  - [Lab - Load flights fares from an external application using User Provided Services](#load-flights-fares-from-an-external-application-using-user-provided-services)
  - [Lab - Let external application access a platform provided service](#let-external-application-access-a-platform-provided-service)
- [Routes and Domains](#routes-and-domains)
  - [Lab - Organizing application routes](#organizing-application-routes)
  - [Lab - Private and Public routes/domains](#private-and-public-routesdomains)
  - [Lab - Blue-Green deployment](#blue-green-deployment)
  - [Lab - Routing Services](#routing-services)
- [Build packs](buildpack-README.md)
  - [Lab - Adding functionality](buildpack-README.md#adding-functionality)
  - [Lab - Changing functionality](buildpack-README.md#changing-functionality)

<!-- /TOC -->
# Introduction

`git clone https://github.com/MarcialRosales/java-pcf-workshops.git`

# Pivotal Cloud Foundry Technical Overview

Reference documentation:
- https://docs.pivotal.io
- [Elastic Runtime concepts](http://docs.pivotal.io/pivotalcf/concepts/index.html)


## Run Spring boot app
We have a spring boot application which provides a list of available flights based on some origin and destination.

1. `git fetch` (`git branch -a` lists all the remote branches e.g `origin/load-flights-from-in-memory-db`)
2. `git checkout load-flights-from-in-memory-db`
2. `cd java-pcf-workshops/apps/flight-availability`
3. `mvn spring-boot:run`
4. `curl 'localhost:8080?origin=MAD&destination=FRA'`

We would like to make this application available to our clients. How would you do it today?

## Run web site
We also want to deploy the Maven site associated to our flight-availability application so that the team can check the latest java unit reports and/or the javadocs.

1. `git checkout load-flights-from-in-memory-db`
2. `cd java-pcf-workshops/apps/flight-availability`
3. `mvn site:run`
4. Go to your browser, and check out this url `http://localhost:8080`

We would like to make this application available only within our organization, i.e. not publicly available to our clients. How would you do it today?

# Deploying simple apps

Reference documentation:
- [Using Apps Manager](http://docs.pivotal.io/pivotalcf/1-9/console/index.html)
- [Using cf CLI](http://docs.pivotal.io/pivotalcf/1-9/cf-cli/index.html)
- [Deploying Applications](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/deploy-app.html)
- [Deploying with manifests](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/manifest.html)

## Deploy Spring boot app
Deploy flight availability and make it publicly available on a given public domain

1. `git checkout load-flights-from-in-memory-db`
2. `cd java-pcf-workshops/apps/flight-availability`
3. Build the app  
  `mvn install`
4. Deploy the app  
  `cf push flight-availability -p target/flight-availability-0.0.1-SNAPSHOT.jar --random-route`
5. Try to deploy the application using a manifest
6. Check out application's details, whats the url?  
  `cf app flight-availability`  
7. Check out the health of the application ([actuator](https://github.com/MarcialRosales/java-pcf-workshops/blob/master/apps/flight-availability/pom.xml#L37-L40)):  
  `curl <url>/health`

## Deploy web site
Deploy Maven site associated to the flight availability and make it internally available on a given private domain

1. `git checkout load-flights-from-in-memory-db`
2. `cd java-pcf-workshops/apps/flight-availability`
3. Build the site  
  `mvn site`
4. Deploy the app  
  `cf push flight-availability-site -p target/site --random-route`
5. Check out application's details, whats the url?  
  `cf app flight-availability-site`  

# Cloud Foundry services

## Load flights from an in-memory database

We want to load the flights from a relational database. We are implementing the `FlightService` interface so that we can load them from a `FlightRepository`. We need to convert `Flight` to a *JPA Entity*. We [added](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-flights-from-db/apps/flight-availability/pom.xml#L41-L49) **hsqldb** a *runtime dependency* so that we can run it locally.

1. `git checkout load-flights-from-in-memory-db`
2. `cd apps/flight-availability`
3. Run the app  
  `mvn spring-boot:run`
4. Test it  
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`

Can we deploy this application directly to PCF?

## Load flights from a provisioned database

We want to load the flights from a relational database (mysql) provisioned by the platform not an in-memory database.

1. `git checkout load-flights-from-db`
2. `cd apps/flight-availability`
3. Run the app  
  `mvn spring-boot:run`
4. Test it  
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`
5. Before we deploy our application to PCF we need to provision a mysql database. If we tried to push the application without creating the service we get:
	```
	...
	FAILED
	Could not find service flight-repository to bind to mr-fa
	```

  `cf marketplace`  Check out what services are available

  `cf marketplace -s p-mysql pre-existing-plan ...`  Check out the service details like available plans

  `cf create-service ...`   Create a service instance with the name `flight-repository`

  `cf service ...`  Check out the service instance. Is it ready to use?

6. Push the application using the manifest. See the manifest and observe we have declared a service:

  ```
  applications:
  - name: flight-availability
    instances: 1
    memory: 1024M
    path: @project.build.finalName@.@project.packaging@
    random-route: true
    services:
    - flight-repository

  ```

7. Check out the database credentials the application is using:  
  `cf env flight-availability`

8. Test the application. Whats the url?

9. We did not include any jdbc drivers with the application. How could that work?


## Load flights fares from an external application

We want to load the flights from a relational database and the prices from an external application. For the sake of this exercise, we are going to mock up the external application in cloud foundry.

1. `git checkout load-fares-from-external-app`
2. `cd apps/flight-availability` (on terminal 1)
3. Run the flight-availability app
  `mvn spring-boot:run`
4. `cd apps/fare-service` (on terminal 2)
5. Run the fare-service apps  
  `mvn spring-boot:run`
4. Test it  (on terminal 3)  
  `curl 'localhost:8080/fares/origin=MAD&destination=FRA'` shall return something like this `[{"fare":"0.016063185475725605","origin":"MAD","destination":"FRA","id":"2"}]`

Let's have a look at the `fare-service`. It is a pretty basic REST app configured with basic auth (Note: *We could have simply relied on the spring security default configuration properties*):
```
server.port: 8081

fare:
  credentials:
    user: user
    password: password

```
And it simply returns a random fare for each requested flight:
```
@RestController
public class FareController {

	private Random random = new Random(System.currentTimeMillis());

	@PostMapping("/")
	public String[] applyFares(@RequestBody Flight[] flights) {
		return Arrays.stream(flights).map(f -> Double.toString(random.nextDouble())).toArray(String[]::new);
	}
}
```

Let's have a look at how the `flight-availability` talks to the `fare-service`. First of all, the implementation of the `FareService` interface uses `RestTemplate` to call the Rest endpoint.
```
@Service
public class FareServiceImpl implements FareService {

	private final RestTemplate restTemplate;

	public FareServiceImpl(@Qualifier("fareService") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public String[] fares(Flight[] flights) {

		 return restTemplate.postForObject("/", flights, String[].class);

	}

}
```
And we build the `RestTemplate` specific for the `FareService` (within `FlightAvailabilityApplication.java`). See how we setup the RestTemplate with basic auth and the root uri for any requests to the `fare-service` endpoint:
```
@Configuration
@ConfigurationProperties(prefix = "fare-service")
class FareServiceConfig {
	String uri;
	String username;
	String password;
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Bean(name = "fareService")
	public RestTemplate fareService(RestTemplateBuilder builder, FareServiceConfig fareService) {
		return builder.basicAuthorization(getUsername(), getPassword()).rootUri(getUri()).build();
	}
}
```

And we provide the credentials for the `fare-service` in the `application.yml`:
```
fare-service:
  uri: http://localhost:8081
  username: user
  password: password

```

We tested it that it works locally. Now let's deploy to PCF. First we need to deploy `fare-service` to PCF. Then we deploy  `flight-availability` service. Do we need to make any changes? We do need to configure the credentials to our fare-service.

We have several ways to configure the credentials for the `fare-service` in `flight-availability`.

1. Set credentials in application.yml, build the flight-availability app (`mvn install`) and push it (`cf push <myapp> -f target/manifest.yml`).
	```
	fare-service:
		uri: <copy the url of the fare-service in PCF>
	```
2. Set credentials as environment variables in the manifest. Thanks to Spring boot configuration we can do something like this:
	```
	env:
	  FARE_SERVICE_URI: http://<fare-service-uri>
		FARE_SERVICE_USERNAME: user
		FARE_SERVICE_PASSWORD: password
	```
	Rather than modifying the manifest again lets simly verify that this method works. Lets simply set a wrong username via command-line:
	```
	cf set-env <myapp> FARE_SERVICE_USERNAME "bob"
	cf env <myapp> 	(dont mind the cf restage warning message)
	cf restart <myapp>
	```
	And now test it,
	`curl 'https://mr-fa-cronk-iodism.apps-dev.chdc20-cf.solera.com/fares?origin=MAD&destination=FRA'`
	should return  
	`{"timestamp":1490776955527,"status":500,"error":"Internal Server Error","exception":"org.springframework.web.client.HttpClientErrorException","message":"401 Unauthorized","path":"/fares"``


3. Inject credentials using a User Provided Service.
We are going to tackle this step in a separate lab.


## Load flights fares from an external application using User Provided Services

**Reference documentation**:
- [Spring Cloud Connectors](http://cloud.spring.io/spring-cloud-connectors/spring-cloud-connectors.html)
- [Extending Spring Cloud Connectors](http://cloud.spring.io/spring-cloud-connectors/spring-cloud-connectors.html#_extending_spring_cloud_connectors)
- [Configuring Service Connections for Spring applications in Cloud Foundry](https://docs.cloudfoundry.org/buildpacks/java/spring-service-bindings.html)


1. Create a User Provided Service which encapsulates the credentials we need to call the `fare-service`:  
 	`cf uups fare-service -p '{"uri": "https://user:password@<your-fare-service-uri>" }'`  
2. Add `fare-service` as a service to the `flight-availability` manifest.yml
	```
	  ...
		services:
		- flight-repository
		- fare-service
	```
	When we push the `flight-availability`, PCF will inject the `fare-service` credentials to the `VCAP_SERVICES` environment variable.   

3. Create a brand new project called `cloud-services` where we extend the *Spring Cloud Connectors*. This project is able to parse `VCAP_SERVICES` and extract the credentials of standard services like relational database, RabbitMQ, Redis, etc. However we can extend it so that it can parse our custom service, `fare-service`. This project can work with any cloud, not only CloudFoundry. However, given that we are working with Cloud Foundry we will add the implementation for Cloud Foundry:
	```
		<dependency>
        	<groupId>org.springframework.cloud</groupId>
        	<artifactId>spring-cloud-cloudfoundry-connector</artifactId>
        	<version>1.2.3.RELEASE</version>
    </dependency>  

	```

4. Create a *ServiceInfo* class that holds the credentials to access the `fare-service`. We are going to create a generic [WebServiceInfo](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-fares-from-external-app-with-cups/apps/cloud-services/src/main/java/io/pivotal/demo/cups/cloud/WebServiceInfo.java) class that we can use to call any other web service.  
5. Create a *ServiceInfoCreator* class that creates an instance of *ServiceInfo* and populates it with the credentials exposed in `VCAP_SERVICES`. Our generic [WebServiceInfoCreator](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-fares-from-external-app-with-cups/apps/cloud-services/src/main/java/io/pivotal/demo/cups/cloud/cf/WebServiceInfoCreator.java). We are extending a class which provides most of the implementation. However, we cannot use it as is due to some limitations with the *User Provided Services* which does not allow us to tag our services. Instead, we need to set the tag within the credentials attribute. Another implementation could be to extend from `CloudFoundryServiceInfoCreator` and rely on the name of the service starting with a prefix like "ws-" for instance "ws-fare-service".
6. Register our *ServiceInfoCreator* to the *Spring Cloud Connectors* framework by adding a file called [org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-fares-from-external-app-with-cups/apps/cloud-services/src/main/resources/META-INF/services/org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator) with this content:
	```
	io.pivotal.demo.cups.cloud.cf.WebServiceInfoCreator
	```
7. Provide 2 types of *Configuration* objects, one for *Cloud* and one for non-cloud (i.e. when running it locally). The *Cloud* one uses *Spring Cloud Connectors* to retrieve the `WebServiceInfo` object. First of all, we build a *Cloud* object and from this object we look up the *WebServiceInfo* and from it we build the *RestTemplate*.
	```
	@Configuration
	@Profile({"cloud"})
	class CloudConfig  {

		@Bean
		Cloud cloud() {
			return new CloudFactory().getCloud();
		}

	    @Bean
	    public WebServiceInfo fareServiceInfo(Cloud cloud) {
	        ServiceInfo info = cloud.getServiceInfo("fare-service");
	        if (info instanceof WebServiceInfo) {
	        	return (WebServiceInfo)info;
	        }else {
	        	throw new IllegalStateException("fare-service is not of type WebServiceInfo. Did you miss the tag attribute?");
	        }
	    }

	    @Bean(name = "fareService")
		public RestTemplate fareService(RestTemplateBuilder builder, WebServiceInfo fareService) {
			return builder.basicAuthorization(fareService.getUserName(), fareService.getPassword()).rootUri(fareService.getUri()).build();

		}
	}
	```

8. Build and push the `flight-availability` service
9. Test it `curl 'https://<my flight availability app>/fares?origin=MAD&destination=FRA'`
10. Maybe it fails ...
11. Maybe we had to declare the service like this: `cf uups fare-service -p '{"uri": "https://user:password@<your fare service uri>", "tag": "WebService" }'`  


Note in the logs the following statement: `No suitable service info creator found for service fare-service Did you forget to add a ServiceInfoCreator?`. *Spring Cloud Connectors* can go one step further and create the ultimate application's service instance rather than only the *ServiceInfo*.
We leave to the attendee to modify the application so that it does not need to build a *FareService* Bean instead it is built via the *Spring Cloud Connectors* library.

	- Create a FareServiceCreator class that extends from `AbstractServiceConnectorCreator<FareService, WebServiceInfo>`
	- Register the FareServiceCreator in the file `org.springframework.cloud.service.ServiceConnectorCreator` under the `src/main/resources/META-INF/services` folder. Put the fully qualified name of your class in the file. e.g:
		```
		com.example.web.FareServiceCreator
		```
	- We don't need now the *Cloud* configuration class because the *Spring Cloud Connectors* will automatically create an instance of *FareService*.

## Let external application access a platform provided service

Most likely, all the applications will run within the platform. However, if we ever had an external application access a service provided by the platform, say a database, there is a way to do it.

1. Create a service instance
2. Create a service key
	`cf create-service-key <serviceInstanceName> <ourServiceKeyName>`
3. Get the credentials `cf service-key <serviceInstanceName> <ourServiceKeyName>`. Share the credentials with the external application.

Creating a service-key is equivalent to binding an application to a service instance. The service broker creates a set of credentials for the application.


# Routes and Domains

**Reference documentation**:
- http://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html


## Organizing application routes

It is common to find to have a single DNS and a proxy/server listening on that address and this proxy maps paths to to applications. Say for instance we need to implement this scenario. For simplicity sake, we are going to use the same flight-availability source code but we are going to deploy it as 3 different applications like if they were in production. In business terms it is saying "brokers" should be attended by a flight-availability app tailored for brokers, whatever that mean. Likewise, "agency" path for agencies and finally, the rest of the users are served using the standard version.

<public_domain>/v1/brokers -> flight-availability-1
<public_domain>/v1/agency -> flight-availability-2

### 1st step
we create the manifest (name it `routes.yml`) for the first 2 applications like this. You can put it in the root folder of the flight-availability project.
```
---
applications:
- name: flight-availability-1
  memory: 1G
  instances: 1
  path: target/flight-availability-0.0.1-SNAPSHOT.jar
  random-route: true
  env:
    MANAGEMENT_CONTEXTPATH: /v1/broker
  services:
  - registry-service

- name: flight-availability-2
  memory: 1G
  instances: 1
  path: target/flight-availability-0.0.1-SNAPSHOT.jar
	random-route: true
	env:
		MANAGEMENT_CONTEXTPATH: /v1/agency
  services:
  - registry-service
```

> Make sure we have the spring boot actuator dependency because we will rely on the actuator endpoint to figure out which application is serving the request:
```
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2nd step
We push both applications: `cf push -f routes.yml`

### 3rd step
Although not absolutely necessary but we can create subdomain like this to further organize the routes: `cf create-domain <org> mr.apps-dev.chdc20-cf.solera.com`.

### 4th step
Create first route : `cf create-route development mr.apps-dev.chdc20-cf.solera.com --path v1/brokers `. This step is only necessary if we want to reserve the route. Say we don't have the applications ready but we want to reserve their routes.

### 5th step
Check the routes: `cf routes` returns at least this line:
  `development                                     mr.apps-dev.chdc20-cf.solera.com          /v1/brokers`

Map the route to flight-availability-1 :  `cf map-route flight-availability-1 mr.apps-dev.chdc20-cf.solera.com --path v1/brokers`

Test it: `curl https://mr.apps-dev.chdc20-cf.solera.com/v1/brokers/env | jq .vcap | grep vcap.application.name ` shall return `"vcap.application.name": "flight-availability-1"`

> Note: The applications, in this case flight-availability-1, receives the full url, i.e. `/v1/brokers/*`.

### 6th step
Create 2nd route: `cf map-route flight-availability-2 mr.apps-dev.chdc20-cf.solera.com --path v1/agency `

Test it: `curl https://mr.apps-dev.chdc20-cf.solera.com/v1/agency/env | jq .vcap | grep vcap.application.name ` shall return `"vcap.application.name": "flight-availability-2"`


As you can see it is a pretty basic proxy with limited capability to do any fancy stuff like url rewrites and/or define routes per operation (e.g. GET goes to route /x and POST to route /y). But at least, with this lab we get an idea of the kind of things we can build.


## Private and Public routes/domains (internal vs external facing applications)

What domains exists in our organization? try `cf domains`.  Anyone is private? and what does it mean private?  Private domain is that domain which is registered with the internal IP address of the load balancer. And additionally, this private domain is not registered with any public  DNS name. In other words, there wont be any DNS server able to resolve the private domain.

The lab consists in leveraging private domains so that only internal applications are accessible within the platform. Lets use the `fare-service` as an internal application.

There are various ways to implement this lab. One way is to actually declare the private domain in the application's manifest and redeploy it. Another way is to play directly with the route commands (`create-route`, and `delete-route`, `map-route`, or `unmap-route`).


## Blue-Green deployment

Use the demo application to demonstrate how we can do blue-green deployments using what you have learnt so far with regards routes.

How would you do it? Say Blue is the current version which is running and green is the new version.

Key command: `cf map-route`


## Routing Services (intercept every request to decide whether to accept it or enrich it or track it)

**Reference documentation**:
- https://docs.pivotal.io/pivotalcf/services/route-services.html
- https://docs.pivotal.io/pivotalcf/devguide/services/route-binding.html

The purpose of the lab is to take any application and add a proxy layer that only accepts requests which carry a JWT Token else it fails with it a `401 Unauthorized`.
*Reminder: Routing service is a mechanism that allows us to filter requests never to alter the original endpoint. We can reject the request or pass it on as it is or modified, e.g adding extra headers.*

### Create the Proxy (or router service)
**Lab**: The code is already provided in the `routes` branch however we are going to walk thru the code below:

1. Create a Spring Boot application with a `web` dependency
  ```xml
    <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
  ```
3. Create a @Controller class :
	```java
	@RestController
	class RouteService {

		static final String FORWARDED_URL = "X-CF-Forwarded-Url";

		static final String PROXY_METADATA = "X-CF-Proxy-Metadata";

		static final String PROXY_SIGNATURE = "X-CF-Proxy-Signature";

		private final Logger logger = LoggerFactory.getLogger(this.getClass());

		private final RestOperations restOperations;

		@Autowired
		RouteService(RestOperations restOperations) {
			this.restOperations = restOperations;
		}

	}
	```
2. Add a single request handler that receives all requests:
  ```java
	@RequestMapping(headers = { FORWARDED_URL, PROXY_METADATA, PROXY_SIGNATURE })
	ResponseEntity<?> service(RequestEntity<byte[]> incoming, @RequestHeader(name = "Authorization", required = false) String jwtToken ) {
		if (jwtToken == null) {
			this.logger.error("Incoming Request missing JWT Token: {}", incoming);
			return badRequest();
		}else if (!isValid(jwtToken)) {
			this.logger.error("Incoming Request missing or not valid JWT Token: {}", incoming);
			return notAuthorized();
		}

		RequestEntity<?> outgoing = getOutgoingRequest(incoming);
		this.logger.debug("Outgoing Request: {}", outgoing);


		return this.restOperations.exchange(outgoing,  byte[].class);
	}

  }
  ```
3. Validate JWT token header by simply checking that it starts with "Bearer". If it is not valid and/or it is missing, log it as an error.
	```java
	private static boolean isValid(String jwtToken) {
		return jwtToken.contains("Bearer"); // TODO add JWT Validation
	}
	```
4. Forward request to the uri in `X-CF-Forwarded-Url` along with the other 2 headers `X-CF-Proxy-Metadata` and `X-CF-Proxy-Signature`. We remove the `Authorization` header as it is longer needed:
	```java
	private static RequestEntity<?> getOutgoingRequest(RequestEntity<?> incoming) {
		HttpHeaders headers = new HttpHeaders();
		headers.putAll(incoming.getHeaders());

		URI uri = headers.remove(FORWARDED_URL).stream().findFirst().map(URI::create)
				.orElseThrow(() -> new IllegalStateException(String.format("No %s header present", FORWARDED_URL)));
		headers.remove("Authorization");

		return new RequestEntity<>(incoming.getBody(), headers, incoming.getMethod(), uri);
	}
	```
5. Build the app `mvn install`

### Test the proxy locally

To test it locally we proceed as follow:
1. Run the previous flight-availability app (assume that it is running on 8080)
3. Run this route-service app on port 8888 (or any other that you prefer) on a separate terminal : `mvn spring-boot:run -Dserver.port=8888`
7. Simulate request coming from a client via **CF Router** for url `http://localhost:8080` without any JWT token:
  ```
   curl -v -H "X-CF-Forwarded-Url: http://localhost:8080/" -H "X-CF-Proxy-Metadata: some" -H "X-CF-Proxy-Signature: signature "  localhost:8888/
  ```
  We should get a 400 Bad Request
8. Simulate request coming from a client via **CF Router** for url `http://localhost:8080` with invalid JWT token:
  ```
   curl -v -H "X-CF-Forwarded-Url: http://localhost:8080" -H "X-CF-Proxy-Metadata: some" -H "X-CF-Proxy-Signature: signature " -H "Authorization: hello" localhost:8888/
  ```
  We should get a 401 Unauthorized
9. Simulate request coming from a client via **CF Router** for url `http://localhost:8080` with valid JWT token:
  ```
   curl -v -H "X-CF-Forwarded-Url: http://localhost:8080" -H "X-CF-Proxy-Metadata: some" -H "X-CF-Proxy-Signature: signature " -H "Authorization: Bearer hello" localhost:8888/
  ```
  We should get a 200 OK and the body `hello`

### Test the proxy in Cloud Foundry using Router Service functionality
Let's deploy it to Cloud Foundry.

1. `cf push -f target/manifest.yml`
2. Create a user provided service that points to the url of our deployed `route-service`.
  ```
  cf cups route-service -r https://<route-service url>
  ```
3. Deploy the flight-availability app if it is not already deployed:

4. Configure Cloud Foundry to intercept all requests for `flight-availability` with the router service `route-service`:
  ```
  cf bind-route-service <application_domain> route-service --hostname <app_hostname>
  ```
  If you are not sure about the application_domain or app_hostname run: `cf app flight-availability | grep urls`. It will be <app_hostname>.<application_domain>    
5. Check that `flight-availability` is bound to `route-service`: `cf routes`
  ```
  space         host                                 domain                          port   path   type   apps            		service
  development   route-service-circulable-mistletoe   apps-dev.chdc20-cf.xxxxxx.com                        route-service
  development   app1-sliceable-jerbil                apps-dev.chdc20-cf.xxxxxx.com                        flight-availability route-service
  ```
5. Run in a terminal `cf logs route-service` to watch its logs
6. Try a url which has no JWT token:
  ```
  curl -v https://<app_hostname>.<application_domain>
  ```
  We should get back a 400 Bad Request
7. Try a url which has an invalid JWT token:
```
curl -v -H "Authorization: hello" https://<app_hostname>.<application_domain>
```
We should get back a 401 Unauthorized
8. Finally, try a url which has a valid JWT Token:
```
curl -v -H "Authorization: Bearer hello" https://<app_hostname>.<application_domain>
```
We should get back a 200 OK and the outcome from the `/` endpoint which is `hello`.
