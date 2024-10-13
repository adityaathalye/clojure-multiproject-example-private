# Clojure Multi-Project Example Layout and Tool Use

Very alpha-level thinking. Works-for-me-on-my-machine quality
guarantee.

The attempt is...
- to share a common "system" (which I'm calling grugstack)
- across multiple apps; whether standalone (like example_app), or for
  a customer (e.g. acmecorp/snafuapp).
- using only out-of-the-box tooling (not out-of-the-box thinking)

It is aspirational --- ideally, I want to get away with:
- the simplest possible project tree layout, even if it is a bit
  repetitive or verbose. My grug brain can easily plod along long
  paths, as long as they are made painfully obvious.
- the fewest possible bespoke ideas or terms of art,
- no build custom tooling. I just want to appropriate the public
  contracts provided by deps.edn, tools.build, and Clojure CLI.

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

- Start a REPL at a random port
  ```shell
  clj -M:root/all:root/dev:root/test:cider
  ```

- Start REPL at unix domain socket. Potentially use this to isolate project REPLs.
  ```shell
  clj -M:root/all:root/dev:root/test:com.example.core:cider --socket
  "example_app.socket"
  ```
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

# License & Copyright

Copyright (c) 2025 Aditya Athalye.

Distributed under the MIT license.
