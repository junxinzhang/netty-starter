##Netty学习第一天：同步阻塞式I/O源码分析

###服务端TimerServer
    TimerServer根据传入的参数设置监听端口，如果没有入参，使用默认值8080。
    启动后会发现主线程阻塞在ServerSocket的accept()方法上：可通过JConsole查看线程堆栈日志可以发现。
   ####JConsole是一个图形监视工具，用于监视本地或远程计算机上的Java虚拟机（JVM）和Java应用程序。)[点击查看详情](https://en.wikipedia.org/wiki/JConsole)
    
###客户端TimerClient
    客户端通过PrintWriter向服务端发送"QUERY TIME ORDER"指令，然后通过BufferedReader的readLine读取响应并打印。
    
###综上所述：发现同步阻塞式I/O存在的问题:
    每当有一个新的客户端请求接入时，服务端必须创建一个新的线程处理新接入的客户端链路，一个线程只能处理一个客户端连接。
    在高性能服务器应用领域，往往需要成千上万个客户端的并发连接，这种模型显然无法满足高性能，高并发的接入场景。
    