GENERAL JAVA GUIDELINES
-----------------------

- Avoid the `var` keyword. Prefer explicit types for clarity and readability.
- Make fields `final` whenever possible.
- Prefer clear, descriptive variable and method names.
  - Avoid single-letter names such as `s`, `c`, or `x` unless the meaning is obvious
    (e.g. loop indices or very small scopes).

- Code should follow SOLID principles.
  - Pay close attention to class responsibility.
  - Explicitly warn if a class starts to take on multiple responsibilities.
  - Prefer smaller, focused classes over "god objects".
  - DRY is a guiding principle, not an absolute rule.

- Avoid heavy abstraction frameworks when simpler alternatives exist.
  - Prefer `JdbcClient`, `RestClient`, or similarly direct APIs.
    - ALWAYS use named parameters where supported.
  - Avoid ORM / entity-style frameworks unless there is a strong, explicit reason
    (for example: it already exists in the project and replacing it is out of scope).


FORMATTING STYLE
------------------

- Prefer compact readable formatting over aggressive vertical formatting.
- Do not put every method argument on its own line automatically.
- Keep short method calls on a single line when readable.
- Use multiline formatting only when it improves structure or clarity.
- Prefer this style:

    someCall(arg1, arg2, arg3);

instead of:

    someCall(
            arg1,
            arg2,
            arg3);

- For longer calls, allow partial wrapping:

    repository.save(mapper.toEntity(
            value1, value2, value3));

- Keep chained calls vertically aligned.
- Use whitespace between logical sections inside methods.
- Avoid excessive line breaks for ternaries, builders, or simple expressions.
- Optimize for human scanning, not minimal line length.

TESTING GUIDELINES
------------------

- Test behaviour, not implementation details.
- Mockito is allowed and often appropriate.
- Avoid Mockito "magic" and invisible behaviour.
  - Do not use `@ExtendWith(MockitoExtension.class)`.
  - Do not rely on annotation-driven injection (`@Mock`, `@InjectMocks`) when it hides setup.
  - Prefer explicit, readable mock creation and wiring in the test body or setup methods.
- Do not rely on `verify(...)` unless the interaction itself is part of the behaviour being tested.
- Use awitility when you need delays.
- Use MockWebServer if you are testing a generated http client. Do not mock rest clients or the http layer.
- Use `@JdbcTest` when testing database behaviour.
  - Ask if you are unsure how to set this up.
- Use parameterized tests when:
  - There are many similar test cases.
  - The behaviour is identical but inputs and outputs vary.
- Always isolate constants in tests.
  - Do not repeat literals across the test.
  - Example:
    - If a test creates a user named "petter", store the value in a constant
      and assert against that constant instead of repeating the string literal.


GENERAL PRINCIPLES
------------------

- Optimize for readability and maintainability over cleverness.
- Code should be easy to understand for someone unfamiliar with the implementation.
- Prefer simple, explicit solutions over indirect or overly abstract ones.


HARD RULES
----------

- Never add comments that merely restate what the code already says
  (e.g. "// this is the changed code").
- Only add comments when they explain why something exists or why a decision was made.
- Make the code as self-documenting as possible.
- Add @NonNull when code explicitly expects a non-null value.
- Never guess.
  - If information is missing, ambiguous, or unclear, ask for clarification.
  - Do not assume intent, domain rules, or existing architectural constraints.


POST-GENERATION REQUIREMENT
---------------------------

After generating code, always add a clearly labeled section titled:

Self-review / risks

In this section:
- Briefly list potential problems, risks, tradeoffs, or assumptions.
- Keep it concise and concrete.
  - Do not include long explanations or theoretical background.
  - Example of good:
    - "This could deadlock if called concurrently from two schedulers."
  - Example of bad:
    - Two paragraphs explaining how semaphores work.
- If the code is trivial or low-risk, it is acceptable to state that explicitly, e.g.:
  - "This is a simple mapping class; I don't see any meaningful risks."
  - "Null handling is incomplete and would need tightening in production code."
