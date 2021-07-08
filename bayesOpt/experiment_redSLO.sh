CONF="exp_paper/simulator_reducedSLO.properties"
OUTDIR="exp_paper/results"
PDSEC="q-learning-pds-ec"

[[ -d $OUTDIR ]] || mkdir -p $OUTDIR

time python optimize_slo.py --conf $CONF --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/pipeline25.txt

time python optimize_slo.py --conf $CONF --app exp_paper/diamond.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/diamond25.txt

time python optimize_slo.py --conf $CONF --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex25.txt
time python optimize_slo.py --conf $CONF --app exp_paper/complex.app --iters 10 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex10.txt
time python optimize_slo.py --conf $CONF --app exp_paper/complex.app --iters 50 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex50.txt
time python optimize_slo.py --conf $CONF --app exp_paper/complex.app --iters 30 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex30.txt
time python optimize_slo.py --conf $CONF --app exp_paper/complex.app --iters 20 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex20.txt

time python optimize_slo.py --conf $CONF --trainonlyconf exp_paper/train.properties --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex25_train25k.txt
time python optimize_slo.py --conf $CONF --trainonlyconf exp_paper/train100.properties --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/complex25_train100k.txt

time python optimize_slo.py --conf $CONF --trainonlyconf exp_paper/trainvi.properties --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg vi > $OUTDIR/complex25_trainvi.txt




time python optimize_slo.py --conf $CONF --app exp_paper/pipeline3b.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/pipeline25b.txt

time python optimize_slo.py --conf $CONF --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/pipeline25.txt
time python optimize_slo.py --conf $CONF --app exp_paper/pipeline3.app --iters 10 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/pipeline10.txt
time python optimize_slo.py --conf $CONF --app exp_paper/pipeline3.app --iters 50 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/pipeline50.txt
time python optimize_slo.py --conf $CONF --trainonlyconf exp_paper/train.properties --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/pipeline_train25k.txt
time python optimize_slo.py --conf $CONF --trainonlyconf exp_paper/train100.properties --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/pipeline_train100k.txt
