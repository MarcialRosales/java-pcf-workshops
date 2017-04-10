Load Trusted Certificates
==

# Introduction

In order to securely connect to external applications over HTTPS we need to have the certificate of the server we want to talk to or one of its certificate issuers so that our application can validate it.

# Trusted Certificate Viewer

This sample application available under `apps/trust-store-viewer` is a Spring Boot application that exposes all the trusted certificates available to the JRE via the `/info` actuator endpoint.

1. Build the application
	`mvn install`
2. Push the application to Cloud Foundry
	`cf push trust-store-viewer -p target/trust-store-viewer-0.0.1-SNAPSHOT.jar`
3. Get what SSL certificates are available to the application
	`https://myapplication.mydomain/info | jq `
	It produces an output like this:
	```
	{
	  "trustedCerts": [
	    {
	      "serialNumber": "800023",
	      "issuer": "CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US",
	      "notAfter": "Mon Jun 22 06:00:00 CEST 2020",
	      "notBefore": "Mon Jun 21 06:00:00 CEST 1999",
	      "subject": "CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US",
	      "alternateSubject": []
	    },
			....
		]
	}
	```
