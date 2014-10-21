package org.lantern.proxy.pt;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.lantern.LanternUtils;
import org.lantern.proxy.FallbackProxy;
import org.littleshoot.util.FiveTuple.Protocol;

/**
 * Specialized fallback proxy for flashlight. In particular, this lazily
 * initializes the WAN host because it dynamically determines it based on the
 * available candidates.
 */
public class FlashlightProxy extends FallbackProxy {

    private static volatile String PINNED_WAN_HOST;
    private static Object PINNED_WAN_HOST_MUTEX = new Object();
    
    private final FlashlightMasquerade masquerade;

    public FlashlightProxy(final String host, final int priority, 
            final FlashlightMasquerade masquerade,
            String configAddr,
            String cloudConfig,
            String cloudConfigCA) {
        this.masquerade = masquerade;
        final Properties props = new Properties();
        
        props.setProperty("type", "flashlight");
        props.setProperty(Flashlight.SERVER_KEY, host);
        props.setProperty(Flashlight.CONFIG_ADDR_KEY, configAddr);
        props.setProperty(Flashlight.CLOUDCONFIG_KEY, cloudConfig);
        props.setProperty(Flashlight.CLOUDCONFIG_CA_KEY, cloudConfigCA);
        
        setPt(props);
        setJid(LanternUtils.newURI("flashlight@"+ host));
        setPort(443);
        setProtocol(Protocol.TCP);
        setPriority(priority);
    }
    
    @Override
    public String getWanHost() {
        // We lazily initialize the wan host because dynamically
        // determining the host to use requires network access. So anything
        // calling this needs to be initialized as the result of network
        // access.
        if (StringUtils.isBlank(wanHost)) {
            // We pin the WAN host globally to avoid spawning multiple
            // flashlight instances with different WAN hosts
            synchronized (PINNED_WAN_HOST_MUTEX) {
                if (PINNED_WAN_HOST == null) {
                    PINNED_WAN_HOST = masquerade.determineMasqueradeHost()
                            .getKey();
                }
                wanHost = PINNED_WAN_HOST;
            }
            getPt().setProperty(Flashlight.MASQUERADE_KEY, wanHost);
        }
        return wanHost;
    }
    
}