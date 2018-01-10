# netty4114
netty4114



核心组件：
Channel  
EventLoop EventLoopGroup 
ChannelFuture 
ChannelPipeline 
ChannelHandlerContext 
ChannelHandler

关系梳理：

  1：一个EventLoopGroup 包含1个或者多个EventLoop；

  2：一个EventLoop在它的生命周期内只和一个Thread绑定；
  
  3 : 所有由EvetLoop处理的IO事件都将在它专有的Thread上被处理;
  
  4：一个Channel 在它的生命周期内只注册于一个EventLoop ;
  
  5: 一个EventLoop 可能会被分配给一个或者多个Channel  ;
  
  6：Each channel has its own pipeline and it is created automatically when a new channel is created。一个Channel 在它的生命周期内  绑定一个ChannelPipeline。
  
7：一个 Channel 包含了一个 ChannelPipeline, 而 ChannelPipeline 中又维护了一个由 ChannelHandlerContext 组成的双向链表. 这个链表的头是 HeadContext, 链表的尾是 TailContext, 并且每个 ChannelHandlerContext 中又关联着一个 ChannelHandler.



 核心思维：
  netty是个异步执行的网络框架。很多操作都是基于Future/Promise模型.
  
  典型的源码：
  
   // netty异步编程模型  很多方法都是异步的。 看其很多实现方法都有体现  
           private ChannelFuture doResolveAndConnect0(final Channel channel, SocketAddress remoteAddress,
                                               final SocketAddress localAddress, final ChannelPromise promise) {
           try {
            final EventLoop eventLoop = channel.eventLoop();
            final AddressResolver<SocketAddress> resolver = this.resolver.getResolver(eventLoop);
            if (!resolver.isSupported(remoteAddress) || resolver.isResolved(remoteAddress)) {
                // Resolver has no idea about what to do with the specified remote address or it's resolved already.
                doConnect(remoteAddress, localAddress, promise);
                return promise;
            }
            //异步操作 解析地址
            final Future<SocketAddress> resolveFuture = resolver.resolve(remoteAddress);
            if (resolveFuture.isDone()) {
            	 //异步操作完成执行
                final Throwable resolveFailureCause = resolveFuture.cause();
                    // Failed to resolve immediately
                    channel.close();
                    promise.setFailure(resolveFailureCause);
                } else {
                    // Succeeded to resolve immediately; cached? (or did a blocking lookup)
                    doConnect(resolveFuture.getNow(), localAddress, promise);
                }
                return promise;
            }
            // Wait until the name resolution is finished.
          //异步操作未完成执行 添加监听器 监听器里面执行下一步操作 类似js的callback
            resolveFuture.addListener(new FutureListener<SocketAddress>() {
                @Override
                public void operationComplete(Future<SocketAddress> future) throws Exception {
                    if (future.cause() != null) {
                        channel.close();
                        promise.setFailure(future.cause());
                    } else {
                        doConnect(future.getNow(), localAddress, promise);
                    }
                }
            });
        } catch (Throwable cause) {
            promise.tryFailure(cause);
        }
        return promise;
    }


 netty Future/Promise模型 接口
     
     
     Future接口扩展了Java的java.util.concurrent.Future，最主要的改进就是增加了监听器Listener接口，通过监听器可以让异步执行更加的有效率，不需要通过get来等待异步执行结束，而是通过监听器回调来精确地控制异步执行。
     该接口是只读的。
     ChannelFuture接口扩展了Netty的Future接口，表示一种没有返回值的异步调用，同时关联了Channel,跟一个Channel绑定.
     
     Promise接口扩展了Future接口表示一种可写的future,可以改变future状态。可以设置异步执行的结果。
     
     ChannelPromise接口扩展了Promise和ChannelFuture，绑定了Channel，又可写异步执行结构，又具备了监听者的功能，是Netty实际编程使用的表示异步执行的接口。
     
     DefaultPromise是一个Promise的实现类。并且继承了AbstractFuture 类 ，此类实现了大部分 Promise 和 Future接口的方法。
     
     DefaultChannelPromise是ChannelPromise的实现类，并且继承了DefaultPromise,  它是实际运行时的Promoise实例。Channel接口提供了newPromise接口(内部调用DefaultChannelPipeline 的newPromise)，表示Channel要创建一个异步执行的动作
     
     
     
     
     ChannelPipeline 增加ChannelHandler 有2种方式：
     一种是 每个channel(ChannelPipeline).都新new一个对象。对每个channel 都是独立的。 这样的话，每个ChannelHandler可以有对象属性存储一些信息。
     
     
  public class DataServerHandler extends SimpleChannelInboundHandler<Message> {
     private boolean loggedIn;
      @Override
     public void channelRead0(ChannelHandlerContext ctx, Message message) {
         Channel ch = e.getChannel();
         if (message instanceof LoginMessage) {
             authenticate((LoginMessage) message);
             loggedIn = true;
         } else (message instanceof GetDataMessage) {
             if (loggedIn) {
                 ch.write(fetchSecret((GetDataMessage) message));
             } else {
                 fail();
             }
         }
     }
     ...
 }
 
 
  // Create a new handler instance per channel.
 // See ChannelInitializer.initChannel(Channel).
 public class DataServerInitializer extends ChannelInitializer<Channel> {
      @Override
     public void initChannel(Channel channel) {
         channel.pipeline().addLast("handler", new DataServerHandler());
     }
 }
 
     第二种是共享一个ChannelHandler,每个channel(ChannelPipeline)都一样。这个时候需要对ChannelHandler增加@Sharable注解。这个时候如果有对象属性不是线程安全的，而且每个channel都可以改变。虽然ChannelHandler是共享的，但是添加到ChannelPipeline里面的，是包装了ChannelHandlerContext的。每个ChannelHandlerContext都是唯一的，是每次都要new的。所以这个时候需要单独存储信息，可以使用ChannelHandlerContext的 attr
     
     
  @Sharable
 public class DataServerHandler extends SimpleChannelInboundHandler<Message> {
     private final AttributeKey<Boolean> auth =
           AttributeKey.valueOf("auth");
      @Override
     public void channelRead(ChannelHandlerContext ctx, Message message) {
         Attribute<Boolean> attr = ctx.attr(auth);
         Channel ch = ctx.channel();
         if (message instanceof LoginMessage) {
             authenticate((LoginMessage) o);
             attr.set(true);
         } else (message instanceof GetDataMessage) {
             if (Boolean.TRUE.equals(attr.get())) {
                 ch.write(fetchSecret((GetDataMessage) o));
             } else {
                 fail();
             }
         }
     }
     ...
 }
 public class DataServerInitializer extends ChannelInitializer<Channel> {
     private static final DataServerHandler SHARED = new DataServerHandler();
      @Override
     public void initChannel(Channel channel) {
         channel.pipeline().addLast("handler", SHARED);
     }
 }
 
 
 
 

客户端连接：
典型的样例：
  // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(new EchoClientHandler());
                 }
             });
            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }

启动线程

Bootstrap.connect--> Bootstrap.doResolveAndConnect-->AbstractBootstrap.initAndRegister(创建channel创建 绑定一个ChannelPipeline)-->Bootstrap.init-->ChannelPipeline.addLast(这里会把上面设置在Bootstrap的handler添加到此channel的ChannelPipeline维护的双向列表里面,这个handler就是ChannelInitializer。后面执行他的handlerAdded才会把真正我们设置的handler添加到ChannelPipeline里面，添加完并移除ChannelInitializer )-->DefaultChannelPipeline.newContext(每个handler在增加到ChannelPipeline里面之前都会包装在Context里面, Context添加到ChannelPipeline里面会根据hander设置的EventExecutorGroup 选择EventGroup,默认是channel的EventGroup(IO线程),添加到ChannelPipeline之后，会根据channel是否注册去延迟(DefaultChannelPipeline.callHandlerCallbackLater)还是立刻执行(DefaultChannelPipeline.callHandlerAdded0-->AbstractChannelHandlerContext.handler.handlerAdded) 。这里channel还没有注册是延迟执行.最终执行handlerAdded是设置handler时候设置的EventGroup)-->MultithreadEventLoopGroup.register(Bootstrap设置的EventLoopGroup这里是NioEventLoopGroup,初始化会根据配置(MultithreadEventExecutorGroup构造函数初始化)初始化n个EventGroup，然后根据选择器选择一个返回)-->SingleThreadEventLoop.register-->AbstractUnsafe.register(把channel注册到上一步返回的NioEventLoop,注意这里的NioEventLoop还没有startthead)-->SingleThreadEventExecutor.execute(这里要执行的是AbstractUnsafe.register0)-->SingleThreadEventExecutor.startThread(上面channel绑定的NioEventLoop开启thread)-->SingleThreadEventExecutor.addTask(把真正的注册AbstractUnsafe.register0放到上面channel绑定的io线程NioEventLoop的执行队列。此时会有一个IO线程启动执行)-->Bootstrap.doResolveAndConnect0(如果已经注册成功执行这是当前线程执行,否则会在regFuture.addListener()里面等注册完成再执行，这个时候是在事件循环线程执行)-->f.channel().closeFuture().sync()




io事件循环线程：再上面的启动线程运行到SingleThreadEventExecutor.startThread会启动io事件循环线程，NioEventLoop事件循环线程会一直在循环执行根据一定的策略主要执行 select选择器任务，普通任务，定时延迟任务。
启动开始的时候，会接收到一个任务AbstractUnsafe.register0然后执行.
AbstractUnsafe.register0-->AbstractNioChannel.doRegister(把channel绑定的java的 SelectableChannel绑定到NioEventLoop的Selector,此时channel 注册成功,把channel注册到 selector上ops用0  这里没有绑定到OP_READ OP_WRITE OP_ACCEPT 在connect 或者bind成功后 fireChannelActive里面会readIfIsAutoRead 会调用 激活read方法 然后会注册感兴趣事件)-->ChannelPipeline.invokeHandlerAddedIfNeeded-->(执行ChannelPipeline里面因为没有注册成功而延迟执行的handler的handlerAdded,这里面会执行上面ChannelInitializer的延迟方法)ChannelInitializer.handlerAdded-->ChannelInitializer.initChannel(这里执行上面自定义增加的handler已经每个handler的handlerAdded,最后移除自己,注意执行handlerAdded是设置handler时候设置的EventGroup有可能不是io事件循环线程)-->AbstractUnsafe.safeSetSuccess(设置注册future为注册成功)-->Bootstrap.doResolveAndConnect0(regFuture成功回调里面执行)-->Bootstrap.doConnect(中间还有resolveFuture解析地址的，略过不考虑,这里会把真正的连接方法channel.connect()放到io事件循环线程里面待处理,所以这里会继续处理AbstractUnsafe.register0的代码)-->ChannelPipeline.fireChannelRegistered(handler注册事件回调顺序是 绑定的channel先registered 然后才能调用 handlerAdded,其他的回调方法调用前都要检查当前Context是否ADD_COMPLETE AbstractChannelHandlerContext.invokeHandler())-->AbstractUnsafe.isActive(如果已经是active会根据情况执行pipeline.fireChannelActive()和beginRead()这里是false不需要执行)-->channel.connect-->ChannelPipeline.connect-->TailContext.connect-->HeadContext.connect-->AbstractNioUnsafe.connect(这里面会调用javaChannel去连接服务端socket,非阻塞的io会立刻返回。然后设置超时任务处理连接超时没返回调用close资源,还有增加取消连接监听器,取消连接也要close资源然后返回connectPromise)-->NioEventLoop.processSelectedKey(在io事件循环里面会监听select事件一旦connect完成)-->AbstractNioUnsafe.finishConnect-->NioSocketChannel.doFinishConnect-->AbstractNioChannel.fulfillConnectPromise(设置connectPromise连接成功,调用ChannelPipeline.fireChannelActive,最后设置connectPromise为null,客户端连接结束)


服务端连接：
典型的样例：
 // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(new ServerHandler());
                 }
             });
           
            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }




启动线程: 
    服务端和客户端大致流程一样，服务端可以配置两个group，一个是 bossGroup, 用于处理客户端的连接请求; 另一个是 workerGroup, 用于处理与各个客户端连接的 IO 操作。handler option都有child。分别用于serverchannel 和 channel
   AbstractBootstrap.bind--> AbstractBootstrap.doBind-->AbstractBootstrap.initAndRegister(创建channel创建 绑定一个ChannelPipeline)-->ServerBootstrap.init(这里会定义一个ChannelInitializer,先添加上面配置的handler，然后再添加一个ServerBootstrapAcceptor handler去处理子channel,这个添加时放在事件循环线程的任务队列，也就是说他会在serverchannel 定义的handler执行完毕ChannelRegistered 再添加)-->ChannelPipeline.addLast(这里会把上面设置在ServerBootstrap的handler添加到此serverchannel的ChannelPipeline维护的双向列表里面,这个handler就是ChannelInitializer。后面执行他的handlerAdded才会把真正我们设置的handler添加到ChannelPipeline里面，添加完并移除ChannelInitializer )-->DefaultChannelPipeline.newContext(每个handler在增加到ChannelPipeline里面之前都会包装在Context里面, Context添加到ChannelPipeline里面会根据hander设置的EventExecutorGroup 选择EventGroup,默认是channel的EventGroup(IO线程),添加到ChannelPipeline之后，会根据channel是否注册去延迟(DefaultChannelPipeline.callHandlerCallbackLater)还是立刻执行(DefaultChannelPipeline.callHandlerAdded0-->AbstractChannelHandlerContext.handler.handlerAdded) 。这里channel还没有注册是延迟执行.最终执行handlerAdded是设置handler时候设置的EventGroup)-->MultithreadEventLoopGroup.register(Bootstrap设置的EventLoopGroup这里是NioEventLoopGroup,初始化会根据配置(MultithreadEventExecutorGroup构造函数初始化)初始化n个EventGroup，然后根据选择器选择一个返回)-->SingleThreadEventLoop.register-->AbstractUnsafe.register(把channel注册到上一步返回的NioEventLoop,注意这里的NioEventLoop还没有startthead)-->SingleThreadEventExecutor.execute(这里要执行的是AbstractUnsafe.register0)-->SingleThreadEventExecutor.startThread(上面channel绑定的NioEventLoop开启thread)-->SingleThreadEventExecutor.addTask(把真正的注册AbstractUnsafe.register0放到上面channel绑定的io线程NioEventLoop的执行队列。此时会有一个IO线程启动执行)-->AbstractBootstrap.doBind0(如果已经注册成功执行这是当前线程执行,否则会在regFuture.addListener()里面等注册完成再执行，这个时候是在事件循环线程执行)-->f.channel().closeFuture().sync()


io事件循环线程


AbstractUnsafe.register0-->AbstractNioChannel.doRegister(把channel绑定的java的 SelectableChannel绑定到NioEventLoop的Selector,此时channel 注册成功,把channel注册到 selector上ops用0  这里没有绑定到OP_READ OP_WRITE OP_ACCEPT 在connect 或者bind成功后 fireChannelActive里面会readIfIsAutoRead 会调用 激活read方法 然后会注册感兴趣事件)-->ChannelPipeline.invokeHandlerAddedIfNeeded-->(执行ChannelPipeline里面因为没有注册成功而延迟执行的handler的handlerAdded,这里面会执行上面ChannelInitializer的延迟方法)ChannelInitializer.handlerAdded-->ChannelInitializer.initChannel(这里执行上面自定义增加的handler已经每个handler的handlerAdded,最后移除自己,注意执行handlerAdded是设置handler时候设置的EventGroup有可能不是io事件循环线程)-->AbstractUnsafe.safeSetSuccess(设置注册future为注册成功)-->AbstractBootstrap.doBind0(regFuture成功回调里面执行这里会把真正的连接方法channel.bind()放到io事件循环线程里面待处理,所以这里会继续处理AbstractUnsafe.register0的代码)-->ChannelPipeline.fireChannelRegistered(handler注册事件回调顺序是 绑定的channel先registered 然后才能调用 handlerAdded,其他的回调方法调用前都要检查当前Context是否ADD_COMPLETE AbstractChannelHandlerContext.invokeHandler())-->AbstractUnsafe.isActive(如果已经是active会根据情况执行pipeline.fireChannelActive()和beginRead()这里是false不需要执行)-->ChannelPipeline.addLast(ServerBootstrapAcceptor 这个hander这个时候会被添加)-->channel.bind-->ChannelPipeline.bind-->TailContext.bind-->HeadContext.bind-->AbstractNioUnsafe.bind-->NioServerSocketChannel.doBind(这里面会调用javaChannel bind成功之后 isActive()就为true)-->AbstractNioUnsafe.invokeLater(放到事件循环队列稍后执行 这里要执行的是fireChannelActive)-->AbstractNioUnsafe.safeSetSuccess-->ChannelPipeline.fireChannelActive-->HeadContext.readIfIsAutoRead-->channel.read-->HeadContext.read-->AbstractUnsafe.beginRead-->AbstractNioUnsafe.doBeginRead(这里会注册serverchannel 为OP_ACCEPT,去监听连接。启动完毕)


服务端启动完毕后 。bossGroup的serverchannel会监听OP_ACCEPT事件，如果有连接接入。会执行NioMessageUnsafe.read()这里最终会触发fireChannelRead,然后会执行serverchannel的handler链.ServerBootstrapAcceptor的channelRead里面会把 接收到的channel连接注册到另一个workerGroup里面，并且添加child的handler option等。





read write flush:
   read:
      ChannelOutboundInvoker.read(channel.read)  NioMessageUnsafe.read  ChannelInboundInvoker.fireChannelRead的关系：
      channel.read 或者 ChannelOutboundInvoker.read的作用是  调用unsafe.beginRead 注册通道感兴趣的事件 因为通道注册到selector的时候 注册的是0.这里可能注册 OP_READ  OP_ACCEPT 事件注册到通道以后,有事件发生会调用NioByteUnsafe.read 去把channel里面的数据读取到object msg里面然后会调用Inbound的fireChannelRead 去Inbound链路里面处理数据
 
  write flush:
      write flush 关系到  ChannelOutboundBuffer outboundBuffer 的使用。 Entry(flushedEntry) --> ... Entry(unflushedEntry) --> ... Entry(tailEntry)
    用一个entry链表 flushedEntry,unflushedEntry,tailEntry三个标记位置 来 进行操作处理flushedEntry + flushed 标记已经flush的  unflushedEntry一直到tailEntry 表示unflushed.
    write操作是把当前需要写的msg 放入entry队列 并且都是unflushedEntry的。 flush操作会把当前队列unflushedEntry设置为null。并增加 flushed值 因为flushedEntry始终在头部，所以这样可以控制flushedEntry个数。 然后会write到channel。如果不能write 会设置  key.interestOps(interestOps | SelectionKey.OP_WRITE)等channel可以write的时候 再write.

   

 
 
 内存泄漏检测 ResourceLeakDetector 
   大致的原理是 :
 netty的内存泄漏检测用的是PhantomReference和ReferenceQueue。
      主要原理是 用PhantomReference包装的对象。如果不可达就会被加入到ReferenceQueue。当检测ReferenceQueue的数据的时候，根据逻辑判断。
netty的内存检测如果检测到ReferenceQueue的数据的时候，就说明有内存泄漏。因为PhantomReference包装的对象都是ReferenceCounted对象的实例。并且最后都包装成DefaultResourceLeak对象 放到PhantomReference 里面。外部使用的ReferenceQueue对象  都被包装成SimpleLeakAwareByteBuf或者AdvancedLeakAwareByteBuf 。 这样如果SimpleLeakAwareByteBuf或者AdvancedLeakAwareByteBuf release全部引用的时候会调用DefaultResourceLeak.close清除这个
DefaultResourceLeak。这样就该对象不可达的时候,虽然ReferenceQueue有记录。但是也不会报告泄漏。 检测泄漏的逻辑是 ReferenceQueue有记录 并且在内部的记录带检测对象 里面 allLeaks也有记录，才会报告内存泄漏。 
  Allocator生成每个ByteBuf之后都会调用AbstractByteBufAllocator的toLeakAwareBuffer方法。包装ByteBuf最终返回的是 SimpleLeakAwareByteBuf或者AdvancedLeakAwareByteBuf 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 InternalThreadLocalMap  FastThreadLocal
 
 ResourceLeakDetectorFactory
 
 HashedWheelTimer
 
 Recycler
 
 PooledByteBufAllocator UnpooledByteBufAllocator
     
 AccessController.doPrivileged