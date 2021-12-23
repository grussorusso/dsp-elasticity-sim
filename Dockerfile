FROM openjdk:8-jre-alpine3.9
 
# copy the packaged jar file into our docker image
COPY target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar /dspsim.jar

COPY traces/ /traces
 
# set the startup command to execute the jar
CMD ["java", "-jar", "/dspsim.jar", "/conf/conf.properties"]
