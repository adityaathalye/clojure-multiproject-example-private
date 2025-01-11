#!/usr/bin/env bash
this_helper_script_path="$(realpath ${0})"
multiproject_root="$(dirname $(dirname this_helper_script_path))"

declare -a multiproject_commands=( $(sed -E -ne "s/(^run_+.*)\(\)\s+\{/\1/p" "${this_helper_script_path}" | sort ) )

# Copy all project deps to clipboard
# clj -X:deps aliases |
#     sed -E -n -e "s/\:([[:alnum:][:punct:]]+)\s+.*project.*/\1/p" |
#     xargs -I{} printf "\"%s\" " {} |
#     xclip -sel clipboard
declare -a multiproject_all_toplevel_aliases=(
    "cider" "grugstack" \
            "com.acmecorp.snafuapp.core" "com.example.core" "usermanager.main" \
            "root/all" "root/build" "root/dev" "root/run-x" "root/test"
)

declare -a multiproject_app_aliases=(
    "grugstack" "com.acmecorp.snafuapp.core" "com.example.core" "usermanager.main"
)

main() {
    local command_chosen=""

    cat <<EOF
==================================================
CHOOSE COMMAND to run.
  (Next menu: Choose project scope by alias.)
  (Hit 'x' to cancel.)
==================================================
EOF
    select mul_cmd in ${multiproject_commands[*]}
    do
        case ${REPLY} in
            "x" )
                echo "Cancelling..."
                command_chosen=""
                break
                ;;
            [[:digit:]]|[[:digit:]][[:digit:]] )
                if ! [ -z $mul_cmd ]
                then echo "Command chosen: ${mul_cmd}."
                     command_chosen="${mul_cmd}"
                     break
                else
                    echo "Invalid choice."
                    command_chosen=""
                fi
                ;;
            * )
                echo "Invalid choice."
                command_chosen=""
                ;;
        esac
    done

    if ! [ -z $command_chosen ]
    then
        cat <<EOF
==================================================
CHOOSE PROJECT ALIAS to run command: ${command_chosen}
  (Next menu: choose command to run for alias.)
  (Hit 'x' to cancel.)
==================================================
EOF
        select app_alias in "ACROSS_FULL_MULTIPROJECT" ${multiproject_app_aliases[*]}
        do
            case ${REPLY} in
                "x" )
                    echo "Cancelling..."
                    break
                    ;;
                [[:digit:]]|[[:digit:]][[:digit:]] )
                    if ! [ -z $app_alias ]
                    then echo "Running command: ${command_chosen} for alias: ${app_alias}"
                         ${command_chosen} "${app_alias}"
                         break
                    else
                        echo "Invalid choice."
                    fi
                    ;;
                * )
                    echo "Invalid choice."
                    ;;
            esac
        done
    fi
}

run_REPL() {
    local app_alias=${1:?"Fail. App alias is mandatory."}
    local clj_opts_full_multiproject="-M:root/all:root/dev:root/test:root/build:cider"
    local clj_opts_for_alias="-M:root/all:root/dev:root/test:${app_alias}:cider"

    if [ ${app_alias} = "ACROSS_FULL_MULTIPROJECT" ]
    then echo "Starting socket REPL. Alias: ${app_alias}. Opts: ${clj_opts_full_multiproject}"
         clj ${clj_opts_full_multiproject} "--socket" "multiproject.generic.repl"

    else echo "Starting socket REPL. Alias: ${app_alias}. Opts: ${clj_opts_for_alias}"
         clj ${clj_opts_for_alias} "--socket" "${app_alias}.repl"
    fi
}

run_TESTS() {
    local app_alias=${1:?"Fail. App alias is mandatory."}
    local clj_opts_full_multiproject="-X:root/all:root/test"
    local clj_opts_for_alias="-X:root/all:root/test:${app_alias}"

    if [ ${app_alias} = "ACROSS_FULL_MULTIPROJECT" ]
    then echo "Running tests. Alias: ${app_alias}. Opts: ${clj_opts_full_multiproject}"
         clj ${clj_opts_full_multiproject}
    else echo "Running tests. Alias: ${app_alias}. Opts: ${clj_opts_for_alias}"
         clj ${clj_opts_for_alias}
    fi
}

run_CI() {
    local app_alias=${1:?"Fail. App alias is mandatory."}
    local clj_opts_full_multiproject="TODO"
    local clj_opts_for_alias="-T:root/build ci :app-alias :${app_alias}"

    if [ ${app_alias} = "ACROSS_FULL_MULTIPROJECT" ]
    then echo "NOT yet implemented."
    else echo "Running full CI job. Alias: ${app_alias}. Opts: ${clj_opts_for_alias}"
         clj ${clj_opts_for_alias}
    fi
}

run_UBERJAR() {
    local app_alias=${1:?"Fail. App alias is mandatory."}
    local clj_opts_full_multiproject="TODO"
    local clj_opts_for_alias="-T:root/build uberjar :app-alias :${app_alias}"
    local jar_cmd="java -jar target/${app_alias}/${app_alias}*.jar"

    if [ ${app_alias} = "ACROSS_FULL_MULTIPROJECT" ]
    then echo "NOT yet implemented."
    else
        echo "Running UBERJAR job. Alias: ${app_alias}. Opts: ${clj_opts_for_alias}"
        clj ${clj_opts_for_alias}

        cat <<EOF

==================================================
Running JAR with command: ${jar_cmd}
==================================================

EOF
        ${jar_cmd}
    fi
}

# Always execute all commands from root of project
(
    cd ${multiproject_root}
    main
)
