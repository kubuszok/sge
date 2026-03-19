---
description: Load and apply the SGE code style rules including license headers, braces, split packages, and naming conventions
---

Load and apply the SGE code style rules.

Read the code style guide:
$READ docs/contributing/code-style.md

Key rules:
- License header: Apache 2.0 with original source attribution
- Braces required for all class/trait/method definitions (`-no-indent`)
- Split package declarations (`package sge` / `package graphics`)
- Preserve all original comments
- No `return`, no `null`, no `scala.Enumeration`
- Case classes must be `final`
- No Java-style getters/setters

Apply these rules when reviewing or writing SGE code. If $ARGUMENTS names a file,
check it for style compliance.
