package dev.gleitonfranco;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class HostMyJarApp {
    public static void main(final String[] args) {
        App app = new App();

        StackProps stackProps = StackProps.builder().env(Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build()
        ).build();

        VpcStack vpcStack = new VpcStack(app, "VpcStack", stackProps);

        RdsStack rdsStack = new RdsStack(app
                , "RdsStack"
                , stackProps
                , vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        var service = new EC2ServiceStack(app
                , "EC2Stack"
                , stackProps
                , vpcStack.getVpc()
                , new RoleStack(app, "Role", stackProps).getRole()
        );
        service.addDependency(rdsStack);

        app.synth();    }
}
// COMMAND LINE EXAMPLES:
// cdk deploy --parameters RdsStack:project=marques2 --parameters RdsStack:dbPassword=postgres --parameters EC2Stack:jars3arn=s3://marquesjars/marques2.jar --parameters EC2Stack:sqls3arn=s3://marquesjars/import.sql --parameters EC2Stack:keypair=marqueskey --require-approval never --all
// cdk destroy --force --all
