import sys
import os
import re
import subprocess
from jinja2 import Environment, FileSystemLoader

regex = 'singleOp-coloredPolicy-(\d+)'

def create_frame(input_file):
    print(f"Plotting: {input_file}")
    # Create gp file
    environment = Environment(loader=FileSystemLoader("."))
    template = environment.get_template("plot.gp.j2")
    output=f"./colored.png"
    content = template.render(TITLE="Final", FILENAME=input_file, OUTPUT=output)
    with open("./plot.gp", mode="w", encoding="utf-8") as message:
        message.write(content)

    subprocess.run(["gnuplot", "./plot.gp"])
    return output

frames=[]
# get all files inside a specific folder
dir_path = '.'
for path in os.scandir(dir_path):
    if not path.is_file():
        continue
    m = re.match(regex, path.name)
    if m is None:
        continue
    iter = int(m.groups()[0])
    frames.append((iter, path.name))
frames.sort(key=lambda x: x[0])
frame = create_frame(frames[-1][1])
