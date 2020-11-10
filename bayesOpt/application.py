import random

class App:
    def __init__ (self, file=None):
        self.operators = []
        self.edges = []

        if file != None:
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

    def write_with_quotas (self, quotas, outfile):
        with open(outfile, "w") as of:
            for i in range(len(self.operators)):
                op = self.operators[i]
                name,stmean,stscv=op

                of.write("op,{},{},{},{}\n".format(name,stmean,stscv,quotas[i]))
            for e in self.edges:
                of.write("{}\n".format(",".join(e)))

    def write (self, outfile):
        with open(outfile, "w") as of:
            for i in range(len(self.operators)):
                op = self.operators[i]
                of.write("op,{},{},{}\n".format(op[0],op[1],op[2]))
            for e in self.edges:
                of.write("{}\n".format(",".join(e)))

    def approximate (self):
        new_app = App()
        for op in self.operators:
            name,stmean,stscv=op
            stscv = 1.0
            stmean = stmean + 0.05*stmean*random.gauss(0.0,1.0)
            new_app.operators.append((name, stmean, stscv))
        for e in self.edges:
            new_app.edges.append(e)

        return new_app

