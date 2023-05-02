# prometheus-android-exporter
Prometheus exporter for android phones. Can traverse NAT by leveraging PushProx proxy. Not intended
to run as ROOT. Exposes various hardware sensor metrics.

# Repository contents
- Folder ./client contains Jetpack Compose android Prometheus exporter written in kotlin.
- Folder ./server contains ansible playbook for simple deployment of prometheus monitoring stack
    on a virtual private server, that is deployment of prometheus database itself, grafana
    monitoring dashboard and pushprox, a prometheus proxy used to traverse NAT while still following
    the pull model.
- Folder ./local contains simple docker-compose.yaml to spin up prometheus database on localhost
    quickly.

# Not public
143.42.59.63:9090 - prometheus

# Client

### ADB port forwarding
ADB port forwarding is usefull when running the client application 
on android emulator and prometheus database on the host
ADB port forwarding allows to map specific host's port to emulator's port
Syntax is as follows (for port 8080)
```
$ adb forward tcp:8080 tcp:8080
```

# Server

## TL;DR
```
cd ./server

# edit ansible_inventory

# To apply ansible playbook
$ ansible-playbook ansible_playbook.yaml

# To only apply changed configuration
$ ansible-playbook ansible_playbook.yaml --tags config
``` 

