import random

class App:
    def __init__ (self, file=None):
        self.operators = []
        self.edges = []
        self.adj = {}

        if file != None:
            with open(file, "r") as f:
                for line in f:
                    fields = line.strip().split(",")
                    if fields[0] == "op":
                        name,stmean,stscv = fields[1:4]
                        name = name.strip()
                        stmean = float(stmean)
                        stscv = float(stscv)
                        self.operators.append((name, stmean, stscv))
                    else:
                        fields[1] = fields[1].strip()
                        fields[2] = fields[2].strip()
                        self.edges.append(fields)

        self.build_adj_dict()

    def build_adj_dict(self):
        for op in self.operators:
            self.adj[op[0]] = set()
        for e in self.edges:
            i,j = e[1:3]
            self.adj[i].add(j)

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

    def find_sources (self):
        srcs = []
        for op in self.adj:
            src = True
            for other_op in self.adj:
                if op == other_op:
                    continue
                if op in self.adj[other_op]:
                    src = False
                    break
            if src:
                srcs.append(op)
        return srcs

    # Recursive function to print all paths
    def dfs(self, s, path, visited, sources):
        paths = []
        path.append(s)
        visited[s] = True

        # Path started with a node
        # having in-degree 0 and
        # current node has out-degree 0,
        # print current path
        if len(self.adj[s]) == 0 and path[0] in sources:
            paths.append([v for v in path])

        # Recursive call to print all paths
        for node in self.adj[s]:
            if not visited[node]:
                paths.extend(self.dfs(node, path, visited, sources))

        # Remove node from path
        # and set unvisited
        path.pop()
        visited[s] = False

        return paths

    def get_paths (self):
        srcs = self.find_sources()
        visited = { op[0]:False for op in self.operators }
        paths = []
        for src in srcs:
            paths.extend(self.dfs(src, [], visited, srcs))
        return paths

