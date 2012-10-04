package org.geoserver.wps.resource;

import java.io.File;

import org.geoserver.wps.WPSTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WPSResourceManagerTest extends WPSTestSupport {

    WPSResourceManager resourceMgr;
    
    @Before
    public void setUpInternal() throws Exception {
        resourceMgr = new WPSResourceManager();
    }

    @Test
    public void testAddResourceNoExecutionId() throws Exception {
        File f = File.createTempFile("dummy", "dummy", new File("target"));
        resourceMgr.addResource(new WPSFileResource(f));
    }
}