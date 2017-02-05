# scavenger8

[![Build Status](https://travis-ci.org/donbonifacio/scavenger8.svg?branch=master)](https://travis-ci.org/donbonifacio/scavenger8) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8906f8889ae04106b2aa885ca98f8a05)](https://www.codacy.com/app/donbonifacio_1472/scavenger8?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=donbonifacio/scavenger8&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/donbonifacio/scavenger8/badge.svg?branch=master)](https://coveralls.io/github/donbonifacio/scavenger8?branch=master) [![Technical debt ratio](https://sonarqube.com/api/badges/measure?key=code.donbonifacio:scavenger8&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard?id=code.donbonifacio%3Ascavenger8) [![Lines of Code](https://sonarqube.com/api/badges/measure?key=code.donbonifacio:scavenger8&metric=ncloc)](https://sonarqube.com/dashboard?id=code.donbonifacio%3Ascavenger8) [![Vulnerabilities](https://sonarqube.com/api/badges/measure?key=code.donbonifacio:scavenger8&metric=vulnerabilities)](https://sonarqube.com/dashboard?id=code.donbonifacio%3Ascavenger8) [![Code Smells](https://sonarqube.com/api/badges/measure?key=code.donbonifacio:scavenger8&metric=code_smells)](https://sonarqube.com/dashboard?id=code.donbonifacio%3Ascavenger8)

This is an implementation for the [scavenger challenge](https://github.com/donbonifacio/scavenger8/blob/master/Challenge.md). It requires **Java8+**
and consists of an application that creates a pipeline to process a stream of URLs.
For each url, the content is fetched and that is passed to a processor that tries
to identify known technologies in use on that URL.

#### Quick run

Test it quickly by doing:

```
# Fetch uberjar
wget https://github.com/donbonifacio/scavenger8/releases/download/v1.0.0/scavenger8.jar

# Fetch file with 1M urls
wget https://github.com/talkdesk-challenges/challenges/raw/master/problems/assets/technology%20scavenger/alexa1M.zip
unzip alexa1M.zip

# Run!
java -jar scavenger8.jar -file alexa1M.txt
```

You'll get as output connection errors or processed URLs. You can follow the
system metrics, by doing:

```
$ watch cat metrics.txt

# Sample result

Used memory: 37 MB
URLs submitted: 206

URLs queue 100

BodyRequester current tasks 80
BodyRequester processed tasks 25

Pages queue 0

TechnologyProcessor current tasks 0
TechnologyProcessor processed tasks 25

Technologies queue 0

OutputSink processed 25
Speed: 20 per second
It wil take 100 minutes to process 100K at this speed
```

## Dev Tools

You need a proper development environment with **Maven** and **make**.

* `make test` - runs the tests
* `make ci` - Travis utility, runs the continuous integration suite
* `make uberjar` - generates the standalone uberjar file
* `make alexa1M` - if you have the file at the root of the process, generates a clean build and processes it

# Architecture

The system is composed by several worker services, each one with executor services,
that are connected via `BlockingQueue`'s. All classes are immutable or thread safe.
The general flow is:

`UrlFileLoader` -> *queue* -> `BodyRequester` -> *queue* -> `TechnologyProcessor` -> *queue* -> `OutputSink`

When a service
is finished, it sends a special [POISON](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/PageInfo.java#L23)
object to notify the next service that there is no more work to receive.
When the `POISON` reaches the end, all work is done.

The following sections describe each service.

#### [UrlFileLoader](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/UrlFileLoader.java)
Opens the source file as a stream of lines and publishes those lines to the
output queue. Creates a single thread executor to perform this task.

#### [BodyRequester](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/BodyRequester.java)
For each url present in the input queue, performs an HTTP request to obtain
the response body and sends that info to the output queue.

It creates a single thread executor that acts as a gate keeper, and also a
fixed thread pool executor to perform the tasks. The gate keeper is in charge
of:
* scheduling tasks
* doing back pressure if necessary
* shuting down when the poison is detected

#### [TechnologyProcessor](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/TechnologyProcessor.java)
The technology processor works very similar to the body requester. But instead
for gathering response bodies, it receives them on the input queue, and then
pass that info to a pipeline of objects that check if the given data match
some technology.

It has a collection of objects that implement the [TechnologyMatcher](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/technologies/TechnologyMatcher.java)
interface. Those objects are stateless and given a `PageInfo` with url and response
body, return a `boolean` that indicates if it's a match.

At this moment the matching is a simple regex agains the HTML body.

#### [OutputSink](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/OutputSink.java)
Simple service that gathers `PageInfo`'s and just logs them to the class's logger.

### Utilities

There are also utility classes that work around the main services.

#### [System](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/System.java)
The `System` wraps all actors and is able to create all the objects to run the system.

#### [MetricsMonitor](https://github.com/donbonifacio/scavenger8/blob/master/src/main/java/code/donbonifacio/scavenger8/MetricsMonitor.java)
Given a `System`, the `MetricsMonitor` will gather stats from all the actors and
queues and dump them to a `metrics.txt` file every second. It's possible to
see the realtime status of the system by doing:

`watch cat metrics.txt`
