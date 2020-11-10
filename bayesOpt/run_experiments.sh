#!/bin/bash

function run {
	time python gpyopt_sim.py $@
}

ITERS=15
OUTDIR="results_mb"

[[ -d $OUTDIR ]] || mkdir $OUTDIR

for app in pipeline{3,4,5}.app; do
for conf in simulator.properties*; do
	echo $app
	if [[ "$app" == "pipeline3.app" ]]; then
		rmax="0.020"
	elif [[ "$app" == "pipeline4.app" ]]; then
		rmax="0.040"
	elif [[ "$app" == "pipeline5.app" ]]; then
		rmax="0.050"
	fi
	run --omalg model-based --iters $ITERS --conf $conf --app $app --rmax ${rmax} > $OUTDIR/output_${conf}_${app}.txt
	# --approximate-model
done
done

