# Coding style and modularisation

Approved by Boey on 2026-10-14 for application code and future agent-generated changes. These rules support the project brief's page 6 design criteria and page 7 robustness/usability criteria. Read this document before planning or editing code. [agents.md](../agents.md) governs identity, ownership, approval and publication; [TEAM_WORKFLOW.md](TEAM_WORKFLOW.md) governs collaboration and human acceptance.

## Responsibilities and dependencies

- Give each class one coherent responsibility that can be explained in a sentence. Keep validation, execution, grading policy, persistence and presentation in clear components
- Keep coordinating classes focused on the sequence of work. Delegate substantial parsing, rendering or file-writing logic to collaborators with explicit inputs and outputs
- Give each method one logical operation and a name that describes its purpose. Keep related steps together when extraction would obscure the flow
- Use composition for collaboration. Introduce interfaces when alternative implementations or substitution have a concrete benefit. Explain any inheritance relationship with an actual use case
- Keep public APIs small. Prefer package-private implementation collaborators and private helpers; preserve existing public contracts unless an approved plan changes them
- Put related implementation classes in the existing feature package. Coordinate shared models, configuration, contracts and pipeline wiring before changing them
- Keep presentation dependent on completed results. Grading and identity decisions belong to their owning services; renderers and feedback generators consume those decisions
- Give shared helpers a cohesive concern, such as HTML escaping or configuration paths, and consistent semantics for their callers
- Use one top-level class per Java file. Small nested records/enums or private implementation types can remain with their owner when useful only there

## Size review guidelines

These are review triggers, not mandatory caps. Existing files are assessed through the approved review and subsequent refactoring plans; exceeding a trigger does not authorise an edit.

| Scope | Review trigger | Counting convention |
| --- | --- | --- |
| Production Java file | More than 300 lines | Total physical lines, including imports, comments and blank lines |
| Java method or constructor | More than 40 lines | Nonblank, noncomment lines from declaration through closing brace; multiline statements count as multiple lines |
| Test files and non-Java assets | No automatic file cap | Review readability, scenario separation and responsibilities |

Java adapter resources are production source when compiled into a student workspace. Embedded text-block content is data; assess a long fixture/template separately rather than treating its contents as Java statements. CSS and JavaScript have no Java-specific numeric cap; review coherent selectors, interactions and functions.

When a trigger is exceeded, inspect responsibility boundaries, nesting, repeated logic, shared mutable state and dependencies. Extract a component when it owns a meaningful concern. A cohesive state machine, immutable data aggregate, constructor or integration scenario may justify an exception.

Record an exception in the review report or PR using this form:

```text
File/method:
Size and counting convention:
Responsibility:
Why keeping it together helps:
Relevant checks and remaining limits:
```

Do not add Java comments just to record size exceptions. Preserve method-level AI attribution for human reviewers.

## Java conventions

- Use the existing Java 17 target and four-space indentation consistently
- Use meaningful names, ordinary loops and clear conditionals when they express the operation directly
- Keep imports explicit and consistently grouped; avoid wildcard imports in new or deliberately edited imports
- Prefer final fields and immutable result/view objects. Copy mutable collections at ownership boundaries and avoid exposing writable internal collections
- Keep local mutation limited to the operation that owns it; return explicit results rather than sharing partially populated mutable state across unrelated services
- Externalise changeable operational policy. Use named constants for fixed protocol/format mechanics where a name clarifies the value
- Preserve decimal scoring semantics. Reporting derives views from recorded points and configured maxima; it does not introduce a second grading policy
- Catch exceptions at the layer that can handle or report them. Retain contextual diagnostics, propagate interruptions and preserve the distinction between student failures and infrastructure failures
- Use try-with-resources for owned streams and other closeable resources
- Keep comments minimal, lowercase and without full stops, following the existing project convention. Explain a necessary reason or constraint rather than restating the code
- Agents must not insert AI-attribution comments or prepare human declarations. The team owns method-level attribution and the presentation AI Use appendix required by the brief

Formatting changes stay within the intended scope of a reviewed edit. A behaviour-preserving extraction must not silently alter validation, defaults, error messages, ordering or exception handling.

## Configuration and file handling

Keep relative paths resolved against the configuration file's directory. Preserve canonicalisation, symlink-aware input protection, overlap checks, extraction limits and safe output filenames. Keep supplied ZIPs, testers, starters, support classes, fixtures, score sheets and references unchanged; corrections belong in separate working copies.

Configuration loaders can delegate parsing and path policy while retaining the current `AppConfig.load(Path)` and getter surface. Changing property names, defaults or public contracts requires an explicit approved plan and coordination with callers.

## Reporting and browser code

Keep HTML self-contained with inline CSS and vanilla JavaScript. Escape all student-controlled text and attributes through one consistent implementation. Keep student data out of executable scripts; use DOM text APIs for display changes.

Use the authoritative withholding map. Exported-only aggregates exclude withheld diagnostic scores, while preparation warnings remain a separate status. Preserve real case statuses, score labels, configured question order, ordinal anchors, row/detail pairing, filters, sorting and question navigation.

Display only recorded evidence. Do not invent expected/actual assertions, absent roster students, analysis results, truncation flags or whole-run timing. CSV persistence and HTML generation retain their existing order and failure behaviour.

## Tests and review evidence

- Organise tests around observable behaviour and failure scenarios. Keep readable fixtures near their tests; share fixture helpers only where they reduce repeated setup without hiding the scenario
- Preserve meaningful public-boundary and integration assertions during extraction. Add focused tests when a new seam exposes a real regression risk; avoid tests that only assert delegation
- Prefer static checks and targeted unit/integration tests. Use DOM/browser checks when changed interactions or rendering require them, subject to tool restrictions
- Run the narrowest useful checks after each affected batch. Use a clean build and representative sample run at the final integration boundary for substantial Java refactoring
- Compare semantic results across separate runs: scores, decisions, case statuses and required fields. Paths, durations and run IDs can differ. Byte-identical CSV assertions apply to the same completed run before/after HTML rendering
- Report commands actually run, results and limits. Source inspection, a mock DOM fixture, a real browser pass and human acceptance are different evidence
- Keep correctness findings separate from behaviour-preserving refactoring. Record trigger, location, consequence and confidence; seek approval before fixing them

## Agent and PR checklist

Before editing, confirm the current branch/owner, inspect local work, read relevant code/tests and propose a concise plan for substantial changes. Each new task uses a descriptive branch without a `codex/` prefix, such as `code-modularisation` or `kiya-feature-1-plagiarism`; preserve existing branches until their owner authorises cleanup.

Main retains architecture, shared decisions and integration. Delegate bounded useful work under the current Agent Workflow, give agents explicit file ownership, use at most two concurrent subagents and prohibit subagent spawning. Inspect delegated findings and diffs before integration.

Before publication, review responsibilities, size exceptions, API compatibility, preserved behaviour, actual test evidence and documentation. Commit only intended files, excluding supplied inputs, generated output, local handoffs and unrelated metadata. Publish approved reviewed work through a PR into `main`, give the driver its URL and ask them to contact the group. Teammate messages and merging require explicit authorisation.

The current review is recorded in [CODE_REVIEW.md](CODE_REVIEW.md). Boey directly approved the M1-M5 behaviour-preserving implementation; its code and checks are recorded there. Further substantial recommendations require their own plan and approval. These guidelines do not approve optional bonuses or broad rewrites.

CS102 Coding Convention

https://smu.sg/cs102-coding-style
Java Coding Style

1. Line Length	2
2. Simple Statements	2
3. Variable Declaration	2
4. Meaningful variable names	3
5. Indentation	3
6. Blank Lines	4
7. Blank Space	5
8. for loops	5
9. if, if-else, if-else-if-else	6
10. for loops	8
11. Returning values	9



Line Length
Maximum line length is 80 characters. Long lines are not handled well by many terminals and tools. 
Short lines are more comfortable to read.
When an expression will not fit on a single line, break it according to these general principles:
Break after a comma.
Break before an operator. 
Align the new line with the beginning of the expression at the same level on the previous line. 
If the above rules lead to confusing code or to code that’s squished up against the right margin, just indent 8 spaces instead.
do_something(long_expression1, long_expression2, long_expression3,
             long_expression4, long_expression5); 

int result = do_something_else(long_expression1, 
        long_expression2, long_expression3));


Simple Statements
 Each line should contain at most one statement. 
// Good
Scanner sc = new Scanner(System.in);

System.out.println("Enter n:"); 
int n = sc.nextInt();


// BAD
Scanner sc = new Scanner(System.in); 
System.out.println("Enter n:"); int n = sc.nextInt();


Variable Declaration
One declaration per line is recommended since it encourages commenting.
Use declaration and initialization together instead of separately
Always initialize variables. Most problems can be traced back to a pointer or variable left uninitialized.
// Good
int level = 0; // indentation level 
int size = 0; // size of table


// BAD: Only variable size is initialized
int level, size = 0;





Meaningful variable names
Variable names should be short yet meaningful.
One-character variable names should be avoided.
Use lowerCamelCasing for variable names and method names. For example, numFemales instead of num_females.
Give as descriptive a name as possible, within reason. Do not worry about saving horizontal space as it is far more important to make your code immediately understandable by a new reader. 
Do not use abbreviations that are ambiguous or unfamiliar to readers outside your project, and do not abbreviate by deleting letters within a word. Abbreviations that would be familiar to someone outside your project with relevant domain knowledge are OK.

// Good 
double height = 1.98;
double weight = 78.5;
double bmi = weight / (height * height);


// Bad 
double a = 1.98;
double b = 78.5;
double c = b / (a * a);


No magic number so that it is easier to read and understand by others.
// Good
if (input >= '0' && input <= '9') {
    System.out.println("digit");
}

// Bad
if (48 <= input && input <= 57) {
    System.out.println("digit");
} 



Indentation
4 spaces used for indentation (instead of tabs)
K&R brace placement.
Curly braces MUST be used even for single statement blocks.
if (x > 3) {           // start curly brace on the if statement
    System.out.println("happy\n"); // 4 spaces before System
}







Blank Lines
Declare variables in as local a scope as possible, and as close to the first use as possible. This makes it easier for the reader to find the declaration and see what type the variable is and what it was initialized to.
Leave a blank line after each logical chunk of code

// Good
Scanner sc = new Scanner(System.in);

// prompt for n1 ("one logical chunk")
System.out.print("Enter n1:");
int n1 = sc.nextInt();
       
// prompt for n2
System.out.print("Enter n2:");
int n2 = sc.nextInt();
       
// prompt for n3
System.out.print("Enter n3:");
int n3 = sc.nextInt();
       
// compute and display
int total = n1 + n2 + n3;
System.out.printf("%d + %d + %d = %d%n", n1, n2, n3, total);



// BAD
Scanner sc = new Scanner(System.in);
int n1;   int n2;
int n3;
int total;
System.out.print("Enter n1:");
n1 = sc.nextInt();
System.out.print("Enter n2:");
n2 = sc.nextInt();
System.out.print("Enter n3:");
n3 = sc.nextInt();
total = n1 + n2 + n3;
System.out.printf("%d + %d + %d = %d%n", n1, n2, n3, total);



Blank Space
All binary operators should be separated from their operands by spaces. 
Blank spaces should never separate unary operators such as unary minus, increment ("++"), and decrement ("--") from their operands.

// Good
int a = -3;
a += c + d; 
a = (a + b) / (c * d); 


// BAD
int a=- 3;
a+=c+d; 
a=(a+b)/(c*d); 


for loops
Do not write conditionals in loop to execute a piece of code in the first/last iteration.

// Good
int n = 5;
for (int i = 0; i < n - 1; i++) {
    printf("sad");
}
printf("happy");   

// Bad
int n = 5;
for (int i = 0; i < n - 1; i++) {
    printf("sad");
    if (i == n - 1) {
        printf("happy");
    }
}   



if, if-else, if-else-if-else
The if-else class of statements should have the following form:
// there is a space between the keywords (if, else) and the 
// following parentheses, and between the parentheses and 
// the next curly bracket. 
if (condition) { 
    statements; 
} 

if (condition) { 
    statements; 
} else { 
    statements; 
} 

if (condition) { 
    statements; 
} else if (condition) { 
    statements; 
} else if (condition) { 
    statements; 
}

Do not check boolean values for equality. By using implicit comparisons, the resulting code can be read more like conversational English. 
bool is_happy = false;

// Good
if (is_happy) {
  // do something
}

if (!is_happy) {
  // do something
}

// BAD
if (is_happy == true) {
  // do something
}

if (is_happy == false) {
  // do something
}


If there is a branching logic with one empty branch, rewrite it without the empty branch.
// original
if (x > 3) {
    // do nothing
} else {
    printf("apple\n");
}

// suggested
if (x <= 3) {
    printf("apple\n");
}

Doing something to do nothing.
// BAD
if (isAttentive) {
    classParticipation += 10;
} else {
    classParticipation += 0;
}

// GOOD 
if (isAttentive) {
    classParticipation += 10;
} 


Avoid nesting if statements that can be written as one if statement.
// good
if (x > 30) {
    printf("apple\n");
} else if (x > 20) {
    printf("orange\n");
} else if (x > 10) {
    printf("pear\n");
} else {
    printf("papaya\n");
}


// bad
if (x > 30) {
    printf("apple\n");
} else {
    if (x > 20) {
        printf("orange\n");
    } else {
        if (x > 10) {
            printf("pear\n");
        } else {
            printf("papaya\n");
        }
    }
}


If the intention is that only one block of code will execute, use if-else-if-else.
// good
if (color == 'R') {
    printf("red\n");
} else if (color == 'G') {
    printf("green\n");
} else if (color == 'B') {
    printf("blue\n");
}

// bad
if (color == 'R') {
    printf("red\n");
}

if (color == 'G') {
    printf("green\n");
} 

if (color == 'B') {
    printf("blue\n");
}







for loops
A for statement should have the following form: 
for (initialization; condition; update) { 
    statements; 
}

// GOOD
for (int i = 0; i < 3; i++) {
    printf("%d\n", i);
}

// BAD
for(int i=0;i<3;i++){
    printf("%d\n", i);
}


Avoid break and continue in an infinite loop.
// bad
for (int i = 0;  ; i++) {
    if (i == 3) {
        break;
    }
    // stmts here
}

// good
for (int i = 0; i < 3; i++) {
    // stmts here
}


Avoid writing unrelated statements in a for loop.
// bad
int sum;
for (i = 0, sum = 0; i < 5; ++i) {
    // stmts 
}

int y = 0;
for (i = 0; i < 5; ++i, y = y + 2) {
   // Loop body 
}

// good
int sum = 0;
for (int i = 0; i < 5; i++) {
    // stmts
}

int y = 0;
for (i = 0; i < 5; i++) {
   // Loop body 
    y = y + 2;
}




Returning values
Return the boolean expression directly instead of checking a boolean expression to return a boolean value.

// BAD
public boolean isPositive(int x) {
    if (x > 0) {
        return true;
    } else {
        return false;
    } 
}


// GOOD
public boolean isPositive(int x) {
    return x > 0;
}



Methods
Early Return pattern: Check for invalid values and return fast instead of nesting and return later.

// GOOD
public int doSomething(int x, int y) {
    if (x < 0) {
        return y;
    }

    if (y < 0) {
        return x;
    }


    // complicated logic here
}



// BAD
public int doSomething(int x, int y) {
    if (x >= 0 && y >= 0) {
        // complicated logic here
    } else if (x < 0) {
       return y;
    } else {
       return x;
    }
}


