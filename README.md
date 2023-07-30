# prometheus-android-exporter

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/birdthedeveloper/prometheus-android-exporter/tree/master.svg?style=svg&circle-token=6a31d132a46fd4e7cf04dd49ef390f1776e38cfc)](https://dl.circleci.com/status-badge/redirect/gh/birdthedeveloper/prometheus-android-exporter/tree/master)

Prometheus Exporter for Android phones. It is not yet available in Google Play. 
Apart from simply exporting available metrics on default HTTP port 10101, it can also traverse NAT
by connecting to the PushProx proxy.
It also supports scraping metrics locally and storing them in memory while offline and
exporting them later while online.

## Operation
This application can operate in three modes:
- as a Prometheus exporter by exposing metrics on HTTP port 10101
- as a PushProx proxy client, to traverse NAT and other network barriers while still following
    the pull model
- as a batch exporter, which can store metrics to memory while device is offline and later export
    them to Prometheus via remote write protocol while device becomes online

Application is configurable either via its UI or via YAML configuration file.

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

## To format code in android studio
```
CTRL + SHIFT + ALT + L
```

## File configuration
Client application is configurable via a configuration file.
Place such file on your android device at a following path:
```
data/user/0/com.birdthedeveloper.prometheus.android.exporter/files/
```
The name of such configuration file can be either `config.yaml` or `config.yml`

Configurable fields are described in `./config_file_structure.yaml`, all
fields are optional.

## ADB port forwarding
ADB port forwarding is usefull when running the client application 
on android emulator and prometheus database on the host
ADB port forwarding allows to map specific host's port to emulator's port
Syntax is as follows (for port 8080)
```
$ adb forward tcp:8080 tcp:8080
```

# Server configuration for PushProx proxy

## TL;DR
```
cd ./server

# edit ansible_inventory

# To apply ansible playbook
$ ansible-playbook ansible_playbook.yaml

# To only apply changed configuration
$ ansible-playbook ansible_playbook.yaml --tags config
``` 

## List of exported metrics:

### Android hardware sensors
`android_sensor_heading_degrees` - Data from the Android heading sensor
`android_sensor_proximity_metres` - Data from the proximity sensor
`android_sensor_heading_accuracy_degrees` - Data from Android the heading sensor
`android_sensor_hinge_angle_degrees` - How much is the hinge opened
`android_sensor_accelerometer{axis}` - Data from the accelerometer
`android_sensor_magnetic_field_tesla{axis}` - Data from the magnetic field sensor in base units
`android_sensor_gravity_acceleration` - Data from gravity acceleration sensor, in m/s^2 units
`android_sensor_linear_acceleration` - Data from the Android linear acceleration sensor in m/s^2 units.
`android_sensor_pressure_pascal` - Data from the Android pressure in pascals
`android_sensor_ambient_light_lux` - Data from Android ambient light sensor in lux
`android_sensor_gyroscope_radians_per_second_squared` - Data from Android gyroscope in radians/second^2
`android_sensor_ambient_temperature_celsius` - Ambient temperature in celsius
`android_sensor_rotation_vector` - Data from the Android Rotation Vector sensor, how is the device rotated, without a unit
`android_sensor_rotation_vector_cosinus_theta_half` - Data from the Android Rotation Vector sensor, how is the device rotated, without a unit
`android_sensor_rotation_vector_accuracy_radians` - Android rotation vector sensor accuracy in radians


### Miscellaneous
`android_battery_charge_ratio` - Current battery charge
`android_system_info{manufacturer, model, os_release, cpu_core_count}` - Information about Android system
`android_uptime_seconds` - Phone uptime in seconds
`android_cpu_active_seconds{core}` - Active CPU time in seconds since last time system booted
`android_cpu_total_seconds{core}` - Total CPU time in seconds since last time system booted
`android_system_temperature_celsius{where}` - Temperature on the device
`android_scrape_duration_seconds` - Duration of the metric scrape

### PushProx client mode specific metrics
`pushprox_client_poll_errors_total` - Number of errored /poll requests
`pushprox_client_scrape_errors_total` - Total number of scrape errors the PushProx client mode has encountered
`pushprox_client_push_errors_total` - Total number of errored /push requests
