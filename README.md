# Teamcity Container Cloud plugin
This plugin implements "Agent cloud" functionality for running agents within
containers, such as Docker. 

This allows an easy way to customize the build environment for every build,
instead of maintaining a set of generalized build agent that should build 
anything, or having dozens of mostly idle specialized agents.

As a starting point to creating agent images, have a look at Jetbrain's
[teamcity-agent](https://hub.docker.com/r/jetbrains/teamcity-agent/) and
[teamcity-minimal-agent](https://hub.docker.com/r/jetbrains/teamcity-minimal-agent/)
Docker containers.

# Compatibility
This plugin has been developed and tested against TeamCity 10 only, so far.

# Installation
The plugin is distributed as a single archive which TeamCity can load directly,
`container-cloud.zip`. Either drop the file within your server's plugin dir, or
upload it in the web interface. A server restart is required.

# Usage
There are two steps to get started using the plugin:

1. Create a Cloud profile
2. Configure a build to run within a container

## Cloud profiles
TeamCity has a concept of Cloud profiles, which can start agents on demand.
In this plugin, a Cloud profile is created with a single Container provider,
which is then responsible for starting the containers as requested by TeamCity.
If required, you can create several Cloud profiles using the plugin with
different configurations.

To create a cloud profile, go to Administration > Cloud agents, then click
"Create new profile". Enter a name for the profile, then select "Container cloud"
from the "Cloud type" drop down. Note that the "Terminate instance idle time" is
ignored, and "Terminate instance" will always be set to "After first build 
finished", no matter is configured here.

Select the desired Container provider and fill in any options required.

## Run in Container Cloud build feature
A new build feature "Run in Container Cloud" is included with the plugin, which
allows specifying a profile and image for the build directly from the build 
configuration. An image added here is automatically added to the profile's list
of images if not already present.

## Container providers
Currently there are two providers supported, described below, Docket socket and 
[Helios](https://github.com/spotify/helios), but many more are planned.

### Docker socket
This provider connects to a single Docker instance, either over a local unix
socket or using http(s), and runs agents there. Simple to get started, but 
limited to as many agents as can be run on a single machine.

#### Configuration options
`API endpoint`: How to reach the Docker socket. Defaults to using the local
`DOCKER_HOST` environment variable.


### Helios
This provider uses Spotify's Helios orchestration tool to run agents on any
host joined to the Helios cluster. Specifying a subset of hosts is possible
with either host name pattern matching or label selectors.

Currently, a random agent matching the given criteria will be chosen when 
starting a new build agent.

#### Configuration options
`Master url`: Url a single master, or the SRV record designating the a cluster.
Required.

`Host name pattern`: Substring pattern agent host names must match. For example
given agents `foo.mydomain` and `foobar.mydomain`, a pattern of `foo` would 
match both agents, while the pattern `bar` would only match the second one.
Optional, if not given, all agents will be matched.

`Host selectors`: Comma-separated list of selectors used to filter agents. For
example, `role=builder,az=us-east-1` would select only agents with both those
labels set.
Optional, if not given, all agents will be matched.

## Building only in a specific container image or profile
TeamCity will compute which agents can process a certain build and then choose
one of those. If you want to make sure a build is only run with a specific 
container image, add an Agent requirement with 
`env.CONTAINER_CLOUD_AGENT_IMAGE_ID` equalling the desired image.

In the same way, to restrict a build to a certain profile, add a requirement
for `system.cloud.profile_id`.

# Building
The plugin is built using Maven. There are a few dependencies that are not part
of the Teamcity open apis which you will need to aquire from a TeamCity 
installation first. An up-to-date list is documented in `pom.xml`, but one
example is `cloud-server.jar`. This is located under 
`webapps/ROOT/WEB-INF/lib/cloud-server.jar` in the server installation.
Copy this file to the Maven local repository, including the version number:
`.m2/repository/org/jetbrains/teamcity/cloud-server/10.0/cloud-server-10.0.jar`

Once you're setup, simply compile and package:

```bash
$ mvn clean compile package
```

If all works, the finished plugin is located in `target/container-cloud.zip`.