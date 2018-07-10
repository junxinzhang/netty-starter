# Netty学习day01：同步阻塞式I/O源码分析

## 服务端TimerServer
`TimerServer`根据传入的参数设置监听端口，如果没有入参，使用默认值8080。
启动后会发现主线程阻塞在ServerSocket的accept()方法上：可通过`JConsole`查看线程堆栈日志可以发现。
（_`JConsole`是一个图形监视工具，用于监视本地或远程计算机上的Java虚拟机（JVM）和Java应用程序。_）[点击查看详情](https://en.wikipedia.org/wiki/JConsole)
    
## 客户端TimerClient
客户端通过PrintWriter向服务端发送"QUERY TIME ORDER"指令，然后通过BufferedReader的readLine读取响应并打印。
    
## 同步阻塞式I/O存在的问题:
每当有一个新的客户端请求接入时，服务端必须创建一个新的线程处理新接入的客户端链路，一个线程只能处理一个客户端连接。
在高性能服务器应用领域，往往需要成千上万个客户端的并发连接，这种模型显然无法满足高性能，高并发的接入场景。
为了改进一个线程对应一个连接的模型，后来又演变出了一种通过线程池或者消息队列实现一个或多个线程处理N个客户端的模型，
由于它的底层通信机制仍然是使用同步阻塞I/O，所以被称为`伪异步`。
    
# Netty学习day02：**`伪异步`**阻塞式I/O源码分析

## 服务端TimerServer改造
`伪异步`I/O的主函数代码发生了变化，我们首先创建一个服务器时间处理类的线程池，但接收到新的客户端连接时，将请求Socket封装成一个task，
然后调用线程池的execute()方法执行，从而避免了每一个请求进入都创建一个新的线程。
由于线程池和消息队列都是有界的，因此，无论客户端并发连接数多大， 它都不会导致线程个数过于膨胀或者内存溢出，相比于一连接一线程模型，是
一种改良。
`伪异步`I/O通信框架采用了线程池实现，因此避免了为每个请求都创建一个独立线程造成的线程资源耗尽的问题。但是由于它底层的通信依然采用同步
阻塞模型，因此无法从根本上解决问题。

## `伪异步`I/O存在的问题:
当对Socket的输入流进行读取操作的时候，它会一直阻塞下去，直到发生如下三种事件
- 有数据可读
- 可用数据已经读取完毕
- 发生NPE或者I/O异常  
这意味着当对方发送请求或者应答消息比较缓慢，或者网络传输较慢时，读取输入流一方的通信将被长时间阻塞，如果对方要60s才能够将数据发送完成，
读取一方的I/O线程也将会被阻塞60s，在此期间，其他接入消息只能在消息队列中排队。  
当调用OutputStream的write()方法写输出流的时候，它将会被阻塞，知道所有要发送的字节全部写入完毕，或者发生异常。学习过TCP/IP相关知识
的人都知道，当消息的接收方处理缓慢的时候，将不能及时的从缓冲区中读取数据，这将会导致发送方的`tcp windows size`不断减小，直到为0，双
方处于`Keep-Alive`状态，消息发送方将不能再向TCP缓冲区写入消息，这时如果采用的是同步阻塞I/O，write操作将会被无限期阻塞，直到`tcp 
windows size`大于0或者发生异常。  
通过对输入流和输出流的API文档进行分析，我们了解到读和写操作都是同步阻塞的，阻塞的时间取决于对方I/O线程的处理速度和网络I/O的传输速度。
本质上来讲，我们无法保证生产环境的网络状况和对端的应用程序能足够快，如果我们的应用程序依赖于对方的处理速度，它的可靠性就非常差。也许在实
验室环境的性能测试结果令人满意，但是一旦上线运行，而面对恶劣的网络环境和良莠不齐的第三方系统，问题就会如火山一样喷发。  
`伪异步`I/O实际上仅仅是对之前I/O线程模型的一个简单优化，它无法从根本上解决同步I/O导致的通信线程阻塞问题。
### 通信对方返回应答时间过长会引起的级联故障
- 服务端处理缓慢，返回应答消息耗费60s，平时只需要10ms
- 采用`伪异步`I/O的线程正在读取故障服务节点的响应，由于读取输入流是阻塞的，它将会被同步阻塞60s
- 加入所有的线程都被故障服务器阻塞，那后续所有的I/O消息都将在队列中排队
- 由于线程池采用阻塞队列实现，但队列积满之后，后续入队的操作将被阻塞。
- 由于前端只有一个Accptor线程接收客户端接入，他被阻塞在线程池的同步阻塞队列之后，新的客户端请求消息将被拒绝，客户端将会产生大量的连接超时。
- 由于几乎所有的连接都超时，调用者会认为系统已经崩溃，无法接收新的请求消息。

# Netty学习day03：NIO编程

## NIO简介
- 官方：NIO有人称之为New I/O，原因在于它相对于之前的I/O类库是新增的
- 民间：由于之前老的I/O类库是阻塞式I/O，New I/O类库的目标就是要让Java支持非阻塞I/O，所以，更多人喜欢称之为非阻塞I/O（Non-block I/O）

## NIO类库简介
### 缓冲区Buffer
- ByteBuffer：字节缓冲区
- CharBuffer：字符缓冲区
- ShortBuffer：短整型缓冲区
- IntBuffer：整型缓冲区
- LongBuffer：长整形缓冲区
- FloatBuffer：浮点型缓冲区
- DoubleBuffer：双精度浮点型缓冲区
### 通道Channel
Channel可以分为两大类：用于网络读写的SelectableChannel和用于文件操作的FileChannel
### 多路复用器Selector
它是Java NIO编程的基础，熟练的掌握Selector对于NIO编程至关重要。多路复用器提供选择已经就绪的任务的能力。简单来说，Selector会不断地
轮询注册在其上的Channel，如果某个Channel上面发生读或写事件，这个Channel就处于就绪状态，会被Selector轮询出来，然后通过SelectionKey
可以获取就绪的Channel的集合，然后进行后续的I/O操作。
## NIO创建的TimerServer源码分析
- 在`MultiplexerTimerServer`的构造方法中进行资源初始化。创建多路复用器Selector、ServerSocketChannel，对Channel和TCP参数进行
配置。例如，将ServerSocketChannel设置为异步非阻塞模式，它的backlog设置为1024。系统资源初始化成功后，将ServerSocketChannel注册
到Selector，监听SelectionKey.OP_ACCEPT操作位。如果资源初始化失败（例如端口被占用），则退出。
- 在`MultiplexerTimerServer`的 _run()_ 方法中，while循环体中循环遍历selector，它的休眠时间为1s。无论是否有读写等事件发生，selector
每隔1s都被唤醒一次。selector也提供了一个无参的select()方法：当有处于就绪状态的Channel时，selector将返回该Channel的SelectionKey
集合。通过对就绪状态的Channel集合进行迭代，可以进行网络的异步读写操作。
- 在`MultiplexerTimerServer`的 *handlerInput()* 方法中，处理新接入的客户端请求消息，根据SelectionKey的操作位进行判断即可获知
网络事件的类型，通过ServerSocketChannel的accept()接收客户端的连接请求并创建SocketChannel实例。完成上述操作后，相当于完成了TCP的
三次握手，TCP物理链路正式建立。注意：我们需要将新创建的SocketChannel设置为异步非阻塞，同时也可以对其TCP参数进行设置。例如，TCP接收和
发送缓冲区的大小等。

# Netty学习day04：AIO编程
## AIO简介
`NIO 2.0`引入了新的异步通道的概念，并提供了异步文件通道和异步套接字通道的实现。异步通道通过以下两种方式来获取操作结果。  
1、通过java.util.concurrent.Future类来标识异步操作的结果；  
2、在执行异步操作的时候传入一个java.nio.channels。  
ComletionHandler接口的实现类为操作完成的回调。  
`NIO 2.0`的异步套接字通道是真正的异步非阻塞I/O，对应于UNIX网络编程中的事件驱动I/O（AIO）。它不需要通过多路复用器（Selector）对注册
的通道进行轮询操作即可实现异步读写，从而简化了NIO的编程模型。
## AIO创建的TimerServer源码分析
- 在`AsyncTimerServerHandler`的 *run()* 方法中，初始化CountDownLatch对象，它的作用是在完成一组正在执行的操作之前，允许当前的线
程一直阻塞。在代码中，我们让线程在此阻塞，防止服务端执行完成退出。
- 在`AsyncTimerServerHandler`的 *doAccept()* 方法中，我们用于接收客户端的连接，由于是异步操作，我们可以传递一个CompletionHandler
<AsynchronousSocketChannel, ? super A>类型的handler实例接收accept操作成功的通知消息。
- 在`AcceptCompletionHandler`中有两个方法*completed()*，_failed()_  
-- completed接口的实现，我们从attachment获取成员变量AsynchronousServerSocketChannel，然后继续调用它的accept方法。这里有个疑问：
既然已经接收客户端成功了，为什么还要再次调用accept方法呢？原因是：调用AsynchronusServerSocketChannel的accept方法后，如果有新的
客户端连接接入，系统将回调我们传入的CompletionHandler实例的completed方法，表示新的客户端已经接入成功。因为一个AsynchronousServerSocketChannel
可以接收成千上万个客户端，所以需要继续调用它的accept方法，接收其他的客户端连接，最终形成一个循环。每当接收一个客户端的连接成功后，再异步
接收新的客户端连接。
- 链路建立成功之后，服务端需要接收客户端的请求消息，在代码中，我们创建新的ByteBuffer，与分配1MB的缓冲区。通过调用AsynchronousSocketChannel
的read方法进行异步读操作。read方法的参数有三个，**_ByteBuffer dst_**:接收缓冲区，用于从异步Channel中读取数据包；**_A attachment_**:
异步Channel携带的附件，通知回调的时候作为入参使用；**_CompletionHandler<Integer, ? super A>_**：接收通知回调的业务Handler，
在这里为`ReadCompletionHander`
- `ReadCompletionHander`的构造方法将AsynchronousSocketChannel通过参数传递到ReadCompletionHandler中，当作成员变量来使用，
主要用户读取半包消息和发送应答。这里不对半包读写进行具体说明。
- `ReadCompletionHander`的 *completed()* 方法，首先对attachment进行flip操作，为后续从缓冲区读取数据做准备。根据缓冲区的可读
字节数创建byte数组，然后通过new String方法创建请求消息，对请求消息进行判断。然后调用doWrite方法发送给客户端。
## AIO创建的TimerClient源码分析
由于在AsyncTimerClientHandler中大量使用了匿名内部类，所以代码看起来有点复杂。
- 首先在构造方法中通过AsynchronousSocketChannel的open方法创建一个新的AsynchronousSocketChannel对象。
- 然后在*run()* 方法中创建CountDownLatch进行等待，防止异步操作没有执行完成线程就退出。通过connect方法发起异步操作，它有两个参数：
**_A attachment_**：AsynchronousSocketChannel的附件，用于回调通知时作为入参被传递，调用这可以自定义；**_CompletionHandler
<Void, ? suuper A> handler_**：异步操作回调通知接口，由调用者实现。在例子中，这两个参数都使用 AsyncTimerClientHandler 类本身
因为它实现了CompletionHandler接口。

