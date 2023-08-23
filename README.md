# integration-adaptor-gp2gp
National Integration Adaptor - GP2GP

The existing GP2GP solution uses a legacy messaging standard and infrastructure (HL7v3 and Spine TMS). Reliance 
on these standards going forward presents a significant barrier to successful GP2GP implementation by new suppliers, 
and perpetuation of these standards in the long term presents a risk to the continued operation of GP2GP across all 
suppliers.

A hybrid solution approach has been selected as the best option for GP2GP adoption by NMEs and transition by existing 
incumbent suppliers.

The "National Integration Adaptor - GP2GP" implements a GP2GP 2.2b producer using the supplier's existing GP Connect 
Provider implementation to extract the Electronic Health Record. Suppliers that have not already implemented a 
GP2GP 2.2b producer, or those wishing to decommission their existing producer, may deploy the GP2GP adaptor in its place.

## Requirements:

* JDK 11 - We develop the adaptor in Java with Spring Boot
* Docker - We release the adaptor using Docker images on [Dockerhub](https://hub.docker.com/repository/docker/nhsdev/nia-gp2gp-adaptor)

## Configuration

The adaptor reads its configuration from environment variables. The following sections describe the environment variables
 used to configure the adaptor.

Variables without a default value and not marked optional, *MUST* be defined for the adaptor to run.

### General Configuration Options

| Environment Variable     | Default                   | Description                                                                               |
|--------------------------|---------------------------|-------------------------------------------------------------------------------------------|
| GP2GP_SERVER_PORT        | 8080                      | The port on which the GP2GP Adapter will run.                                             |
| GP2GP_ROOT_LOGGING_LEVEL | WARN                      | The logging level applied to the entire application (including third-party dependencies). |
| GP2GP_LOGGING_LEVEL      | INFO                      | The logging level applied to GP2GP adaptor components.                                    |
| GP2GP_LOGGING_FORMAT     | (*)                       | Defines how to format log events on stdout                                                |

Logging levels are ane of: DEBUG, INFO, WARN, ERROR

The level DEBUG **MUST NOT** be used when handling live patient data.

(*) GP2GP API uses logback (http://logback.qos.ch/). The built-in [logback.xml](service/src/main/resources/logback.xml) 
defines the default log format. This value can be overridden using the `GP2GP_LOGGING_FORMAT` environment variable.
You can provide an external `logback.xml` file using the `-Dlogback.configurationFile` JVM parameter.

### Database Configuration Options

The adaptor requires a Mongodb-compatible database to manage its internal state.

| Environment Variable            | Default                   | Description                                                                                                                                                    |
|---------------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_MONGO_URI                 | mongodb://localhost:27017 | Whole Mongo database connection string. Has a priority over other Mongo variables.                                                                             |
| GP2GP_MONGO_DATABASE_NAME       | gp2gp                     | The database name.                                                                                                                                             |
| GP2GP_MONGO_HOST                |                           | The database host. Leave undefined if GP2GP_MONGO_URI is used.                                                                                                 |
| GP2GP_MONGO_PORT                |                           | The database port. Leave undefined if GP2GP_MONGO_URI is used.                                                                                                 |
| GP2GP_MONGO_USERNAME            |                           | The database username. Leave undefined if GP2GP_MONGO_URI is used.                                                                                             |
| GP2GP_MONGO_PASSWORD            |                           | Mongo database password. Leave undefined if GP2GP_MONGO_URI is used.                                                                                           |
| GP2GP_MONGO_OPTIONS             |                           | Mongodb URL encoded parameters for the connection string without a leading "?". Leave undefined if GP2GP_MONGO_URI is used.                                    |
| GP2GP_MONGO_AUTO_INDEX_CREATION | true                      | (Optional) Should auto index for Mongo database be created.                                                                                                    |
| GP2GP_MONGO_TTL                 | P7D                       | (Optional) Time-to-live value for inbound and outbound state collection documents as an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations). |
| GP2GP_COSMOS_DB_ENABLED         | false                     | (Optional) If true the adaptor will enable features and workarounds to support Azure Cosmos DB.                                                                |

**Trust Store Configuration Options**

You can configure a trust store with private CA certificates if required for TLS connections. The trust store does not 
replace Java's default trust store. At runtime the application adds these additional certificates to the default trust 
store. Only an s3:// url is currently supported, and the current use-case is to support AWS DocumentDb.

| Environment Variable           | Default | Description                                                                           |
|--------------------------------|---------|---------------------------------------------------------------------------------------|
| GP2GP_SSL_TRUST_STORE_URL      |         | (Optional) URL of the trust store JKS. The only scheme currently supported is `s3://` |
| GP2GP_SSL_TRUST_STORE_PASSWORD |         | (Optional) Password used to access the trust store                                    |

### File Storage Configuration Options

The adaptor uses AWS S3 or Azure Storage Blob to stage translated GP2GP HL7 and ebXML documents.

| Environment Variable                  | Default   | Description                                                                         |
|---------------------------------------|-----------|-------------------------------------------------------------------------------------|
| GP2GP_STORAGE_TYPE                    | LocalMock | The type of storage solution. One of: S3, Azure, LocalMock                          |
| GP2GP_STORAGE_CONTAINER_NAME          |           | The name of the Azure Storage container or Amazon S3 Bucket                         |
| GP2GP_AZURE_STORAGE_CONNECTION_STRING |           | The connection string for Azure Blob Storage. Leave undefined if type is not Azure. |
| AWS_ACCESS_KEY_ID                     |           | The access key for Amazon S3. Leave undefined if using an AWS instance role.        |
| AWS_SECRET_ACCESS_KEY                 |           | The secret access key for Amazon S3. Leave undefined if using an AWS instance role. |
| AWS_REGION                            |           | The region for Amazon S3. Leave undefined if using an AWS instance role.            |

### Message Broker Configuration Options

The adaptor requires an AMQP 1.0 compatible message broker to 1) receive inbound Spine messages via MHS adaptor and 2)
queue its own internal asynchronous tasks

| Environment Variable                  | Default               | Description                                                                                                                                                                                                       |
|---------------------------------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_AMQP_BROKERS                    | amqp://localhost:5672 | A comma-separated list of URLs to AMQP brokers (*)                                                                                                                                                                |
| GP2GP_AMQP_USERNAME                   |                       | (Optional) username for the AMQP server                                                                                                                                                                           |
| GP2GP_AMQP_PASSWORD                   |                       | (Optional) password for the AMQP server                                                                                                                                                                           |
| GP2GP_AMQP_MAX_REDELIVERIES           | 3                     | The number of times an message will be retried to be delivered to consumer. After exhausting all retires, it will be put on DLQ.<queue_name> dead letter queue                                                    |
| GP2GP_TASK_QUEUE                      | gp2gpTaskQueue        | Defines name of internal taskQueue.                                                                                                                                                                               |
| GP2GP_TASK_QUEUE_CONSUMER_CONCURRENCY | 1                     | Defines the number of concurrent task queue consumers in a single application. https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/annotation/JmsListener.html#concurrency-- |

(*) Active/Standby: The first broker in the list always used unless there is an error, in which case the other URLs 
will be used. At least one URL is required.

### GP Connect API Configuration Options

The adaptor fetches patient records and documents with the GP Connect Consumer Adaptor 
([Github](https://github.com/nhsconnect/integration-adaptor-gpc-consumer) / 
[Dockerhub](https://hub.docker.com/repository/docker/nhsdev/nia-gpc-consumer-adaptor)) consuming the 
[GP Connect API](https://developer.nhs.uk/apis/gpconnect/).

| Environment Variable           | Default                                           | Description                                                                                                                                            |
|--------------------------------|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_GPC_GET_URL              | http://localhost:8090/@ODS_CODE@/STU3/1/gpconnect | (*) The base URL of the GP Connect Consumer Adaptor. @ODS_CODE@ is a placeholder replaced in runtime with the actual ODS code of the loosing practice. |
| GP2GP_GPC_STRUCTURED_FHIR_BASE | /fhir                                             | The path segment for Get Access Structured FHIR server                                                                                                 |
| GP2GP_GPC_MAX_REQUEST_SIZE     | 150000000 (150 MB)                                | Buffer size when downloading data from GPC                                                                                                             |                                                                                                             

(*) `GP2GP_GPC_GET_URL` could be set to the base URL of a GP Connect Producer for limited testing purposes 

### MHS Adaptor Configuration Options

The GP2GP uses the [MHS Adaptor](https://github.com/nhsconnect/integration-adaptor-mhs) to send/receive messages to/from Spine.

| Environment Variable                         | Default                                 | Description                                                                                                                                                                                                              |
|----------------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_MHS_OUTBOUND_URL                       | http://localhost:8081/mock-mhs-endpoint | URL to the MHS adaptor's outbound endpoint                                                                                                                                                                               |
| GP2GP_MHS_INBOUND_QUEUE                      | inbound                                 | Name of the queue for MHS inbound                                                                                                                                                                                        |
| GP2GP_MHS_INBOUND_QUEUE_CONSUMER_CONCURRENCY | 1                                       | Defines the number of concurrent mhs inbound queue consumers in a single application. https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/annotation/JmsListener.html#concurrency-- |


### GP2GP Configuration Options

| Environment Variable             | Default | Description                                                                                                                                                      |
|----------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GP2GP_LARGE_ATTACHMENT_THRESHOLD | 4500000 | Value in bytes. Defines the max size of a single attachment sent to MHS. If a document is larger than this value, it's content will be split and sent in chunks. | 

## How to operate the adaptor

The following sections describe how to run the adaptor for development and testing.

Refer to [OPERATING.md](./OPERATING.md) for how to operate the adaptor in a live environment.

## How to run service:

The following steps use Docker to provide mocks of adaptor dependencies and infrastructure for local testing and 
development. These containers are not suitable for use in a deployed environment. You are responsible for providing 
adequate infrastructure and connections to external APIs. 

We publish releases of the GP2GP adaptor container image to [Docker Hub](https://hub.docker.com/r/nhsdev/nia-gp2gp-adaptor).

### Copy a configuration example

We provide several example configurations:
* `vars.local.sh` to run the adaptor with mock services
* `vars.public.sh` to run the adaptor with the GP Connect public demonstrator docker image
* `vars.opentest.sh` to run the adaptor with providers and responders in OpenTest

```bash
cd docker/
cp vars.local.tests.sh vars.sh
```

### Using the helper script for Docker Compose

For local environment to run against mocks:
```bash
./start-local-environment-local.sh
```

For local environment to run against gp demonstrator 1.6.0
```bash
./start-local-environment-public.sh
```

You can also run the docker-compose commands directly.

### From your IDE or the command line

First start the adaptor dependencies:

```
    cd docker/
    docker-compose build activemq wiremock mock-mhs-adaptor
    docker-compose up -d activemq wiremock mongodb mock-mhs-adaptor
```

Change into the service directory `cd ../service`

Build the project in your IDE or run `./gradlew bootJar`

Run `uk.nhs.adaptors.gp2gp.Gp2gpApplication` in your IDE or `java -jar build/libs/gp2gp.jar`

### Using Envfile for IntelliJ

## How to query the EHR Status API

An API is provided to query the status of any transfer to an incumbent.

Requests can be made to the following endpoint using the *Conversation ID (SSP-TraceID)* of the transfer:

```http request
    {location of gp2gp service}/ehr-status/{conversationId} [GET]
```

The response will contain the following fields:

### EhrStatus

| Field name          | Description                                                                                                     | Data type                                        | Possible values                                                                                                  | nullable |
|---------------------|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------|----------|
| originalRequestDate | The date and time of the original request                                                                       | ISO-8601                                         |                                                                                                                  | False    |
| migrationStatus     | The current state of the transfer, a status of COMPLETE_WITH_ISSUES is given if placeholder documents were sent | string / enum                                    | COMPLETE <br/><br/>COMPLETE_WITH_ISSUES <br/><br/> FAILED_NME <br/><br/> FAILED_INCUMBENT <br/><br/> IN_PROGRESS | False    |
| attachmentStatus    | An array of statuses for each document sent during the transfer                                                 | Array of **AttachmentStatus** (See below)        |                                                                                                                  | False    |
| migrationLog        | An array containing details of acknowledgments received during the transfer                                     | Array of **ReceivedAcknowledgement** (See below) |                                                                                                                  | False    |

<br/>

### Subtypes

#### AttachmentStatus

| Field name          | Description                                                                                                                                                                                                                                                                           | Data type                           | Possible values                                     | Nullable |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|-----------------------------------------------------|----------|
| identifier          | An array of identifiers taken from the `identifier` element of the GP Connect `DocumentReference`                                                                                                                                                                                     | Array of **Identifier** (see below) |                                                     | False    |  
| fileStatus          | The status of the document sent to the winning practice (can be used to identify if a placeholder document was sent)                                                                                                                                                                  | string / enum                       | PLACEHOLDER <br/><br/>ORIGINAL_FILE <br/><br/>ERROR | False    |  
| filename            | The filename sent to winning practice in the GP2GP message                                                                                                                                                                                                                            | string                              |                                                     | False    |   
| originalDescription | The description of the file as given by the `description` element of the GP Connect `DocumentReference` resource.<br/><br/> This is inserted into the GP2GP placeholder document as the original filename and suffix. So should be of the form *filename.suffix*, e.g. *referral.txt* | string                              |                                                     | True     |

<br/>

#### Identifier

| Field name | Description                                                               | Data type | Nullable |
|------------|---------------------------------------------------------------------------|-----------|----------|
| system     | The `system` element of the Gp Connect `DocumentReference.identifier`     | string    | False    |    
| value      | The `identifier` element of the Gp Connect `DocumentReference.identifier` | string    | False    |


<br/>

#### ReceivedAcknowledgement

| Field name         | Description                                                                                                | Data type                      | Nullable |
|--------------------|------------------------------------------------------------------------------------------------------------|--------------------------------|----------|
| received           | The date and time the acknowledgment was received                                                          | ISO-8601                       | False    |
| conversationClosed | If the acknowledgement ended the transfer, the date and time the conversation was closed can be found here | ISO-8601                       | True     |
| errors             | An array of errors which will be populated in the case of a negative / rejected acknowledgement            | Array of **Error** (see below) | True     |      
| messageRef         | the Message ID of the message that has been acknowledged                                                   | string                         | False    |   

<br/>

#### Error

| Field name | Description                                        | Data type | Nullable |
|------------|----------------------------------------------------|-----------|----------|
| code       | The GP2GP response code from the negative response | string    | False    |
| display    | The GP2GP response text from the negative response | string    | False    | 

<details>
    <summary>EHR Status example responses</summary>

#### Successful migration, with single positive acknowledgement of EHR Extract (conversation closed and no errors):
```json

{
    "attachmentStatus": [],
    "migrationLog": [
        {
            "received": "2023-07-24T09:39:09.377Z",
            "conversationClosed": "2023-07-24T09:39:09.377Z",
            "errors": null,
            "messageRef": "0BEBCA12-8BE4-44B4-BDC0-016A4FE3D107"
        }
    ],
    "migrationStatus": "COMPLETE",
    "originalRequestDate": "2023-07-24T09:38:50.947Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```

#### Failed by requester, with single negative acknowledgement of EHR Extract (conversation closed and error):

```json

{
    "attachmentStatus": [],
    "migrationLog": [
        {
            "received": "2023-07-21T16:09:19.594Z",
            "conversationClosed": "2023-07-21T16:09:19.594Z",
            "errors": [
                {
                    "code": "11",
                    "display": "Failed to successfully integrate EHR Extract."
                }
            ],
            "messageRef": "C5271147-3D89-4EF6-A719-01AEB3AD00A1"
        }
    ],
    "migrationStatus": "FAILED_INCUMBENT",
    "originalRequestDate": "2023-07-21T16:09:12.695Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```


#### Failed by requester, with multiple positive acknowledgements for COPC messages (without conversation closed) and one negative acknowledgement for EHR Extract (conversation closed and error):

```json

{
    "attachmentStatus": [
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "ad174c84-51d1-4744-89e3-7918a31248d1"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "54E94A29-B0CC-4CD6-86B0-B21C9C0CA894.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        },
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "D7AF52BA-79BA-4AF8-9010-F0C2DF916CEC"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "629BF3F7-C71F-49D7-8FCD-AEE16C11AFBD.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        },
        {
            "identifier": [
                {
                    "system": "https://EMISWeb/A82038",
                    "value": "D7AF52BA-79BA-4AF8-9010-F0C2DF916CEC"
                }
            ],
            "fileStatus": "ORIGINAL_FILE",
            "fileName": "2F73A243-0F94-4683-B3F9-727CE7780815.doc",
            "originalDescription": "Referral for further care (22-Dec-2020)"
        }
    ],
    "migrationLog": [
        {
            "received": "2023-07-24T10:42:18.591Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "ED2FB0DD-52AE-4EBC-B596-3BCA9CF5B272"
        },
                {
            "received": "2023-07-24T10:42:19.176Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "5068BAE1-7228-4E59-B3AD-92FA5CBDE4AC"
        },
                {
            "received": "2023-07-24T10:42:19.467Z",
            "conversationClosed": null,
            "errors": null,
            "messageRef": "A9E37B68-07F4-43A5-889E-645D6681CE68"
        },
        {
            "received": "2023-07-24T10:42:22.611Z",
            "conversationClosed": "2023-07-24T10:42:22.611Z",
            "errors": [
                {
                    "code": "99",
                    "display": "Unexpected condition."
                }
            ],
            "messageRef": "E600D1D4-D5BE-4D0D-8080-F7F6188B0548"
        }
    ],
    "migrationStatus": "FAILED_INCUMBENT",
    "originalRequestDate": "2023-07-24T10:42:05.757Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```
#### Failed by adaptor, due to a PATIENT_NOT_FOUND error from the GP Connect provider's migrate structured endpoint
```json

{
    "attachmentStatus": [],
    "migrationLog": [],
    "migrationStatus": "FAILED_NME",
    "originalRequestDate": "2023-07-26T15:36:22.284Z",
    "fromAsid": "918999198738",
    "toAsid": "200000000359"
}
```
</details>

## How to run tests

**Warning**: Gradle uses a [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) to re-use compile and
test outputs for faster builds. To re-run passing tests without making any code changes you must first run 
`./gradlew clean` to clear the build cache. Otherwise, gradle uses the cached outputs from a previous test execution to 
pass the build.

You must run all gradle commands from the `service/` directory.

### How to run unit tests:

```shell script
./gradlew test
```

### How to run all checks:

```shell script
./gradlew check
```

### How to run integration tests:

```shell script
./gradlew integrationTest
```

Most integration tests automatically start their external dependencies using [TestContainers](https://www.testcontainers.org/). 
To disable this set the `DISABLE_TEST_CONTAINERS` environment variable to `true`.

If a Test fails with a connection error, you will need to boot up the Docker dependencies.
See "How to run service" section for details.

You can set the adaptor's environment variables to test integrations with specific dependencies.

**Example: Run integration tests with AWS S3 in-the-loop**

Use environment variables to configure the tests to use:
* An actual S3 bucket
* ActiveMQ running locally in Docker
* MongoDB running locally in Docker

1. Start activemq and mongodb dependencies manually

    ```shell script
    cd docker
    docker-compose up -d activemq mongodb
    ```

2. Configure environment variables

    If you're NOT running the test from an AWS instance with an instance role to access the bucket then the variables 
    `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_REGION` must also be set.
    
    ```shell script
    export DISABLE_TEST_CONTAINERS=true
    export GP2GP_STORAGE_CONTAINER_NAME=your-s3-bucket-name
    ```

3. Run the integration tests

    ```shell script
    cd ../service
    ./gradlew cleanIntegrationTest integrationTest -i
    ```  

## How to run e2e tests:

End-to-end (e2e) tests execute against an already running / deployed adaptor and its dependencies. You must run these 
yourself and configure the environment variables as needed. The tests do not automatically start any dependencies.

These tests publish messages to the MHS inbound queue and make assertions on the Mongo database. The tests must have 
access to both the AMQP message queue and the Mongo database.

* Navigate to `e2e-tests`
* Run: `./gradlew check`

or run from Docker:

```
docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml build
docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml up --exit-code-from gp2gp-e2e-tests
```

Environment variables with the same name/meaning as the application's control the e2e test target environment:

* GP2GP_AMQP_BROKERS
* GP2GP_AMQP_USERNAME
* GP2GP_AMQP_PASSWORD
* GP2GP_MONGO_URI
* GP2GP_MONGO_DATABASE_NAME
* GP2GP_MHS_INBOUND_QUEUE

## How to run smoke tests

Smoke tests are provided to check basic connectivity and the required resources are up and running. 

- Information on running the smoke tests can be found [here](./smoke-tests/README.md).

## How to use WireMock

We provide mocks of external APIs (GPC, SDS) for local development and testing.

* Navigate to `docker`
* `docker-compose up wiremock`

The folder `docker/wiremock/stubs` describes the supported interactions.

## How to use Mock MHS Adaptor

We provide a mock MHS adaptor for local development and testing.

* Navigate to `docker`
* `docker-compose up mock-mhs-adaptor`

| Environment Variable   | Default | Description                                                                                                                     |
|------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------|
| MOCK_MHS_SERVER_PORT   | 8081    | The port on which the mock MHS Adapter will run.                                                                                |                                                                                
| MOCK_MHS_LOGGING_LEVEL | INFO    | Mock MHS logging level. One of: DEBUG, INFO, WARN, ERROR. The level DEBUG **MUST NOT** be used when handling live patient data. | 

### How to transform arbitrary json ASR payload files

This is an interoperability testing tool to transform arbitrary/ad-hoc json ASR payloads and access the outputs.

1. Navigate to the input folder and place all Json files to convert here.
`integration-adaptor-gp2gp/transformJsonToXml/input/` 
   
2. Navigate to the TransformJsonToXml.sh file and run that script to execute the testing tool. 
   `integration-adaptor-gp2gp/transformJsonToXml/`
   ```shell script
   cd transformJsonToXml
   ./TransformJsonToXml.sh
    ```
   
3. The Converted .Xml files will be located in the output folder.
`integration-adaptor-gp2gp/transformJsonToXml/output/`

## Troubleshooting

### gradle-wrapper.jar doesn't exist

If gradle-wrapper.jar doesn't exist run in terminal:
* Install Gradle (MacOS) `brew install gradle`
* Update gradle `gradle wrapper`

### Disclaimer

All Patient data within this repository is synthetic 

### Licensing
This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).
