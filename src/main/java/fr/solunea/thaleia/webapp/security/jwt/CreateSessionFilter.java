package fr.solunea.thaleia.webapp.security.jwt;

import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class CreateSessionFilter implements Filter {

    private final static Logger logger = Logger.getLogger(ThaleiaSession.class);

    public void init(FilterConfig config) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) throws ServletException,
            IOException {
        HttpSession httpSession = ((HttpServletRequest) request).getSession(true);
        logger.debug("Session HTTP créée.");
        chain.doFilter(request, resp);
    }

    public void destroy() {
    }

}
