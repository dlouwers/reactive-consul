# Reactive Consul

* Investigate why not all arrow symbols are getting converted to ⇒ by Scalariform
* Come up with a good interface for the actual client
* What is best-practice to verify that a child actor has been created? Can Props be mocked?

## What is a good flow for the service broker?

The broker has a few services under it's wing.
The broker will register for service availability updates.
The broker will start a connection pool for a new service and clean one up for one that disappeared.
