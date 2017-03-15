PCF Developers workshop
==

# Introduction

`git clone https://github.com/MarcialRosales/java-pcf-workshops.git`

# Pivotal Cloud Foundry Technical Overview

## Lab 1 - We have a spring boot application which provides a list of available flights based on some origin and destination.

You can use the existing code or
1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn spring-boot:run`
3. `curl 'localhost:8080?origin=MAD&destination=FRA'``

We would like to make this application available to our clients. How would you do it today?

## Lab 2 - We also want to deploy the Maven site associated to our flight-availability application so that the team can check the latest java unit reports and/or the javadocs.

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn site:run`
3. Go to your browser, and check out this url `http://localhost:8080`

We would like to make this application available within our organization, i.e. not publicly available to our clients. How would you do it today?

# Deploying simple apps

## Lab 1 - Deploy flight availability and make it publicly available on a given public domain

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn install`
3. `cf push flight-availability -p target/flight-availability-0.0.1-SNAPSHOT.jar --random-route`
4. Try to deploy the application using a manifest


## Lab 2 - Deploy Maven site associated to the flight availability and make it internally available on a given private domain

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn site`
3. Try to deploy the site
