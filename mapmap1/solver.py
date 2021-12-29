from butools.queues import MMAPPH1FCFS, MAPMAP1
import numpy.matlib as ml
from butools.ph import APHFrom2Moments, MomentsFromPH
from butools.map import MarginalMomentsFromMAP, CheckMAPRepresentation, MAPFromFewMomentsAndCorrelations

def map_mean (d0, d1):
    return MarginalMomentsFromMAP(d0, d1, 1)[0]

def map_scale(d0, d1, new_mean):
    ratio=map_mean(d0,d1)/new_mean;
    return (d0*ratio,d1*ratio)



#D0=ml.matrix([[-8.,1.,2.],[0.,-6.,4.],[3.,0.,-3.]])
#D1=ml.matrix([[4.,1.,0.],[0.,2.,0.],[0.,0.,0.]])

D0=ml.matrix([[-1.5, 0.5],[0.5, -4.5]])
D1=ml.matrix([[1, 0], [0, 4]])

exp0=ml.matrix([[-1.0]])
exp1=ml.matrix([[1.0]])

mu = 20.0
phases = 1
#----
stMean = 1.0/mu
stSCV = 1.0/phases
stMom2 = stMean**2 * (stSCV + 1)
stMoms = [stMean, stMom2]
alpha,A = APHFrom2Moments(stMoms)
stMoms = MomentsFromPH(alpha, A, 2) # just to check

# Fit a MAP
#S0,S1 = MAPFromFewMomentsAndCorrelations([stMean, stMom2], 0.0)
#print(S0)
#print(S1)


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

for rate in range(0,int(mu+1)):
    print("Arrival rate: {}".format(rate))
    util = stMoms[0]*rate
    print(f"util: {util}")
    if 
    respT = evaluate(D0, D1, alpha, A, rate)
    print(respT)
