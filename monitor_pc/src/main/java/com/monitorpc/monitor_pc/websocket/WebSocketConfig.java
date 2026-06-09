package com.monitorpc.monitor_pc.websocket;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//Config and enable STOMP for websocket
@EnableWebSocketMessageBroker
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker (@NonNull MessageBrokerRegistry messageBrokerRegistry){
        messageBrokerRegistry.enableSimpleBroker("/topic");
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app");
    }

    //STOMP endpoints for connecting
    @Override
    public void registerStompEndpoints (StompEndpointRegistry stompEndpointRegistry){
        stompEndpointRegistry.addEndpoint("/ws").setAllowedOrigins("http://localhost:4200");
    }
}
