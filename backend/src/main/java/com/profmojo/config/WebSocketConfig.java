package com.profmojo.config;

import com.profmojo.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.*;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint where the frontend connects
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for server-to-client messages
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");
        // For private notifications to specific users
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT token from headers
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");

                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        String authHeader = authHeaders.get(0);

                        if (authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);

                            try {
                                // Validate JWT
                                if (jwtUtil.validateToken(token)) {
                                    String username = jwtUtil.extractUsername(token);
                                    String role = jwtUtil.extractRole(token);

                                    // Create authentication
                                    UsernamePasswordAuthenticationToken auth =
                                            new UsernamePasswordAuthenticationToken(
                                                    username,
                                                    null,
                                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                            );

                                    accessor.setUser(auth);
                                    SecurityContextHolder.getContext().setAuthentication(auth);
                                }
                            } catch (Exception e) {
                                // Invalid token, connection will be rejected
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}