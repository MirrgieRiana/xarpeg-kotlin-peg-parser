# General Rules

## Conversation

### Follow the original prompt

**If a pull request has an original prompt, your only instructions are the original prompt and the original issue.**

**You must not look at the title of the pull request or any non-original AI-generated summaries. They may contain misleading interpretations of the instructions.**

### Follow the User’s Natural Language

When interacting with people on issues or pull requests, you must always write your natural-language text in the natural-language that person normally uses.

Scope

- This includes comments, replies, titles, descriptions, progress updates, and any other natural-language text on issues or pull requests.
- However, the contents of committed files and commit metadata such as commit titles and descriptions are artifacts rather than communication, so they are out of scope for this rule.

Who to Follow

- When replying to someone, you must follow the natural-language of the user you are replying to.
- When acting in a space managed by a human, you must follow that person’s natural-language.
- When acting in a space managed by an AI, you must follow the natural-language of the human who provided the prompt to that AI.

If you find existing natural-language text that violates this rule, you must fix it immediately. In particular, you must keep in mind that a AI ​​will always initialize the pull request title and description in English ignoring the natural-language of the original prompt and the original issue.

### Conversational Responses

Before you finish working on a pull request, you must explain the your work in the pull request.

### Declaration of Compliance with this Instructions

If you comply with the contents of this file, you must declare that you comply with this file when posting your first message in a conversation.

## Edit

## Consistency of Writing Style

Whenever you edit any text in this repository, you must first look for structurally similar sections within the repository and make a best effort to keep the style consistent—code formatting, layout, phrasing, and any other aspects of the writing.

## Execution

### Commit frequently

To ensure steady progress, you must:

- **Commit at least once every five minutes.**
- Divide your work into short steps based on this frequency.
- Each commit should represent a meaningful chunk of work.
- These are not orders that interrupt the execution in the middle of your task.
- **You must fully perform the given orders.**

# Repository Instructions

## Edit

### Edit Integrity

When you edit a pull request to merge it into the main branch, you must ensure that it passes the `check` workflow.

### Write documents in English

You must write all project documentation (such as `README.md` and the contents under `docs/`) in English.

### Keep Code, Tests, and Documentation Synchronized

When making changes to the codebase, you must always ensure that code, tests, and documentation are edited correspondingly:

* If you modify code behavior, update the related tests to reflect the changes
* If you add or change functionality, update the relevant documentation
* If you update documentation, ensure the code and tests match the documented behavior
* Never leave code, tests, and documentation in an inconsistent state

### Documentation Code Examples

When writing code examples in documentation (README.md, pages/docs/*.md):

* **Never use `println()` in sample code** - Output statements provide no validation
* **Always use assertions (`check()`, `require()`)** - These verify correctness and will fail if expectations are not met
* **Sample code must be contextually relevant** - Each example should reinforce the explanation it accompanies
* **All code examples must pass doc-test validation** - Invalid examples damage credibility

Good example:

```kotlin
val result = parser.parseAllOrThrow("input")
check(result == expectedValue)  // Validates the result
```

Bad example:

```kotlin
val result = parser.parseAllOrThrow("input")
println(result)  // Provides no validation
```

## Execution

### Gradle Build

This repository has multiple Gradle build configurations.

You must understand that tasks must be executed in the Gradle build configuration that contains the code you are editing.

### Domain blocking

When you invoke Chrome (for example, to analyze HTML files), specify the following options to suppress Chrome's own background network activity and prevent connection attempts to disallowed domains:

```
--disable-background-networking
--disable-component-extensions-with-background-pages
--disable-component-update
```

---

If you encounter an error while running a Gradle task that says access to a specific domain is blocked, your correct response is to suggest adding an allowance for that domain via chat, by providing the following URL:

https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/settings/copilot/coding_agent/allowlist
