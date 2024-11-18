# kstd-examples

TODO

### Local Playground

#### 1. Start docker compose stack
```bash
docker compose up -d
```

#### 2. Build gradle project
```bash
./gradlew clean build
```

#### 3. Produce test data
```bash
./gradlew common-datagen:produceBaggageTrackingEvents
./gradlew common-datagen:produceFlightEvents 
./gradlew common-datagen:produceUserFlightBookingEvents 
```

#### 4. Run individual projects
All examples are built with micronaut, you can run them using your preferred IDE or also from command line:

```bash
./gradlew todo:run
```


#### 5. Tear down docker compose stack
``` bash
docker compose down
```

