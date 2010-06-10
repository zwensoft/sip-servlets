/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.resource;

import org.mobicents.media.server.impl.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.clock.TimerImpl;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.spi.clock.Timer;

/**
 *
 * @author kulikov
 */
public class ProxyConnectionsTest {

    private Timer timer = new TimerImpl();
    
    private TestSource src = new TestSource("source");
    private TestSink sink = new TestSink("sink");
    
    private Proxy proxy = new Proxy("test-proxy");
    
    public ProxyConnectionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        timer = new TimerImpl();
        src.setSyncSource(timer);
        timer.start();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of connect method, of class AbstractSink.
     */
    @Test
    public void testProxyNegotiation() {
        src.connect(proxy);
        sink.connect(proxy);
        
        boolean res = src.getFormat() != null && src.getFormat().matches(AVProfile.PCMA);
        assertTrue("Format mismatch",res);
    }


    @Test
    public void testProxyNegotiation2() {
        sink.connect(proxy);
        src.connect(proxy);
        
        boolean res = src.getFormat() != null && src.getFormat().matches(AVProfile.PCMA);
        assertTrue("Format mismatch",res);
    }

    @Test
    public void testProxyNegotiation3() {
        proxy.connect(src);
        proxy.connect(sink);
        
        boolean res = src.getFormat() != null && src.getFormat().matches(AVProfile.PCMA);
        assertTrue("Format mismatch",res);
    }

    @Test
    public void testProxyNegotiation4() {
        proxy.connect(sink);
        proxy.connect(src);
        
        boolean res = src.getFormat() != null && src.getFormat().matches(AVProfile.PCMA);
        assertTrue("Format mismatch",res);
    }
    
    @Test
    public void testDisconnect() {
        src.connect(proxy);
        sink.connect(proxy);
        
        src.disconnect(proxy);
        sink.disconnect(proxy);
        
        assertTrue("Format still assigned",src.getFormat() == null);
        assertTrue("Format still assigned",sink.getFormat() == null);
    }

    @Test
    public void testDisconnect1() {
        sink.connect(proxy);
        src.connect(proxy);
        
        src.disconnect(proxy);
        sink.disconnect(proxy);
        
        assertTrue("Format still assigned",src.getFormat() == null);
        assertTrue("Format still assigned",sink.getFormat() == null);
    }

    @Test
    public void testDisconnect2() {
        sink.connect(proxy);
        src.connect(proxy);
        
        src.disconnect(proxy);
        sink.disconnect(proxy);
        
        assertTrue("Format still assigned",src.getFormat() == null);
        assertTrue("Format still assigned",sink.getFormat() == null);
    }

    @Test
    public void testDisconnect3() {
        sink.connect(proxy);
        src.connect(proxy);
        
        //src.disconnect(proxy);
        sink.disconnect(proxy);
        
        assertTrue("Format still assigned",src.getFormat() == null);
//        assertTrue("Format still assigned",sink.getFormat() == null);
    }
    
    private class TestSource extends AbstractSource {

        public TestSource(String name) {
            super(name);
        }

        public Format[] getFormats() {
            return new Format[]{AVProfile.PCMA, AVProfile.PCMU, AVProfile.L16_MONO};
        }

        public String getOtherPartyName() {
            return this.otherParty.getName();
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
        }
    }

    private class TestSink extends AbstractSink {

        public TestSink(String name) {
            super(name);
        }

        public Format[] getFormats() {
            return new Format[]{AVProfile.PCMA};
        }

        public boolean isAcceptable(Format format) {
            return true;
        }

        public String getOtherPartyName() {
            return this.otherParty.getName();
        }

        @Override
        public void onMediaTransfer(Buffer buffer) {
        }
    }

}