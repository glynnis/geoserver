package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.RunTestSetup;
import org.geoserver.test.SystemTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockrunner.mock.web.MockHttpServletResponse;

@Category(SystemTest.class)
public class GeoServerCustomFilterTest extends GeoServerSystemTestSupport {

    enum Pos {
     FIRST,LAST,BEFORE,AFTER;   
    };

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    @After
    public void removeCustomFilterConfig() throws Exception {
        GeoServerSecurityManager secMgr= getSecurityManager();
        if(secMgr.listFilters().contains("custom")) {
            secMgr.removeFilter(secMgr.loadFilterConfig("custom"));
        }
    }
    @Test public void testInactive() throws Exception {
        HttpServletRequest request = createRequest("/foo");
        MockHttpServletResponse response = dispatch(request);
        assertNull(response.getHeader("foo"));
    }

    void setupFilterEntry(Pos pos, String relativeTo, boolean assertSecurityContext) 
        throws Exception {
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        
        FilterConfig config = new FilterConfig();
        config.setName("custom");
        config.setClassName(Filter.class.getName());
        config.setAssertAuth(assertSecurityContext);        
        secMgr.saveFilter(config);

        SecurityManagerConfig mgrConfig = secMgr.getSecurityConfig();
        mgrConfig.setConfigPasswordEncrypterName(getPlainTextPasswordEncoder().getName());

        mgrConfig.getFilterChain().remove("custom");
        if (pos==Pos.FIRST)
            mgrConfig.getFilterChain().insertFirst("/**",  "custom");
        if (pos==Pos.LAST)
            mgrConfig.getFilterChain().insertLast("/**",  "custom");
        if (pos==Pos.BEFORE)
            mgrConfig.getFilterChain().insertBefore("/**",  "custom",relativeTo);
        if (pos==Pos.AFTER)
            mgrConfig.getFilterChain().insertAfter("/**",  "custom",relativeTo);
        
        secMgr.saveSecurityConfig(mgrConfig);
    }

    @Test 
    public void testFirst() throws Exception {
        setupFilterEntry(Pos.FIRST, null, false);

        HttpServletRequest request = createRequest("/foo");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Test
    public void testLast() throws Exception {
        setupFilterEntry(Pos.LAST, null, true);

        HttpServletRequest request = createRequest("/foo");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Test
    public void testBefore() throws Exception {
        setupFilterEntry(Pos.BEFORE, 
            GeoServerSecurityFilterChain.ANONYMOUS_FILTER, false);

        HttpServletRequest request = createRequest("/foo");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Test
    public void testAfter() throws Exception {
        setupFilterEntry(Pos.AFTER, 
            GeoServerSecurityFilterChain.ANONYMOUS_FILTER, true);

        HttpServletRequest request = createRequest("/foo");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Arrays.asList((javax.servlet.Filter)applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends GeoServerSecurityFilter> getFilterClass() {
            return Filter.class;
        }
        @Override
        public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
            Filter f = new Filter();
            f.setAssertAuth(((FilterConfig)config).isAssertSecurityContext());
            return f;
        }
    }

    static class FilterConfig extends SecurityFilterConfig {
        boolean assertAuth = true;

        public void setAssertAuth(boolean assertAuth) {
            this.assertAuth = assertAuth;
        }

        public boolean isAssertSecurityContext() {
            return assertAuth;
        }
    }

    static class Filter extends GeoServerSecurityFilter {

        boolean assertAuth = true;

        public Filter() {
        }

        public void setAssertAuth(boolean assertAuth) {
            this.assertAuth = assertAuth;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (assertAuth) {
                assertNotNull(auth);
            }
            else {
                assertNull(auth);
            }
            ((HttpServletResponse)response).setHeader("foo", "bar");
            chain.doFilter(request, response);
        }
    }
}
