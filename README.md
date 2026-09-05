# FreeMarker Code-First Mode

Code-first mode inverts FreeMarker's default behavior: **logic is the default** and text output requires explicit delimiters. This is designed for use cases where FreeMarker is used as a **code generation language** rather than a document template processor.

## Versioning

FreeMarker Codegen and the embedded Apache FreeMarker engine have independent versions:

- `Configuration.getCodegenVersion()` and the `.codegen_version` / `.codegenVersion` special variables report the
  Codegen release version.
- `Configuration.getVersion()` and the `.version` special variable continue to report the upstream FreeMarker version
  used for compatibility checks and `incompatible_improvements`.

The authoritative values are stored together in
`freemarker-core/src/main/resource-templates/freemarker/version.properties`.

## FMPP integration

FreeMarker Codegen can be used with FMPP by replacing the `freemarker.jar` in the FMPP installation with the JAR from
this project. If necessary, rename `freemarker-codegen-gae-<version>.jar` to `freemarker.jar` so existing FMPP launch
scripts continue to find it. Remove the original JAR rather than leaving both versions on the classpath.

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
| Directive syntax | `<#if cond>...</#if>` | `if cond`...`end` |
| Text output | Implicit (everything is output) | Explicit (`emit`) — no whitespace surprises |
| Comparisons | `(x > 0)` or `x gt 0` | `x > 0` — just works |
| Bitwise ops | Not available | `&`, `\|`, `^`, `~`, `<<`, `>>` |
| Hex literals | Not available | `0xFF` (also enabled in classic mode) |
| Comments | `<#-- comment -->` | `// comment` or `/* comment */` |
| Assignment | `<#assign x = 1>` | `assign x = 1` |

The result is templates that **read like the code they generate**, with a clean imperative syntax that any developer can follow without learning FreeMarker's tag conventions.

## Design principles

- **Parser-only change.** All code-first syntax maps to existing FreeMarker AST nodes. There is no runtime difference — the same engine evaluates both modes identically.
- **Zero impact on existing templates.** Standard `.ftl` files and behavior are completely unchanged. Code-first mode is strictly opt-in.
- **Full interoperability.** `.ftl` and `.ftlc` files can freely import and include each other. Each file is parsed independently with its own mode. Both modes produce identical AST nodes, so there is no runtime difference:

```
// In a .ftlc file:
import "utils.ftl" as u
```

```ftl
<#-- In a .ftl file: -->
<#import "generator.ftlc" as gen>
```

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
assign name = "Alice"
emit "Hello, ${name}!\n"
```

Interpolation is a property of the string literal itself — it works in **any** string literal, not just in `emit`. For example in assignments, function arguments, or sequence/hash literals:

```
assign greeting = "Hello, ${name}!"
assign items    = ["file-${id}.txt", "backup-${id}.bak"]
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

## Variables and Assignment

An assignment says which scope it writes to, with `assign`, `local` or `global`:

| Keyword | Scope | Equivalent classic FTL |
|---|---|---|
| `assign` | Current namespace | `<#assign>` |
| `local` | Local (macro/function only) | `<#local>` |
| `global` | Global | `<#global>` |

```
// Template level
assign x = 1
assign name = "World"
assign items = ["a", "b", "c"]

macro greet(who)
  local greeting = "Hello, ${who}"
  emit greeting
/macro

macro compute()
  local temp = heavyCalc()     // local to this call
  assign result = temp * 2     // writes to the template namespace
  global cached = result       // writes to the global scope
end
```

> **Changed:** the bare `x = 1` form is no longer accepted. It could only mean "namespace at
> the top level, local inside a macro or function", which makes moving a fragment of a template
> into a macro silently change where the value is written. The parser now says which keyword to
> use:
>
> ```
> macro m()
>   temp = 1        // error: Assignment without a scope keyword isn't supported:
>                   //        write "local temp = ..." instead.
> end
> ```
>
> The underlying problem is FreeMarker's `#assign`/`#local` pair. Were there a `#set` for
> assignment and a `#var` for block-scoped declaration instead, a scopeless assignment would
> just mean `#set`, with no dependence on where it appears — but that's a change for FreeMarker
> itself, not for this mode.

### Compound assignment operators

```
assign x = 10
assign x += 5
assign x -= 2
assign x *= 3
assign x /= 4
assign x %= 3
assign x++
assign x--
```

These work with all three scope keywords: `assign x += 1`, `local count++`, `global total -= n`.

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

### Hex literals

Hex integer literals are supported in **both** classic and code-first modes:

```
assign x = 0xFF        // 255
assign y = 0x00FF00    // 65280
assign color = 0xDEAD  // 57005
```

There is no limit on the number of digits: values that fit in a signed 32-bit integer produce
`Integer`, values that fit in a signed 64-bit integer produce `Long`, and anything larger
produces `BigInteger`.

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
assign flags = 0xFF
assign flags &= 0x0F       // AND assign
assign flags |= 0x80       // OR assign
assign flags ^= 0x01       // XOR assign
assign flags <<= 4         // left shift assign
assign flags >>= 2         // right shift assign
```

Example — extracting color channels from an RGB value:

```
assign color = 0x1A803C
assign red = (color >> 16) & 0xFF
assign green = (color >> 8) & 0xFF
assign blue = color & 0xFF
emit "R=${red?c} G=${green?c} B=${blue?c}\n"
// Output: R=26 G=128 B=60
```

---

## Block Directives

Block directives use keyword syntax, and a block is closed in one of two ways:

| Closer | Meaning |
|---|---|
| `end` | Closes whatever block is open |
| `/if`, `/list`, `/macro`, `/function`, `/switch`, `/sep`, `/items`, `/attempt`, `/autoesc`, `/noautoesc` | Closes that specific block, and is checked |

These aren't two spellings of one thing. `end` is generic; `/something` states what is being
closed, so the parser can tell you when a block was closed by mistake:

```
function f()
  return 1
/macro          // error: Expected /function or end, not /macro
```

`end` alone can't catch that — a mismatch only shows up later, at the outer level, with a
vaguer message. **Prefer `/something` for anything non-trivial**, and keep `end` for short
blocks where the opener is still on screen. The pairing is the one Ada uses (`end;` versus
`end Foo;`); bare `end` on its own is the Pascal-family convention, and what Ruby, Lua, Julia
and Elixir settled on too.

> **Changed:** the per-block keywords `end`, `end`, `end`, `end`,
> `end`, `end`, `end`, `end`, `end` and `end` have been
> replaced by the single generic `end`. Ten keywords became one, and `end`, `end` and so on
> are no longer reserved words. (`/attempt`, `/items`, `/autoesc` and `/noautoesc` were
> documented before this change but didn't actually parse; they work now.)

### if / elseif / else

```
if user.active
  emit "Welcome back, ${user.name}!\n"
elseif user.pending
  emit "Your account is pending.\n"
else
  emit "Please register.\n"
end
```

Conditions are terminated by end-of-line. The `>` and `>=` operators work without parentheses (unlike classic mode):

```
if score > 90
  emit "Excellent!\n"
end
```

### Multiline expressions

Wrap the expression in `()` to span multiple lines. Inside parentheses, newlines are ignored:

```
if (longConditionA &&
    longConditionB &&
    longConditionC)
  emit "all true\n"
end
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
end
```

Both `()` and `\` can be used — choose whichever reads better in context.

### list

```
list users as user
  emit "${user.name}\n"
end
```

With key-value iteration:

```
list settings as key, value
  emit "${key} = ${value}\n"
end
```

With `else` for empty lists:

```
list results as result
  emit "${result}\n"
else
  emit "No results found.\n"
end
```

### sep

```
list items as item
  emit item
  sep
    emit ", "
  end
end
// Output: a, b, c
```

### items

The alternate `list` form uses `items` to separate the iterable expression from the loop variable. This allows content before and after the loop, and an `else` that fires when the list is empty:

```
list users
  emit "<ul>\n"
  items as user
    emit "  <li>${user.name}</li>\n"
  end
  emit "</ul>\n"
else
  emit "<p>No users.</p>\n"
end
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
end
```

---

## Control Flow

### break and continue

```
list items as item
  if item == "skip"
    continue
  end
  if item == "stop"
    break
  end
  emit "${item}\n"
end
```

### return

Inside a function, `return` provides the return value:

```
function double(n)
  return n * 2
end
```

Inside a macro, `return` exits early:

```
macro conditionalGreet(name)
  if !name?has_content
    return
  end
  emit "Hello, ${name}!\n"
end
```

### stop

Aborts template processing with an error message:

```
if !requiredParam??
  stop "Missing required parameter: requiredParam"
end
```

### attempt / recover

Error handling — if the `attempt` block fails, execution continues in the `recover` block:

```
attempt
  assign result = riskyOperation()
recover
  emit "Operation failed, using default.\n"
  assign result = defaultValue
end
```

---

## Macros and Functions

### macro

```
macro page(title, body)
  emit "<!DOCTYPE html>\n"
  emit "<html><head><title>${title}</title></head>\n"
  emit "<body>${body}</body></html>\n"
end
page("Home", "Welcome!")
```

Parameters can have defaults:

```
macro button(label, type = "submit")
  emit "<button type=\"${type}\">${label}</button>\n"
end
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
  end
end
emit max(10, 20)?c
// Output: 20
```

### Calling macros and functions

Use `name(args)` syntax with positional or named arguments:

```
greet("World")
assign x = add(1, 2)
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

### Method-style function calls (`?`)

A function can be called with `?` syntax, where the value on the left becomes the function's first argument:

```
function shout(s)
  return s?upper_case
end
emit "hi"?shout()        // same as shout("hi") → "HI"
```

`x?name(args)` is exactly equivalent to `name(x, args)` — it's pure syntactic sugar. The benefit is readability when chaining transformations, which read left-to-right in the order they apply:

```
emit text?trimmed()?shout()?indent("  ")
// same as: indent(shout(trimmed(text)), "  ")
```

Name resolution follows the usual rules — bare names resolve in the current namespace, dotted names in an imported one:

```
import "lib/utils.ftlc" as u
emit name?u.format()     // same as u.format(name)
```

**Built-ins always take precedence.** `x?upper_case` is the built-in, even if you define a function named `upper_case`. The `?name(...)` form only resolves to a function when `name` is not a built-in.

This is available **only in code-first mode**. In classic `.ftl`, a function body can produce text whose presence depends on whitespace-stripping settings, so calling a function in an expression context could have surprising output side effects. Code-first mode produces output only via `emit`, so a function call in an expression is guaranteed to have no hidden output — which is what makes this safe here.

### nested

Inside a macro, `nested` outputs the caller-provided body content:

```
macro wrapper(title)
  emit "<div class=\"box\">\n"
  emit "  <h2>${title}</h2>\n"
  nested
  emit "</div>\n"
end
```

`nested` can also pass loop variables back to the caller:

```
macro repeat(count)
  list 1..count as i
    nested i
  end
end
```

---

## import and include

```
import "lib/utils.ftl" as u
include "header.ftl"
```

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
assign header = "include/${name}.h"
assign source = "src/${name}.c"

emit "#ifndef ${name?upper_case}_H\n" to header
emit "#define ${name?upper_case}_H\n" to header
emit "#include \"${name}.h\"\n" to source

list functions as f
  emit f.prototype + ";\n" to header        // declaration in .h
  emit f.body to source                      // implementation in .c
end
emit "#endif\n" to header
```

The `.h` and `.c` files are generated in a single pass, with related code emitted adjacent in the template — making cross-file consistency easy to maintain.

### Reading: `read from` and `readln from`

Two read primitives are available as expressions:

```
assign text = read from "data.txt"                 // entire file as a string
assign text = read from stdin                      // entire stdin as a string

list (readln from "big.csv") as line        // lazy line-by-line iteration
  emit line + "\n"
end
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
end
```

This iterates the source once, top to bottom, without ever loading the whole file.

**Operations not supported** on a lazy collection (would require materialization):

- `?size`, `[i]` indexing — error
- `?sort`, `?reverse` — error

If you need these, materialize explicitly with `?sequence`:

```
assign all = (readln from "data.txt")?sequence       // reads ENTIRE file into memory
emit all?size?c                                // works
emit all?sort?join("\n")                       // works
```

`?sequence` is the explicit boundary between streaming and in-memory operations. It makes the cost visible at the call site.

### Multiple `readln` on the same file

Each `readln from <target>` call opens a fresh reader. Two calls on the same path are independent:

```
list (readln from "data.csv") as line     // first pass
  // validate
end
list (readln from "data.csv") as line     // second pass — file reopened
  // emit
end
```

Unlike output handles (which are cached for write coalescing), input handles are not cached — repeated reads of the same file are always fresh.

### Reading a freshly-written file

If you write to a file with `emit ... to <path>` and read it back with `read from <path>` or `readln from <path>` later in the same template, the runtime flushes the open writer before opening the reader. The newly-written content is visible.

### Stdin termination

Reading from `stdin` ends naturally at EOF — Ctrl-D in a terminal, or end-of-pipe when piped from another command:

```
list readln from stdin as line
  emit line + "\n"
end
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

With `output_eol` unset (the default), this produces `line1\nline2\n`. With `output_eol` set to `"\r\n"`, it produces `line1\r\nline2\r\n`.

| Escape | Meaning |
|---|---|
| `\n` | Always literal `\n` (Unix LF) |
| `\e` | Configured EOL — resolves to `output_eol` at runtime |

### Text block normalization

Line endings inside text blocks (`emit """..."""`) are automatically normalized to the configured `output_eol`. This means a template edited on Windows (with `\r\n`) produces the same output as one edited on Unix (with `\n`) — the template file's line endings never leak through.

### Configuring `output_eol`

`output_eol` is **unset by default**, which means FreeMarker doesn't prescribe any line ending. Setting it
affects three things:

| | `output_eol` unset (default) | `output_eol` set |
|---|---|---|
| `\e` in a string literal | `\n` | the configured value |
| Text blocks (`emit """..."""`) | normalized to `\n` | normalized to the configured value |
| Static text of a **classic** `.ftl` | left as the template file has it | normalized to the configured value |

The last row is the only one that behaves differently depending on whether the setting is set, and it
doesn't arise in code-first mode at all: a `.ftlc` file has no static text, since every character of output
comes from an `emit`.

Values inserted by `${...}` are never affected — this setting is about the template, not about the data.
Neither is `\n`, which always produces a line feed; that distinction is the point of having both.

**Via configuration (Java):**

```java
cfg.setOutputEol("\r\n");  // Windows line endings
```

**Via `setting` directive:**

```
setting output_eol = "\r\n"
```

The value `"JVM default"` is also accepted, resolving to the platform's line separator. Prefer naming the
line ending explicitly: with `"JVM default"` the same template and data produce different bytes on
different machines, which shows up as line-ending churn if the generated files are committed to version
control.

The `\e` escape and the static-text normalization both work in classic mode too — only the text block
normalization is code-first specific.

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
end
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
end
noautoesc
  emit rawHtml    // no escaping
end
```

---

## Built-in reference

These built-ins are available in **both** classic and code-first modes. They are part of Apache
FreeMarker as of 2.3.35 — see the FreeMarker Manual for the authoritative reference.

### `?indent(prefix)` / `?indent(prefix, rightTrim)`

Prepends `prefix` to **every** line, then removes the trailing whitespace of each resulting line
unless `rightTrim` is `false` (it defaults to `true`).

```
assign body = "int x;\nint y;\n"
emit body?indent("    ")
// Output:
//     int x;
//     int y;
```

The prefix is added to blank lines too, and the trimming is what keeps that from leaving junk
behind. With a `"# "` prefix a blank line becomes `"#"`, not `"# "` — the space in `"# "` is a
separator, only wanted when the line has content:

```
emit "First paragraph.\n\nSecond paragraph."?indent("# ")
// Output:
// # First paragraph.
// #
// # Second paragraph.
```

With a whitespace-only prefix this leaves blank lines empty, so the trimming is only visible with
prefixes like the above. It also means empty lines and whitespace-only lines behave identically,
and that accidental trailing whitespace is removed from content lines as well. A non-breaking
space (U+00A0) is not trimmed — that is the point of a non-breaking space.

An empty prefix does nothing at all, not even trimming.

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | string | yes | Prefix to prepend to each line |
| 2 | boolean | no | Right-trim each resulting line (default: `true`) |

### `?dedent` / `?dedent(prefix)` / `?dedent(prefix, rightTrim)`

Removes leading indentation. Two forms — note the no-argument form takes **no parentheses**:

**No-argument form `?dedent`** — Python `textwrap.dedent`-style. Finds the longest leading
whitespace (spaces and tabs only) that is a common prefix of every non-empty line, and removes it.
Lines that are empty or contain whitespace only are ignored when computing the prefix, and come out
empty.

```
"    int x;\n  int y;\n      int z;"?dedent
// Output (common prefix is "  ", 2 spaces):
//   int x;
// int y;
//     int z;
```

A leading tab and a leading space are distinct characters — they have no common prefix. This
matches Python's behaviour.

**Explicit-prefix form `?dedent(prefix)`** — removes from each line the longest prefix of `prefix`
that the line actually starts with. A line carrying the whole prefix loses all of it; a line
carrying only part of it loses that part; a line sharing nothing with it is untouched.

```
assign body = "    int x;\n    int y;\n"
emit body?dedent("    ")
// Output:
// int x;
// int y;
```

Partial matches are shortened rather than ignored, which is what a code editor does when you dedent
a block where some lines have already reached column 0:

```
"  short\n    full\n"?dedent("    ")
// Output:
// "short\n"   (2 of the 4 spaces matched, both removed)
// "full\n"    (all 4 matched, removed)
```

Like `?indent`, this trims trailing whitespace unless the 2nd parameter is `false`; that is what
makes a whitespace-only line come out empty rather than keeping the whitespace the prefix didn't
cover. An empty prefix does nothing at all.

Round-trip with `?indent`:

```
text?indent("  ")?dedent("  ")   // returns original text
```

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | string | no | Prefix to remove. Omit (with no parentheses) for common-leading-whitespace mode. |
| 2 | boolean | no | Right-trim each resulting line (default: `true`) |

### `?wrap(width)` / `?wrap(width, firstPrefix)` / `?wrap(width, firstPrefix, restPrefix)`

Word-wraps the string to fit within `width` columns, using `firstPrefix` for the first line and
`restPrefix` for subsequent lines. If `restPrefix` is omitted, `firstPrefix` is used for all lines;
if both are omitted, no prefix is used. Output always ends with a newline.

```
assign text = "This is a long description that should be wrapped"
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

All whitespace in the input is collapsed, including line breaks, so the input's own line structure
does not carry through.

**Parameters:**

| # | Type | Required | Description |
|---|---|---|---|
| 1 | number | yes | Maximum line width (in characters, not display columns) |
| 2 | string | no | Prefix for the first line (default: none) |
| 3 | string | no | Prefix for subsequent lines (default: same as first) |

> **Note on widths and whitespace:** widths are counted in Java `char`s (UTF-16 code units), not
> visual display columns — same as `?right_pad` / `?left_pad`. A tab counts as one character, not as
> "advance to next tab stop". If you need visual alignment for content containing tabs, expand them
> to spaces first. Word boundaries are detected via Java's `\s+`, which does **not** include U+00A0
> (non-breaking space); a non-breaking space therefore stays inside a word and is never used as a
> break point, even if that makes the line overflow `width`. This is intended — you cannot wrap what
> must not be split.

### Aligning a block of lines

There is no multi-line right-pad built-in. To align a column of lines, split and loop, which keeps
every single-line built-in available and lets you append after the padding of each line:

```
list code?lines as line
  emit (line + " |")?right_pad(76) + "\\\n"
end
```

`?lines` (also new in FreeMarker 2.3.35) splits a string into its lines.

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
end
emit "\n"

// Class declaration
emit """
public class ${className} {

"""

// Fields
list fields as field
  emit "    private ${field.type} ${field.name};\n"
end
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
end
emit "}\n"
```

---

## Syntax Summary

| Classic FTL | Code-First |
|---|---|
| `<#if cond>...</#if>` | `if cond`...`/if` or `end` |
| `<#list xs as x>...</#list>` | `list xs as x`...`/list` or `end` |
| `<#macro m(a)>...</#macro>` | `macro m(a)`...`/macro` or `end` |
| `<#function f(a)>...</#function>` | `function f(a)`...`/function` or `end` |
| `<#assign x = 1>` | `assign x = 1` |
| `<#local x = 1>` | `local x = 1` |
| `<#global x = 1>` | `global x = 1` |
| `<#return expr>` | `return expr` |
| `<#import "x" as y>` | `import "x" as y` |
| `<#include "x">` | `include "x"` |
| `<#switch x>...</#switch>` | `switch x`...`/switch` or `end` |
| `<#nested>` | `nested` |
| `<#stop "msg">` | `stop "msg"` |
| `<#attempt>...<#recover>...</#attempt>` | `attempt`...`recover`...`end` |
| `<#items as x>...</#items>` | `items as x`...`end` |
| `<#visit node>` | `visit node` |
| `<#recurse>` | `recurse` |
| `<#fallback>` | `fallback` |
| `<#flush>` | `flush` |
| `<#setting k=v>` | `setting k = v` |
| `<#autoesc>...</#autoesc>` | `autoesc`...`end` |
| `<#noautoesc>...</#noautoesc>` | `noautoesc`...`end` |
| `${expr}` | `emit expr` |
| `<#-- comment -->` | `// comment` or `/* comment */` |
| `text` (direct output) | `emit "text"` or `emit """text"""` |
| Multi-line text | `emit """..."""` (text block) |
| `@macro args` | `macro(args)` or `ns.macro(args)` |
| `0xFF` (not supported) | `0xFF` (both modes) |
| (not available) | `&`, `\|`, `^`, `~`, `<<`, `>>` (bitwise) |
| (not available) | `&=`, `\|=`, `^=`, `<<=`, `>>=` (bitwise assign) |
| (not available) | `emit expr to <target>` (multi-target output) |
| (not available) | `read from <target>` (eager file read) |
| (not available) | `readln from <target>` (lazy line iteration) |
| `@function x` (no equivalent) | `x?func(args)` (method-style call → `func(x, args)`) |
