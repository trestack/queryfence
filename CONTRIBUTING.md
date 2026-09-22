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
