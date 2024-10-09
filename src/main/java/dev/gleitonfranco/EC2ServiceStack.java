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
    private Instance instance;
    private static final String USER = "ec2-user";
    private static final String HOME = "/home/" + USER;
    private static final String TO_RC = ">> " + HOME + "/.bashrc";


    public EC2ServiceStack(final Construct scope, final String id) {
        this(scope, id, null, null, null);
    }

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
        autenticacao.put("SPRING_DATASOURCE_URL", String.format("jdbc:postgres://%s/%s"
                , databaseEndpoint
                , databaseName));
        autenticacao.put("SPRING_DATASOURCE_USERNAME", "postgres");
        autenticacao.put("SPRING_DATASOURCE_PASSWORD", databasePassword);

        this.instance = Instance.Builder.create(this, "EC2Linux")
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
        instance.addUserData(String.format("aws s3 cp %s %s/project.jar"
                , jarS3Arn
                , HOME));
        instance.addUserData(String.format("aws s3 cp %s %s/import.sql", sqlS3Arn, HOME));
        instance.addUserData(String.format("chown %s %s/marques_create_db.jar", USER, HOME));
        instance.addUserData(String.format("chown %s %s/import.sql", USER, HOME));
        instance.addUserData(this.toRC("export SPRING_DATASOURCE_USERNAME=postgres"));
        instance.addUserData(this.toRC("export SPRING_DATASOURCE_PASSWORD=" + databasePassword));
        instance.addUserData(this.toRC(String.format("export SPRING_DATASOURCE_URL=jdbc:postgresql://%s/%s"
                , databaseEndpoint
                , databaseName)));
        instance.addUserData(String.format("source %s/.bashrc", HOME));
        instance.addUserData(String.format("PGPASSWORD=%s psql -h %s -p 5432 -U postgres -c \"CREATE DATABASE %s;\""
                , databasePassword
                , databaseEndpoint
                , databaseName));
        instance.addUserData(
                String.format(
                        "java -cp %s/project.jar -Dloader.main=br.com.javerde.marques.config.FlywayMigrationRunner org.springframework.boot.loader.PropertiesLauncher", HOME));
        instance.addUserData(String.format("PGPASSWORD=%s psql -h %s -p 5432 -d %s -U postgres -f %s/import.sql"
                , databasePassword
                , databaseEndpoint
                , databaseName
                , HOME));
        instance.addUserData(String.format("java -jar %s/project.jar", HOME));
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

    public Instance getInstance() {
        return instance;
    }
}
