# Upstream issues

Reports we send to the projects QueryFence depends on. Each file is a ready-to-paste issue: a
minimal statement that reproduces the problem, the version we saw it on, what we expected and what
happened, and why it matters for QueryFence.

| File | Project | Status |
|---|---|---|
| `jsqlparser-json-table.md` | [JSqlParser](https://github.com/JSQLParser/JSqlParser) | investigated, no bug — do not file |
| `jsqlparser-unqualified-column-named-number.md` | [JSqlParser](https://github.com/JSQLParser/JSqlParser) | ready to file |

When an issue is filed, add its link to the table. When a fix is released, upgrade the dependency,
add corpus cases for the statements that used to fail, and remove the entry.

## Template

```markdown
**Title:** <one line>

**Version:** <library version>, Java <version>

### Minimal reproduction
<the smallest statement or snippet that shows the problem>

### Expected / Actual

### Why this matters to QueryFence
<what the user sees because of it>

### Environment
```
