package dev.gleitonfranco;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.List;


public class RdsStack extends Stack {
    private DatabaseInstance database;


    public RdsStack(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);
        CfnParameter password = CfnParameter.Builder.create(this, "dbPassword")
                .type("String")
                .description("Database password")
                .build();
        CfnParameter project = CfnParameter.Builder.create(this, "project")
                .type("String")
                .description("Project Name")
                .build();
        final String projectName = project.getValueAsString();
        final String databaseName = project.getValueAsString() + "db";
        final String databasePassword = password.getValueAsString();

        ISecurityGroup pgConnSecurityGroup = SecurityGroup.fromSecurityGroupId(this
                , "sg-banco-pg"
                , vpc.getVpcDefaultSecurityGroup());
        pgConnSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(5432));

        SubnetGroup subnetGroup = SubnetGroup.Builder.create(this, "SubnetGroup")
                .description("Public subnet")
                .vpc(vpc)
                .removalPolicy(RemovalPolicy.DESTROY)
                .subnetGroupName(projectName + "SubnetGroup")
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .build();

        this.database = DatabaseInstance.Builder
                .create(this, "RdsPg")
                .instanceIdentifier(projectName + "-db")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_16_4)
                        .build()))
                .vpc(vpc)
                .subnetGroup(subnetGroup)
                .credentials(Credentials.fromUsername("postgres", CredentialsFromUsernameOptions.builder()
                        .password(SecretValue.unsafePlainText(databasePassword))
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(List.of(pgConnSecurityGroup))
                .vpcSubnets(SubnetSelection.builder().subnets(vpc.getPrivateSubnets()).build())
                .build();

        CfnOutput.Builder.create(this, "project-name")
                .exportName("project-name")
                .value(projectName)
                .build();
        CfnOutput.Builder.create(this, "database-name")
                .exportName("database-name")
                .value(databaseName)
                .build();
        CfnOutput.Builder.create(this, "db-endpoint")
                .exportName("db-endpoint")
                .value(this.database.getDbInstanceEndpointAddress())
                .build();
        CfnOutput.Builder.create(this, "db-password")
                .exportName("db-password")
                .value(databasePassword)
                .build();
    }

    public DatabaseInstance getDatabase() {
        return database;
    }
}
