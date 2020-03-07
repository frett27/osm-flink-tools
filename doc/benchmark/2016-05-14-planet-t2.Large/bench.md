#Benchmark planet executing osm construct May 14th 2016 on t2.large instance with 1Tb Magnetic Disk

Planet integration, using flink 0.10.2

 
	ubuntu@ip-172-31-20-57:~/tmp$ ll -h
	total 32G
	drwxrwxr-x 4 ubuntu ubuntu 4.0K May 14 10:17 ./
	drwxr-xr-x 9 ubuntu ubuntu 4.0K May 14 10:18 ../
	-rw-rw-r-- 1 ubuntu ubuntu  32G May  5 06:54 planet-latest.osm.pbf
	drwxrwxr-x 3 ubuntu ubuntu 4.0K May 14 10:17 result/
	drwxrwxr-x 2 ubuntu ubuntu 4.0K May 14 09:09 results/


Generated CSV Folder Size : 

	


##Protocol

local cluster execution (1 taskmanager), 2 CPU, 8gb TOTAL RAM, 4Gb used


##Timing - configuration

![](timeline.png)

XXXXX hours on a EC2, t2.large instance, 2 CPU, 400 go magnetic storage unit.


#During execution

IO Stat : 

	Every 1.0s: iostat                                                                                                    Sat May 14 10:31:26 2016
	
	Linux 3.13.0-74-generic (ip-172-31-20-57)       05/14/2016      _x86_64_        (2 CPU)
	
	avg-cpu:  %user   %nice %system %iowait  %steal   %idle
	          13.55    0.02    0.30    2.49    0.62   83.02
	
	Device:            tps    kB_read/s    kB_wrtn/s    kB_read    kB_wrtn
	xvda             82.26       646.34      8977.01    4147213   57600416
	
Other measure :
	
	ubuntu@ip-172-31-20-57:~$ date
	Sat May 14 14:25:40 UTC 2016
	ubuntu@ip-172-31-20-57:~$ iostat
	Linux 3.13.0-74-generic (ip-172-31-20-57)       05/14/2016      _x86_64_        (2 CPU)
	
	avg-cpu:  %user   %nice %system %iowait  %steal   %idle
	          44.32    0.00    0.61    0.72   29.08   25.26
	
	Device:            tps    kB_read/s    kB_wrtn/s    kB_read    kB_wrtn
	xvda             55.83      1740.23      4937.38   35628301  101084904

	
	
sar :
	
	01:14:02 PM     CPU     %user     %nice   %system   %iowait    %steal     %idle
	01:16:01 PM     all     43.09      0.00      0.74      0.00     54.03      2.14
	01:18:01 PM     all     41.94      0.00      0.73      0.02     55.05      2.26
	01:20:01 PM     all     41.92      0.00      0.77      0.03     55.10      2.18
	01:22:01 PM     all     44.03      0.00      0.79      0.03     52.77      2.37
	01:24:01 PM     all     44.16      0.00      0.74      0.00     52.82      2.28
	01:26:01 PM     all     42.96      0.00      0.86      0.04     54.18      1.97
	01:28:01 PM     all     41.94      0.00      0.77      0.03     55.05      2.22
	01:30:01 PM     all     42.05      0.00      0.72      0.00     55.27      1.97
	01:32:01 PM     all     44.15      0.00      0.78      0.01     53.02      2.03

-> on aws, we have a 50% CPU steal ... a complete CPU ???

	ubuntu@ip-172-31-20-57:~$ iostat
	Linux 3.13.0-74-generic (ip-172-31-20-57)       05/16/2016      _x86_64_        (2 CPU)
	
	avg-cpu:  %user   %nice %system %iowait  %steal   %idle
	          43.68    0.02    0.87    0.10   49.40    5.92
	
	Device:            tps    kB_read/s    kB_wrtn/s    kB_read    kB_wrtn
	xvda             54.69      2344.63      4208.27  398856977  715891228


