import sys
import GPy
from GPyOpt.methods import BayesianOptimization
import numpy as np
import subprocess
import re
from application import App

JAR_FILE="/home/gabriele/Programmazione/dspelasticitysimulator/out/artifacts/dspelasticitysimulator_jar/dspelasticitysimulator.jar"

def parse_output (s):
    regex="AvgCost\s*=\s*(0.\d+)"
    #regex="Slo Violations = (\d+)"

    m=re.search(regex, s)
    if m == None:
        print(s)
        print("Could not parse output.")
        exit(2)

    cost = float(m.groups()[0])
    return cost

def simulate (quotas, app, base_conf, long_sim=False):
    TEMP_CONF="/tmp/gp.properties"
    TEMP_APP="/tmp/gp.app"

    # Generate app file
    app.write_with_quotas(quotas, TEMP_APP)

    # Temporary conf is used to specify the app file to load
    with open(TEMP_CONF,"w") as tempf:
        conf_line = "dsp.app.file = {}\n".format(TEMP_APP)
        tempf.write(conf_line)

        if long_sim:
            tempf.write("simulation.stoptime = 999999\n")

    # Run the simulation
    try:
        cp = subprocess.run(["java", "-jar", JAR_FILE, base_conf, TEMP_CONF], capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        s = e.stderr.decode("utf-8")
        print(s)
        raise(e)

    s = cp.stdout.decode("utf-8")
    cost = parse_output(s)

    return cost

def evaluate (X, app, base_conf):
    quotas = X[0]
    cost = simulate(quotas, app, base_conf)

    print("{} -> {}".format(" ".join(["{:.3f}".format(q) for q in quotas]), cost))
    return np.array([cost])


def optimize_quotas (app, base_conf):
    n_op = app.get_n_operators()

    # Create domain
    domain = []
    for i in range(n_op):
        domain.append({'name': 'x{}'.format(i+1), 'type': 'continuous', 'domain': (0.01,0.99)})

    print("Domain: {}".format(domain))

    c_ub = "-1"
    c_lb = "0.99"
    for i in range(n_op):
        c_ub = c_ub + "+x[:,{}]".format(i)
        c_lb = c_lb + "-x[:,{}]".format(i)

    constraints = [{'name': 'constr_1', 'constraint': c_ub},
                {'name': 'constr_2', 'constraint': c_lb}]
    print(constraints)


    kernel = GPy.kern.Matern52(input_dim=n_op, variance=1.0, lengthscale=1.0)

    # --- Solve your problem
    myBopt = BayesianOptimization(f=lambda x : evaluate(x, app, base_conf),
            kernel=kernel,
            normalize_Y=True,
            maximize=False,
            initial_design_numdata=5,
            constraints=constraints,
            domain=domain)
    myBopt.run_optimization(max_iter=10)

    print("="*20)
    print("x_opt =  "+str(myBopt.x_opt))
    print("fx_opt = "+str(myBopt.fx_opt))
    print("="*20)

    #myBopt.plot_acquisition()
    return myBopt.x_opt


def main():
    if len(sys.argv) != 1+2:
        print("Usage: gpyopt_sim.py <simulator conf> <base .app file>")
        exit(1)

    base_conf = sys.argv[1]
    app_file = sys.argv[2]

    app = App(app_file)
    opt_quotas = optimize_quotas(app, base_conf)

    # Run final simulation
    cost = simulate(opt_quotas, app, base_conf, True)
    print("Final cost: {}".format(cost))

    # Run baseline simulation
    #cost = simulate([1.0/n_op for i in range(n_op)], app, base_conf, True)
    #print("Baseline cost: {}".format(cost))
    

main()


