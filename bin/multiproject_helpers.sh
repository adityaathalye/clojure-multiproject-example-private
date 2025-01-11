#!/usr/bin/env bash

[ -z ${multiproject_root} ] &&
    declare -r multiproject_root="$(realpath $HOME/src/github/adityaathalye/clojure-multiproject-example)"

echo "Sourcing utils for $multiproject_root:"
sed -E -ne "s/(^.*)\(\)\s+\{/\1/p" "${multiproject_root}/bin/multiproject_helpers.sh"

grug_repl() {
    local aliases=$(sed -E -n -e "s/.*\:ns\-default\s+(.*)\s?.*/\1/p" "${multiproject_root}/deps.edn")

    echo $aliases

    select alias in "CANCEL" ${aliases}
    do
        case ${REPLY} in
            1) echo "Cancelling..."
               break
               ;;
            *) local clj_cmd_opts="-M:root/all:root/dev:root/test:${alias}:cider"
               echo "Starting dev REPL for project alias: ${alias} with opts ${clj_cmd_opts}"
               clj ${clj_cmd_opts} --socket "${alias}.repl"
               break
               ;;
        esac
    done
}
