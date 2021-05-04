import sys

factor = float(sys.argv[1])

for line in sys.stdin:
    val = int(float(line.strip()) * factor)
    print(val)
