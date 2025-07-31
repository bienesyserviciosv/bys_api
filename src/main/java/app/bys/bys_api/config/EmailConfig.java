package app.bys.bys_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.linkbuilder.ILinkBuilder;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.username}")
    String username;

    @Value("${spring.mail.password}")
    String password;

    @Value("${spring.mail.port}")
    int port;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    String starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable}")
    String sslEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    String sslTrust;

    @Bean
    public SpringTemplateEngine thymeleafTemplateEngine(ITemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        templateEngine.setLinkBuilder(linkBuilder());
        return templateEngine;
    }

    @Bean
    public ITemplateResolver thymeleafTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    @Bean
    public ResourceBundleMessageSource emailMessageSource() {
        return new ResourceBundleMessageSource();
    }

    //TODO Refactor this module
    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(port);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.ssl.enable", sslEnable);
        props.put("mail.smtp.ssl.trust", sslTrust);
        props.put("mail.debug", "true");

        return mailSender;
    }

    private ILinkBuilder linkBuilder() {
        return new CustomLinkBuilder();
    }
}
