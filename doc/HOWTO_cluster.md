# Howto cluster osm flink tools



## AWS setup

JobManager : T2 instance , 300 go magnetic hard drive 1 instance



### Choose instance for jobmanager

300 go hard drive

security group osmplanet



### Choose instance for taskmanager

6 instances : 300 go hard drive

security group osmplanet



## Configure the ssh access

#### Define the id_rsa for connecting with ssh on the main jobmanager host

connect to jobmanager with ssh using key

```
vi ~/.ssh/id_rsa
```



add private key in the file :

-----BEGIN RSA PRIVATE KEY-----
MIIEpQIBAAKCAQEAsIFqLSKPtmLc1cKp7/bihQP3uT+5Eyxn7Bnb9iZhNvKDNmez
gYu2Yxs/ig4s0xyMSTYicIF1PYhRPaP7ehx36yEwjknMeQtyWOA7+g9WINKlhhKh
mGqPiN/9AhsGa8iwF4T4b/AU9tJuBqu/16wshijorpfAggUNyIbRS0UiStolqj7n
jADU5FurqnxicfMSU6qp5g5EViCYVFH8smN0wGY+T6dvt6QbJS1aO8GIAbt48Oe

....
d0JlE76RFSXYeADgAflbJyIelfGZT3FU1L2zb/WgRwto2xEb2uNb3z3rvQiMcjjS
nWQ/A7WC1lnD1sEIDwPZj6G4f+T03+r++y8ZD2u17MVq1ZeWH8vDcolnkjV8x5et
4ET+ihECgYEA4jy4ZArSXOEqvLJ9OAOUlCmkX7mf9xSS+KMSojtXcOW+eOGpWWcZ
MOsmKoRBFbE9DooDn2T4TCPu5s3G76TjDvm2pf29jQtTsomY79iHvX+N+/PiM4y2
zLyIrYcqGlodljgvhQ7CWQjWyCXvKRsp5tKta7I2aLOJKw7FCMia9VQ=
-----END RSA PRIVATE KEY-----

save the file

```
chmod 400 ~/.ssh/id_rsa
```



in AWS console, in security group add all traffic



## install latest ansible software

```
sudo apt-get update
sudo apt-get install -y software-properties-common
sudo apt-add-repository ppa:ansible/ansible
sudo apt-get update
sudo apt-get install -y ansible
```



clone recipe ansible repo

```
git clone https://git.frett27.net/frett27/aws_flink_ansible
```

```
cd aws_flink_ansible/
```



edit inventory to specify machines internal dns, and ip for master



edit the ansible configuration to avoid the ssh key host cheking

```
sudo vi /etc/ansible/ansible.cfg
```

uncomment this to disable SSH key host checking

```
host_key_checking = False
```

launch the python install and ground zero the required packages

```
ansible-playbook -i inventory ansible-bootstrap-ubuntu-16.yml
```





```

PLAY [all] ***************************************************************************************************

TASK [install python 2] **************************************************************************************
changed: [ip-172-31-21-111.eu-west-1.compute.internal]
changed: [ip-172-31-28-119.eu-west-1.compute.internal]
changed: [ip-172-31-23-195.eu-west-1.compute.internal]
changed: [ip-172-31-22-68.eu-west-1.compute.internal]
changed: [ip-172-31-22-37.eu-west-1.compute.internal]
changed: [ip-172-31-23-107.eu-west-1.compute.internal]
changed: [ip-172-31-27-201.eu-west-1.compute.internal]

PLAY RECAP ***************************************************************************************************
ip-172-31-21-111.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-22-37.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-22-68.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-23-107.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-23-195.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-27-201.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0
ip-172-31-28-119.eu-west-1.compute.internal : ok=1    changed=1    unreachable=0    failed=0

```



install flink cluster , with a /data nfs share, using ansible

```
ansible-playbook -i inventory playraw
```



# Download the OSM Datas and run the process

```
cd /data
wget http://download.geofabrik.de/europe/france-latest.osm.pbf
```



start the flink cluster, using the `/opt/flink/bin/start-cluster.sh` script

Launch process in the webconsole, using the osm-flink-tools jar

