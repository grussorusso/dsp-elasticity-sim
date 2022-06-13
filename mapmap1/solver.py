import math
import sys
import butools
from butools.queues import MMAPPH1FCFS, MAPMAP1
import numpy.matlib as ml
import numpy as np
from butools.ph import APHFrom2Moments, MomentsFromPH
from butools.map import MarginalMomentsFromMAP, CheckMAPRepresentation, MAPFromFewMomentsAndCorrelations

def map_mean (d0, d1):
    return MarginalMomentsFromMAP(d0, d1, 1)[0]
def map_var (d0, d1):
    moms = MarginalMomentsFromMAP(d0, d1, 2)
    return moms[1] - moms[0]**2

def map_scv (d0, d1):
    return map_var(d0,d1)/map_mean(d0,d1)**2

def map_scale(d0, d1, new_mean):
    ratio=map_mean(d0,d1)/new_mean;
    return (d0*ratio,d1*ratio)



butools.checkPrecision = 10**-7


def evaluate (d0, d1, alpha, A, arrival_rate):
    nD0, nD1 = map_scale(d0,d1, 1.0/arrival_rate)
    nD0, nD1 = map_scale(d0,d1, 1.0/arrival_rate)
    if not CheckMAPRepresentation(nD0,nD1):
        print("Invalid arrival MAP!")
        exit(1)
    #print("Arrival rate: {}".format(1.0/map_mean(nD0,nD1)))
    #util = stMoms[0]/map_mean(nD0,nD1)
    #print(f"util: {util}")

    #stm=MAPMAP1(nD0,nD1,S0,S1,"stMoms",1)
    stm=MMAPPH1FCFS([nD0,nD1],[alpha],[A],"stMoms",1)
    return stm[0]



if len(sys.argv) < 4:
    print("Usage: solver.py <outfile> <stMean> <stVar> [<map states> <D0,0,0> <D0,0,1> ... <D1,0,0> ...]")
    exit(1)

if len(sys.argv) == 4:
    # Default arrival process
    D0=ml.matrix([[-1.5, 0.5],[0.5, -4.5]])
    D1=ml.matrix([[1, 0], [0, 4]])
else:
    # parse MAP
    N = int(sys.argv[4])
    maps_params = [float(x) for x in sys.argv[5:]]
    if len(maps_params) != 2*N**2:
        print("Usage: solver.py <outfile> <stMean> <stVar> [<map states> <D0,0,0> <D0,0,1> ... <D1,0,0> ...]")
        exit(2)

    D0 = np.asmatrix(maps_params[:N**2]).reshape((N,N))
    D1 = np.asmatrix(maps_params[N**2:]).reshape((N,N))



stMean = float(sys.argv[2])
stVar = float(sys.argv[3])
#----
mu = 1.0/stMean
stSCV = stVar/(stMean**2)
stMom2 = stMean**2 * (stSCV + 1)
stMoms = [stMean, stMom2]
alpha,A = APHFrom2Moments(stMoms)

MAX_RATE = 5000

with open(sys.argv[1], "w") as of:
    maxRate = min(math.ceil(mu), MAX_RATE)
    print("SCV:" + str(map_scv(D0,D1)), file=of)
    for rate in range(0,maxRate):
        util = stMoms[0]*rate
        if util == 0.0:
            respT = 0
        else:
            respT = evaluate(D0, D1, alpha, A, rate)
        print(f"{rate};{util};{respT}", file=of)
        of.flush()
