# MERLOT Organisations Orchestrator
The Organisations Orchestrator is a microservice in the MERLOT marketplace
which handles all participant (organisation) related information as well as
onboarding of new participants.

Internally, this service wraps self-description communication with the [XFSC Federated Catalog](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/tree/1.0.1?ref_type=tags)
while also augmenting the self-description with some MERLOT-internal metadata such as organisation roles and
parameters needed for a later data transfer.

## Structure

```
├── src/main/java/eu/merloteducation/organisationsorchestrator
│   ├── config          # configuration-related components
│   ├── controller      # external REST API controllers
│   ├── mappers         # mappers for DTOs
│   ├── models          # internal data models of participant-related data
│   ├── repositories    # DAOs for accessing the stored data
│   ├── service         # internal services for processing data from the controller layer
```

API related models such as the DTOs can be found at [models-lib](https://github.com/merlot-education/models-lib/tree/main)
which is shared amongst the microservices.

## Dependencies
- A properly set-up keycloak instance (quay.io/keycloak/keycloak:20.0.5)
- [XFSC Federated Catalogue](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/tree/1.0.1?ref_type=tags)
- [XFSC Self-Description Wizard](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-api)
- rabbitmq (rabbitmq:3-management)

## Build

To build this microservice you need to provide a GitHub read-only token in order to be able to fetch maven packages from 
GitHub. You can create this token at https://github.com/settings/tokens with at least the scope "read:packages".
Then set up your ~/.m2/settings.xml file as follows:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

        <servers>
            <server>
                <id>github</id>
                <username>REPLACEME_GITHUB_USER</username>
                <!-- Public token with `read:packages` scope -->
                <password>REPLACEME_GITHUB_TOKEN</password>
            </server>
        </servers>
    </settings>

Afterward you can build the service with

    mvn clean package

## Run

    INITDATA_CONNECTORS="src/main/resources/initial-orga-connectors.json" INITDATA_ORGANISATIONS="src/main/resources/organisations" KEYCLOAK_CLIENTSECRET="mysecret" java -jar target/organisations-orchestrator-X.Y.Z.jar

## Deploy (Docker)

This microservice can be deployed as part of the full MERLOT docker stack at
[localdeployment](https://github.com/merlot-education/localdeployment).

## Deploy (Helm)
TODO