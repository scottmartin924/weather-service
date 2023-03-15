# Simple Weather Service
Weather app utilizing the [National Weather Service API](https://www.weather.gov/documentation/services-web-api) to
look up a basic version of the current weather for a given location. 

## Running the Project
Run the project using 
```shell
sbt run
```

This will start a web server on the port 8090---that is configurable in [application.properties](src/main/resources/application.properties)

### Exposed Endpoints
There are only two endpoints exposed

#### Get Forecast
Primarily, to retrieve the forecast for a given lat/long; for example, `lat=35.4628&long=-94.1430`
```shell
curl --request GET \
  --url 'http://localhost:8090/forecast?lat=35.4628&long=-94.1430'
```
If the request succeeds (which hopefully it does) your response should look like
```json
{
	"forecast": "Mostly Sunny",
	"temperature": "MODERATE"
}
```
_Note: Only very basic lat/long checking is done. Certainly there are libraries that do this
far better than what I did, but I mostly relied on the NOAA API to validate. One consequence of this
is that only locations covered by the US National Weather service can be retrieved._

#### Health
A simple health endpoint is given to show that status of the service (and because it was useful for testing)
```shell
curl --request GET \
  --url http://localhost:8090/health
```
The response is
```json
{
	"weatherClientStatus": "ok"
}
```

### Tests
To run the tests use
```shell 
sbt test
```

Hopefully they all pass

## Discussion
### Tech Stack
- ZIO
- zio-http

### Technology Choice
I chose to use zio-http not for any particular love of ZIO, but just
because that was the framework I used most recently. In an attempt to continue learning
---and because I'm probably more interested in the "traditional" pure fp approach---
I've also been trying to get more familiar with the Typelevel stack ([here's](https://github.com/scottmartin924/practical-fp-in-scala) a repo with some
code from a book I've been working through)

### Known Issues/Improvements
There are a few (well...many) known areas for improvement. I'll list a few here in no particular order, but this is by no means
exhaustive.
- There are several issues with the cache. First, it could overflow since there's no mechanism to evict. One solution to that is to put in place a background process to do that, or use a cache library that handles it, or move it out of memory all together. Second, it's obviously not persistent.
- Every call requires at least one call to the NOAA API (to convert lat/long to NOAA grid point). We could try to do a sort of grid-based caching of lat/longs or something perhaps more clever there
- Error handling is currently far from exhaustive. The NOAA API could throw many errors that we'd just return a 500 for (and we could get our API throttled)
- Currently very little API response standardization and no endpoint versioning, both would need to be improved
- Could use more tests. In particular, currently there's no real test of the retry logic in [ForecastService](src/main/scala/com/example/weatherservice/service/ForecastService.scala)
- The [temperature classifier](src/main/scala/com/example/weatherservice/service/TemperatureClassifier.scala) could be configurable instead of being hard-coded and, though more complex, could depend on the location you're looking up