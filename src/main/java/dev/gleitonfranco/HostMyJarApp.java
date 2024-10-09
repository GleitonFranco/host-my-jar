package dev.gleitonfranco;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

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

