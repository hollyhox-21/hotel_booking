package org.example.booking_service.booking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.booking_service.auth.dto.request.Credentials;
import org.example.booking_service.auth.service.AuthService;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.saga.BookingTransactions;
import org.example.booking_service.booking.saga.CompensationWorker;
import org.example.booking_service.booking.saga.repository.CleanupRepository;
import org.example.booking_service.booking.saga.repository.OperationRepository;
import org.example.booking_service.booking.service.BookingService;
import org.example.booking_service.integration.hotel.HotelPort;
import org.example.booking_service.integration.hotel.dto.request.ReserveInput;
import org.example.booking_service.integration.hotel.dto.response.ReservationView;
import org.example.booking_service.user.dto.request.UserInput;
import org.example.booking_service.user.service.UserService;
import org.example.common.ApiException;
import org.example.common.JwtSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:booking_tests;DB_CLOSE_DELAY=-1",
      "app.recovery-delay-ms=3600000"
    })
@AutoConfigureMockMvc
public class BookingIntegrationTest {
  @Autowired BookingService service;
  @Autowired BookingTransactions tx;
  @Autowired UserService accounts;
  @Autowired AuthService auth;
  @Autowired CompensationWorker worker;
  @Autowired MockMvc mvc;
  @Autowired JwtEncoder encoder;
  @MockitoBean HotelPort hotel;
  @Autowired CleanupRepository cleanup;
  @Autowired OperationRepository operations;
  @Autowired org.springframework.transaction.PlatformTransactionManager manager;
  Long userId;
  final LocalDate start = LocalDate.now().plusDays(10), end = start.plusDays(3);

  public BookingInput input() {
    return new BookingInput(1L, null, false, start, end);
  }

  @BeforeEach
  public void setup() {
    userId =
        accounts
            .create(
                new UserInput(
                    "test-" + UUID.randomUUID().toString().substring(0, 10),
                    "Password123!",
                    "USER"))
            .id();
    when(hotel.reserve(any()))
        .thenAnswer(
            call -> {
              ReserveInput i = call.getArgument(0);
              return new ReservationView(
                  i.requestId(),
                  i.roomId() == null ? 1L : i.roomId(),
                  i.bookingId(),
                  i.startDate(),
                  i.endDate(),
                  true);
            });
  }

  @Test
  public void successfulBookingAndRedeliveryOnlyCallHotelOnce() {
    UUID key = UUID.randomUUID();
    var first = service.save(userId, null, key, input());
    var replay = service.save(userId, null, key, input());
    assertThat(first).isEqualTo(replay);
    assertThat(first.status()).isEqualTo("CONFIRMED");
    verify(hotel, times(1)).reserve(any());
    assertThatThrownBy(
            () -> service.save(userId, null, key, new BookingInput(2L, null, false, start, end)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("другим запросом");
  }

  @Test
  public void remoteFailureCancelsAndCompensatesEvenWhenResponseWasLost() {
    UUID key = UUID.randomUUID();
    doThrow(new ApiException(503, "timeout")).when(hotel).reserve(any());
    assertThatThrownBy(() -> service.save(userId, null, key, input()))
        .isInstanceOf(ApiException.class);
    var list = tx.list(userId, 0, 10);
    assertThat(list.content()).hasSize(1);
    assertThat(list.content().getFirst().status()).isEqualTo("CANCELLED");
    verify(hotel).release(key, true);
  }

  @Test
  public void failedCompensationIsRetriedFromPersistentTask() {
    UUID key = UUID.randomUUID();
    doThrow(new ApiException(503, "timeout")).when(hotel).reserve(any());
    doThrow(new ApiException(503, "offline")).doNothing().when(hotel).release(key, true);
    assertThatThrownBy(() -> service.save(userId, null, key, input()))
        .isInstanceOf(ApiException.class);
    assertThat(tx.task(key).done()).isFalse();
    // Next scheduled attempt uses the persisted task, independent of the original request.
    assertThat(tx.task(key).compensation()).isTrue();
    new org.springframework.transaction.support.TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              var task = cleanup.findById(key).orElseThrow();
              task.setNextAttemptAt(Instant.now().minusSeconds(1));
            });
    worker.runCleanup();
    verify(hotel, times(2)).release(key, true);
    assertThat(tx.task(key).done()).isTrue();
  }

  @Test
  public void failedMovePreservesOriginalAndSuccessfulMoveReleasesOldSlot() {
    UUID original = UUID.randomUUID();
    var first = service.save(userId, null, original, input());
    doThrow(new ApiException(409, "occupied")).when(hotel).reserve(any());
    UUID failed = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                service.save(
                    userId, first.id(), failed, new BookingInput(2L, null, false, start, end)))
        .isInstanceOf(ApiException.class);
    assertThat(tx.get(userId, first.id()).roomId()).isEqualTo(1L);
    assertThat(tx.get(userId, first.id()).status()).isEqualTo("CONFIRMED");
    verify(hotel, never()).release(original, false);
    doAnswer(
            c -> {
              ReserveInput i = c.getArgument(0);
              return new ReservationView(
                  i.requestId(), 2L, i.bookingId(), i.startDate(), i.endDate(), true);
            })
        .when(hotel)
        .reserve(any());
    var moved =
        service.save(
            userId, first.id(), UUID.randomUUID(), new BookingInput(2L, null, false, start, end));
    assertThat(moved.roomId()).isEqualTo(2L);
    verify(hotel).release(original, false);
  }

  @Test
  public void concurrentSameRequestDoesNotCreateDuplicate() throws Exception {
    UUID key = UUID.randomUUID();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    doAnswer(
            c -> {
              ReserveInput i = c.getArgument(0);
              entered.countDown();
              release.await(10, TimeUnit.SECONDS);
              return new ReservationView(i.requestId(), 1L, i.bookingId(), start, end, true);
            })
        .when(hotel)
        .reserve(any());
    var pool = Executors.newSingleThreadExecutor();
    try {
      var first = pool.submit(() -> service.save(userId, null, key, input()));
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> service.save(userId, null, key, input()))
          .isInstanceOf(ApiException.class)
          .hasMessageContaining("выполняется");
      release.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo("CONFIRMED");
      assertThat(tx.list(userId, 0, 10).totalElements()).isEqualTo(1);
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }

  @Test
  public void cancellationIsIdempotentAndHistoryIsOwned() {
    UUID original = UUID.randomUUID();
    var b = service.save(userId, null, original, input());
    UUID cancel = UUID.randomUUID();
    var result = service.cancel(userId, b.id(), cancel);
    assertThat(service.cancel(userId, b.id(), cancel)).isEqualTo(result);
    assertThat(result.status()).isEqualTo("CANCELLED");
    verify(hotel, times(1)).release(original, false);
    assertThatThrownBy(() -> tx.get(2L, b.id()))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("не найдено");
  }

  @Test
  public void securityValidationAndPagination() throws Exception {
    mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
    String admin = JwtSupport.token(encoder, "1", "ADMIN");
    mvc.perform(get("/api/bookings").header("Authorization", "Bearer " + admin))
        .andExpect(status().isForbidden());
    String token = JwtSupport.token(encoder, userId.toString(), "USER");
    mvc.perform(
            get("/api/bookings").header("Authorization", "Bearer " + token).param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content(
                    "{\"roomId\":1,\"startDate\":\"" + end + "\",\"endDate\":\"" + start + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void registrationCannotEscalateAndDisabledAccountCannotLogin() throws Exception {
    String name = "new-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    "{\"username\":\""
                        + name
                        + "\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.role").value("USER"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
    accounts.delete(userId, 1L);
    assertThatThrownBy(
            () -> auth.login(new Credentials(accounts.get(userId).username(), "Password123!")))
        .isInstanceOf(ApiException.class);
  }

  @Test
  public void stalePendingIsRecoveredAndLateConfirmationCannotResurrectIt() {
    UUID key = UUID.randomUUID();
    var work = tx.begin(userId, null, key, input(), "CREATE");
    new org.springframework.transaction.support.TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              operations.findById(key).orElseThrow().setCreatedAt(Instant.now().minusSeconds(120));
            });
    worker.recover();
    assertThat(tx.get(userId, work.bookingId()).status()).isEqualTo("CANCELLED");
    assertThatThrownBy(() -> tx.confirm(userId, key, input(), 1L)).isInstanceOf(ApiException.class);
    verify(hotel).release(key, true);
  }

  @Test
  public void openApiSpecificationIsGenerated() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths").isNotEmpty());
  }
}
