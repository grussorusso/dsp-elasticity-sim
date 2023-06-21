import matplotlib.pyplot as plt
import numpy as np

y = np.loadtxt("results/AvgCost-avg.txt")

plt.xlim(left=10, right=400000)
plt.xscale("log")
plt.xlabel("Simulated Steps")
plt.ylabel("Cumulative Avg. Cost")
  
# plot line
plt.plot(y)
plt.show()
