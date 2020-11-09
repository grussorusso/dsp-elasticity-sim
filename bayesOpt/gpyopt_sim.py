import sys
import random
import GPy
from GPyOpt.methods import BayesianOptimization
import numpy as np
import argparse
import subprocess
import re
from application import App

JAR_FILE="/home/gabriele/Programmazione/dspelasticitysimulator/out/artifacts/dspelasticitysimulator_jar/dspelasticitysimulator.jar"

def parse_output (s):
    regex="AvgCost\s*=\s*(0.\d+)"
    regexV="Violations = (\d+)"
    regexR="Reconfigurations = (\d+)"
    regexRC="ResourcesCost =\s*(\d+.\d+)"

    m=re.search(regex, s)
    cost = float(m.groups()[0])

    m=re.search(regexV, s)
    vio = int(m.groups()[0])

    m=re.search(regexR, s)
    rcf = int(m.groups()[0])

    m=re.search(regexRC, s)
    rc = float(m.groups()[0])

    return (cost, (vio, rcf, rc))

def simulate (app_file, base_conf, slo_setting_method = "fromfile", rmax=None,  long_sim=False):
    TEMP_CONF="/tmp/gp.properties"

    # Temporary conf is used to specify the app file to load
    with open(TEMP_CONF,"w") as tempf:
        tempf.write("dsp.app.file = {}\n".format(app_file))
        tempf.write("dsp.slo.operator.method = {}\n".format(slo_setting_method))

        if long_sim:
            tempf.write("simulation.stoptime = 999999\n")
        if rmax != None:
            tempf.write("dsp.slo.latency = {}\n".format(rmax))

    # Run the simulation
    try:
        cp = subprocess.run(["java", "-jar", JAR_FILE, base_conf, TEMP_CONF], capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        s = e.stderr.decode("utf-8")
        print(s)
        raise(e)

    s = cp.stdout.decode("utf-8")
    cost,stats = parse_output(s)

    return (cost,stats)

def simulate_with_quotas (quotas, app, base_conf, rmax=None, long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write_with_quotas(quotas, TEMP_APP)

    return simulate(TEMP_APP, base_conf, "fromfile", rmax, long_sim)

def simulate_default_slo (app, base_conf, rmax=None, long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write(TEMP_APP)

    return simulate(TEMP_APP, base_conf, "default", rmax, long_sim)

def simulate_heuristic (app, base_conf, rmax=None, long_sim=False):
    # Generate app file
    TEMP_APP="/tmp/gp.app"
    app.write(TEMP_APP)

    return simulate(TEMP_APP, base_conf, "heuristic", rmax, long_sim)

def evaluate (X, app, base_conf):
    quotas = X[0]
    cost,stats = simulate_with_quotas(quotas, app, base_conf)

    print("{} -> {}".format(" ".join(["{:.3f}".format(q) for q in quotas]), cost))
    return np.array([cost])


def optimize_quotas (app, base_conf, n_iterations):
    n_op = app.get_n_operators()

    # Create domain
    domain = []
    for i in range(n_op):
        domain.append({'name': 'x{}'.format(i+1), 'type': 'continuous', 'domain': (0.01,0.99)})

    #print("Domain: {}".format(domain))

    c_ub = "-1"
    c_lb = "0.99"
    for i in range(n_op):
        c_ub = c_ub + "+x[:,{}]".format(i)
        c_lb = c_lb + "-x[:,{}]".format(i)

    constraints = [{'name': 'constr_1', 'constraint': c_ub},
                {'name': 'constr_2', 'constraint': c_lb}]

    # Additional model-based constraints
    #for i in range(n_op-1):
    #    c = "+x[:,{}]-x[:,{}]".format(i,i+1)
    #    constraints.append({'name': 'constr_extra', 'constraint': c})

    for constr in constraints: 
        print(constr)


    kernel = GPy.kern.Matern52(input_dim=n_op, variance=1.0, lengthscale=1.0)

    # --- Solve your problem
    myBopt = BayesianOptimization(f=lambda x : evaluate(x, app, base_conf),
            kernel=kernel,
            normalize_Y=True,
            maximize=False,
            initial_design_numdata=5,
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
    parser.add_argument('--conf', action='store', required=True)
    parser.add_argument('--app', action='store', required=True)
    parser.add_argument('--rmax', action='store', required=False, type=float)
    parser.add_argument('--iters', action='store', required=False, default=10, type=int)
    parser.add_argument('--seed', action='store', required=False, default=123, type=int)
    parser.add_argument('--approximate-model', action='store_true', required=False, default=False)

    args = parser.parse_args()
    base_conf = args.conf
    rmax = args.rmax
    approximate_model = args.approximate_model

    random.seed(args.seed)

    app = App(args.app)
    if approximate_model:
        eval_app = app.approximate()
    else:
        eval_app = app

    opt_quotas = optimize_quotas(eval_app, base_conf, args.iters)

    # Run final simulation
    cost,stats = simulate_with_quotas(opt_quotas, app, base_conf, rmax, long_sim=True)
    print("Final cost: {} : {}".format(cost, stats))

    # Run baseline simulation
    cost,stats = simulate_default_slo(app, base_conf, rmax, long_sim=True)
    print("Baseline cost: {} : {}".format(cost, stats))

    # Run heuristic simulation
    cost,stats = simulate_heuristic (app, base_conf, rmax, long_sim=True)
    print("Heuristic cost: {} : {}".format(cost, stats))
    

main()


