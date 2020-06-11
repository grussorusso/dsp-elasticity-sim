#!/bin/bash
if [[ $# -lt 1 ]]; then
	echo "Usage: run.sh <experiment_tag> [<config files...>]"
	exit 1
fi

EXPERIMENT_TAG="$1"
OUT_DIR="results_${EXPERIMENT_TAG}"
[[ -e $OUT_DIR ]] || mkdir $OUT_DIR

echo "results will be in ${OUT_DIR}"

conf_file=$(mktemp)
cat ${@:2} > $conf_file
echo "output.base.path = $OUT_DIR/" >> $conf_file
echo "output.log = $OUT_DIR/simLog.log" >> $conf_file

#--------------------
# Running simulator
#--------------------
echo "Running experiment: ${EXPERIMENT_TAG}"
java -Xmx12g -jar out/artifacts/dspelasticitysimulator_jar/dspelasticitysimulator.jar $conf_file
[[ $? -eq 0 ]] || exit 1
