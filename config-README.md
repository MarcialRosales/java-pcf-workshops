Centralized configuration
---

# Introduction

[Slides](docs/SpringCloudConfigSlides.pdf)

Very interesting talk  [Implementing Config Server and Extending It](https://www.infoq.com/presentations/config-server-security)

## Additional comments
- We can store our credentials encrypted in the repo and Spring Config Server will decrypt them before delivering them to the client.
- Spring Config Service (PCF Tile) does not support server-side decryption. Instead, we have to configure our client to do it. For that we need to make sure that the java buildpack is configured with `Java Cryptography Extension (JCE) Unlimited Strength policy files`. For further details check out the <a href="http://docs.pivotal.io/spring-cloud-services/config-server/writing-client-applications.html#use-client-side-decryption">docs</a>.
- We should really use <a href="https://spring.io/blog/2016/06/24/managing-secrets-with-vault">Spring Cloud Vault</a> integrated with Spring Config Server to retrieve secrets.

# Configuration Management
We are going to introduce a new feature into `fare-service` that will return the number of loyalty points granted to each flight in addition to the fare. However, this feature will be controlled by a configuration setting, a.k.a a *feature-flag*.

## Make code changes


## Provision configuration server in PCF

This time we are going to work directly on the cloud, rather than running first locally and then in the cloud. It would be great that each attendee had their own github account where they can create a repository for their own configuration server.

1. Check the config server in the market place
`cf marketplace -s p-config-server`
2. Create a service instance
`cf create-service -c '{"git": { "uri": "https://github.com/MarcialRosales/spring-cloud-workshop-config" }, "count": 1 }' p-config-server standard config-server`

3. Make sure the service is available (`cf services`)

4. Our repo has already our `fare-service.yml`. If we did not have our the setting `spring.application.name`, the `spring-auto-configuration` jar injected by the java buildpack will automatically create a `spring.application.name` environment variable based on the env variable `VCAP_APPLICATION { ... "application_name": "fare-service" ... }`.

5. Push the `fare-service` app.

6. Check that our application is now bound to the config server
`cf env cf-demo-app`

7. Check that it loaded the application's configuration from the config server.
`curl cf-demo-app.cfapps-02.haas-40.pez.pivotal.io/env | jq .`

We should have these configuration at the top :
```
"configService:https://github.com/MarcialRosales/spring-cloud-workshop-config/demo.yml": {
    "mymessage": "Good afternoon"
  },
  "configService:https://github.com/MarcialRosales/spring-cloud-workshop-config/application.yml": {
    "info.id": "${spring.application.name}"
  },
```  

8. Check that our application is actually loading the message from the central config and not the default message `Hello`.
`curl cf-demo-app.cfapps-02.haas-40.pez.pivotal.io/hello?name=Marcial`

9. We can modify the demo.yml in github, and ask our application to reload the settings.
`curl -X POST cf-demo-app.cfapps-02.haas-40.pez.pivotal.io/refresh`

Check the message again.
`curl cf-demo-app.cfapps-02.haas-40.pez.pivotal.io/hello?name=Marcial`


10. Add a new configuration for production : `demo-production.yml` to the repo.

11. Configure our application to use production profile by manually setting an environment variable in CF:
`cf set-env cf-demo-app SPRING_PROFILES_ACTIVE production`

we have to restage our application because we have modified the environment.

12. Check our application returns us a different value this type
`curl cf-demo-app.cfapps-02.haas-40.pez.pivotal.io/env | jq .`

We should have these configuration at the top :


## Configuration Management - Dynamically reconfigure your application [Lab]

What about if we want to change a property at runtime? what if we want to change the logging level without restarting the application and across all instances of the same application? Extend that logging level or any other property like feature-flags.


### Dynamically changing settings at runtime in the application
- Spring will automatically rebuild a `@Bean` which is either annotated as `@RefreshScope` or `@ConfigurationProperties`. Spring assumes that a `ConfigurationProperties` is something we most likely want to refresh if any of its settings change.
- Spring will rebuild a bean when it receives a signal such as `RefreshRemoteApplicationEvent`. Spring config server sends this event to the application when it receives an event from the remote repo (via webhooks) or when it detects a change in the local filesystem -assuming we are using `native` repo.


Note about Reloading configuration: This works provided you only have one instance. Ideally, we want to configure our config server to receive a callback from Github (webhooks onto the actuator endpoint `/monitor`) when a change occurs. The config server (if bundled with the jar `spring-cloud-config-monitor`).
If we have more than one application instances you can still reload the configuration on all instances if you add the dependency `spring-cloud-starter-bus-amqp` to all the applications. It exposes a new endpoint called `/bus/refresh` . If we call that endpoint in one of the application instances, it propagates a refresh request to other instances.

Logging level is the most common settings we want to adjust at runtime. An Exercise is to modify the code to add a logger and add the logging level the demo.yml or demo-production.yml :
```
logging:
  level:
    io.pivotal.demo.CfDemoAppApplication: debug    

```


## Resiliency - What do we do if Config server is down?
We can either fail fast which means our applications fail to start. PCF would try to deploy the application a few times before giving up.
`spring.cloud.config.failFast=true`. Or if we can retry a few times before failing to start: `spring.cloud.config.failFast=false`, add the following dependencies to your project `spring-retry` and `spring-boot-starter-aop` and configure the retry mechanism via  `spring.cloud.config.retry.*`.

What happens if Config server cannot connect to the remote repo? @TODO test it!


## How to organize my application's configuration around the concept of a central repository [Lab]

#### Get started very quickly with spring config server : local file system (no git repo required)
```
---
spring.profiles: native
spring:
  cloud:
    config:
      server:
        native:
          searchLocations: ../../spring-cloud-workshop-config      
```

#### Use local git repo (all files must be committed!). One repo for all our applications and each application and profile has its own folder.
```
---
spring.profiles: git-local-common-repo
spring:
  cloud:
    config:
      server:
        git:
          uri: file:../../spring-cloud-workshop-config
          searchPaths: groupA-{application}-{profile}
```

#### Use local git repo. But different repos for different profiles
Spring Config server will try to resolve a pattern against ${application}/{profile}

```
---
spring.profiles: git-local-multi-repos-per-profile
spring:
  cloud:
    config:
      server:
        git:
          uri: file:../../emptyRepo
          repos:
            dev-repos:
              pattern: "*/dev"
              uri: file:../../dev-repo
            prod-repos:
              pattern: "*/prod"
              uri: file:../../prod-repo
```
 In this case, we have decided to have one repo specific for dev profile and another for prod profile              
 `curl localhost:8888/quote-service2/dev | jq .`

#### Use local git repo. Multiple repos per teams.

```
 ---
 spring.profiles: git-local-multi-repos-per-teams
 spring:
   cloud:
     config:
       server:
         git:
           uri: file:../../emptyRepo
           repos:
             trading:
               pattern: trading-*
               uri: file:../../trading
             pricing:
               pattern: pricing-*
               uri: file:../../pricing
             orders:
               pattern: orders-*
               uri: file:../../orders

```

We have 3 teams, trading, pricing, and orders. One repo per team responsible of a business capability.               
`curl localhost:8888/trading-execution-service/default | jq .`
`curl localhost:8888/pricing-quote-service/default | jq .`

#### Use local git repo. One repo per application.
```
---
spring.profiles: git-local-one-repo-per-app
spring:
  cloud:
    config:
      server:
        git:
          uri: file:../../{application}-repo
```          
