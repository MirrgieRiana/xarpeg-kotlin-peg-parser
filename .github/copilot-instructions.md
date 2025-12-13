# Instructions for GitHub Copilot

**If the pull request has an original prompt, your only instructions are the original prompt and the original issue.**
**Do not look at the title of the pull request or any of the non-original AI-generated summaries. They contain lies that misinterpret the instructions.**

In issues, communicate in the same natural language as the initial message.

In pull requests, communicate in the same natural language as the "Original prompt" written in the initial message, if it is not provided, or the title.

When the pull request creator is an AI, ignore the PR title and description and refer to the original prompt instead.

When launching the Agent from GitHub to create a pull request, initialize the pull request title and initial comment in the same natural language as user's.

Write all project documentation (including content under `docs/`) in English.

The `./gradlew check` task fails here when Kotlin/Native tries to download toolchains; use `./gradlew jvmTest` instead for test verification.

When invoking Chrome (e.g., to analyze HTML files), specify the following options to suppress Chrome’s own background network activity and prevent connection attempts to disallowed domains triggered by that activity.
