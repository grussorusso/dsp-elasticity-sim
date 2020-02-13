#!/bin/bash
if [[ $# -lt 1 ]]; then
	echo "Usage: run.sh <experiment_tag> [<config files...>]"
	exit 1
fi

EXPERIMENT_TAG="$1"


#--------------------
# Running simulator
#--------------------
echo "Running experiment: ${EXPERIMENT_TAG}"
java -Xmx12g -jar out/artifacts/dspelasticitysimulator_jar/dspelasticitysimulator.jar ${@:2}

#--------------------
# Processing results
#--------------------
OUTDIR=results_${EXPERIMENT_TAG}
[[ -e $OUTDIR ]] || mkdir $OUTDIR

mv test_results/* ${OUTDIR}/

bash processDetailedLog.sh
mv detail* ${OUTDIR}
cp plotDetailedLog.gp $OUTDIR
