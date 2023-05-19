# SSL Service

💥💥💥 Read this before reading the ssl-client readme 💥💥💥  

*ssl-service and ssl-client were created in VS code using the **Spring Initializr Java Support** extension.*  
*Make sure your terminal is in src/main/resources of this project: `$ cd src/main/resources`*

## Purpose: To understand the service-side of SSL

1. To create a root certificate authority (rootCA).  
1. To create a certificate signed with the rootCA and configure *ssl-service* to use it.  
1. To configure your web-browser to trust the rootCA.  
1. To configure *ssl-service* to trust only clients that use a certificate signed by the rootCA.  
1. To create a client certificate signed by the rootCA.  
1. To configure the web browser to use this client certificate.  
1. After completing this guide: give the client certificate to *ssl-client*.

## About ssl-service

ssl-service is an API with one endpoint that multiplies a given request parameter by 2.  
You can run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
You can visit [the site](http://localhost:8081/request?value=14) in a browser.  

## Create a Certificate Authority (CA)

`$ openssl req -x509 -sha256 -days 3650 -newkey rsa:4096 -keyout rootCA.key -out rootCA.crt`  

## Create a certificate for ssl-server  

### Create service certificate signing request (CSR)

`$ openssl req -new -newkey rsa:4096 -keyout service.key -out service.csr`

### Create localhost.ext (additional configuration)

``` bash
#create localhost.ext
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
```

### Sign service request with rootCA

`$ openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in service.csr -out service.crt -days 365 -CAcreateserial -extfile localhost.ext`

### Create service certificate

`$ openssl pkcs12 -export -out service.pfx -name "service" -inkey service.key -in service.crt`

### Set ssl-service's application settings

``` bash
#application.properties
server.ssl.key-store=classpath:service.pfx
server.ssl.key-store-password=password
server.ssl.key-alias=service
server.ssl.key-password=password
server.ssl.enabled=true
server.port=8081
```

## Test ssl-server's certificate

*Your web browser can be a client of ssl-server, so we can us it to test with.*  

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`. Visit [the site](https://localhost:8081/request?value=14) in a browser.  
1. Notice the warning about the connection not being private.  
**This means ssl-server is presenting a certificate that your browser does not trust.**  
1. Configure your browser to trust the certificate.  
1. Open a new tab and put `edge://settings/?search=certificates` in the address bar. Click the first link.  
1. Click `Trusted Root Certification Authorities` and import the rootCA.crt.  
1. Restart your browser and visit [the site](https://localhost:8081/request?value=14) again.  
1. Notice the warning goes away.  
**This means the browser trusts ssl-server's certificate.**  
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.

## Configure ssl-server to trust only certain clients

### Create truststore

`$ keytool -import -trustcacerts -noprompt -alias ca -ext san=dns:localhost,ip:127.0.0.1 -file rootCA.crt -keystore truststore.jks`

### Update service's application.properties with truststore settings

``` bash
#application.properties
server.ssl.trust-store=classpath:truststore.jks
server.ssl.trust-store-password=password
server.ssl.client-auth=need
```

## Test that ssl-server does not trust the browser

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
1. Visit [the site](https://localhost:8081/request?value=14) in a browser.  
1. Notice that ssl-service denies the browser's request outright(!)  
**This means ssl-server does not trust the browser.**  
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.

## Create client-side certificate

*Perhaps this belongs in the client readme, but it is here for brevity's sake.*

### Create client certificate signing request (CSR)

`$ openssl req -new -newkey rsa:4096 -nodes -keyout client.key -out client.csr`

### Sign client service request with rootCA

`$ openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in client.csr -out client.crt -days 365 -CAcreateserial`

### Create client certificate

`$ openssl pkcs12 -export -out client.pfx -name "client" -inkey client.key -in client.crt`

### Install pfx on computer

Open a windows explorer, navigate to this directory, and double-click on client.pfx to install it.  

## Test that the service now trusts the browser

1. Run the application: `[CTRL+SHFT+P] [springBootDashboard.Debug] [ENTER]`.  
1. Visit [the site](https://localhost:8081/request?value=14) in a browser.  
1. Notice that it prompts you to select the certificate you just installed from the pfx.  
1. Notice that the server accepts your request.  
**This means ssl-server now trusts the browser.**  
1. Stop the application: `[CTRL+SHFT+P] [springBootDashboard.stop] [ENTER]`.

## Give client.pfx file to ssl-client

`$ cp client.pfx ../../../../ssl-client/src/main/resources/`  

## Done with ssl-service; go to ssl-client

*Make sure ssl-service is running while testing  ssl-client*  
*Switch to ssl-client to continue*  
