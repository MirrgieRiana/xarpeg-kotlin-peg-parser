# Instructions for GitHub Copilot

## Follow the original prompt

**If a pull request has an original prompt, your only instructions are the original prompt and the original issue.**

**You must not look at the title of the pull request or any non-original AI-generated summaries. They may contain misleading interpretations of the instructions.**

## Follow the original natural language

In issues, you must communicate in the same natural language as the initial message.

In pull requests, you must communicate in the same natural language as the "Original prompt" provided in the initial message. If there is no "Original prompt", use the natural language of the title.

When launching the Agent from GitHub to create a pull request, you must initialize the pull request title and initial comment in the same natural language as the user's message.

## Write documents in English

You must write all project documentation (such as `README.md` and the contents under `docs/`) in English.

## Domain blocking

When you invoke Chrome (for example, to analyze HTML files), specify the following options to suppress Chrome's own background network activity and prevent connection attempts to disallowed domains:

```
--disable-background-networking
--disable-component-extensions-with-background-pages
--disable-component-update
```

---

If you encounter an error while running a Gradle task that says access to a specific domain is blocked, your correct response is to suggest adding an allowance for that domain via chat, by providing the following URL:

https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/settings/copilot/coding_agent/allowlist

## Keep Code, Tests, and Documentation Synchronized

When making changes to the codebase, you must always ensure that code, tests, and documentation are edited correspondingly:

* If you modify code behavior, update the related tests to reflect the changes
* If you add or change functionality, update the relevant documentation
* If you update documentation, ensure the code and tests match the documented behavior
* Never leave code, tests, and documentation in an inconsistent state

## Commit frequently

To ensure steady progress, you must:

- **Commit at least once every five minutes.**
- Divide your work into short steps based on this frequency.
- Each commit should represent a meaningful chunk of work.

## Declaration of Compliance with this Instructions

If you comply with the contents of this file, you must declare that you comply with this file when posting your first message in a conversation.
