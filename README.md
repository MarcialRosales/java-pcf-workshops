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
  - [Load flights from a database](#load-flights-from-a-database)

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

We want to load the flights from a relational database (mysql) provisioned by the platform. We are implementing the `FlightService` interface so that we can load them from a `FlightRepository`. We need to convert `Flight` to a *JPA Entity*. We add **hsqldb** a *runtime dependency* so that we can run it locally.

1. `git checkout load-flights-from-db`
2. `cd apps/flight-availability`
3. Run the app
  `mvn spring-boot:run`
4. Test it
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`

5. Before we deploy our application to PCF we need to provision a mysql database.

  `cf marketplace`  Check out what services are available

  `cf marketplace -s p-mysql `  Check out the service details like available plans

  `cf create-service ...`   Create a service instance with the name `flight-repository`

  `cf service ...`  Check out the service instance. Is it ready to use?

6. Push the application using the manifest. See the manifest and observe we have declared a service:
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
7. Check out the database credentials the application is using: 
  `cf env flight-availability`

8. Test the application. Whats the url?

9. We did not include any jdbc drivers with the application. How could it that it works?
