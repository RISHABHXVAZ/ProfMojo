package com.profmojo.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "profmojo.ratelimit")
@Getter
@Setter
public class RateLimitProperties {

    private SendOtp sendOtp = new SendOtp();

    @Getter
    @Setter
    public static class SendOtp {
        private Dimension ip = new Dimension(3, 60);
        private Dimension user = new Dimension(2, 300);
    }

    @Getter
    @Setter
    public static class Dimension {
        private int limit;
        private int windowSeconds;

        public Dimension() {
        }

        public Dimension(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }
    }
}
