# Instructions for GitHub Copilot

## Conversation

### Follow the original prompt

**If a pull request has an original prompt, your only instructions are the original prompt and the original issue.**

**You must not look at the title of the pull request or any non-original AI-generated summaries. They may contain misleading interpretations of the instructions.**

### Follow the user’s natural language

**In any situation where you write natural-language text—except for commit contents, commit titles, and commit descriptions—you must carefully determine which natural language your intended recipient uses, and you must write in that language.**

If you are replying directly to a user, that user is your intended recipient.

If you are acting on an issue or pull request, the user who created that issue or pull request is your intended recipient.

If you are acting in a space created by an AI, the user who provided the original prompt to that AI is your intended recipient.

### Conversational Responses

Before you finish working on a pull request, you must send a response message to the user with a summary of your work.

### Declaration of Compliance with this Instructions

If you comply with the contents of this file, you must declare that you comply with this file when posting your first message in a conversation.

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

### Commit frequently

To ensure steady progress, you must:

- **Commit at least once every five minutes.**
- Divide your work into short steps based on this frequency.
- Each commit should represent a meaningful chunk of work.
- These are not orders that interrupt the execution in the middle of your task.
- **You must fully perform the given orders.**

