package uk.gov.pay.adminusers.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.pay.adminusers.app.util.TrustStoreLoader.getSSLContext;

public class NotificationService {

    private final ExecutorService executorService;
    private final NotifyClientProvider notifyClientProvider;
    private final MetricRegistry metricRegistry;

    private final String secondFactorSmsTemplateId;
    private final String inviteEmailTemplateId;
    private final String forgottenPasswordEmailTemplateId;

    public NotificationService(ExecutorService executorService,
                               NotifyConfiguration notifyConfiguration,
                               MetricRegistry metricRegistry) {
        this.executorService = executorService;

        this.notifyClientProvider = new NotifyClientProvider(notifyConfiguration, getSSLContext());
        this.secondFactorSmsTemplateId = notifyConfiguration.getSecondFactorSmsTemplateId();
        this.inviteEmailTemplateId = notifyConfiguration.getInviteEmailTemplateId();
        this.forgottenPasswordEmailTemplateId = notifyConfiguration.getForgottenPasswordEmailTemplateId();

        this.metricRegistry = metricRegistry;
    }

    CompletableFuture<String> sendSecondFactorPasscodeSms(String phoneNumber, String passcode) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, String> personalisation = newHashMap();
            personalisation.put("code", passcode);
            Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
            try {
                SendSmsResponse response = notifyClientProvider.get().sendSms(secondFactorSmsTemplateId, phoneNumber, personalisation, null);
                return response.getNotificationId().toString();
            } catch (Exception e) {
                metricRegistry.counter("notify-operations.sms.failures").inc();
                throw AdminUsersExceptions.userNotificationError(e);
            } finally {
                responseTimeStopwatch.stop();
                metricRegistry.histogram("notify-operations.sms.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }, executorService);
    }

    CompletableFuture<String> sendInviteEmail(String sender, String email, String inviteUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("username", sender);
        personalisation.put("link", inviteUrl);
        return sendEmailAsync(inviteEmailTemplateId, email, personalisation);
    }

    CompletableFuture<String> sendForgottenPasswordEmail(String email, String forgottenPasswordUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("code", forgottenPasswordUrl);
        return sendEmailAsync(forgottenPasswordEmailTemplateId, email, personalisation);
    }

    private CompletableFuture<String> sendEmailAsync(final String templateId, final String email, final Map<String, String> personalisation) {
        return CompletableFuture.supplyAsync(() -> {
            Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
            try {
                SendEmailResponse response = notifyClientProvider.get().sendEmail(templateId, email, personalisation, null);
                return response.getNotificationId().toString();
            } catch (Exception e) {
                metricRegistry.counter("notify-operations.email.failures").inc();
                throw AdminUsersExceptions.userNotificationError(e);
            } finally {
                responseTimeStopwatch.stop();
                metricRegistry.histogram("notify-operations.email.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }, executorService);
    }
}