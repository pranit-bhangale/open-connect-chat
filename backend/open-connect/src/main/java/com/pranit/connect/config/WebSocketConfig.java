package com.pranit.connect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket & STOMP Message Broker Configuration.
 * Configured with strict timeouts to prevent unresponsive/slow clients from blocking server resources.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers the STOMP handshake endpoints that clients connect to.
     *
     * @param registry the endpoint registry used to register STOMP over WebSocket endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String allowedOrigin = "http://localhost:5173";
        // SockJS fallback endpoint (browser clients behind proxies)
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns(allowedOrigin)
                .withSockJS();
        // Direct native WebSocket endpoints (mobile, CLI, Postman, high-performance clients)
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns(allowedOrigin);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigin);
    }

    /**
     * Configures transport-level parameters and guards against resource exhaustion.
     * Unresponsive or slow consumers are disconnected after 5s rather than blocking threads or buffers.
     *
     * @param registration the transport registration object to configure limits
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024);       // 64 KB max payload
        registration.setSendBufferSizeLimit(256 * 1024);   // 256 KB outbound buffer per session
        registration.setSendTimeLimit(5 * 1000);           // 5s send timeout: slow/stalled consumers are terminated, not blocked
        registration.setTimeToFirstMessage(15 * 1000);     // 15s to send initial CONNECT frame, otherwise disconnected
    }

    /**
     * Handles messages arriving from WebSocket clients using lightweight Java Virtual Threads.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadFactory(Thread.ofVirtual().name("ws-inbound-vt-", 0).factory());
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(1000);
        executor.setQueueCapacity(10000);
        executor.initialize();
        registration.taskExecutor(executor);
    }

    /**
     * Handles messages dispatched to WebSocket clients using Java Virtual Threads.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadFactory(Thread.ofVirtual().name("ws-outbound-vt-", 0).factory());
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(1000);
        executor.setQueueCapacity(10000);
        executor.initialize();
        registration.taskExecutor(executor);
    }

    /**
     * Configures the message broker that routes messages between clients and server controllers.
     *
     * @param config the broker registry used to configure message routing options
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Dedicated thread pool for heartbeat timers
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();
        // Enable in-memory broker with /topic (pub-sub broadcast) and /queue (private 1-to-1)
        // 10s incoming, 10s outgoing heartbeat. If client fails to send heartbeat for 20s, it is dropped.
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler)
                .setHeartbeatValue(new long[]{10000, 10000});
        // Client-to-server messages handled by application controllers (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-targeted destinations (@SendToUser)
        config.setUserDestinationPrefix("/user");
    }
}
