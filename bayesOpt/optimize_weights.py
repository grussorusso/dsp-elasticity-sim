import sys
import random
import GPy
from GPyOpt.methods import BayesianOptimization
import numpy as np
import argparse
from application import App
from simulation import simulate



def simulate_with_weights (wvec, app, base_confs, ompolicy="vi", long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write(TEMP_APP)

    return simulate(TEMP_APP, base_confs, "heuristic", None, ompolicy, long_sim, weights=wvec)

def evaluate (X, app, base_confs):
    wvec = X[0]
    cost,stats = simulate_with_weights(wvec, app, base_confs)


    # Optimization obj
    obj = stats[2] # res cost
    if stats[0]/stats[3] > 0.05: #perc violations
        obj += 100
    if stats[1]/stats[3] > 0.1: # reconf
        obj += 100

    print("{} -> {} ({})".format(" ".join(["{:.3f}".format(w) for w in wvec]), obj, stats))
    return np.array([obj])

def optimize_weights (app, base_confs, n_iterations):
    # Create domain
    domain = []
    domain.append({'name': 'w_res', 'type': 'continuous', 'domain': (0.05,0.95)})
    domain.append({'name': 'w_rcf', 'type': 'continuous', 'domain': (0.05,0.95)})
    domain.append({'name': 'w_slo', 'type': 'continuous', 'domain': (0.05,0.95)})

    #print("Domain: {}".format(domain))

    c1 = "-1.02 + x[:,0] + x[:,1] + x[:,2]"
    c2 = "0.98 - x[:,0] - x[:,1] - x[:,2]"
    constraints = [{'name': 'constr1', 'constraint': c1},
                {'name': 'constr2', 'constraint': c2}]

    for constr in constraints: 
        print(constr)


    kernel = GPy.kern.Matern52(input_dim=3, variance=1.0, lengthscale=1.0)

    # --- Solve your problem
    myBopt = BayesianOptimization(f=lambda x : evaluate(x, app, base_confs),
            kernel=kernel,
            normalize_Y=True,
            maximize=False,
            initial_design_numdata=3,
            constraints=constraints,
            domain=domain)
    myBopt.run_optimization(max_iter=n_iterations)

    print("="*20)
    print("x_opt =  "+str(myBopt.x_opt))
    print("fx_opt = "+str(myBopt.fx_opt))
    print("="*20)

    #myBopt.plot_acquisition()
    return myBopt.x_opt



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--conf', action='append', required=True)
    parser.add_argument('--app', action='store', required=True)
    parser.add_argument('--iters', action='store', required=False, default=10, type=int)
    parser.add_argument('--seed', action='store', required=False, default=123, type=int)
    parser.add_argument('--omalg', action='store', required=False, default="vi")
    parser.add_argument('--approximate-model', action='store_true', required=False, default=False)
    parser.add_argument('--noevaluate', action='store_true', required=False, default=False)

    args = parser.parse_args()
    base_confs = args.conf
    approximate_model = args.approximate_model
    omalg = args.omalg

    print("Conf: {}".format(base_confs))
    print("App: {}".format(args.app))
    print("Approximate: {}".format(approximate_model))
    print("Testing algorithm: {}".format(omalg))

    random.seed(args.seed)

    app = App(args.app)
    eval_app = app.approximate() if approximate_model else app

    opt_weights = optimize_weights(eval_app, base_confs, args.iters)

    if args.noevaluate:
        return

    # Run final simulation
    #cost,stats = simulate_with_quotas(opt_quotas, app, base_confs, rmax, omalg, long_sim=True)
    #print("Final cost: {} : {}".format(cost, stats))
    

main()


