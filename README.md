# Clojure Multi-Project Example Layout and Tool Use

Warning: This project is very alpha-quality. It is made available with
a works-for-me-on-my-machine (and for my purposes) quality guarantee.

# Design notes

Here's my thinking. If you want to give me design notes, please do, in
the project issues!

The attempt is to...
- Share a common "system" (which I'm calling grugstack).
- Across multiple apps; whether standalone (like example_app), or for
  a customer (e.g. acmecorp/snafuapp). (Want to build a Micro-SaaS?
  For outside customers? Inside customers? Private tool? Hire me!)
- Using only out-of-the-box tooling (not out-of-the-box thinking).

Conceptually, it is aspirational.
- The *Way* is not Purely Functional /or/ Purely Object Oriented, but
  a "Best of Both" approach, all the way down to code layout.
  - A system of parts (functions) that glue together *à la carte*,
  - via carefully constructed (namespaced, structured) plain Clojure
    data,
  - into any number of runnable apps (open-ended polymorphism) (see
    [credits](#credits)).

Ideally, I want to get away with:
- **the simplest possible project tree layout**, even if it makes code
  and command-line invocations repetitive or verbose. My grug brain
  can easily plod along long paths, as long as they are made painfully
  obvious.
- **the fewest possible bespoke abstractions or terms of art**,
  because making a Domain Specific Language is easy, but keeping it
  sensible is hard.
- **no custom build tooling.** I just want to appropriate the raw
  machinery and public contracts provided by deps.edn, tools.build,
  and Clojure CLI. I've chosen to used in the usual way, via a single
  multi-project-level `build.clj`. Stare at the `build.clj` file and
  the multi-project-level `deps.edn` file side-by-side to see the
  main trick I've used... I figured out a permutation of (alias names
  x grouping of paths x grouping of dependencies x grouping of args
  that must get overridden to narrow context to the last alias).

By design, I want to run tools / start REPLs etc. ***only at the root
of the multi-project***, and use explicit command-line options to
broaden or narrow scope of the command / REPL to the part(s) of the
multi-project that I want to target. This helps me keep a simple
mental model of managing the whole or the part of the multi-project.

# Requirements

Required:

A [working Clojure
installation](https://clojure.org/guides/install_clojure "Clojure
install instructions") (prefer the latest available Clojure).

A `deps.edn` at the root of the source, that is used to orchestrate
projects / apps. Each project/app must have a minimal `deps.edn` under
its project root.

Optional:

Help LSP recognise individual apps/projects by placing a `.gitignore`
under the project root. This helps in completions, refactoring, etc.

# Usage

Use aliases from the root-level deps.edn to run everything from the
root of the source repo. Here "everything" including tests, builds, CI
tasks.

## REPL Development

I prefer to start REPLs at the shell, and connect to them via my code
editor. This lets me independently kill/restart/manage the REPL
process and the Editor process. Sometimes one of them can go into a
bad state. Live REPL state tends to collect orphan objects the longer
they are live. Sometimes I want to force-invalidate some editor
state, which may require killing and cold-starting it. etc...

### Start a REPL at a random port.

This is the most common way to start a REPL. This works just fine for
conventional single-repo single-app style projects.
  ```shell
  clj -M:root/all:root/dev:root/test:cider
  ```
### Start REPL at specific UNIX domain socket.

This is my preferred tactic to trivially share or isolate REPLs from
each other, in a multi-project context. The trick is to name socket
paths along the project directory paths structure. For example:
- To imply a REPL is shared across the whole multi-project, create the
  UNIX domain socket at the root of the multi-project repo.

  ```shell
  clj -M:root/all:root/dev:root/test:cider --socket "repl.socket" # shared REPL for
  ```
- To imply a REPL is specific to a project, create the socket at the
  root of the project directory.

  ```shell
  clj -M:root/all:root/dev:root/test:com.example.core:cider --socket "projects/example_app/repl.socket"

  clj -M:root/all:root/dev:root/test:com.acmecorp.snafuapp:cider --socket "projects/acmecorp/snafuapp/repl.socket"
  ```

How this lets us trivially isolate REPL-specific state when we need to:

I've created a multi-project-level  code to bootstrap utilities into the
`user` namespace can sit at the project root `dev/user.clj`, in
our case. It contains handy REPL utilities to manipulate `system`
state (start / stop / restart), as well as spin up dev tools like
portal. If we have three REPLs running at independent sockets, the
state is isolated automatically, by construction.  At the same
time, we can access any permutation of the multi-project codebase
in each REPL context. Thus, inert code is shared, but live state
is isolated.

## TEST running

- Test all apps:
  ```shell
  clj -X:root/all:root/test
  ```
  - The trick is to declare all test target directories; viz. root of
    all apps and of grugstack. PLUS, inject grugstack as an extra-deps
    (via :root/all).

- Test only one app:
  - To narrow test target to a single project at a time, we rely on
    the published alias override/merge behaviour of deps.edn.
  - Here do it for SNAFUapp by ACME Corp. By appending its deps.edn
    alias, we narrow test target to only SNAFuapp test dirs. This is
    due to the "win last" override/merge order of aliases
    [defined by Clojure CLI](https://clojure.org/reference/clojure_cli#aliases).
  ```shell
  clj -X:root/all:root/test:com.acmecorp.snafuapp.core
  ```
  - Here we do the same narrowing for `grugstack` itself.
  ```shell
  clj -X:root/all:root/test:grugstack
  ```

## BUILD options

### Full CI

Run full CI sequence for individual apps.

- BUILD default example project specified by alias under this path in
  our deps config -> [:root/build :extra-args :app-alias].
  ```shell
  clj -T:root/build
  ```
  Then check it with...
  ```
  java -jar target/com.example.core/com.example.core-*.jar
  ```
- BUILD project identified by :app-alias
  ```
  clj -T:root/build :app-alias ':com.acmecorp.snafuapp.core'
  ```
  Then check it with...
  ```
  java -jar target/com.acmecorp.snafuapp.core/com.acmecorp.snafuapp.core-*.jar

  # Followed by

  curl localhost:13337
  ```

### UBERJAR only

Package a single app into an uberjar.

Run only "uberjar" part of the CI sequence, for app identified by
`:app-alias`.

```shell
clj -T:root/build uberjar :app-alias ':com.acmecorp.snafuapp.core'
```

Then check it as described above

### TEST only

Run only "test" part of the CI sequence, for apps identified by
`:app-alias`.

```shell
clj -T:root/build test :app-alias ':com.acmecorp.snafuapp.core'
```

# TODO

Lots.

# Contributing

As of now, I am in discussions-only mode... Ping me in the Clojurians
Slack or Zulip or [this thread](https://groups.google.com/g/clojure/c/uroL-ftfrqY)
in the official mailing list.

# Credits

I've consumed way too much web-stack buildin' prior art from across
the Clojure ecosystem. Clojure core, community librarians, builders,
architects, teachers, book authors: the whole lot of you; thank you!

Special mentions to authors making projects and explanations for use
by solo/indie web app builders: biff, duct, zodiac, caveman.

Last but not least, special mention to `polylith`, which lit a key
light bulb in my head... The "Expression Problem" applies to code
layout and application architecture too! Not Functions v/s Objects;
but Functions *and* Objects.

That said, I'm not sure I've "got it" yet. So to take it back to the
top, please [send design notes my way](#design-notes)!

# License & Copyright

Copyright (c) 2025 Aditya Athalye.

Distributed under the MIT license.
