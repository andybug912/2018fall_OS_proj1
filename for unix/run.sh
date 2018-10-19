javac MyServer.java MyClient.java Message.java Request.java

nohup java MyServer 3 0 > output/server0.out 2>&1 &
nohup java MyServer 3 1 > output/server1.out 2>&1 &
nohup java MyServer 3 2 > output/server2.out 2>&1 &

sleep 1
nohup java MyClient 1 3000 y > output/read0.out 2>&1 &
sleep 1
nohup java MyClient 2 4000 y > output/read1.out 2>&1 &
sleep 1
nohup java MyClient 0 0 n distributed_me_requests.txt 1 150 > output/write0.out 2>&1 &
sleep 1
nohup java MyClient 1 1000 n distributed_me_requests.txt 151 330 > output/write1.out 2>&1 &
sleep 1
nohup java MyClient 2 2000 n distributed_me_requests.txt 331 499 > output/write2.out 2>&1 &