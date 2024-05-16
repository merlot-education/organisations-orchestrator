# MERLOT Organisations Orchestrator
The Organisations Orchestrator is a microservice in the MERLOT marketplace
which handles all participant (organisation) related information as well as
onboarding of new participants.

Internally, this service wraps self-description communication with the [XFSC Federated Catalog](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/tree/1.0.1?ref_type=tags) and [XFSC Self-Description Wizard](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-api)
while also augmenting the self-descriptions with some MERLOT-internal metadata such as organisation roles and
parameters needed for a later data transfer.

## Development

To start development for the MERLOT marketplace, please refer to [this document](https://github.com/merlot-education/.github/blob/main/Docs/DevEnv.md)
to set up a local WSL development environment of all relevant services.
This is by far the easiest way to get everything up and running locally.

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

REST API related models such as the DTOs can be found at [models-lib](https://github.com/merlot-education/models-lib/tree/main)
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

    export INITDATA_CONNECTORS="src/main/resources/initial-orga-connectors.json"
    export INITDATA_ORGANISATIONS="src/main/resources/organisations"
    export KEYCLOAK_CLIENTSECRET="mysecret"
    java -jar target/organisations-orchestrator-X.Y.Z.jar

The INITDATA_* specify which data to use to seed the organisation database of MERLOT.

The KEYCLOAK_CLIENTSECRET corresponds to the client secret that is configured for the XFSC Federated Catalogue Realm in your Keycloak instance.

Replace the X.Y.Z with the respective version of the service.

## Deploy (Docker)

This microservice can be deployed as part of the full MERLOT docker stack at
[localdeployment](https://github.com/merlot-education/localdeployment).

## Deploy (Helm)
### Prerequisites
Before you begin, ensure you have Helm installed and configured to the desired Kubernetes cluster.

### Setting Up Minikube (if needed)
If you don't have a Kubernetes cluster set up, you can use Minikube for local development. Follow these steps to set up Minikube:

1. **Install Minikube:**
   Follow the instructions [here](https://minikube.sigs.k8s.io/docs/start/) to install Minikube on your machine.

2. **Start Minikube:**
   Start Minikube using the following command:
   ```
   minikube start
   ```
3. **Verify Minikube Status:**
   Check the status of Minikube to ensure it's running:   
   ```
   minikube status
   ```

### Usage
1. **Clone the Repository:**
   Clone the repository containing the Helm chart:
   ```
   git clone https://github.com/merlot-education/gitops.git
   ```

2. **Navigate to the Helm Chart:**
   Change into the directory of the Helm chart:
   ```
   cd gitops/charts/orchestrator
   ```

3. **Customize Values (if needed):**
   If you need to customize any values, modify the values.yaml file in this directory according to your requirements. This file contains configurable parameters such as image repository, tag, service ports, etc. An example containing the values used in Merlot dev environment is available in gitops/environments/dev/organisations-orchestrator.yaml

4. **Install the Chart:**
   Run the following command to install the chart from the local repository:
   ```
   helm install [RELEASE_NAME] .
   ```
   Replace [RELEASE_NAME] with the name you want to give to this deployment. In this case it can be organisations-orchestrator.

5. **Verify Deployment:**
   Check the status of your deployment using the following commands:
   ```
   kubectl get pods
   kubectl get services
   ```

### Additional Resources 
- [Helm Documentation](https://helm.sh/docs/)
