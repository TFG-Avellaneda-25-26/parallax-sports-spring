package dev.parallaxsports.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.parallaxsports.TestcontainersConfiguration;
import dev.parallaxsports.core.exception.UnauthorizedException;
import dev.parallaxsports.follow.model.UserSportNotificationChannel;
import dev.parallaxsports.follow.model.UserSportNotificationChannelId;
import dev.parallaxsports.follow.model.UserSportSettings;
import dev.parallaxsports.follow.model.UserSportSettingsId;
import dev.parallaxsports.follow.repository.UserSportNotificationChannelRepository;
import dev.parallaxsports.follow.repository.UserSportSettingsRepository;
import dev.parallaxsports.notification.dto.AlertWorkerStatusCallbackRequest;
import dev.parallaxsports.notification.event.EventsIngestedEvent;
import dev.parallaxsports.notification.model.UserEventAlert;
import dev.parallaxsports.notification.repository.UserEventAlertRepository;
import dev.parallaxsports.notification.service.AlertCallbackService;
import dev.parallaxsports.notification.service.UserEventAlertDispatchScheduler;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.Sport;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.SportRepository;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserSettings;
import dev.parallaxsports.user.repository.UserRepository;
import dev.parallaxsports.user.repository.UserSettingsRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
public class AlertPipelineIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        REDIS.start();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired StringRedisTemplate redisTemplate;

    @Autowired UserRepository userRepository;
    @Autowired UserSettingsRepository userSettingsRepository;
    @Autowired SportRepository sportRepository;
    @Autowired EventRepository eventRepository;
    @Autowired UserSportSettingsRepository userSportSettingsRepository;
    @Autowired UserSportNotificationChannelRepository userSportNotificationChannelRepository;
    @Autowired UserEventAlertRepository userEventAlertRepository;
    @Autowired UserEventAlertDispatchScheduler dispatchScheduler;
    @Autowired AlertCallbackService alertCallbackService;

    @Test
    public void eventIngested_generatesAlert_publishesToStream_andCallbackMarksSent() {
        Fixture f = transactionTemplate.execute(s -> seedFixture());
        assertThat(f).isNotNull();

        transactionTemplate.executeWithoutResult(s ->
            eventPublisher.publishEvent(new EventsIngestedEvent(List.of(f.eventId)))
        );

        List<UserEventAlert> alerts = userEventAlertRepository.findAll();
        assertThat(alerts).hasSize(1);
        UserEventAlert alert = alerts.getFirst();
        assertThat(alert.getChannel()).isEqualTo("discord");
        assertThat(alert.getStatus()).isEqualTo("scheduled");
        assertThat(alert.isArtifactRequired()).isTrue();

        transactionTemplate.executeWithoutResult(s -> {
            UserEventAlert due = userEventAlertRepository.findById(alert.getId()).orElseThrow();
            due.setSendAtUtc(OffsetDateTime.now().minusMinutes(1));
            userEventAlertRepository.save(due);
        });

        String streamName = "alerts.discord.v1";
        redisTemplate.delete(streamName);

        dispatchScheduler.dispatchDueAlerts();

        UserEventAlert queued = userEventAlertRepository.findById(alert.getId()).orElseThrow();
        assertThat(queued.getStatus()).isEqualTo("queued");
        assertThat(queued.getStreamMessageId()).isNotBlank();
        assertThat(queued.getQueuedAtUtc()).isNotNull();

        Long streamLen = redisTemplate.opsForStream().size(streamName);
        assertThat(streamLen).isEqualTo(1L);

        List<MapRecord<String, Object, Object>> records =
            redisTemplate.opsForStream().read(StreamOffset.fromStart(streamName));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getValue())
            .containsEntry("channel", "discord")
            .containsEntry("userEmail", "user@test.dev")
            .containsEntry("userTimezone", "Europe/Madrid")
            .containsKey("renderHash");

        AlertWorkerStatusCallbackRequest callback = new AlertWorkerStatusCallbackRequest(
            "sent", "ktor-test-worker", queued.getStreamMessageId(),
            "provider-msg-123", null, null, null, 42
        );
        alertCallbackService.processStatusCallback(alert.getId(), "test-api-key-1234", callback);

        UserEventAlert sent = userEventAlertRepository.findById(alert.getId()).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo("sent");
        assertThat(sent.getSentAtUtc()).isNotNull();
        assertThat(sent.getProviderMessageId()).isEqualTo("provider-msg-123");
    }

    @Test
    public void callbackRejectsUnknownApiKey() {
        AlertWorkerStatusCallbackRequest callback = new AlertWorkerStatusCallbackRequest(
            "sent", "w", "sm", "pm", null, null, null, null
        );
        assertThatThrownBy(() ->
            alertCallbackService.processStatusCallback(9999L, "wrong-key", callback)
        ).isInstanceOf(UnauthorizedException.class);
    }

    record Fixture(Long userId, Long sportId, Long eventId) {}

    private Fixture seedFixture() {
        User user = userRepository.save(User.builder()
            .email("user@test.dev")
            .displayName("Test User")
            .emailVerified(true)
            .build());

        userSettingsRepository.save(UserSettings.builder()
            .user(user)
            .theme("dark")
            .defaultView("cards")
            .timezone("Europe/Madrid")
            .locale("es-ES")
            .build());

        Sport sport = sportRepository.save(Sport.builder().key("nba").name("NBA").build());

        Event event = eventRepository.save(Event.builder()
            .sport(sport)
            .eventType("game")
            .name("Test Game")
            .status("scheduled")
            .startTimeUtc(OffsetDateTime.now().plusMinutes(30))
            .participantsMode("teams")
            .build());

        userSportSettingsRepository.save(UserSportSettings.builder()
            .id(new UserSportSettingsId(user.getId(), sport.getId()))
            .user(user)
            .followAll(true)
            .notifyDefault(true)
            .build());

        userSportNotificationChannelRepository.save(UserSportNotificationChannel.builder()
            .id(new UserSportNotificationChannelId(user.getId(), sport.getId(), "discord"))
            .user(user)
            .enabled(true)
            .defaultLeadTimeMinutes(10)
            .build());

        return new Fixture(user.getId(), sport.getId(), event.getId());
    }
}
