import subprocess
import re
JAR_FILE="/home/gabriele/Programmazione/dspelasticitysimulator/target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar"

DEBUG=False

def parse_output (s):
    regex="AvgCost\s*=\s*(0.\d+)"
    regexV="Violations = (\d+)"
    regexR="Reconfigurations = (\d+)"
    regexRC="ResourcesCost =\s*(\d+.\d+)"
    regexTime="SimulationTime =\s*(\d+)"

    m=re.search(regex, s)
    cost = float(m.groups()[0])

    m=re.search(regexV, s)
    vio = int(m.groups()[0])

    m=re.search(regexR, s)
    rcf = int(m.groups()[0])

    m=re.search(regexRC, s)
    rc = float(m.groups()[0])

    m=re.search(regexTime, s)
    time = int(m.groups()[0])

    return (cost, (vio, rcf, rc, time))

def simulate (app_file, base_confs, slo_setting_method = "fromfile", rmax=None, ompolicy="vi",  long_sim=False, weights=None):
    TEMP_CONF="/tmp/gp.properties"

    # Temporary conf is used to specify the app file to load
    with open(TEMP_CONF,"w") as tempf:
        tempf.write("dsp.app.file = {}\n".format(app_file))
        tempf.write("dsp.slo.operator.method = {}\n".format(slo_setting_method))

        if long_sim:
            tempf.write("simulation.stoptime = 999999\n")
        if ompolicy != None:
            tempf.write("edf.om.type = {}\n".format(ompolicy))
        if rmax != None:
            tempf.write("dsp.slo.latency = {}\n".format(rmax))

        if weights is not None:
            wres,wrcf,wslo = weights
            tempf.write(f"edf.rl.om.resources.weight = {wres}\n")
            tempf.write(f"edf.rl.om.reconfiguration.weight = {wrcf}\n")
            tempf.write(f"edf.rl.om.slo.weight = {wslo}\n")

    # Run the simulation
    try:
        cmd_list = ["java", "-jar", JAR_FILE, *base_confs, TEMP_CONF]
        if DEBUG:
            print(cmd_list)
        cp = subprocess.run(cmd_list, capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        s = e.stderr.decode("utf-8")
        print(s)
        raise(e)

    s = cp.stdout.decode("utf-8")
    cost,stats = parse_output(s)

    return (cost,stats)
