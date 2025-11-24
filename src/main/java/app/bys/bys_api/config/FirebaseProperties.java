package app.bys.bys_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@Setter
@Getter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private Resource serviceAccount;
}
