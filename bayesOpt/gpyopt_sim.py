import sys
import GPy
from GPyOpt.methods import BayesianOptimization
import numpy as np
import subprocess
import re

if len(sys.argv) != 1+2:
    print("Usage: gpyopt_sim.py <simulator conf> <base .app file>")
    exit(1)

CONF=sys.argv[1]
APPFILE=sys.argv[2]
JAR_FILE="/home/gabriele/Programmazione/dspelasticitysimulator/out/artifacts/dspelasticitysimulator_jar/dspelasticitysimulator.jar"

class App:
    def __init__ (self, file):
        self.operators = []
        self.edges = []

        with open(file, "r") as f:
            for line in f:
                fields = line.strip().split(",")
                if fields[0] == "op":
                    name,stmean,stscv = fields[1:4]
                    stmean = float(stmean)
                    stscv = float(stscv)
                    self.operators.append((name, stmean, stscv))
                else:
                    self.edges.append(fields)

    def get_n_operators(self):
        return len(self.operators)

    def write (self, quotas, outfile):
        with open(outfile, "w") as of:
            for i in range(len(self.operators)):
                op = self.operators[i]
                of.write("op,{},{},{},{}\n".format(op[0],op[1],op[2],quotas[i]))
            for e in self.edges:
                of.write("{}\n".format(",".join(e)))

def simulate (X, app):
    TEMP_CONF="/tmp/gp.properties"
    TEMP_APP="/tmp/gp.app"

    quotas = X[0]
    app.write(quotas, TEMP_APP)

#    print(X)

    with open(TEMP_CONF,"w") as tempf:
        conf_line = "dsp.app.file = {}\n".format(TEMP_APP)
        tempf.write(conf_line)

    try:
        cp = subprocess.run(["java", "-jar", JAR_FILE, CONF , TEMP_CONF], capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        s = e.stderr.decode("utf-8")
        print(s)
        raise(e)
    s = cp.stdout.decode("utf-8")

    regex="AvgCost\s*=\s*(0.\d+)"
    #regex="Slo Violations = (\d+)"
    m=re.search(regex, s)

    if m == None:
        print(s)
        print("Could not parse output.")
        exit(2)

    cost = float(m.groups()[0])
    print("{} -> {}".format(" ".join(["{:.3f}".format(q) for q in quotas]), cost))
    return np.array([cost])



def main():
    app = App(APPFILE)
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
    myBopt = BayesianOptimization(f=lambda x : simulate(x, app),
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

    myBopt.plot_acquisition()

main()


