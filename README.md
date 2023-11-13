# prometheus-android-exporter

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/birdthedeveloper/prometheus-android-exporter/tree/master.svg?style=svg&circle-token=6a31d132a46fd4e7cf04dd49ef390f1776e38cfc)](https://dl.circleci.com/status-badge/redirect/gh/birdthedeveloper/prometheus-android-exporter/tree/master)

## DISCLAIMER
This is not yet available on Google Play. On some android phones, there are system policies preventing
longer run of this exporter in background.
If you are interested in this project and would like to contribute to it, please create an issue or 
hit me up at martin.ptace@gmail.com.


Prometheus exporter for Android phones.
Prometheus Android Exporter is implemented in Kotlin in Jetpack Compose.
Apart from simply exporting available metrics on the default HTTP port 10101, it can also traverse NAT
by connecting to the PushProx proxy.
It also supports scraping metrics locally and storing them in memory while offline and
exporting them later while online using the Prometheus remote write protocol.


## Operation
This application can operate in three modes (simultaneously):
- as a Prometheus exporter by exp osing metrics on HTTP default port 10101 or configured port.
- as a PushProx proxy client, to traverse NAT and other network barriers while still following
    the pull model.
- as a batch exporter, which can store metrics to memory while device is offline and later export
    them to Prometheus via remote write protocol while device becomes online.

Application is configurable either via its UI or via YAML configuration file.

### UI configuration
Each tab on the homepage of the application corresponds to one application mode.
For example, to configure PushProx client to traverse NAT while still following the pull model,
I can open the application, go to tab "PushProx", fill out FQDN - fully qualified domain name and PushProx
URL, switch this mode on by tapping on the switch, and tap "Start". FQDN is the identifier of the device
for Prometheus instance, must be in a format of a valid domain or subdomain name.

### File configuration
Client application is configurable via a configuration file.
Place such file on your android device at a following path:
```
data/user/0/com.birdthedeveloper.prometheus.android.exporter/files/
```
The name of such configuration file can be either `config.yaml` or `config.yml`

Configurable fields are described in `./config_file_structure.yaml`, most fields are optional.

After configuring the application via configuration file, one has to still start the exporter manually by
opening the app and hitting "Start" button.


## Repository contents
- `.circleci` contains configuration file for continuous integration pipeline, that runs
  on the CircleCI platform. This CI pipeline runs linter, unit tests, and builds the project for each commit.
- Folder `client` contains Jetpack Compose Prometheus Android Exporter written in Kotlin.
  This is a Android Studio project.
  Inside the Android project, inside the java/com/birdthedeveloper/prometheus/android/exporter 
  folder are the following Kotlin files:
    - `compose/Configuration.kt`: parser of the YAML file.
    - `compose/HomeActivity.kt`: homepage UI, all three tabs are defined here.
    - `compose/MainActivity.kt`: main activity, application navigation is defined here.
    - `compose/PromViewModel.kt`: View model, global state management of the application.
    - `compose/SettingsActivity.kt`: The settings page with attribution and license notice.
    - `ui/*`: these files were generated with the project.
    - `worker/AndroidCustomExporter.kt`: here is implemented a custom collector for metrics along with all the functions that collect various hardware and software metrics from the device.
    - `worker/ExponentialBackoff.kt`: here is implemented a simple exponential backoff strategy, used when sending HTTP requests using either batch exporter or PushProx client mode.
    - `worker/MetricsEngine.kt`: functions for accessing hardware sensors and other metrics.
    - `worker/PrometheusServer.kt`: implementation of the Prometheus exporter - HTTP server via Ktor.
    - `worker/PromWorker.kt`: implementation of the Android WorkManager worker. This worker consumes configuration from either UI or YAML configuration file and starts the configured application modes as Kotlin coroutines.
    - `worker/PushProxClient.kt`: in the file is implemented the PushProx client mode.
    - `worker/RemoteWriteSender.kt`: in this file is implemented the batch exporter mode.
    - `worker/RemoteWriteSenderMemStorage.kt`: memory store for metrics when the device is offline. Used by the batch exporter.
    - `worker/RemoteWriteSenderStorage.kt`: abstract class that serves as a contract for the batch exporter storage.
       If you want to add persistent storage to metrics, extend this class.
    - `worker/Util.kt`: this file contains utility functions used accross the whole app, such as function to determine whether a network is available.
- Folder `server` contains ansible playbook for simple deployment of prometheus monitoring stack
    on a virtual private server, that is deployment of prometheus database itself, grafana
    monitoring dashboard and pushprox, a prometheus proxy used to traverse NAT while still following
    the pull model.
- Folder `local` contains simple docker-compose.yaml files to spin up prometheus database on localhost
    quickly for testing purposes with desired configuration.
- Configuration options for the YAML file are described in `config_file_structure.yaml`.
- Grafana dashboard as an exported json is present in file `grafana_dashboard.json`.


## Some helpful information for development

### ADB port forwarding
ADB port forwarding is useful when running the client application 
on an android emulator and a Prometheus database on the host.
ADB port forwarding allows to map specific host's port to emulator's port.
Syntax is as follows (for port 8080):
```
$ adb forward tcp:8080 tcp:8080
```

## Server configuration for PushProx proxy
To configure new VPS linux server for Prometheus with PushProx proxy, you can use the provided 
ansible playbook. 
This playbook is meant to be use against freshly provisioned RedHat Linux or Rocky Linux or Alma Linux server. 

If you wish to run locally instead, just use the included docker-compose.yaml file as a template.

### TL;DR
```
$ cd ./server

# edit ansible_inventory to add your own linux server

# To apply ansible playbook
$ ansible-playbook ansible_playbook.yaml

# To only apply changed configuration (for example change prometheus.yaml file)
$ ansible-playbook ansible_playbook.yaml --tags config
``` 

## List of exported metrics:

### Android hardware sensors
- `android_sensor_heading_degrees` - Data from the Android heading sensor
- `android_sensor_proximity_metres` - Data from the proximity sensor
- `android_sensor_heading_accuracy_degrees` - Data from Android the heading sensor
- `android_sensor_hinge_angle_degrees` - How much is the hinge opened
- `android_sensor_accelerometer{axis}` - Data from the accelerometer
- `android_sensor_magnetic_field_tesla{axis}` - Data from the magnetic field sensor in base units
- `android_sensor_gravity_acceleration{axis}` - Data from gravity acceleration sensor, in m/s^2 units
- `android_sensor_linear_acceleration{axis}` - Data from the Android linear acceleration sensor in m/s^2 units.
- `android_sensor_pressure_pascal` - Data from the Android pressure in pascals
- `android_sensor_ambient_light_lux` - Data from Android ambient light sensor in lux
- `android_sensor_gyroscope_radians_per_second_squared{axis}` - Data from Android gyroscope in radians/second^2
- `android_sensor_ambient_temperature_celsius` - Ambient temperature in celsius
- `android_sensor_rotation_vector{axis}` - Data from the Android Rotation Vector sensor, how is the device rotated, without a unit
- `android_sensor_rotation_vector_cosinus_theta_half` - Data from the Android Rotation Vector sensor, how is the device rotated, without a unit
- `android_sensor_rotation_vector_accuracy_radians` - Android rotation vector sensor accuracy in radians

### Android network
- `android_cellular_network_connected` - Whether cellular network is connected
- `android_wifi_connected` - Whether WiFi is connected

### Miscellaneous
- `android_battery_charge_ratio` - Current battery charge
- `android_battery_is_charging` - Whether the battery is charging
- `android_system_info{manufacturer, model, os_release, cpu_core_count}` - Information about Android system
- `android_uptime_seconds` - Phone uptime in seconds
- `android_scrape_duration_seconds` - Duration of the metric scrape

### PushProx client mode specific metrics
- `pushprox_client_poll_errors_total` - Number of errored /poll requests
- `pushprox_client_scrape_errors_total` - Total number of scrape errors the PushProx client mode has encountered
- `pushprox_client_push_errors_total` - Total number of errored /push requests
 