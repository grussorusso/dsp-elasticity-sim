all:
	mvn clean compile install package -DskipTests=true -Djavacpp.platform=linux-x86_64
