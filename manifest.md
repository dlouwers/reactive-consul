# Solid Scala testing manifest

1. Most systems should contain three type of tests: unit tests, integration tests and end-to-end tests.
2. Developers should strive to be able to run all three types of tests locally in full isolation.
3. Developers should strive to be able to run all three types of tests on a build server, in parallel and in isolation.
4. Developers should strive to generate deliverables that behave as consistent as possible in different environments.
 
## Test Types

### Unit Tests

Unit tests are tests that are independent of external systems and are generally small in scope e.g. test a single 
class.

### Integration Tests

Integration tests test the integration of two parts of the internal system or a part of the internal system and the
interaction with an external system. For the purpose of this manifest I will mainly be referring to the latter.

### End-To-End Tests

End to end tests test the functioning of the system as a whole, including the interaction with external systems. These
are generally hardest to run in isolation. These tests can contain failover scenarios if they are part of the system 
requirements.

### Test devision
There should a clear partitioning between the different kinds of tests in order to be able to run them in different
environments. Travis CI can, for instance, not run any integration or end-to-end tests when there are external
systems involved. In the example project there are three test folders: test, it and e2e, for unit, integration and 
end-to-end testing respectively.

## Running tests locally
All types of tests should be able to run locally. It is evident how this is done for unit tests and intra system 
integration tests. For tests interacting with external systems like a database or mail server, tests should set up
their own dependencies in isolation. Virtualization and and expecially containerization make this possible nowadays. 

The example project uses Docker to setup its dependencies, either through reusing public images or creating its own.
Please note that eacht project that produces an application should also provide the means to deploy and interact with
this. In this case we found it easiest to containerize it using Docker and publish the resulting image.

Orchestration tools like docker-compose or vagrant can be used to setup a multi-system environment for end-to-end
testing.

## Running tests on a build server
Running tests on a build server like Jenkins or Bamboo is very similar to running them locally. However, these servers
usually have to run multiple of these tests at the same time. In order to support this, no ports should be exposed to
a fixed IP address, but rather bound to any available port, in order to be run multiple test scenarios in conjunction.

## Consistent deliverables
Just commiting a finished story to version control isn't enough nowadays. The way an application is deployed and on 
what system plays a part in determining wether it will function correctly. Applications should come with automated and
tested means to deploy on the production environment. 

This can be achieved with all manner of tools, like ansible, puppet or chef.
