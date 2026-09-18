// Registers the `runAgent` task on the project it is applied to (the root build). Kept as a
// separate convention so only the root gets the task, not every implementation module.

import duckshop.RunAgentTask

tasks.register<RunAgentTask>("runAgent") {
    group = "duck-shop"
    description = "Generate an agent artifact via an OpenAI-compatible chat API. " +
        "Params: -Pprovider=ollama|mistral|anthropic -Pmodel=<m> -Pmode=tests|verify-exercise|" +
        "verify-harden|attack|spec|spec-advanced|spec-compress|spec-extract|impl-from-spec " +
        "[-Pagent=<name>] [-Pdry]. -Pmode is required, and `impl` was removed with the " +
        "opening-schedule exercise."

    // Names no task, because this plugin on its own registers none that could follow. The builds that
    // do override it: duck-shop.spec points at verifyDivergence, the capstone at its own step.
    implNextStep.convention(
        "put it in place of the implementation under test, then run your own tests against it",
    )
}
