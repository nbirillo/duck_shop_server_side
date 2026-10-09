# The Capstone — A Franchise of Duck Shops

You've worked with every part of this process separately.
Now it's time to put everything together.

## 1. The situation you're walking into

Imagine a franchise of duck shops.
Each shop has its own admission policy and promotions. The franchise also has rules that apply across the chain.
A customer brings one duck and wants to know:
**Which shop should they choose, and how much will they pay?**
That sounds straightforward. But you're not starting from scratch.
Someone has already worked on this feature.

In `inherited/`, you'll find a specification, an implementation written by an AI agent, and the tests that came with it.
You don't know whether they agree with one another.
You also don't know which decisions were made deliberately and which were simply assumed.
That's the situation you're taking over.

### What you inherited

| Path                                            | What you'll find                                                                                                              |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `inherited/SPEC.md`                             | The existing specification. Read it before changing anything.                                                                 |
| `inherited/src/main/kotlin/…/BestOffer.kt`      | The agent-written implementation. You'll **replace this file** with the implementation generated from your own specification. |
| `inherited/src/main/kotlin/…/Pricing.kt`        | The existing `priceFor` implementation. Its behavior was settled earlier in the course. **Use it; don't reopen it.**          |
| `inherited/src/test/kotlin/…/FranchiseTests.kt` | The tests that came with the inherited implementation.                                                                        |
| [`surface.md`](surface.md)                      | The existing API surface you give an agent together with **your specification**, and nothing else.                            |
| `work/src/test/kotlin/`                         | The place for **your own tests**. It starts empty.                                                                            |

The `:core` module comes from the neighboring `spec-test-driven/` project, which you already have.
There is no reference implementation, answer key, or hidden grading suite in this folder.

### A note about the inherited work

You don't have to defend any of it.
Treat it as the starting point, not as the truth.
It was produced the way real software often is: under time pressure, with several people involved and an AI agent writing some of the code.
Your job is to find out what it actually guarantees, what it leaves undecided, and what needs to change.

---

## 2. What you need to deliver

There are three deliverables.

### 1. A specification you would trust someone else to implement

You can rewrite `inherited/SPEC.md` or organize your specification into several documents if that makes it clearer.
Don't make it long just to make it look thorough.
The goal is a specification that another developer — human or AI — can use without having to guess what you meant.

### 2. A test suite that makes your decisions observable

Write tests for the behavior your specification requires.
**This is the most important deliverable.**
Throughout the module, you've seen why: a tool can tell you whether a test passes, but it can't decide which behavior your business contract should require.
That's your responsibility.
Your tests should make those decisions visible.

### 3. An implementation generated from your specification alone

Give an AI agent:

- Your specification.
- The existing API surface from [`surface.md`](surface.md).

**Nothing else.**
Don't give it your tests, the inherited implementation, the original brief, or your earlier conversation about the feature.
You want to measure what the specification communicates on its own.

### The question you'll be graded on

**Does the implementation your agent produced actually do what you meant?**
If it doesn't, investigate why.
Did the agent contradict a clear requirement?
Or did your specification leave something open, allowing the agent to make a reasonable choice you didn't intend?
Be ready to explain the difference.

---

## 3. How to run the capstone

### Step 1 — Check the inherited starting point

Run the existing tests against the existing implementation:

```bash
./gradlew :inherited:test
```

This tells you where you're starting.
It doesn't prove the inherited feature is correct.
It only tells you whether the inherited tests pass against the inherited code.

### Step 2 — Write your specification

Create a Markdown file in this folder.
For the commands below, we'll call it:

`my-spec.md`

You can choose another filename.
Your specification should describe the behavior you want the new implementation to follow.

### Step 3 — Ask an agent to implement your specification

For example:

```bash
./gradlew runAgent \
    -Pmode=impl-from-spec \
    -Pagent=my-run \
    -Pspec=my-spec.md \
    -Pprovider=ollama \
    -Pmodel=qwen2.5-coder:14b
```

Here's what the arguments mean:

| Argument                    | Purpose                                                         |
|-----------------------------|-----------------------------------------------------------------|
| `-Pmode=impl-from-spec`     | Asks the agent to implement the feature from a specification.   |
| `-Pagent=my-run`            | Names this run so you can keep it separate from other attempts. |
| `-Pspec=my-spec.md`         | Selects **your specification**.                                 |
| `-Pprovider=ollama`         | Chooses the API provider.                                       |
| `-Pmodel=qwen2.5-coder:14b` | Chooses the model.                                              |

The agent receives exactly your specification and `surface.md`.
It doesn't receive the inherited code or your tests.

The generated implementation is written to:
`implementations/my-run/my-spec/src/main/kotlin/…/pricing/Pricing.kt`

### Step 4 — Replace the inherited implementation

Take the file your agent generated and use it to **replace**:
`inherited/src/main/kotlin/…/pricing/BestOffer.kt`

Overwrite the existing file.
Don't add the new implementation alongside the old one.

You can save a copy of the original somewhere else if you want to compare them later.
The point is to test the implementation produced from **your** specification, not the one you inherited.

### Step 5 — Run your own tests

Now run:

```bash
./gradlew :work:test
```

This runs your tests against whichever implementation is currently in `inherited/`.
If a test fails, investigate the disagreement.
Don't immediately change the test to match the code.
Ask whether the code violated your specification or whether your specification failed to express the behavior you expected.

### Step 6 — Compare agent runs

Run the same agent more than once before drawing conclusions about it.
Then, if possible, try a different agent using exactly the same specification.
This can be even more revealing.
When two agents make different choices, investigate what allowed that difference.
**If both implementations are reasonable readings of your text, you've found a decision your specification didn't settle.**

---

## 4. The hidden check your tests will face

There's one more evaluation tool.
**You won't run it yourself.**
We'll run it against your work and demonstrate the results in class.

Here's what it does.
It takes **two complete implementations** of the franchise feature.
Both implementations are sensible.
Both pass the tests that came with the inherited project.
But they make **different choices about one question the original business text never settled**.

The evaluator runs your test suite against both implementations.
Then it asks one question:
**Can your tests tell these two interpretations apart?**

### Possible outcomes

| Result        | What it means                                                                                                                  |
|---------------|--------------------------------------------------------------------------------------------------------------------------------|
| **SETTLED**   | Your tests accept one implementation and reject the other. You've made a decision, and that decision is visible in your tests. |
| **LEFT OPEN** | Your tests accept both implementations. The behavior is still undecided as far as your test suite is concerned.                |

### Why isn't LEFT OPEN just a bug?

Because neither implementation is necessarily wrong.
The original business text didn't settle the question.
The people who produced the inherited work didn't settle it either.
And if your tests accept both interpretations, you haven't settled it yet.
The evaluator doesn't tell you which interpretation to choose.
It tells you whether you've made that choice **explicit and testable**.

That's different from detecting an implementation defect.
A mutation test can reveal that code violates an established requirement.
It can't tell you which requirement to establish when the business text allows more than one reasonable answer.

### Why can't you see the two implementations?

Because each one is a complete, working implementation of the feature.
Giving you their source code would effectively give you our answers.
It would also make those answers available to any AI agent you point at this folder.
That would undermine the central experiment: can you write a specification and tests that express your own decisions without being shown the expected solutions?
So the implementations stay outside the student project.

---

## 5. How your work will be evaluated

**You are not graded on whether your implementation matches ours.**
When the feature leaves a genuine choice open, we don't have one uniquely correct answer.
We have our own interpretation — a choice we've made and can explain.
You can make a different choice.
What matters is whether you can show that the choice was deliberate.

We'll look at three things:

1. **Did you make the necessary decisions?** Your specification should settle the important questions rather than leave them to the implementing agent.
2. **Do your tests enforce those decisions?** The expected behavior should be observable in executable checks, not just described in prose.
3. **Can you explain what you inherited?** You should be able to distinguish deliberate requirements from assumptions that nobody actually agreed on.

A passing test suite is useful evidence.
But it isn't the whole answer.
The stronger result is being able to explain **why** those tests exist and **which decisions** they protect.

---

## The main takeaway

This capstone isn't about writing the cleverest implementation.
It's about taking responsibility for a feature that someone else started.
You inherit a specification, code, and tests. You investigate what they say, decide what the contract should guarantee, and write tests that hold the implementation to those decisions.
Then you give your specification to an agent and see what comes back.

**If the agent has to guess, that's something you need to know. If your tests can't distinguish two reasonable interpretations, that's something you need to decide.**
By the end, you should have more than working code.
You should have a specification you can hand to someone else, tests that explain what matters, and a clear account of the decisions you've made.