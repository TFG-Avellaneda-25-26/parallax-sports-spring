# AI Code Documentation Guidelines

This note is a project-specific guide for AI-assisted code documentation.
Use it to produce consistent, useful, low-noise comments.

## 1. Goal

Documentation must answer, quickly:

1. What this code does.
1. When it triggers.
1. What contract it returns or enforces.
1. Why this behavior exists (only when not obvious).

Write for maintainers who did not author the code.

## 2. Non-Negotiable Principles

1. Clarity over cleverness.
1. Intent over implementation narration.
1. Contract over internal trivia.
1. Stable wording over noisy, changing details.
1. Keep comments synchronized with behavior changes.

## 3. Preferred Comment Architecture

Use all three layers in medium/large classes.

### 3.1 Section banners (grouping)

Use multiline decorated banners to partition related logic.

```java
/*============================================================
  VALIDATION EXCEPTIONS
  Bean validation and request parsing failures
============================================================*/
```

### 3.2 Scanner line before handlers

Required for exception handlers.

Format:

```java
// -> Triggers: <concise trigger> || Returns: <Status Name> (<code>)
```

Example:

```java
// -> Triggers: malformed JSON / unreadable body || Returns: Bad Request (400)
```

### 3.3 Javadoc for methods

Use Javadoc for public methods and non-trivial private helpers.

Minimum shape:

```java
/**
 * One-line purpose with domain meaning.
 *
 * @param x what matters about this input
 * @param y what matters about this input
 * @return contract-level output
 */
```

## 4. Style Rules (strict)

Direct, explicit, concise. Active voice. Domain vocabulary only. No vague terms ("stuff", "things", "some logic"). No jokes in permanent docs. Avoid repeating the method name in sentence form.

## 5. AI Workflow: How to Document a File

When an AI updates documentation, follow this sequence:

1. Detect responsibilities in the class.
1. Group methods into meaningful sections.
1. Add/normalize section banners.
1. For each handler, add scanner trigger/status line.
1. Add or tighten Javadoc with purpose, params that affect behavior, return contract, and throws only when part of the contract.
1. Remove filler comments that restate obvious syntax.
1. Validate that comments still match behavior.

## 6. What to Document

1. Business intent of methods.
1. Input conditions that change behavior.
1. Output contract (status, payload shape, side effects).
1. Security-sensitive constraints.
1. Observability behavior (structured logs, metrics tags).

## 7. What Not to Document

1. Obvious control flow.
1. Java syntax semantics.
1. Temporary implementation detail likely to churn.
1. Comments with no actionable meaning.

## 8. Exception Handler Documentation Standard

For each `@ExceptionHandler`:

1. Add scanner line in this exact format: `// -> Triggers: ... || Returns: Name (code)`.
1. Add Javadoc that states failure category, key params (`ex`, `request`), and response contract (`ProblemDetail`, status).
1. Keep user-facing messages safe (no secrets/internal traces).
1. Ensure status mapping is explicit and stable.

## 9. Javadoc Quality Bar

Good Javadoc is:

1. Specific to domain behavior.
1. Short (usually 1-3 lines of description).
1. Focused on contract, not line-by-line mechanics.
1. Updated when method behavior changes.

Bad Javadoc examples:

1. "This method handles exceptions".
1. "Sets variable value".
1. Copy-paste text that does not match current logic.

## 10. Presentation Improvements AI Should Apply

When possible, AI should improve readability by:

1. Ordering sections from external contract to internal helpers.
1. Keeping consistent wording across similar handlers.
1. Using one terminology set (`request body`, `validation`, `conflict`, etc.).
1. Keeping scanner comments parallel in shape for quick visual parsing.

## 11. Maintenance Rule

If behavior changes, update related comments in the same commit.
If code and comment diverge, fix the comment immediately.

