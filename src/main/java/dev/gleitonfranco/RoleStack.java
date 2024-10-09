package dev.gleitonfranco;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class RoleStack extends Stack {
    private Role role;
    public RoleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public RoleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.role = Role.Builder.create(this, id).assumedBy(new ServicePrincipal("ec2.amazonaws.com")).build();
        this.role.addToPolicy(PolicyStatement.Builder.create()
                .resources(List.of("*"))
                .effect(Effect.ALLOW)
                .actions(List.of("s3:*", "s3-object-lambda:*", "s3:GetObject", "secretsmanager:*"))
                .build());

        this.role.addToPolicy(PolicyStatement.Builder.create()
                .resources(List.of("*"))
                .effect(Effect.ALLOW)
                .actions(List.of("secretsmanager:*"))
                .build());


    }

    public Role getRole() {
        return role;
    }
}
