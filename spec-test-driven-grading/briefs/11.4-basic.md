# The brief — basic

Teacher-only, and **deliberately not in the student folder.**

The business text is what a learner has to turn into a contract. If it sits in the repository, an
agent pointed at the project finds it — and then a vague specification still produces correct code,
because the agent is answering the brief instead of the learner's text. That defeats the only
measurement 11.4 has.

So this is handed out in class: on a slide, on paper, however you like. What stays in the student
folder is the signature to describe and the instructions — never the rules.

`runAgent -Pmode=spec` reads the business text from this file.

---

> Each shop keeps a list of discount rules, and they apply to a duck's shelf price.
>
> - **Percentage off** — for example, 10% off.
> - **Amount off** — for example, 5 off.
> - **Big-spender bonus** — a duck priced from 100 gets a further 20 off.
>
> Prices are whole units. The shop does not deal in fractions.
