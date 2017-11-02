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
