import sys
import random
import GPy
from GPyOpt.methods import BayesianOptimization
import numpy as np
import argparse
from application import App
from simulation import simulate



def simulate_with_quotas (quotas, app, base_confs, rmax=None, ompolicy="vi", long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write_with_quotas(quotas, TEMP_APP)

    return simulate(TEMP_APP, base_confs, "fromfile", rmax, ompolicy, long_sim)

def simulate_default_slo (app, base_confs, rmax=None, ompolicy="vi", long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write(TEMP_APP)

    return simulate(TEMP_APP, base_confs, "default", rmax, ompolicy, long_sim)

def simulate_heuristic (app, base_confs, rmax=None, ompolicy="vi", long_sim=False, approx=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write(TEMP_APP)

    alg="heuristic"
    if approx:
        alg="heuristic-approx"

    return simulate(TEMP_APP, base_confs, alg, rmax, ompolicy, long_sim)

def evaluate (X, app, base_confs):
    quotas = X[0]
    cost,stats = simulate_with_quotas(quotas, app, base_confs)

    print("{} -> {} ({})".format(" ".join(["{:.3f}".format(q) for q in quotas]), cost, stats))
    return np.array([cost])

def optimize_quotas_multipath (app, base_confs, n_iterations, constraints_mode):
    paths = app.get_paths()
    n_op = app.get_n_operators()

    # Create domain
    domain = []
    for i in range(n_op):
        domain.append({'name': 'x{}'.format(i+1), 'type': 'continuous', 'domain': (0.01,0.99)})

    #print("Domain: {}".format(domain))

    constraints = []

    if constraints_mode > 0:
        constr_index = 1
        for p in paths:
            c_ub = "-1.05"
            c_lb = "0.95"
            for op in p:
                i = app.opname2index[op]
                c_ub = c_ub + "+x[:,{}]".format(i)
                c_lb = c_lb + "-x[:,{}]".format(i)
            _constraints = [{'name': f'constr_{constr_index}', 'constraint': c_ub},
                        {'name': f'constr_{constr_index+1}', 'constraint': c_lb}]
            constr_index += 2
            constraints.extend(_constraints)

    # Additional pairwise constraints
    if constraints_mode > 1:
        for i in app.operators:
            for j in app.operators:
                if app.operators[i].service_rate() > app.operators[j].service_rate()*1.3:
                    _constraint = {'name': f"{i}_{j}", 'constraint': f"x[:,{i}]-x[:,{j}]"}
                    constraints.append(_constraint)
                elif app.operators[i].service_rate() < app.operators[j].service_rate()*1.3:
                    _constraint = {'name': f"{i}_{j}", 'constraint': f"-x[:,{i}]+x[:,{j}]"}
                    constraints.append(_constraint)

                

    for constr in constraints: 
        print(constr)


    kernel = GPy.kern.Matern52(input_dim=n_op, variance=1.0, lengthscale=1.0)

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

    for p in paths:
        for op in p:
            index = app.opname2index[op]
            slo = myBopt.x_opt[index]
            print(f"{op}->{slo};", end='')
        print("")

    #myBopt.plot_acquisition()
    return myBopt.x_opt



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--conf', action='append', required=True)
    parser.add_argument('--app', action='store', required=True)
    parser.add_argument('--rmax', action='store', required=False, type=float)
    parser.add_argument('--iters', action='store', required=False, default=10, type=int)
    parser.add_argument('--seed', action='store', required=False, default=123, type=int)
    parser.add_argument('--omalg', action='store', required=False, default="vi")
    parser.add_argument('--constraints_mode', action='store', required=False, default=1, type=int)
    parser.add_argument('--approximate-model', action='store_true', required=False, default=False)
    parser.add_argument('--noevaluate', action='store_true', required=False, default=False)

    args = parser.parse_args()
    base_confs = args.conf
    rmax = args.rmax
    approximate_model = args.approximate_model
    omalg = args.omalg

    print("Conf: {}".format(base_confs))
    print("App: {}".format(args.app))
    print("Approximate: {}".format(approximate_model))
    print("RMax: {}".format(rmax))
    print("Testing algorithm: {}".format(omalg))

    random.seed(args.seed)

    app = App(args.app)
    eval_app = app.approximate() if approximate_model else app

    opt_quotas = optimize_quotas_multipath(eval_app, base_confs, args.iters, args.constraints_mode)

    if args.noevaluate:
        return

    # Run final simulation
    cost,stats = simulate_with_quotas(opt_quotas, app, base_confs, rmax, omalg, long_sim=True)
    print("Final cost: {} : {}".format(cost, stats))

    # Run baseline simulation
    cost,stats = simulate_default_slo(app, base_confs, rmax, omalg, long_sim=True)
    print("Baseline cost: {} : {}".format(cost, stats))

    # Run heuristic simulation
    cost,stats = simulate_heuristic (app, base_confs, rmax, omalg, approx=approximate_model, long_sim=True)
    print("Heuristic cost: {} : {}".format(cost, stats))
    

main()


