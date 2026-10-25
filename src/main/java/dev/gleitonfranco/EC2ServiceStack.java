package dev.gleitonfranco;

import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;


public class EC2ServiceStack extends Stack {
    private static final String USER = "ec2-user";
    private static final String HOME = "/home/" + USER;
    private static final String TO_RC = ">> " + HOME + "/.bashrc";


    public EC2ServiceStack(final Construct scope, final String id, StackProps props, Vpc vpc, Role role) {
        super(scope, id, props);

        CfnParameter jar = CfnParameter.Builder.create(this, "jars3arn")
                .type("String")
                .description("Bucket S3 ARN containing the executable jar")
                .build();
        CfnParameter sql = CfnParameter.Builder.create(this, "sqls3arn")
                .type("String")
                .description("Bucket S3 ARN containing the SQL script to generate data")
                .build();
        CfnParameter keyPairParam = CfnParameter.Builder.create(this, "keypair")
                .type("String")
                .description("Existing key pair name without .pem extension")
                .build();

        final String databasePassword = Fn.importValue("db-password");
        final String databaseEndpoint = Fn.importValue("db-endpoint");
        final String projectName = Fn.importValue("project-name");
        final String databaseName = Fn.importValue("database-name");
        final String jarS3Arn = jar.getValueAsString();
        final String sqlS3Arn = sql.getValueAsString();
        final String keyPair = keyPairParam.getValueAsString();

        Map<String, String> autenticacao = new HashMap<>();
        autenticacao.put("SPRING_DATASOURCE_URL", String.format("jdbc:postgresql://%s:5432/%s"
                , databaseEndpoint
                , databaseName));
        autenticacao.put("SPRING_DATASOURCE_USERNAME", "postgres");
        autenticacao.put("SPRING_DATASOURCE_PASSWORD", databasePassword);

        final Instance instance = Instance.Builder.create(this, "EC2Linux")
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.T2, InstanceSize.MICRO))
                .machineImage(MachineImage.latestAmazonLinux2023())
                .securityGroup(this.createSecurityGroup(vpc, projectName))
                .keyPair(KeyPair.fromKeyPairName(this, "my-keypair", keyPair))
                .role(role)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .associatePublicIpAddress(true)
                .build();

        instance.addUserData("yum update -y");
        instance.addUserData("yum install -y java-17-amazon-corretto");
        instance.addUserData("yum install -y awscli-2");
        instance.addUserData("yum install -y postgresql15");
        instance.addUserData(String.format("aws s3 cp %s %s/import.sql", sqlS3Arn, HOME));
        instance.addUserData(String.format("aws s3 cp %s %s/project.jar", jarS3Arn, HOME));
        instance.addUserData(String.format("chown %s %s/import.sql", USER, HOME));
        instance.addUserData(String.format("chown %s %s/project.jar", USER, HOME));
        instance.addUserData(this.toRC("export SPRING_DATASOURCE_USERNAME=postgres"));
        instance.addUserData(this.toRC("export SPRING_DATASOURCE_PASSWORD=" + databasePassword));
        instance.addUserData(this.toRC("curl -o global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem"));
        instance.addUserData(this.toRC(String.format("export SPRING_DATASOURCE_URL=%s", autenticacao.get("SPRING_DATASOURCE_URL"))));
        instance.addUserData(String.format("source %s/.bashrc", HOME));
        instance.addUserData(String.format(
                "PGPASSWORD=%s psql \"host=%s port=5432 user=postgres sslmode=verify-full sslrootcert=./global-bundle.pem\" -c \"CREATE DATABASE %s;\"",
                databasePassword,
                databaseEndpoint,
                databaseName));
        instance.addUserData(String.format(
                "(SPRING_DATASOURCE_URL=%s sudo -E -u ec2-user java -jar %s/project.jar) &",
                autenticacao.get("SPRING_DATASOURCE_URL"),
                HOME));
        instance.addUserData(String.format(
                "(sleep 120; PGPASSWORD=%s psql \"host=%s port=5432 user=postgres dbname=%s sslmode=verify-full sslrootcert=./global-bundle.pem\" -f %s/import.sql) &",
                databasePassword,
                databaseEndpoint,
                databaseName,
                HOME));
    }

    private ISecurityGroup createSecurityGroup(IVpc vpc, String projectName) {
        ISecurityGroup iSecurityGroup = SecurityGroup.Builder.create(this, "sg-ssh-web")
                .allowAllOutbound(true)
                .vpc(vpc)
                .description(projectName + "-sg")
                .build();
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(5000));
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22));
        iSecurityGroup.addEgressRule(Peer.anyIpv4(), Port.allTraffic(), "Internet", true);
        return iSecurityGroup;
    }

    private String toRC(String command) {
        return "echo '" + command + "' " + TO_RC;
    }

}
