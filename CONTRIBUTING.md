# Contributing to QueryFence

Thanks for your interest!

## Build

Requires JDK 17+.

```bash
./mvnw verify
```

Format code before committing (google-java-format requires JDK 21+; the format check is skipped when building on JDK 17):

```bash
./mvnw spotless:apply
```

## Pull requests

- Open an issue first for anything larger than a small fix.
- Every change to a rule needs new cases in the golden corpus.
- Keep `queryfence-core` free of dependencies other than JSqlParser.

## Using AI tools

You are welcome to use AI assistants. The usual rules still apply, and you remain the author:

- Read, understand and test every line you submit. You must be able to explain the change in review.
- Rule changes and bug fixes need golden corpus cases, whoever or whatever wrote the code.
- Do not open bulk or automated pull requests that no human has reviewed. They will be closed.
- Mentioning AI assistance in commits or pull requests is optional.
- Never paste secrets, credentials or private customer SQL into an AI tool.
