#!/bin/bash
#
docker run --rm -v "$(pwd)/docker.properties":/conf/conf.properties\
      -v "$(pwd)/results":/tmp/results grussorusso/dspsim:1.0

