package com.trident.apis.entitymanager.filter;

import com.trident.apis.entitymanager.repository.TestModeRepository;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class TestModeFilter implements Filter {

    private static final Logger logger = Logger.getLogger(TestModeFilter.class);

    @Autowired
    private TestModeRepository testModeRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (testModeRepository.isEnabled()) {
            logger.info("Test mode on for this request.");
            RepositoryDecorator.setPrefix("test_dive_");
        } else {
            RepositoryDecorator.setPrefix("dive_");
        }
        chain.doFilter(httpServletRequest, response);
    }

    @Override
    public void destroy() {
    }
}
