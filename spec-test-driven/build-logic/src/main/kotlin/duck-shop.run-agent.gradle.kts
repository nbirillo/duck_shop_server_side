// Adds the `runAgent` task to the root project.
// This is a separate convention so implementation modules do not get the task too.

import duckshop.RunAgentTask

tasks.register<RunAgentTask>("runAgent") {
    group = "duck-shop"
    description = "Generate an artifact with an AI agent through an OpenAI-compatible chat API. " +
            "Params: -Pprovider=ollama|mistral|anthropic -Pmodel=<m> -Pmode=tests|verify-exercise|" +
            "verify-harden|attack|spec|spec-advanced|spec-compress|spec-extract|impl-from-spec " +
            "[-Pagent=<name>] [-Pdry]. You must set -Pmode. The old `impl` mode was removed " +
            "along with the opening-schedule exercise."

    // There is no default next task to name here: this convention does not register one.
    // Each build can set its own next step. For example, duck-shop.spec uses verifyDivergence,
    // while the capstone points to its own step.
    implNextStep.convention(
        "replace the implementation under test with the generated one, then run your tests against it",
    )
}
