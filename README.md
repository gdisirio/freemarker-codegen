# FreeMarker Code-First Mode

Code-first mode inverts FreeMarker's default behavior: **logic is the default** and text output requires explicit delimiters. This is designed for use cases where FreeMarker is used as a **code generation language** rather than a document template processor.

## Why code-first mode?

Standard FreeMarker was designed for HTML and document templating, where most of the file is literal text with occasional logic. When used for code generation, this model becomes a liability:

- **Angle-bracket noise.** Every directive requires `<#...>`, and every closing tag requires `</#...>`. In a template that is 80% logic and 20% output, these delimiters dominate the file and obscure the actual intent.
- **Whitespace battles.** FreeMarker outputs all text literally, including indentation and newlines around directives. Code generators spend significant effort fighting this — using `<#t>`, `<#lt>`, `<#rt>`, or cramming directives onto single lines to avoid blank lines in output.
- **No `>` without parentheses.** The `>` and `>=` operators conflict with the tag-closing `>` in classic mode, forcing `(x > 0)` or workarounds like `gt`. Code generation templates are full of comparisons, making this a constant friction.
- **No bitwise operations.** Low-level code generation (hardware registers, binary protocols, color manipulation) requires bitwise operations that classic FreeMarker simply does not have.
- **No hex literals.** Related to the above — working with bitmasks and hardware constants without hex literals means scattering magic decimal numbers throughout the template.
- **Comment syntax mismatch.** `<#-- ... -->` looks nothing like the `//` and `/* */` that developers read and write every day. When the template itself is logic-heavy, this feels unnatural.

Code-first mode solves all of these:

| Problem | Classic FreeMarker | Code-First |
|---|---|---|
| Directive syntax | `<#if cond>...</#if>` | `if cond`...`endif` |
| Text output | Implicit (everything is output) | Explicit (`emit`) — no whitespace surprises |
| Comparisons | `(x > 0)` or `x gt 0` | `x > 0` — just works |
| Bitwise ops | Not available | `&`, `\|`, `^`, `~`, `<<`, `>>` |
| Hex literals | Not available | `0xFF` (also enabled in classic mode) |
| Comments | `<#-- comment -->` | `// comment` or `/* comment */` |
| Assignment | `<#assign x = 1>` | `x = 1` |

The result is templates that **read like the code they generate**, with a clean imperative syntax that any developer can follow without learning FreeMarker's tag conventions.

## Design principles

- **Parser-only change.** All code-first syntax maps to existing FreeMarker AST nodes. There is no runtime difference — the same engine evaluates both modes identically.
- **Zero impact on existing templates.** Standard `.ftl` files and behavior are completely unchanged. Code-first mode is strictly opt-in.
- **Full interoperability.** `.ftl` and `.ftlc` files can freely import and include each other. Each file is parsed independently with its own mode.

---

## Activation

Code-first mode can be activated in three ways:

### 1. File extension `.ftlc`

Any template loaded with a `.ftlc` extension automatically uses code-first mode:

```java
Template t = cfg.getTemplate("generate.ftlc");
```

### 2. Header directive in any `.ftl` file

Add a `syntax` parameter to the `<#ftl>` header. The header itself uses classic FTL syntax; code-first mode activates immediately after it:

```
<#ftl syntax="code-first">
emit "Hello from code-first mode"
```

### 3. Configuration flag

Enable code-first mode for all templates loaded through a `Configuration`:

```java
Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
cfg.setCodeFirstMode(true);
```

Or per-template via `TemplateConfiguration`:

```java
TemplateConfiguration tc = new TemplateConfiguration();
tc.setCodeFirstMode(true);
```

---

## Interoperability

`.ftl` and `.ftlc` files can freely import and include each other. Each file is parsed independently with its own parser mode. Both modes produce identical AST nodes, so there is no runtime difference:

```
// In a .ftlc file:
import "utils.ftl" as u
```

```ftl
<#-- In a .ftl file: -->
<#import "generator.ftlc" as gen>
```

---

## Comments

```
// This is a single-line comment

/* This is a
   multi-line block comment */
```

Block comments are **not nestable** (C/Java semantics).

---

## Text Output: `emit`

In code-first mode, text is not output by default. Use the `emit` keyword to produce output.

### Single expression

`emit` is followed by an arbitrary expression — its value is written to the output. This can be a string literal, a variable, a function call, or any composition of these:

```
emit "Hello, World!\n"     // string literal
emit x?c                   // variable with built-in
emit someFunction()        // function call
emit "a" + " " + "b"       // string concatenation
```

String literals support `${...}` interpolation:

```
name = "Alice"
emit "Hello, ${name}!\n"
```

Interpolation is a property of the string literal itself — it works in **any** string literal, not just in `emit`. For example in assignments, function arguments, or sequence/hash literals:

```
greeting = "Hello, ${name}!"
items    = ["file-${id}.txt", "backup-${id}.bak"]
format(prefix = "[${level}] ")
```

Interpolation and concatenation are equivalent — pick whichever reads better:

```
emit "Count: ${n}, total: ${total}\n"
emit "Count: " + n + ", total: " + total + "\n"
```

### Text blocks with `"""`

For multi-line output, use `emit """` to start a text block. The block ends at the next `"""`:

```
emit """
<!DOCTYPE html>
<html>
  <head><title>${title}</title></head>
  <body>${body}</body>
</html>
"""
```

Text blocks support `${...}` interpolation just like string literals.

**Leading newline rule:** If `"""` is followed by only whitespace and then a newline, that first newline is stripped — the content starts from the next line. If content follows `"""` on the same line, it is emitted as-is:

```
// These produce identical output:
emit """
Hello!
"""

emit """Hello!
"""
```

This makes it natural to start the content on the line after `"""` without adding an unwanted leading newline.

### Choosing between forms

| Form | Use case |
|---|---|
| `emit expr` | Computed values, variables, function results |
| `emit "..."` | Short single-line text with interpolation |
| `emit """..."""` | Multi-line template blocks |

---

## Multi-Stream I/O

Code-first mode supports writing to multiple output targets and reading from files or stdin in a single template. This enables generating multiple files (e.g., `.h` and `.c` together), emitting diagnostics to stderr, and reading data fixtures inline.

### Writing: `emit ... to <target>`

The `to <target>` clause routes the output to a specific destination:

```
emit "main output"                          // implicit "to default" (legacy)
emit "diagnostic\n" to stderr               // standard error
emit "tabular line\n" to stdout             // standard out
emit "header content\n" to "build/out.h"    // file path
emit "..." to header_path                   // string variable holding a path
```

**Reserved target names:**

| Target | Destination |
|---|---|
| `default` | The main template output (the `Writer` passed to `process()`) |
| `stdout` | `System.out` |
| `stderr` | `System.err` |

Any other expression evaluates to a string treated as a **file path**.

### Output file semantics

- **Lazy open**: the file is opened on the first `emit ... to <path>` for that path
- **Truncate mode**: the file is overwritten — existing content is discarded
- **Cached handle**: subsequent `emit`s to the same path reuse the open handle, preserving order
- **Path normalization**: paths are normalized via `toAbsolutePath().normalize()`, so `"out.txt"`, `"./out.txt"`, and `"sub/../out.txt"` all map to the same handle
- **Auto parent dirs**: missing parent directories are created automatically
- **Auto close**: all open handles are flushed and closed when template processing ends

### Multi-file generation example

```
header = "include/${name}.h"
source = "src/${name}.c"

emit "#ifndef ${name?upper_case}_H\n" to header
emit "#define ${name?upper_case}_H\n" to header
emit "#include \"${name}.h\"\n" to source

list functions as f
  emit f.prototype + ";\n" to header        // declaration in .h
  emit f.body to source                      // implementation in .c
endlist

emit "#endif\n" to header
```

The `.h` and `.c` files are generated in a single pass, with related code emitted adjacent in the template — making cross-file consistency easy to maintain.

### Reading: `read from` and `readln from`

Two read primitives are available as expressions:

```
text = read from "data.txt"                 // entire file as a string
text = read from stdin                      // entire stdin as a string

list (readln from "big.csv") as line        // lazy line-by-line iteration
  emit line + "\n"
endlist
```

| Form | Returns | Use case |
|---|---|---|
| `read from <target>` | String (eager) | Whole file content; small files, fixtures, snippets |
| `readln from <target>` | Collection (lazy) | Line-by-line iteration; any file size |

Reserved target name: `stdin`. Anything else is treated as a file path.

### `readln` returns a lazy collection

The result of `readln from <target>` is a `TemplateCollectionModel` — iteration-only, single-pass, bounded memory. Each iteration reads exactly one line; the file is never fully materialized. Line terminators (`\n`, `\r\n`, `\r`) are stripped from yielded values.

**Streaming-safe built-ins** that work on `readln` results without materializing:

```
readln from "data.txt"?first                   // reads one line, returns it
readln from "data.txt"?join(", ")              // reads to end, returns joined string
readln from "data.txt"?filter(l -> ...)        // lazy filtered collection
readln from "data.txt"?map(l -> l?upper_case)  // lazy transformed collection
readln from "data.txt"?take_while(l -> ...)    // lazy prefix
readln from "data.txt"?drop_while(l -> ...)    // lazy suffix
```

Chained pipelines stay lazy:

```
list (readln from "data.csv")?drop_while(l -> l?starts_with("#"))?filter(l -> l != "") as line
  emit transform(line) + "\n"
endlist
```

This iterates the source once, top to bottom, without ever loading the whole file.

**Operations not supported** on a lazy collection (would require materialization):

- `?size`, `[i]` indexing — error
- `?sort`, `?reverse` — error

If you need these, materialize explicitly with `?sequence`:

```
all = (readln from "data.txt")?sequence       // reads ENTIRE file into memory
emit all?size?c                                // works
emit all?sort?join("\n")                       // works
```

`?sequence` is the explicit boundary between streaming and in-memory operations. It makes the cost visible at the call site.

### Multiple `readln` on the same file

Each `readln from <target>` call opens a fresh reader. Two calls on the same path are independent:

```
list (readln from "data.csv") as line     // first pass
  // validate
endlist
list (readln from "data.csv") as line     // second pass — file reopened
  // emit
endlist
```

Unlike output handles (which are cached for write coalescing), input handles are not cached — repeated reads of the same file are always fresh.

### Reading a freshly-written file

If you write to a file with `emit ... to <path>` and read it back with `read from <path>` or `readln from <path>` later in the same template, the runtime flushes the open writer before opening the reader. The newly-written content is visible.

### Stdin termination

Reading from `stdin` ends naturally at EOF — Ctrl-D in a terminal, or end-of-pipe when piped from another command:

```
list readln from stdin as line
  emit line + "\n"
endlist
// loop ends when stdin closes
```

Stdin is single-pass and not seekable. A second `readln from stdin` after the first one exhausts it yields zero elements.

### Configuration knobs

- **Output base directory**: relative paths in `emit ... to "<path>"` are resolved against the configured output base directory. If unset (default), they're resolved against the JVM's current working directory.
  ```java
  cfg.setOutputBaseDirectory(new File("build/generated"));
  ```
  Absolute paths are unaffected.
- All output flows through the same `output_eol` and `\e` resolution as the main `emit` (see below)
- Writes use UTF-8 encoding by default

---

## End-of-Line Handling

Code-first mode provides consistent, configurable end-of-line handling to prevent the template file's own line endings from leaking into generated output.

### The `\e` escape — platform-independent EOL

Use `\e` in string literals to produce the configured output EOL. Unlike `\n` (which always produces a literal `\n`), `\e` resolves at runtime to the `output_eol` setting:

```
emit "line1\eline2\e"
```

With the default `output_eol` (which is `"\n"`), this produces `line1\nline2\n`. With `output_eol` set to `"\r\n"`, it produces `line1\r\nline2\r\n`.

| Escape | Meaning |
|---|---|
| `\n` | Always literal `\n` (Unix LF) |
| `\e` | Configured EOL — resolves to `output_eol` at runtime |

### Text block normalization

Line endings inside text blocks (`emit """..."""`) are automatically normalized to the configured `output_eol`. This means a template edited on Windows (with `\r\n`) produces the same output as one edited on Unix (with `\n`) — the template file's line endings never leak through.

### Configuring `output_eol`

The default is `"\n"`. To change it:

**Via configuration (Java):**

```java
cfg.setOutputEol("\r\n");  // Windows line endings
```

**Via `setting` directive:**

```
setting output_eol = "\r\n"
```

The `\e` escape and text block normalization both work in classic mode too — only the text block normalization is code-first specific.

---

## Variables and Assignment

Assignment uses bare `name = value` syntax. Scoping is automatic:

- At template level: assigns to the current namespace (equivalent to `<#assign>`)
- Inside a macro or function: assigns to local scope (equivalent to `<#local>`)

```
// Template level — namespace scope
x = 1
name = "World"
items = ["a", "b", "c"]

macro greet(who)
  // Inside macro — local scope
  greeting = "Hello, ${who}"
  emit greeting
/macro
```

### Explicit scope keywords

Use `assign`, `local`, and `global` to explicitly control scope. This is especially useful inside macros and functions where bare assignment always goes to local scope:

```
macro compute()
  local temp = heavyCalc()     // local to this call
  assign result = temp * 2     // writes to template namespace
  global cached = result       // writes to global scope
endmacro
```

| Keyword | Scope | Equivalent classic FTL |
|---|---|---|
| *(bare)* | Auto: namespace at top level, local in macro/function | — |
| `assign` | Current namespace | `<#assign>` |
| `local` | Local (macro/function only) | `<#local>` |
| `global` | Global | `<#global>` |

### Compound assignment operators

```
x = 10
x += 5
x -= 2
x *= 3
x /= 4
x %= 3
x++
x--
```

These work with all scope keywords: `assign x += 1`, `local count++`, `global total -= n`.

---

## Block Directives

Block directives use keyword syntax with `/keyword` closers. Every closing tag also has an `end` alias — both styles can be used interchangeably:

| Slash style | Keyword style |
|---|---|
| `/if` | `endif` |
| `/list` | `endlist` |
| `/macro` | `endmacro` |
| `/function` | `endfunction` |
| `/switch` | `endswitch` |
| `/sep` | `endsep` |
| `/items` | `enditems` |
| `/attempt` | `endattempt` |
| `/autoesc` | `endautoesc` |
| `/noautoesc` | `endnoautoesc` |

### if / elseif / else

```
if user.active
  emit "Welcome back, ${user.name}!\n"
elseif user.pending
  emit "Your account is pending.\n"
else
  emit "Please register.\n"
endif
```

Conditions are terminated by end-of-line. The `>` and `>=` operators work without parentheses (unlike classic mode):

```
if score > 90
  emit "Excellent!\n"
endif
```

### Multiline expressions

Wrap the expression in `()` to span multiple lines. Inside parentheses, newlines are ignored:

```
if (longConditionA &&
    longConditionB &&
    longConditionC)
  emit "all true\n"
endif
```

This works for any directive that takes an expression — `if`, `elseif`, `list`, `switch`, `return`, assignments, etc.

### Line continuation with `\`

A backslash `\` at the end of a line continues the statement on the next line. This is an alternative to parentheses for splitting long lines:

```
local s = ("#define " + name + " ")?right_pad(align) + \
          default

if longConditionA && \
   longConditionB && \
   longConditionC
  emit "all true\n"
endif
```

Both `()` and `\` can be used — choose whichever reads better in context.

### list

```
list users as user
  emit "${user.name}\n"
endlist
```

With key-value iteration:

```
list settings as key, value
  emit "${key} = ${value}\n"
endlist
```

With `else` for empty lists:

```
list results as result
  emit "${result}\n"
else
  emit "No results found.\n"
endlist
```

### sep

```
list items as item
  emit item
  sep
    emit ", "
  endsep
endlist
// Output: a, b, c
```

### items

The alternate `list` form uses `items` to separate the iterable expression from the loop variable. This allows content before and after the loop, and an `else` that fires when the list is empty:

```
list users
  emit "<ul>\n"
  items as user
    emit "  <li>${user.name}</li>\n"
  enditems
  emit "</ul>\n"
else
  emit "<p>No users.</p>\n"
endlist
```

### switch / case / default

```
switch color
case "red"
  emit "#FF0000"
  break
case "green"
  emit "#00FF00"
  break
default
  emit "#000000"
endswitch
```

---

## Macros and Functions

### macro

```
macro page(title, body)
  emit "<!DOCTYPE html>\n"
  emit "<html><head><title>${title}</title></head>\n"
  emit "<body>${body}</body></html>\n"
endmacro

page("Home", "Welcome!")
```

Parameters can have defaults:

```
macro button(label, type = "submit")
  emit "<button type=\"${type}\">${label}</button>\n"
endmacro

button("Save")
button("Cancel", "button")
```

### function

```
function max(a, b)
  if (a > b)
    return a
  else
    return b
  endif
endfunction

emit max(10, 20)?c
// Output: 20
```

### Calling macros and functions

Use `name(args)` syntax with positional or named arguments:

```
greet("World")
x = add(1, 2)
```

Namespace-qualified calls use dot notation:

```
import "lib/utils.ftlc" as u
u.formatText("hello", 80)
```

Named arguments use `name=value` syntax, separated by commas:

```
generatePrototype(
  name   = "myFunc",
  ctype  = "void",
  params = ["int a", "int b"]
)
```

### nested

Inside a macro, `nested` outputs the caller-provided body content:

```
macro wrapper(title)
  emit "<div class=\"box\">\n"
  emit "  <h2>${title}</h2>\n"
  nested
  emit "</div>\n"
endmacro
```

`nested` can also pass loop variables back to the caller:

```
macro repeat(count)
  list 1..count as i
    nested i
  endlist
endmacro
```

---

## Control Flow

### break and continue

```
list items as item
  if item == "skip"
    continue
  endif
  if item == "stop"
    break
  endif
  emit "${item}\n"
endlist
```

### return

Inside a function, `return` provides the return value:

```
function double(n)
  return n * 2
endfunction
```

Inside a macro, `return` exits early:

```
macro conditionalGreet(name)
  if !name?has_content
    return
  endif
  emit "Hello, ${name}!\n"
endmacro
```

### stop

Aborts template processing with an error message:

```
if !requiredParam??
  stop "Missing required parameter: requiredParam"
endif
```

### attempt / recover

Error handling — if the `attempt` block fails, execution continues in the `recover` block:

```
attempt
  result = riskyOperation()
recover
  emit "Operation failed, using default.\n"
  result = defaultValue
endattempt
```

---

## import and include

```
import "lib/utils.ftl" as u
include "header.ftl"
```

---

## XML/Tree Processing

### visit and recurse

`visit` dispatches to a macro matching the node's name. `recurse` processes child nodes. Both accept an optional `using` clause to specify the namespace containing the handler macros:

```
visit node
visit node using handlers
recurse
recurse node
recurse node using handlers
```

### fallback

Inside a macro invoked by `visit`, `fallback` delegates to the next namespace in the search order:

```
macro @element
  fallback
endmacro
```

---

## Other Directives

### flush

Forces output buffers to be flushed:

```
flush                  // flush the default writer
flush to "build/log"   // flush a specific auxiliary writer (file path)
flush to stderr        // flush a predefined target
flush all              // flush the default writer AND all auxiliary writers
```

Useful for long-running code generators where you want progress to be visible incrementally, or for crash safety: `flush all` after each unit of work means partial output survives an unexpected termination. Note that this is a buffer flush — it doesn't `fsync` to disk.

### setting

Changes a runtime setting for the remainder of the template:

```
setting number_format = "0.##"
setting locale = "en_US"
```

### autoesc / noautoesc

Controls auto-escaping within a block:

```
autoesc
  emit message    // escaped according to output format
endautoesc

noautoesc
  emit rawHtml    // no escaping
endnoautoesc
```

---

## Hex Literals

Hex integer literals are supported in **both** classic and code-first modes:

```
x = 0xFF        // 255
y = 0x00FF00    // 65280
color = 0xDEAD  // 57005
```

Values that fit in 32 bits produce `Integer`, larger values produce `Long`.

---

## Expressions

Code-first mode supports the full FreeMarker expression language:

- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `==`, `!=`, `<`, `<=`, `>`, `>=`
- Logical: `&&`, `||`, `!`
- String concatenation: `+`
- Built-ins: `?c`, `?string`, `?size`, `?has_content`, etc.
- Default values: `name!"default"`
- Sequence literals: `["a", "b", "c"]`
- Hash literals: `{"key": "value"}`
- Method calls: `obj.method(args)`
- Ranges: `0..10`, `0..<10`

The `>` and `>=` operators work without parentheses (unlike classic mode where they conflict with the tag-closing `>`).

### Bitwise operators (code-first only)

Code-first mode adds bitwise operators, which are not available in classic FreeMarker:

| Operator | Meaning | Example |
|---|---|---|
| `&` | Bitwise AND | `0xFF & 0x0F` → 15 |
| `\|` | Bitwise OR | `0x0F \| 0xF0` → 255 |
| `^` | Bitwise XOR | `0xFF ^ 0x0F` → 240 |
| `~` | Bitwise NOT | `~0xFF` → -256 |
| `<<` | Left shift | `1 << 8` → 256 |
| `>>` | Right shift | `256 >> 8` → 1 |

`&&` and `||` remain logical operators. The parser distinguishes single `&`/`|` (bitwise) from double `&&`/`||` (logical).

Operator precedence follows C conventions (highest to lowest):

1. `~` (unary bitwise NOT)
2. `<<`, `>>` (shifts)
3. `&` (bitwise AND)
4. `^` (bitwise XOR)
5. `|` (bitwise OR)
6. `&&` (logical AND)
7. `||` (logical OR)

All bitwise operations work on the `long` representation of numbers. Results that fit in 32 bits are returned as `Integer`, otherwise as `Long`.

Bitwise compound assignment operators are also supported:

```
flags = 0xFF
flags &= 0x0F       // AND assign
flags |= 0x80       // OR assign
flags ^= 0x01       // XOR assign
flags <<= 4         // left shift assign
flags >>= 2         // right shift assign
```

Example — extracting color channels from an RGB value:

```
color = 0x1A803C
red = (color >> 16) & 0xFF
green = (color >> 8) & 0xFF
blue = color & 0xFF
emit "R=${red?c} G=${green?c} B=${blue?c}\n"
// Output: R=26 G=128 B=60
```

---

## New Built-ins

These built-ins are new additions, available in **both** classic and code-first modes.

### `?tab_to(column)` / `?tab_to(column, fill)`

Pads a string with spaces (or a custom fill character) until it reaches the target column width. If the string is already at or past the target column, it is returned unchanged — no truncation occurs.

```
"hello"?tab_to(20)           // "hello               "
"hello"?tab_to(20, '.')      // "hello..............."
"hello world"?tab_to(5)      // "hello world" (unchanged)
```

The camelCase alias `?tabTo` is also supported.

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | number | yes | Target column (0-based width) |
| 2 | string | no | Single fill character (default: space) |

**Use case** — aligning generated code:

```
// Code-first example: aligned field declarations
list fields as field
  emit "${field.type}"?tab_to(12) + "${field.name};"?tab_to(32) + "// ${field.comment}\n"
endlist

// Output:
// int         count;                  // item count
// String      name;                   // display name
```

### `?indent(prefix)`

Prepends `prefix` to every non-empty line in the string. Blank lines are preserved without the prefix.

```
body = "int x;\nint y;\n"
emit body?indent("    ")
// Output:
//     int x;
//     int y;
```

```
comment = "First line.\nSecond line."
emit comment?indent(" * ")
// Output:
//  * First line.
//  * Second line.
```

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | string | yes | Prefix to prepend to each line |

### `?dedent(prefix)`

Removes `prefix` from the beginning of each line, if present. Lines that don't start with the prefix are left unchanged. Symmetric with `?indent`.

```
body = "    int x;\n    int y;\n"
emit body?dedent("    ")
// Output:
// int x;
// int y;
```

Lines without the prefix are untouched:

```
"  short\n    full\n"?dedent("    ")
// Output:
// "  short\n"   (only 2 spaces — no match, unchanged)
// "full\n"      (4 spaces matched, removed)
```

Round-trip with `?indent`:

```
text?indent("  ")?dedent("  ")   // returns original text
```

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | string | yes | Prefix to remove from each line |

### `?pad_lines(column)` / `?pad_lines(column, fill)`

Like `?tab_to` but operates on every line of a multi-line string. Each line is padded to the target column. Lines already at or past the column are left unchanged. Empty lines are not padded.

```
"int x;\nString name;\n"?pad_lines(20)
// "int x;              \nString name;        \n"
```

With a custom fill character:

```
"a\nbb\n"?pad_lines(10, '.')
// "a.........\nbb........\n"
```

The camelCase alias `?padLines` is also supported.

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | number | yes | Target column width |
| 2 | string | no | Single fill character (default: space) |

**Use case** — aligning multi-line output for trailing comments:

```
emit code?pad_lines(40) // then append comments per line
```

### `?wrap(width, firstPrefix, restPrefix)`

Word-wraps the string to fit within `width` columns, using `firstPrefix` for the first line and `restPrefix` for subsequent lines. If `restPrefix` is omitted, `firstPrefix` is used for all lines. Output always ends with a newline.

```
text = "This is a long description that should be wrapped"
emit text?wrap(40, " * @brief ", " *          ")
// Output:
//  * @brief This is a long description
//  *          that should be wrapped
```

With a single prefix for all lines:

```
emit "A long comment that needs to be wrapped at a reasonable width"?wrap(40, "// ")
// Output:
// // A long comment that needs to be
// // wrapped at a reasonable width
```

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | number | yes | Maximum line width |
| 2 | string | yes | Prefix for the first line |
| 3 | string | no | Prefix for subsequent lines (default: same as first) |

---

## Complete Example

A code generator that produces a Java class from a data model:

```
// generate-entity.ftlc

import "java-utils.ftl" as ju

emit """
package ${package};

"""

// Imports
list imports as imp
  emit "import ${imp};\n"
endlist
emit "\n"

// Class declaration
emit """
public class ${className} {

"""

// Fields
list fields as field
  emit "    private ${field.type} ${field.name};\n"
endlist
emit "\n"

// Getters and setters
list fields as field
  // Getter
  emit """
    public ${field.type} get${field.name?cap_first}() {
        return this.${field.name};
    }

"""

  // Setter
  emit """
    public void set${field.name?cap_first}(${field.type} ${field.name}) {
        this.${field.name} = ${field.name};
    }

"""
endlist

emit "}\n"
```

---

## Syntax Summary

| Classic FTL | Code-First |
|---|---|
| `<#if cond>...</#if>` | `if cond`...`/if` or `endif` |
| `<#list xs as x>...</#list>` | `list xs as x`...`/list` or `endlist` |
| `<#macro m(a)>...</#macro>` | `macro m(a)`...`/macro` or `endmacro` |
| `<#function f(a)>...</#function>` | `function f(a)`...`/function` or `endfunction` |
| `<#assign x = 1>` | `x = 1` or `assign x = 1` |
| `<#local x = 1>` | `x = 1` (inside macro) or `local x = 1` |
| `<#global x = 1>` | `global x = 1` |
| `<#return expr>` | `return expr` |
| `<#import "x" as y>` | `import "x" as y` |
| `<#include "x">` | `include "x"` |
| `<#switch x>...</#switch>` | `switch x`...`/switch` or `endswitch` |
| `<#nested>` | `nested` |
| `<#stop "msg">` | `stop "msg"` |
| `<#attempt>...<#recover>...</#attempt>` | `attempt`...`recover`...`endattempt` |
| `<#items as x>...</#items>` | `items as x`...`enditems` |
| `<#visit node>` | `visit node` |
| `<#recurse>` | `recurse` |
| `<#fallback>` | `fallback` |
| `<#flush>` | `flush` |
| `<#setting k=v>` | `setting k = v` |
| `<#autoesc>...</#autoesc>` | `autoesc`...`endautoesc` |
| `<#noautoesc>...</#noautoesc>` | `noautoesc`...`endnoautoesc` |
| `${expr}` | `emit expr` |
| `<#-- comment -->` | `// comment` or `/* comment */` |
| `text` (direct output) | `emit "text"` or `emit """text"""` |
| Multi-line text | `emit """..."""` (text block) |
| `@macro args` | `macro(args)` or `ns.macro(args)` |
| `0xFF` (not supported) | `0xFF` (both modes) |
| (not available) | `&`, `\|`, `^`, `~`, `<<`, `>>` (bitwise) |
| (not available) | `&=`, `\|=`, `^=`, `<<=`, `>>=` (bitwise assign) |
| (not available) | `?tab_to(col)`, `?tab_to(col, fill)` (pad to column) |
| (not available) | `emit expr to <target>` (multi-target output) |
| (not available) | `read from <target>` (eager file read) |
| (not available) | `readln from <target>` (lazy line iteration) |
