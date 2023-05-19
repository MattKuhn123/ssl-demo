# SSL Client

💥💥💥 Read ssl-service readme before reading this 💥💥💥  

*ssl-service and ssl-client were created in VS code using the **Spring Initializr Java Support** extension.*  
*Make sure your terminal is in src/main/resources of this project: `$ cd src/main/resources`*  
*Make sure ssl-service is running while testing ssl-client*

## Purpose: To understand the client-side of SSL, and to understand how to use a certificate stored in Azure's KeyVault to make SSL requests

1. To notice that ssl-client cannot make requests to ssl-service because it is secured by SSL.  
1. To move the pfx certificate (given by ssl-service) securely into the azure key vault.  
1. To configure ssl-client to use the certificate from azure key vault.  

## About ssl-client

ssl-client is a simple website that has an input field and a button to send a request to the back-end, which makes a rest call to an external service API.  
You can run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
You can visit [the site](http://localhost:8080/) in a browser. *Note that if ssl-service has already been updated to use ssl, then ssl-client will not work.*

## Test that ssl-service does not trust ssl-client

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
    - *Please not that ssl-service must also be running.*
1. Visit [the site](http://localhost:8080/) in a browser.  
1. Enter a number into the input box and click the button.  
1. Notice an error: `This combination of host and port requires TLS`  
**This means ssl-server requires us to use https**
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.  
1. Update application.properties to use https:

    ``` bash
    #application.properties
    service.url=https://localhost:8081/
    ```

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
    - *Please not that ssl-service must also be running.*  
1. Notice an error: `unable to find valid certification path to requested target`.  
**This means ssl-server does not trust the certificate from ssl-client (or that ssl-client is not using a certificate).**  
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.  

## Receive pfx file from ssl-service

The service has created and shared client.pfx with ssl-client.  

## Get or create an active directory service principle in Azure

*ssl-client needs a service principle to run as in order to use the vault.*  

1. For this demo, we will use an existing user named REVA(D)
    - *In the TVA, developers do not have access to create service principles.*
1. Update .vscode/launch.json with the clientSecret, clientId, and tenantId of your service principle.

    ``` json
    //.vscode/launch.json
    {
        "configurations": [
            {
                "type": "java",
                "name": "Spring Boot-SSLClientApplication<ssl-client>",
                "request": "launch",
                "cwd": "${workspaceFolder}",
                "mainClass": "kuhn.example.sslclient.SSLClientApplication",
                "projectName": "ssl-client",
                // TODO : update vmArgs
                "vmArgs":"-Dazure.clientSecret= -Dazure.clientId -Dazure.tenantId="
                "envFile": "${workspaceFolder}/.env"
            }
        ]
    }   
    ```

## Create Azure Key Vault and upload pfx

1. Log in to azure account: `$ az login`
1. Set subscription: `$ az account set --subscription $SUBSCRIPTION_ID`
1. Create resource group: `$ az group create --name "dev-ssl-test" --location "East US" --tags Creator=MLKuhn Timestamp=test`
1. Create vault: `$ az keyvault create --name "dev-ssl-test-vault" --resource-group "dev-ssl-test" --location "EastUS"`
1. Upload pfx to vault: `$ az keyvault certificate import --file "client.pfx" --name "ssl-client" --vault-name "dev-ssl-test-vault" --password password`  
    - *If you receive an error from the cli, refer to [this stackoverflow answer](https://stackoverflow.com/a/68265261) for help.*
1. Give service principle access to vault: `$ az keyvault set-policy --name "dev-ssl-test-vault" --object-id <object-id-of-service-principle> --secret-permissions get --certificate-permissions get`

## Retrieve pfx from key vault in application code

### Update application.properties to use azure vault and https

``` bash
#application.properties
service.url=https://localhost:8081/
azure.vaultUrl=https://dev-ssl-test-vault.azure.net/
```

### Update SSLClientApplication constructor  

``` java
 // SSLClientApplication.java
 public SSLClientApplication(final Environment env, final KeyVaultService kv) throws Exception {
  serviceUrl = env.getProperty("service.url");
  final HttpClient httpClient = HttpClients.custom().setSSLContext(kv.getSslContext("ssl-client")).build();
  final ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
  restTemplate = new RestTemplate(requestFactory);
 }
```

## Test that ssl-server trusts ssl-client but ssl-client does not trust ssl-server

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
    - *Please not that ssl-service must also be running.*
1. Visit [the site](http://localhost:8080/) in a browser.  
1. Enter a number into the input box and click the button.  
1. Receive an error: `unable to find valid certification path to requested target`.  
**This means the java run-time does not trust ssl-server's certificate.**
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.

## Import the rootCA which we made on ssl-server to java's cacerts

`$ keytool -importcert -alias ssl_service -file ../../../../ssl-service/src/main/resources/rootCA.crt -keystore $JAVA_HOME/jre/lib/security/cacerts`  
*Note that the password it prompts you for is the password for **cacerts**, not the rootCA that you made.*  
*The password for cacerts is most like `changeit` unless you changed it*  
*Note that you may need to swap-in a file path for $JAVA_HOME*  

## Test mutual trust between ssl-client and ssl-server

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`. Visit [the site](http://localhost:8080/) in a browser.  
    - *Please not that ssl-service must also be running.*
1. Enter a number into the input box and click the button.  
1. The error about an invalid certificate chain is gone.  
**This means the java run-time trusts ssl-server's certificate's root authority.**
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.

## Cleanup

Remember to delete your new resource group.  
`$ az group delete --name "dev-ssl-test"`
