#!/bin/bash

DIR="$1"

[[ "x$DIR" != "x" ]] || (echo "Usage: parse_results.sh <dir>"; /bin/false) || exit 1

avgCost=$(rg "Simulation - Application Avg Cost = [0-9\.]+" $DIR/final_stats | tr -d ' ' | awk -F '=' '{ print $2 }')
violations=$(rg "Violations = [0-9\.]+" $DIR/final_stats | tr -d ' ' | awk -F '=' '{ print $2 }')
reconfs=$(rg "Reconfigurations = [0-9\.]+" $DIR/final_stats | tr -d ' ' | awk -F '=' '{ print $2 }')
instances=$(rg "Deployment in resources of type - 0 = [0-9\.]+" $DIR/final_stats | tr -d ' ' | awk -F '=' '{ print $2 }')


printf "%s\t%s\t%s\t%s\t%s\n" $DIR $avgCost $violations $reconfs $instances
