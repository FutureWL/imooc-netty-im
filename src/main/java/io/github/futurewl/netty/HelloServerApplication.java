package io.github.futurewl.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * 功能描述：
 *
 * @author weilai create by 2019-06-25:14:50
 * @version 1.0
 */
public class HelloServerApplication {

    public static void main(String[] args) throws InterruptedException {

        // 定义一对线程组
        // 主线程组，用于接收客户端的连接，但是不做任何处理，跟老板一样，不做事
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 从线程组，老板线程组会把任务丢给他，让手下线程组去做任务
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {

            // netty服务器的创建，ServerBootstrap 启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    // 设置组从线程组
                    .group(bossGroup, workGroup)
                    // 设置 NIO 的双向通道
                    .channel(NioServerSocketChannel.class)
                    // 子处理器，用于处理 workGroup
                    .childHandler(
                            // 初始化器，channel 注册后，会执行里面相应的初始化方法
                            new ChannelInitializer<SocketChannel>() {
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    // 每一个channel由多个handler共同组成管道pipeline
                                    ChannelPipeline pipeline = socketChannel.pipeline();
                                    // 通过 管道添加 Handler
                                    pipeline.addLast("HttpServerCodec", new HttpServerCodec());
                                    // 添加 自定义助手类，返回 Hello netty～
                                    pipeline.addLast("customerHandler",
                                            // 对于请求来讲，入站
                                            new SimpleChannelInboundHandler<HttpObject>() {
                                                protected void channelRead0(
                                                        ChannelHandlerContext channelHandlerContext,
                                                        HttpObject httpObject) throws Exception {
                                                    Channel channel = channelHandlerContext.channel();
                                                    // 显示客户端，远程地址
                                                    System.out.println(channel.remoteAddress());
                                                    // 定义发送的数据消息
                                                    ByteBuf content = Unpooled
                                                            .copiedBuffer("Hello netty~", CharsetUtil.UTF_8);
                                                    // 构建一个 HTTP Response
                                                    FullHttpResponse response = new DefaultFullHttpResponse(
                                                            HttpVersion.HTTP_1_1,
                                                            HttpResponseStatus.OK,
                                                            content
                                                    );
                                                    // 为响应增加数据类型和长度
                                                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                                                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                                                    // 把相应刷到客户端
                                                    channel.writeAndFlush(response);
                                                }
                                            });
                                }
                            });

            // 启动 Server 并且设置 8088 为启动的端口号，同时启动方式为同步
            ChannelFuture channelFuture = serverBootstrap.bind(8088).sync();
            // 监听关闭的 channel ，设置为同步方式
            channelFuture.channel().closeFuture().sync();
        } finally {
            // 优雅关闭
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
