PCF Developers workshop
==

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Introduction](#Introduction)
- [Pivotal Cloud Foundry Technical Overview](#Pivotal-Cloud-Foundry-Technical-Overview)
	- [Lab - Run Spring boot app](#Run-Spring-boot-app)
	- [Lab - Run web site](#Run-web-site)
- [Deploying simple apps](#Deploying-simple-apps)
  - [Lab - Deploy Spring boot app](#Deploy-Spring-boot-app)
  - [Lab - Deploy web site](#Deploy-web-site)

<!-- /TOC -->
# Introduction

`git clone https://github.com/MarcialRosales/java-pcf-workshops.git`

# Pivotal Cloud Foundry Technical Overview

Reference documentation:
- [Elastic Runtime concepts](http://docs.pivotal.io/pivotalcf/1-9/concepts/index.html)


## Run Spring boot app
We have a spring boot application which provides a list of available flights based on some origin and destination.

You can use the existing code or
1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn spring-boot:run`
3. `curl 'localhost:8080?origin=MAD&destination=FRA'`

We would like to make this application available to our clients. How would you do it today?

## Run web site
We also want to deploy the Maven site associated to our flight-availability application so that the team can check the latest java unit reports and/or the javadocs.

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn site:run`
3. Go to your browser, and check out this url `http://localhost:8080`

We would like to make this application available only within our organization, i.e. not publicly available to our clients. How would you do it today?

# Deploying simple apps

Reference documentation:
- [Using Apps Manager](http://docs.pivotal.io/pivotalcf/1-9/console/index.html)
- [Using cf CLI](http://docs.pivotal.io/pivotalcf/1-9/cf-cli/index.html)
- [Deploying Applications](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/deploy-app.html)
- [Deploying with manifests](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/manifest.html)

## Deploy Spring boot app
Deploy flight availability and make it publicly available on a given public domain

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn install`
3. `cf push flight-availability -p target/flight-availability-0.0.1-SNAPSHOT.jar --random-route`
4. Try to deploy the application using a manifest


## Deploy web site
Deploy Maven site associated to the flight availability and make it internally available on a given private domain

1. `cd java-pcf-workshops/deploy-simple-apps/flight-availability`
2. `mvn site`
3. Try to deploy the site
