# Running
Build and publish to local Docker repository:

    sbt compile
    sbt stage
    sbt docker:publishLocal
    
Run the examle by running `docker-compose up` in the example directory. Add more server instances by running:

    docker run --rm -e SERVICE_NAME=<example-service-1|example-service-2> -p 8080 --dns 172.17.42.1 -e INSTANCE_NAME=<name> reactive-consul-example:0.1-SNAPSHOT
