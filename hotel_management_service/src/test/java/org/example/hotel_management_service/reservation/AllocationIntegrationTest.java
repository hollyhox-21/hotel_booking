package org.example.hotel_management_service.reservation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.example.common.ApiException;
import org.example.common.JwtSupport;
import org.example.hotel_management_service.hotel.dto.request.HotelInput;
import org.example.hotel_management_service.hotel.service.HotelService;
import org.example.hotel_management_service.reservation.dto.request.ReserveInput;
import org.example.hotel_management_service.reservation.service.ReservationService;
import org.example.hotel_management_service.room.dto.request.RoomInput;
import org.example.hotel_management_service.room.service.RoomService;
import org.example.hotel_management_service.statistics.service.StatisticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:hotel_tests;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000"
    })
@AutoConfigureMockMvc
public class AllocationIntegrationTest {
  @Autowired HotelService hotels;
  @Autowired RoomService rooms;
  @Autowired ReservationService reservations;
  @Autowired StatisticsService statistics;
  @Autowired MockMvc mvc;
  @Autowired JwtEncoder encoder;
  final LocalDate start = LocalDate.now().plusDays(20), end = start.plusDays(3);
  Long hotelId;

  @BeforeEach
  public void setup() {
    hotelId = hotels.saveHotel(null, new HotelInput("test-" + UUID.randomUUID(), "address")).id();
  }

  public Long room() {
    return rooms
        .saveRoom(null, new RoomInput(hotelId, UUID.randomUUID().toString().substring(0, 8), true))
        .id();
  }

  public ReserveInput input(Long roomId, boolean auto, UUID key, Long bookingId) {
    return new ReserveInput(key, bookingId, roomId, hotelId, auto, start, end, null);
  }

  @Test
  public void concurrentOverlapsAllowOneReservationAndReturnConflicts() throws Exception {
    Long roomId = room();
    int count = 12;
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(count);
    try {
      var futures = new ArrayList<Future<Integer>>();
      for (int i = 0; i < count; i++) {
        long id = i + 1;
        futures.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    reservations.reserve(input(roomId, false, UUID.randomUUID(), id));
                    return 200;
                  } catch (ApiException e) {
                    return e.status();
                  }
                }));
      }
      gate.countDown();
      var codes = new ArrayList<Integer>();
      for (var f : futures) codes.add(f.get(20, TimeUnit.SECONDS));
      assertThat(codes).filteredOn(c -> c == 200).hasSize(1);
      assertThat(codes).filteredOn(c -> c == 409).hasSize(count - 1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void parallelAutoSelectionDistributesAcrossAllRooms() throws Exception {
    for (int i = 0; i < 6; i++) room();
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(6);
    try {
      var futures = new ArrayList<Future<Long>>();
      for (int i = 0; i < 6; i++) {
        long id = i + 100;
        futures.add(
            pool.submit(
                () -> {
                  gate.await();
                  return reservations.reserve(input(null, true, UUID.randomUUID(), id)).roomId();
                }));
      }
      gate.countDown();
      var ids = new HashSet<Long>();
      for (var f : futures) ids.add(f.get(20, TimeUnit.SECONDS));
      assertThat(ids).hasSize(6);
      assertThat(rooms.available(null, null, hotelId, false, true, "timesBooked"))
          .allMatch(r -> r.timesBooked() == 1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void retryIsIdempotentAndReleaseTombstonePreventsLateReserve() {
    Long roomId = room();
    UUID key = UUID.randomUUID();
    var input = input(roomId, false, key, 1L);
    assertThat(reservations.reserve(input)).isEqualTo(reservations.reserve(input));
    assertThat(rooms.getRoom(roomId).timesBooked()).isEqualTo(1);
    reservations.release(key, false, null);
    reservations.release(key, false, null);
    assertThat(rooms.getRoom(roomId).timesBooked()).isEqualTo(1);
    assertThatThrownBy(() -> reservations.reserve(input)).isInstanceOf(ApiException.class);
    UUID unknown = UUID.randomUUID();
    reservations.release(unknown, true, null);
    assertThatThrownBy(() -> reservations.reserve(input(roomId, false, unknown, 2L)))
        .isInstanceOf(ApiException.class);
  }

  @Test
  public void compensationRestoresCountAndAdjacentDatesDoNotOverlap() {
    Long id = room();
    UUID key = UUID.randomUUID();
    reservations.reserve(input(id, false, key, 1L));
    reservations.reserve(
        new ReserveInput(UUID.randomUUID(), 2L, id, hotelId, false, end, end.plusDays(2), null));
    reservations.release(key, true, null);
    reservations.release(key, true, null);
    assertThat(rooms.getRoom(id).timesBooked()).isEqualTo(1);
    var stats = statistics.stats(start, end.plusDays(2), hotelId);
    assertThat(stats.getFirst().reservedNights()).isEqualTo(2);
  }

  @Test
  public void methodSecurityValidatesRolesAndMalformedDates() throws Exception {
    mvc.perform(get("/api/hotels"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.traceId").isNotEmpty());
    String user = JwtSupport.token(encoder, "1", "USER");
    mvc.perform(
            post("/api/hotels")
                .header("Authorization", "Bearer " + user)
                .contentType("application/json")
                .content("{\"name\":\"x\",\"address\":\"y\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/rooms")
                .header("Authorization", "Bearer " + user)
                .param("startDate", end.toString())
                .param("endDate", start.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/rooms/1/release")
                .header("Authorization", "Bearer " + user)
                .contentType("application/json")
                .content("{\"requestId\":\"" + UUID.randomUUID() + "\",\"compensation\":false}"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void replacementKeepsOldSlotAndStatisticsCountUnionUntilRelease() {
    Long id = room();
    UUID old = UUID.randomUUID();
    reservations.reserve(input(id, false, old, 1L));
    UUID replacement = UUID.randomUUID();
    reservations.reserve(
        new ReserveInput(replacement, 1L, id, hotelId, false, start, end.plusDays(1), old));
    assertThat(statistics.stats(start, end.plusDays(1), hotelId).getFirst().occupancyPercent())
        .isEqualTo(100.0);
    assertThatThrownBy(() -> reservations.reserve(input(id, false, UUID.randomUUID(), 2L)))
        .isInstanceOf(ApiException.class);
    reservations.release(old, false, null);
    assertThat(rooms.getRoom(id).timesBooked()).isEqualTo(1);
  }

  @Test
  public void openApiSpecificationIsGenerated() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths").isNotEmpty());
  }
}
