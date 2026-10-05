# EDC Administration API Client

This repository provides a client for usage within [JAD](https://github.com/eclipse-dataspace-hub/jad), that acts
as a proxy between the JAD UI and the EDC runtimes (control plane, identity hub & issuer service). The client forwards
all requests as-is, but performs a
[token exchange](https://github.com/eclipse-dataspace-hub/jad/blob/main/docs/token-exchange.md) before doing so and
uses the exchanged token for forwarding the requests. This allows the JAD UI to use a Keycloak token to access the EDC
runtimes, which are secured by [JWTlet](https://github.com/eclipse-cfm/jwtlet).

# Build & Run

The application is designed to run as part of `JAD`. Therefore, the following instructions focus on deployment within
a Kubernetes cluster.

## Build Docker Image

To build the Docker image for the application, run the following command:

```shell
docker build -t edc-admin-api-client:<tag> .
```

## Deployment

The prepared Kubernetes manifests are targeted at running the application as part of
[JAD](https://github.com/eclipse-dataspace-hub/jad). It therefore expects the following prerequisites to be met in
the target cluster:

- an existing `edc-v` namespace
- a Gateway API installation with a gateway named `edcv-gateway` in that namespace
- a running Keycloak instance (only required at runtime)
- a running JWTlet instance (only required at runtime)

If you are running a cluster locally using e.g. `KinD` or `k3d`, build the application's Docker image and load it into
your local cluster before applying the manifests.

All manifests reside inside the `k8s/` directory. To deploy the application, run the following command:

```shell
kubectl apply -k k8s/
```

After deployment, the application will be available under `http://edc-proxy.localhost`.

## Debugging

The application's `deployment.yaml` defines a debug port on 5005. To enable remote debugging, forward the port to
your local machine before connecting your debugger: 

```shell
kubectl port-forward deployment/edc-administration-api-client <local-port>:5005
```

## Usage

As the client acts as a proxy between the JAD UI and the EDC runtimes, it is not intended to be used directly. When
the base URLs of EDC control plane, identity hub and issuer service are configured in the `application.yaml`, the client
accepts all EDC API requests under the sub-paths respective to the runtime:

- `/controlplane/**` for control plane requests
- `/identityhub/**` for identity hub requests
- `/issuerservice/**` for issuer service requests

It will validate the received token against a Keycloak instance and request a token exchange from `JWTlet`, both of
which are also configured in the `application.yaml`.
