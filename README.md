# Reactive Consul

[![Build Status](https://travis-ci.org/dlouwers/reactive-consul.svg?branch=master)](https://travis-ci.org/dlouwers/reactive-consul)

* Investigate why not all arrow symbols are getting converted to ⇒ by Scalariform
* Come up with a good interface for the actual client
* What is best-practice to verify that a child actor has been created? Can Props be mocked?
* Find out if Registrator can deal with mappings to any available port (-P)
* Actors should be named to provide better logging

Create local docker images for example by running
docker:publishLocal

## Docker testkit
* The docker-testkit depedency on Specs2 needs to be "provided" e.g. should not pull it in 
* docker-testkit needs to be refactored in a way as to offer support for ScalaTest as well
* If possible see about support or JUnit

## Musings

Since Marc and I already determined that in a build server environment there is need for a shared Consul instance, it
could be easier to maintain the same philosophy for local builds and separate tests based on tags that could be randomly
generated. The only difference would be that in a buildserver setting the Consul instance is provided while in a local
test environment it would have to be started and shared.

## Example

Run the examle by running `docker-compose up` in the example directory. Add more server instances by running:

    docker run --rm -e SERVICE_NAME=<example-service-1|example-service-2> -p 8080 --dns 172.17.42.1 -e INSTANCE_NAME=<name> reactive-consul-example:0.1-SNAPSHOT


