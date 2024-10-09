# Generic Jar Infra CDK Java project

Project using CDK development with Java.
Automation AWS Infrastructure As a Code (IaaC), for the particular scenario:
* VPS: integration of the AWS objects;
* EC2: hosting a Java 17 executable jar using Spring Boot on the port 5000;
    * Amazon Linux 2023;
    * Java 17 (Amazon Corretto);
    * AWS-CLI;
* RDS: in which Spring Boot jar's got to be connected;
    * Postgresql 15;
    * Until the moment this document was writen no further version available in Amazon Linux package repository :-(
* IAM Role: allowing the project in EC2 the following AWS communications in the cloud:
    * RDS;
    * S3;
    * Secrets Manager;
* Security Group: allowing the folowing connections:
    * Inbound:
        * SSH;
        * releasing port 5000 for access to the site;
    * Outbound:
        * Internet updates, and integration with external webservices;

## Custom commands
Environment initialization:
```
cdk bootstrap
```
Deploy this stack to your default AWS account/region:
```
cdk deploy --parameters RdsStack:project={project-name} RdsStack:dbPassword={database-password} --parameters EC2Stack:jars3arn={jars3arn} --parameters EC2Stack:sqls3arn={sqls3arn} --parameters EC2Stack:keypair={keypair} --all
```
* `{project-name}`: put the project name and database name are going to be based on this keyword;
* `{database-password}`: put database password for the user postgres;
* `{jars3arn}`: Bucket S3 ARN containing the executable jar;
* `{sqls3arn}`: Bucket S3 ARN containing the SQL script to generate data;
* `{keypair}`: Existing key pair name without .pem extension;<br>

Uninstall all together:
```
cdk destroy --all
``` 
Example:
```
cdk deploy --parameters RdsStack:project=marques --parameters RdsStack:dbPassword=postgres --parameters EC2Stack:jars3arn=s3://marquesjars/marques_create_db.jar --parameters EC2Stack:sqls3arn=s3://marquesjars/import.sql --parameters EC2Stack:keypair=sshlinux --all
```
will generate:<br>
* RDS named marques-db, with inner database named marquesdb, for user postgres and password postgres;
* EC2 named EC2Linux, hosting an executable jar named project.jar;


## Original SDK reference
It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.
* `cdk.json` file tells the CDK Toolkit how to execute your app.
* `mvn compile -q`  compile and first check
* `mvn package`     compile and run tests
* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation

