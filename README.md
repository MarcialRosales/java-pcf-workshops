PCF Developers workshop
==

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Introduction](#introduction)
- [Pivotal Cloud Foundry Technical Overview](#pivotal-cloud-foundry-technical-overview)
	- [Lab - Run Spring boot app](#run-spring-boot-app)
	- [Lab - Run web site](#run-web-site)
- [Deploying simple apps](#deploy-spring-boot-app)
  - [Lab - Deploy Spring boot app](#deploy-spring-boot-app)
  - [Lab - Deploy web site](#deploy-web-site)
- [Cloud Foundry services](#cloud-foundry-services)
	- [Load flights from a database](#load-flights-from-a-database)
	- [Retrieve fares from an external application](#retrieve-fares-from-an-external-application)
- [Buildpacks](#buildpacks)

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
- [Extending Spring Cloud](https://spring.io/blog/2014/08/05/extending-spring-cloud)

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

## Load flights from a database

### Code walk-thru


### Build, Deploy and Test the application

We want to load the flights from a relational database (mysql) provisioned by the platform. We are implementing the `FlightService` interface so that we can load them from a `FlightRepository`. We need to convert `Flight` to a *JPA Entity*. We [added](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-flights-from-db/apps/flight-availability/pom.xml#L41-L49) **hsqldb** a *runtime dependency* so that we can run it locally.

1. `git checkout load-flights-from-db`
2. `cd apps/flight-availability`
3. Run the app
  `mvn spring-boot:run`
4. Test it
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`

5. Before we deploy our application to PCF we need to provision a mysql database.
  `cf marketplace`  Check out what services are available
  `cf marketplace -s p-mysql `  Check out the service details like available plans
  `cf create-service p-mysql pre-existing-plan flight-repository`   Create a service instance
  `cf service ...`  Check out the service instance. Is it ready to use?

6. Push the application using the manifest.  
  `cf push -f target/manifest.yml`

7. See the manifest and observe we have declare a service:  
  ```
  applications:
  - name: flight-availability
    instances: 1
    memory: 1024M
    path: target/@project.build.finalName@.@project.packaging@
    random-route: true
    services:
    - flight-repository

  ```
7. Check out the database credentials the application is using
  `cf env flight-availability`

8. Test the application. Whats the url?

9. We did not include any jdbc drivers with the application. How could it that it works?

## Retrieve fares from an external application

We are going to extend our flight availability application we implemented in the branch `load-flights-from-db` so that it retrieves flights and the fare for each flight. This time we are working off branch `load-fares-from-external-app`.

The idea is this: Flights come straight from the db (via the *flight-repository service*) and fares come from calling another rest service.
```
		--[1]--(rest api)--->[flight-availability]---[3]-(rest api)---->[fare-service]
										|
										+---[2]-(FlightRepository)-->[MySql db]
```


### Code walk-thru

1. Change the [FlightServiceImpl]() so that it calls the [FareService](). We build an instance of [FareServiceImpl]() from a specific **RestTemplate** that points directly to the fare-service.
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
2. We need to configure somehow where the FareService runs and how to connect to it (i.e. credentials). For now, we use spring boot configuration and we have this class in the [FlightAvailabilityApplication]().
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
  And the settings under `src/main/resources/application.yml`
	```
	fare-service:
	uri: http://localhost:8081
	username: user
	password: password
	```


### Build and Test the application

1. `git checkout load-fares-from-external-app`
2. `cd apps/flight-availability`
3. Run the app from one terminal  
	`mvn spring-boot:run`
4. From another terminal:  
	`cd apps/fare-service`  
	`mvn spring-boot:run` The fare-service runs on port 8081.   
5. Test it
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`  
  `curl 'localhost:8080/fares?origin=MAD&destination=FRA'` shall return `[{"fare":"0.8255260037921347", "id":2,"origin":"MAD","destination":"FRA"}]`

### Deploy and Test the application

1. Build fare-service  
 	`mvn install`
2. Deploy fare-service using the manifest  
	`cf push -f target/manifest.yml`  
	`cf app fare-service-app` Check out the url where it is listening
3. Build flight-availability  
	`mvn install`
4. Deploy flight-availability using the manifest. Do we need to make any changes before we deploy?  
	`cf push  -f target/manifest.yml`  
	`cf app flight-availability`  check out the url  
	`curl '<url>/fares?origin=MAD&destination=FRA'` does it work?
5. It does not work because flight-availability is using `http://localhost:8081` as the fare-service url.
6. We can fix this issue by setting the proper environment variables in the manifest:
	```
	applications:
	- name: flight-availability
	  instances: 1
	  memory: 1024M
	  path: target/@project.build.finalName@.@project.packaging@
	  random-route: true
	  services:
	  - flight-repository
		env:
		FARE_SERVICE_URI: <url of fare-service>
		FARE_SERVICE_USERNAME: username
		FARE_SERVICE_PASSWORD: password
	```
	And spring boot will convert those environment variables into properties. It works but it is not elegant and every application that needs to talk to the fare-service needs to be configured with all these credentials. Far from ideal.
7. To properly fix this issue we are going to provide the fare-service's credential to the flight-availability application thru a **User Provided Service**.   
8. Let's create a **User Provided Service** that encapsulates the uri, username and password required to connect to the FareService.  
		`cf cups fare-service -p '{"uri":"<uri of fare-service app>","username":"username", "password":"password" }'`
9. Declare this new service in the flight-availability application's manifest.
	```
	applications:
	- name: flight-availability
	  instances: 1
	  memory: 1024M
	  path: target/@project.build.finalName@.@project.packaging@
	  random-route: true
	  services:
	  - flight-repository  # has to match the service instance or user provided service you have created in PCF
	  - fare-service

	```
9. Deploy the application, but do not test it yet, and check the credentials of the fare-service  
	`cf push -f target/manifest.yml`  
	`cf env flight-availability`  
10. We need to extract the fare-service credentials from `VCAP_SERVICES`. To do that we need a library called **Spring Cloud Connectors** and in particular **Spring Cloud Connector for Cloud Foundry** which knows how to read `VCAP_SERVICES`. If you don't use this library you have to write your own library that looks up the `VCAP_SERVICES` variable and parses its content which is in JSON format.  
	Add the libraries to the pom.xml
	```
	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-spring-service-connector</artifactId>

		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-cloudfoundry-connector</artifactId>

		</dependency>
	```

# Buildpacks

## Goal

Get familiar with build backs and how to extend them and use them to deploy an application.

This is a developers workshop so we are not going to package a build pack and upload it to Cloud Foundry. That would be the job of the platform's operator and we need additional tools like [Ruby](http://rvm.io/) and [Bundler](http://bundler.io/).

## Reference documentation

- Buildpacks: http://docs.cloudfoundry.org/buildpacks/
- Java Buildpack: http://docs.cloudfoundry.org/buildpacks/java/index.html
- Staticfile Buildpack: http://docs.cloudfoundry.org/buildpacks/staticfile/index.html
- Custom buildpacks: http://docs.cloudfoundry.org/buildpacks/custom.html
- Build pack modes : https://github.com/MarcialRosales/java-buildpack/blob/master/docs/buildpack-modes.md
- Understand how to extend the java build pack: https://github.com/MarcialRosales/java-buildpack/blob/master/docs/extending.md

## Lab 1

We are going to extend the Java build pack so that it injects an environment which contains the timestamp when the application was staged.

1. Fork the Cloud Foundry Java Buildpack from GitHub (https://github.com/cloudfoundry/java-buildpack).
2. Clone your fork:
	```
	git clone https://github.com/cloudfoundry/java-buildpack
	cd java-buildpack
	```
3. Open the Buildpack source code directory in your favorite editor.
4. We’ll add a framework component that will set a Java system property containing a timestamp that indicates when the application was staged. To do that, first create java-buildpack/lib/java_buildpack/framework/staging_timestamp.rb and add the following contents:
	```
	require 'java_buildpack/framework'

	module JavaBuildpack::Framework

	  # Adds a system property containing a timestamp of when the application was staged.
	  class StagingTimestamp < JavaBuildpack::Component::BaseComponent
	    def initialize(context)
	      super(context)
	    end

	    def detect
	      'staging-timestamp'
	    end

	    def compile
	    end

	    def release
	      @droplet.java_opts.add_system_property('staging.timestamp', "'#{Time.now}'")
	    end
	  end
	end
	```
5. Next we need to `turn on` our new framework component by adding it to `java-buildpack/config/components.yml` as seen here:
	```
	frameworks:
	  - "JavaBuildpack::Framework::AppDynamicsAgent"
	  - "JavaBuildpack::Framework::JavaOpts"
	  - "JavaBuildpack::Framework::MariaDbJDBC"
	  - "JavaBuildpack::Framework::NewRelicAgent"
	  - "JavaBuildpack::Framework::PlayFrameworkAutoReconfiguration"
	  - "JavaBuildpack::Framework::PlayFrameworkJPAPlugin"
	  - "JavaBuildpack::Framework::PostgresqlJDBC"
	  - "JavaBuildpack::Framework::SpringAutoReconfiguration"
	  - "JavaBuildpack::Framework::SpringInsight"
	  - "JavaBuildpack::Framework::StagingTimestamp" #Here's the bit you need to add!
	```


## Lab 2

Operations team wants to force Java 1.8.0_025 in production.

1. Change `java-buildpack/config/open_jdk_jre.yml` as shown:
	```
	repository_root: "{default.repository.root}/openjdk/{platform}/{architecture}"
	version: 1.8.0_+ # becomes 1.8.0_25
	memory_sizes:
  	metaspace: 64m.. # permgen becomes metaspace
	memory_heuristics:
	  heap: 85
	  metaspace: 10 # permgen becomes metaspace
	  stack: 5
	  native: 10
	```

## Verify your changes

1. We can verify the changes by looking at the logs produced while we push our application or via the actuator endpoint `/env`
2. Push your application specify the url to your buildpack:
	```
	cf push <myappName> -p target/<myappName-version>.jar -b https://github.com/<youraccount>/java-buildpack
	```
	What about if we use manifest instead?

3. Make sure that the command-line has our new system property `staging.timestamp` or go to the `/env` endpoint and look for your system property.

4. Make sure that we are running with Java 1.8.0_25.
