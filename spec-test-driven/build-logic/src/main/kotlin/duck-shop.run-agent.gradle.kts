// Registers the `runAgent` task on the project it is applied to (the root build). Kept as a
// separate convention so only the root gets the task, not every implementation module.

import duckshop.RunAgentTask

tasks.register<RunAgentTask>("runAgent") {
    group = "duck-shop"
    description = "Generate a solutions/<agent>/ implementation via an OpenAI-compatible chat " +
        "API. Params: -Pprovider=ollama|mistral -Pmodel=<m> [-Pagent=<name>] [-Pdry]."
}
