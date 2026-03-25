# FreeMarker Code-First Mode

Code-first mode inverts FreeMarker's default behavior: **logic is the default** and text output requires explicit delimiters. This is designed for use cases where FreeMarker is used as a code generation language rather than a document template processor.

All changes are isolated to the parser/lexer layer. No new AST node types are introduced — all code-first syntax maps to existing FreeMarker internals. Existing `.ftl` files and behavior are completely unchanged.

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

`emit` followed by any expression outputs its value:

```
emit "Hello, World!\n"
emit x?c
emit someFunction()
```

String literals support interpolation with `${...}`:

```
name = "Alice"
emit "Hello, ${name}!\n"
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

Use `name(args)` syntax:

```
greet("World")
x = add(1, 2)
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

---

## import and include

```
import "lib/utils.ftl" as u
include "header.ftl"
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
| `${expr}` | `emit expr` |
| `<#-- comment -->` | `// comment` or `/* comment */` |
| `text` (direct output) | `emit "text"` or `emit """text"""` |
| Multi-line text | `emit """..."""` (text block) |
| `0xFF` (not supported) | `0xFF` (both modes) |
| (not available) | `&`, `\|`, `^`, `~`, `<<`, `>>` (bitwise) |
| (not available) | `&=`, `\|=`, `^=`, `<<=`, `>>=` (bitwise assign) |
