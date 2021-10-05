package org.ethelred.libnetty_fcgi_test;

import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageDecoder;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.simple.SimpleLogger;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.utility.DockerImageName;

public class ExampleServer
{
    public static void main(String... args)
    {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info");
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        startNettyFcgi();
        startNginxProxy();
    }

    private static void startNginxProxy()
    {
        Testcontainers.exposeHostPorts(9000);
        NginxContainer<?> nginxContainer = new NginxContainer<>(DockerImageName.parse("nginx:1.20"));
        nginxContainer.withExposedPorts(80)
                        .withClasspathResourceMapping("nginx_default.conf", "/etc/nginx/conf.d/default.conf", BindMode.READ_ONLY);
        nginxContainer.start();
        System.out.printf("Nginx available at %s:%d%n", nginxContainer.getHost(), nginxContainer.getFirstMappedPort());
    }

    private static void startNettyFcgi()
    {
        FcgiMessageEncoder encoder = new FcgiMessageEncoder();

        NioEventLoopGroup group = new NioEventLoopGroup();
        try

        {
            ServerBootstrap b = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512).childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<Channel>()
                    {
                        @Override
                        protected void initChannel(Channel ch) throws Exception
                        {
                            ch.pipeline()
                                    .addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast(encoder)
                                    .addLast(new FcgiMessageDecoder())
                                    .addLast(new TestServerHandler());
                        }
                    });
            b.bind(9000).sync();
            System.out.println("TestServer started!");
            System.in.read();
        }
        catch (Exception e) {
            System.err.println("Unexpected exception: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        finally
        {
//            group.shutdownGracefully();
        }
    }
}