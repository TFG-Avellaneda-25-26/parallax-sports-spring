# Code Documentation Guidelines

## Goal

Write comments for a developer who has never seen this code before.
Every comment should answer at least one of:

- **What** does this do? (not how — that's already in the code)
- **Why** does it work this way?
- **What comes out of it?** (what it returns, what changes, what fails)

If a comment only restates what the code literally says, delete it.

---

## Comment structure

Use three levels of comments in medium or large classes.

### Section banners

Group related methods under a banner so the file is scannable.

```java
/*============================================================
  SYNC PIPELINE
  Fetches pages from the external API and upserts into the DB
============================================================*/
```

### Exception handler annotation

Every `@ExceptionHandler` method gets a one-liner above it in this exact format:

```java
// -> Triggers: malformed JSON / unreadable body || Returns: Bad Request (400)
```

This lets anyone scanning the file instantly see what breaks what.

### Javadoc for public and non-obvious methods

```java
/**
 * Fetches games for the given league from the external API and saves them to the database.
 * Stops early if the page budget is exhausted or the API rate-limits the request.
 *
 * @param league      the league to sync (NBA or WNBA)
 * @param fromDate    start of the date window to request
 * @param maxPages    maximum number of API pages to consume in one call
 * @return summary of what was fetched and saved, including whether the sync finished or was cut short
 */
```

Only document params that actually change behavior. Skip params that are self-explanatory.

---

## Writing style

- Write in plain sentences. "Resolves the start date for the sync window" not "Performs resolution of the start date artifact".
- Say what the method does to the world, not how its internals work.
- Include a `@return` line whenever the return value isn't obvious from the method name — describe what the value means, not just its type.
- Include a `@throws` only when callers need to handle or expect a specific failure.
- Document `null` if passing `null` meaningfully changes behavior (e.g. `null` league means all leagues).

---

## What to document

- Why a method exists if its name alone doesn't make it clear.
- Inputs that change the output or behavior in non-obvious ways.
- Side effects (sends an event, writes to a table, evicts a cache).
- Security-relevant constraints ("only accessible to admins", "sanitized before use").
- Anything that surprised you when you first read it — it will surprise the next person too.

## What not to document

- What the code already says literally (`// loops over events` above a for loop).
- Java language mechanics.
- Implementation details that change often (specific field names, magic numbers already named by constants).

---

## Javadoc quality check

Before committing Javadoc, ask:

1. Does this tell someone new what the method is *for*?
2. Does the `@return` line say what the value *means*, not just what type it is?
3. Is it still true? If the method changed, update the comment.

Bad examples:

- `"Handles the exception"` — says nothing
- `"Returns the result"` — useless
- `"Sets the value"` — obvious from the setter name

---

## Exception handlers (full standard)

For each `@ExceptionHandler`:

1. Add the trigger/status line: `// -> Triggers: ... || Returns: Name (code)`
2. Javadoc should name the failure category, describe what the response looks like, and note any caller-visible behavior.
3. Never include internal stack traces or secret values in user-facing messages.
