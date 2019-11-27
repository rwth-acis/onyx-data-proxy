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

Sending Onyx data to MobSOS
-----------------------

To send Onyx data to MobSOS, a RESTful POST request is offered.
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

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
| :----------------: | :-----: | :-------: | :--------: |
| BOOTSTRAP | unset | Set the --bootstrap option to bootstrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | Passphrase | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |
