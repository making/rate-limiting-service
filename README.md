# rate-limiting-service

Rate Limiting Cloud Foundry Route Service.


## Deploy to Pivotal Web Services

```
./mvnw clean package -DskipTests=true
cf push
```

## Create Route Service

```
cf create-user-provided-service rate-limiting-service -r https://rate-limiting-service.cfapps.io
```

## Bind Route Service

```
cf bind-route-service cfapps.io rate-limiting-service --hostname <your-subdomain>
```