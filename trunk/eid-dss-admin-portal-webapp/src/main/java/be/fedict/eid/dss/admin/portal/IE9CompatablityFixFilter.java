package be.fedict.eid.dss.admin.portal;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IE9CompatablityFixFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // else RichFaces' ajax4jsf javascript no workie
        ((HttpServletResponse) response).setHeader("X-UA-Compatible", "IE=EmulateIE7");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
