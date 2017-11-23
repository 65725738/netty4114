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
        Bootstrapgroup = new NioEventLoopGroup();
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

启动 线程
Bootstrap.connect--> Bootstrap.doResolveAndConnect-->AbstractBootstrap.initAndRegister(创建channel创建 绑定一个ChannelPipeline)-->Bootstrap.init-->ChannelPipeline.addLast(config.handler())(这里会把上面设置在Bootstrap的handler添加到此channel的ChannelPipeline维护的双向列表里面,这个handler就是ChannelInitializer。后面执行他的handlerAdded才会把真正我们设置的handler添加到ChannelPipeline里面，添加完并移除ChannelInitializer )-->DefaultChannelPipeline.newContext(每个handler在增加到ChannelPipeline里面之前都会包装在Context里面 Context添加到ChannelPipeline里面会根据hander设置的EventExecutorGroup 选择EventGroup默认是channel的EventGroup(IO线程),添加到ChannelPipeline之后，会根据channel是否注册去延迟(DefaultChannelPipeline.callHandlerCallbackLater)还是立刻执行(DefaultChannelPipeline.callHandlerAdded0-->AbstractChannelHandlerContext..handler().handlerAdded) 。这里channel还没有注册是延迟执行.最终执行handlerAdded是设置handler时候设置的EventGroup)-->MultithreadEventLoopGroup.register(Bootstrap设置的Bootstrapgroup这里是NioEventLoopGroup,初始化会根据配置(MultithreadEventExecutorGroup构造函数初始化)初始化n个EventGroup，然后根据选择器选择一个返回)-->SingleThreadEventLoop.register-->AbstractUnsafe.register(把channel注册到上一步返回的NioEventLoop,注意这里的NioEventLoop还没有startthead)-->SingleThreadEventExecutor.execute(Runnable task)(这里要执行的是AbstractUnsafe.register0)-->SingleThreadEventExecutor.startThread(上面channel绑定的NioEventLoop开启thread)-->SingleThreadEventExecutor.addTask(把真正的注册AbstractUnsafe.register0放到上面channel绑定的io线程NioEventLoop的执行队列。此时会有一个IO线程启动执行)-->Bootstrap.doResolveAndConnect0(如果已经注册成功执行,否则会在regFuture.addListener()里面等注册完成再执行)-->f.channel().closeFuture().sync()


io事件循环线程：再上面的启动线程运行到SingleThreadEventExecutor.startThread会启动io事件循环线程，NioEventLoop事件循环线程会一直在循环执行根据一定的策略主要执行 select选择器任务，普通任务，定时延迟任务。
启动开始的时候，会接收到一个任务AbstractUnsafe.register0然后执行.
AbstractUnsafe.register0-->AbstractNioChannel.doRegister(把channel绑定的java的 SelectableChannel绑定到NioEventLoop的Selector此时channel 注册成功)-->ChannelPipeline.invokeHandlerAddedIfNeeded(执行ChannelPipeline里面因为没有注册成功而延迟执行的handler的handlerAdded,这里面会执行上面ChannelInitializer的延迟方法)ChannelInitializer.handlerAdded-->ChannelInitializer.initChannel(这里执行上面自定义增加的handler已经每个handler的handlerAdded,最后移除自己)

 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 EventLoop EventLoopGroup 模型
 
 
 Bootstrap
 
 
 unsafe
 
 
 ChannelOutboundBuffer
 
 
 
 ByteBufAllocator
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
     
