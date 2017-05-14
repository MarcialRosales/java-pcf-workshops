Buildpacks
===


# Introduction

**Reference documentation**:
- [Custom Buildpacks](http://docs.pivotal.io/pivotalcf/buildpacks/custom.html)

Only administrators are allowed to manage build packs. This means adding new ones, update them, change the order, and delete them. We can check what build packs are available by running `cf buildpacks`.

However, developers can specify the URI to a git repository where it resides a custom build pack. Although administrators can disable this option too.


# Adding functionality

We are going to customize the Java Build pack so that it declares a Java system property `staging.timestamp` with the timestamp when the application was staged.


1. Fork the Cloud Foundry Java buildpack from github
2. Clone your fork
3. Open the buildpack in your preferred editor
4. We add a *framework* component that will set the Java system property. To do that, first create `java-buildpack/lib/java_buildpack/framework/staging_timestamp.rb` and add the following contents:
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
5. Next we activate our new component by adding it to `java-buildpack/config/components.yml` as seen here:
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
6. Commit your changes and push it to your repo
7. Push an application that uses your build pack and test that the buildpack did its job.
 	- `git checkout load-flights-from-in-memory-db`
 	- `cd apps/flight-availability`
 	- `cf push -f target/manifest.yml -b https://github.com/MarcialRosales/java-buildpack`
 	- `curl https://<your_app_uri/env | jq . | grep "staging.timestamp"`


# Changing functionality

The Java buildpack in particular is highly customizable and there is an environment setting for almost every aspect of the buildpack. However, for those rare occasions, we are going to demonstrate how we can change some functionality such as fixing the JRE version. By default, the Java build pack downloads the latest JRE patch version, i.e. 1.8.0_x.

We will update our build pack to utilize java 1.8.0_25 rather than simply the latest 1.8.

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
2. Commit and push
3. Push the application again with this build pack and check in the staging logs that we are using JRE 1.8.0_25.
