package org.ethelred.libnetty_fcgi_test;

import com.github.fmjsjx.libnetty.fastcgi.FcgiAbortRequest;
import com.github.fmjsjx.libnetty.fastcgi.FcgiGetValues;
import com.github.fmjsjx.libnetty.fastcgi.FcgiGetValuesResult;
import com.github.fmjsjx.libnetty.fastcgi.FcgiMessage;
import com.github.fmjsjx.libnetty.fastcgi.FcgiParams;
import com.github.fmjsjx.libnetty.fastcgi.FcgiRequest;
import com.github.fmjsjx.libnetty.fastcgi.FcgiResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

class TestServerHandler extends SimpleChannelInboundHandler<FcgiMessage>
{
    static final String JAVA_VERSION = System.getProperty("java.version").split("_")[0];
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FcgiMessage msg) throws Exception {
        // message received
        System.out.println("-- message received --");
        System.out.println(msg);
        if (msg instanceof FcgiRequest) {
            String value = "X-Powered-By: JAVA/" + TestServerHandler.JAVA_VERSION + "\r\n"
                    + "content-type: text/html;charset=UTF-8\r\n"
                    + "\r\n"
                    + "<!DOCTYPE html>\n"
                    + "</html>\n"
                    + "<meta charset=\"UTF-8\" />\n"
                    + "<head>\n"
                    + "<title>Test FastCGI</title>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "<h1>Test Fast-CGI</h1>\n"
                    + paramsTable(((FcgiRequest) msg).params())
                    + "</body>\n"
                    + "\n</html>";
            FcgiResponse resp = new FcgiResponse(msg.protocolVersion(), msg.requestId(), 0,
                    Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
            System.out.println("FCGI_RESPONSE ==>");
            System.out.println(resp);
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        } else if (msg instanceof FcgiGetValues) {
            FcgiGetValuesResult result = new FcgiGetValuesResult(msg.protocolVersion());
            ((FcgiGetValues) msg).names().forEach(name -> result.put(name, "0"));
            ctx.writeAndFlush(result).addListener(ChannelFutureListener.CLOSE);
        } else if (msg instanceof FcgiAbortRequest) {
            ctx.close();
        }
    }

    private String paramsTable(FcgiParams params)
    {
        var buf = new StringBuilder("<table>\n");
        params.forEach((k, v) ->
                buf.append("<tr><td>")
                        .append(k)
                        .append("</td><td>")
                        .append(v)
                        .append("</td></tr>\n")
        );
        buf.append("</table>\n");
        return buf.toString();
    }
}
