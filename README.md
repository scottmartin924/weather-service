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
  "temperature": "MODERATE",
  "alerts": [
    {
      "event": "Hydrologic Outlook",
      "headline": "Hydrologic Outlook issued April 19 at 2:42PM CDT by NWS Shreveport LA"
    }
  ]
}
```

Hint: Most coordinates won't have warnings. If you go to [weather.gov](https://weather.gov) and find a location that has a warning
you should be able to find coordinates that will give you a warning.

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
  "healthStatus": "OK"
}
```

### Tests
To run the tests use
```shell 
sbt test
```

Hopefully they all pass.

## Discussion
### Tech Stack
- CE 3 (effect system)
- http4s (http client and server)
- circe (json parsing)

### Known Issues/Improvements
There are a few (well...many) known areas for improvement. I'll list a few here in no particular order, but this is by no means
exhaustive.
#### Error handling
Right now the error handling is pretty subpar. Many errors--including som NOAA API call failures--results in unhelpful 500s. Ideally we'd make the 
`WeatherClient` significantly more robust and handle errors from it in a nice way like wrapping results in `Either` or at the very least 
handle failed effects better. Honestly, skipping this was mostly done to save time.

#### Caching is incomplete/flakey
There are several issues with the cache. First, it could overflow since there's no mechanism to evict. One solution to that is to put in place a background process to do that, or use a cache library that handles it, or move it out of memory all together. Second,
we don't cache the alerts (this was one of the things not in the original assignment so added it but decided not to cache).
Third, we do have to call the NOAA API to convert lat/long to a NOAA grid point for each call. Caching these would be a good idea, but would require some thought since obviously
you'd want a bit of leeway in the cached lat/long. Finally, it's obviously not persistent.

#### Testing could be better
While there are a fair number of tests written there are certainly some full flows that are _not_ tested and probably should be.

#### "Smaller" Improvements
- Definitely need more logging
- The [temperature classifier](src/main/scala/com/example/weatherservice/service/TemperatureClassifier.scala) could be configurable instead of being hard-coded and, though more complicated, could be region specific
- Currently very little API response standardization and no endpoint versioning, both would need to be improved
- More docs


### Some Context
I'd previously written a similar assignment using the [ZIO](https://zio.dev/) stack, but decided to move it to Typelevel. This means there are a few
things which I sort of left as they were and didn't add to with the new assignment