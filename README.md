# inventory-service
This is the inventory microservice for the Z-Mart ZIO Reference application.

Its currently a work in progress but aims to demonstrate:

- ZIO itself
- How to construct 'services' in ZIO
- How to construct 'DAOs/Repositories' in ZIO using `zio-quill`
- How to use Flyway with ZIO to manage your DB migrations
- How to use `zio-magic` to manage your ZLayers in ZIO 1.x applications
- How to use `zio-json`
- How to use ZMX to track and explose metrics to Prometheus
- How to use `zio-logging`
- How to use Chimney to help transform case classes from one structure to another
- How to use `zio-config`

## Running the Service
To run the service you will need Docker installed.  

First get the database up and running.  Running `docker-compose up database -d` in the root of the project directory will start the db for you.

Then you'll want to execute the `zmart.inventory.startup.Main` class to fire up the RESTful inventory service.

You can use the http clients in `http-client/inventory` to execute various sample calls against the service to validate that things are setup properly.