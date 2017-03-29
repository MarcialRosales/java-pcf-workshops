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
	- [Lab - Load flights fares from an external application using User Provided Services](load-flights-fares-from-an-external-application-using-user-provided-services)


<!-- /TOC -->
# Introduction

`git clone https://github.com/MarcialRosales/java-pcf-workshops.git`

# Pivotal Cloud Foundry Technical Overview

Reference documentation:
- [Elastic Runtime concepts](http://docs.pivotal.io/pivotalcf/1-9/concepts/index.html)


## Run Spring boot app
We have a spring boot application which provides a list of available flights based on some origin and destination.

You can use the existing code or
1. `git checkout master`
2. `cd java-pcf-workshops/apps/flight-availability`
3. `mvn spring-boot:run`
4. `curl 'localhost:8080?origin=MAD&destination=FRA'`

We would like to make this application available to our clients. How would you do it today?

## Run web site
We also want to deploy the Maven site associated to our flight-availability application so that the team can check the latest java unit reports and/or the javadocs.

1. `git checkout master`
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

1. `git checkout master`
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

1. `git checkout master`
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
3. Run the app  
  `mvn spring-boot:run`
4. `cd apps/fare-service` (on terminal 2)
5. Run the app  
  `mvn spring-boot:run`
4. Test it  (on terminal 3)
  `curl 'localhost:8080/fares/origin=MAD&destination=FRA'` shall return something like this `[{"fare":"0.016063185475725605","origin":"MAD","destination":"FRA","id":"2"}]`

Let's have a look at the `fare-service`. It is a pretty basic REST app configured with basic auth:
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

Let's have a look at how the `flight-availability` talks to the `fare-service`. First of all, the implementation of the `FareService` interface uses `RestTemplate` to call the Rest endpoint:
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
And we build the `RestTemplate` specific for the `FareService` (within `FlightAvailabilityApplication.java`):
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

And we provide the configuration properties in the `application.yml`:
```
fare-service:
  uri: http://localhost:8081
  username: user
  password: password

```

We tested it that it works locally. Now let's deploy to PCF. First we need to deploy `fare-service` to PCF. Then we deploy  `flight-availability` service. Do we need to make any changes? We do need to configure the credentials to our fare-service.

We have several ways to configure the credentials for the `fare-service` in `flight-availability`.

1. Set credentials in application.yml, build app (`mvn install`) and push it (`cf push <myapp> -f target/manifest.yml`).
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
	However, we are going to simply test that it works by setting thru command-line a wrong username:
	```
	cf set-env <myapp> FARE_SERVICE_USERNAME "bob"
	cf env <myapp> 	(dont mind the cf restage warning message)
	cf restart <myapp>
	```
	And now test it,
	`curl 'https://mr-fa-cronk-iodism.apps-dev.chdc20-cf.solera.com/fares?origin=MAD&destination=FRA'` should return
`{"timestamp":1490776955527,"status":500,"error":"Internal Server Error","exception":"org.springframework.web.client.HttpClientErrorException","message":"401 Unauthorized","path":"/fares"``


3. Inject credentials using a User Provided Service.
We are going to tackle this step in a separate lab.


## Load flights fares from an external application using User Provided Services


1. Create a User Provided Service which encapsulates the credentials we need to call the `fare-service`.
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
