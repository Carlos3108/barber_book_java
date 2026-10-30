package org.azdev.barber_book.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String from;
    private final boolean logCode;

    public EmailSender(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:no-reply@localhost}") String from,
            @Value("${app.email-verification.log-code:false}") boolean logCode
    ) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.from = from;
        this.logCode = logCode;
    }

    @Async
    public void sendVerificationCode(String to, String code, int validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || mailHost == null || mailHost.isBlank()) {
            if (logCode) {
                log.warn("E-mail não configurado; código de verificação (somente desenvolvimento): {}", code);
            } else {
                log.warn("E-mail não configurado; o código de verificação não foi enviado.");
            }
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Seu código de verificação - Barber Book");
            message.setText("Seu código de verificação é: " + code + "\n\n"
                    + "Ele vale por " + validMinutes + " minutos. "
                    + "Se você não criou uma conta no Barber Book, ignore este e-mail.");
            sender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar o e-mail de verificação: {}", e.getClass().getSimpleName());
        }
    }
}
