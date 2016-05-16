#Configure a new plateform for integration (before docker)


	curl http://apache.mirrors.ovh.net/ftp.apache.org/dist/flink/flink-1.0.3/flink-1.0.3-bin-hadoop27-scala_2.10.tgz | tar xvz


	sudo apt-get update
	

activate sysstat

	sudo apt-get install sysstat


install java

	sudo apt-get install openjdk-7-jdk


download planet 


	wget http://planet.osm.org/pbf/planet-latest.osm.pbf


clone and build the repo

	sudo apt-get install git

	git clone https://github.com/frett27/osm-flink-tools
	
	./gradlew shadowJar


configure the taskmanager of flink 

	edit file flink-conf.yml

launch the flink cluster

	bin/start-local.sh

launch the process

	./process.sh

process launch (without yarn && hadoop stack):

	ubuntu@ip-172-31-18-167:~$ more process.sh
	#!/bin/sh
	
	cd tmp
	
	# wget http://download.geofabrik.de/europe/france-latest.osm.pbf
	
	../flink-1.0.3/bin/flink run ../osm-flink-tools/build/libs/osm-flink-tools-0.1-all.jar `pwd`/planet-latest.osm.pbf `pwd`/result




