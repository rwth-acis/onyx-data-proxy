Onyx Data Proxy Service
===========================================


Build
--------
Execute the following command on your shell:

```shell
ant jar 
```

Start
--------

To start the onyx-data-proxy service, follow the [Starting-A-las2peer-Network tutorial](https://github.com/rwth-acis/las2peer-Template-Project/wiki/Starting-A-las2peer-Network) and bootstrap your service to a [mobsos-data-processing service](https://github.com/rwth-acis/mobsos-data-processing/).
The onyx-data-proxy service may be used in two different ways to generate statements:

1. Using the Opal API the proxy can be connected to multiple Opal courses and will generate xAPI statements for new assessment results and daily course node access statistics.
2. Without using the Opal API the proxy provides a REST method that allows sending assessment result data downloaded from Opal manually to the onyx proxy. From the zip file sent to the proxy it will also generate xAPI statements for the assessment results.

Sending Onyx data to MobSOS manually
-----------------------

To send Onyx data downloaded from Opal to MobSOS manually (without using the Opal API), a RESTful POST request is offered.
```
POST <service-address>/onyx/assessments
```

Therefore, replace *service-address* with your service address.


How to run using Docker
-------------------

First build the image:
```bash
docker build . -t onyx-data-proxy
```

Then you can run the image like this:

```bash
docker run -p port:9011 onyx-data-proxy
```
Please make sure to add the following environment variables to the "docker run" command.

| Variable    | Description |
|-------------|-------------|
|API_ENABLED  | true or false depending on whether the Opal API should be used by the service.     |
|OPAL_USERNAME| Email address / username of the Opal account used to access the API.               |
|OPAL_PASSWORD| Password of the Opal account used to access the API.                               |
|COURSE_LIST  | Comma separated list of the course ids that should be monitored using the Opal API.|
|PSEUDONYMIZATION_ENABLED | true or false depending on whether personal user information (email, name) should be hashed within the xAPI statement. |

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | someNewPass | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |
