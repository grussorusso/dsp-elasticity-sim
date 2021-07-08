CONF="exp_paper/simulator1.properties"
OUTDIR="exp_paper/results_1res"
PDSEC="q-learning-pds-ec"

[[ -d $OUTDIR ]] || mkdir -p $OUTDIR

for seed in 123 124 125 126 127; do

[[ -d $OUTDIR/$seed ]] || mkdir -p $OUTDIR/$seed

time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/pipeline25.txt

time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/diamond.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/diamond25.txt

time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex25.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 10 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex10.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 50 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex50.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 30 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex30.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 20 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex20.txt

time python optimize_slo.py --seed $seed --conf $CONF --trainonlyconf exp_paper/train.properties --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex25_train25k.txt
time python optimize_slo.py --seed $seed --conf $CONF --trainonlyconf exp_paper/train100.properties --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/complex25_train100k.txt

time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 25 --omalg $PDSEC  --trainalg vi > $OUTDIR/$seed/complex25_trainvi.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/complex.app --iters 25 --omalg vi  --trainalg vi > $OUTDIR/$seed/complex25_vi.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3.app --iters 25 --omalg vi  --trainalg vi > $OUTDIR/$seed/pipeline25_vi.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/diamond.app --iters 25 --omalg vi  --trainalg vi > $OUTDIR/$seed/diamond25_vi.txt




time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3b.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/pipeline25b.txt

time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/$seed/pipeline25.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3.app --iters 10 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/$seed/pipeline10.txt
time python optimize_slo.py --seed $seed --conf $CONF --app exp_paper/pipeline3.app --iters 50 --omalg $PDSEC --trainalg $PDSEC > $OUTDIR/$seed/pipeline50.txt
time python optimize_slo.py --seed $seed --conf $CONF --trainonlyconf exp_paper/train.properties --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/pipeline_train25k.txt
time python optimize_slo.py --seed $seed --conf $CONF --trainonlyconf exp_paper/train100.properties --app exp_paper/pipeline3.app --iters 25 --omalg $PDSEC  --trainalg $PDSEC > $OUTDIR/$seed/pipeline_train100k.txt

done
