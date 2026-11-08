package org.example.system;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

/** Uses an isolated user/hotel per test and the real Gateway, Eureka and backend services. */
@Tag("e2e")
class BookingEndToEndTest {
  ApiClient api;
  String admin, user, username;
  long userId, hotelId;
  List<Long> rooms;
  final LocalDate start = LocalDate.now().plusDays(100);

  @BeforeEach
  void createFixture() {
    api = new ApiClient(ApiClient.gatewayUrl());
    admin = api.login("admin", "Admin123!");
    username = "java-e2e-" + UUID.randomUUID().toString().substring(0, 12);
    var account =
        api.expect(
            201,
            "POST",
            "/api/auth/register",
            Map.of("username", username, "password", "Test12345!"),
            null,
            null);
    user = account.path("token").asText();
    userId = account.path("user").path("id").asLong();
    hotelId =
        api.expect(
                201,
                "POST",
                "/api/hotels",
                Map.of("name", username, "address", "Тестовый адрес"),
                admin,
                null)
            .path("id")
            .asLong();
    rooms =
        IntStream.range(0, 4)
            .mapToObj(
                i ->
                    api.expect(
                            201,
                            "POST",
                            "/api/rooms",
                            Map.of(
                                "hotelId", hotelId, "number", String.valueOf(i), "available", true),
                            admin,
                            null)
                        .path("id")
                        .asLong())
            .toList();
  }

  @AfterEach
  void closeFixture() {
    try {
      if (userId != 0) api.expect(204, "DELETE", "/api/users/" + userId, null, admin, null);
    } finally {
      if (api != null) api.close();
    }
  }

  Map<String, Object> booking(long roomId, int offset) {
    return Map.of(
        "roomId",
        roomId,
        "autoSelect",
        false,
        "startDate",
        start.plusDays(offset).toString(),
        "endDate",
        start.plusDays(offset + 3).toString());
  }

  JsonNode create(long roomId, int offset) {
    return api.expect(
        201, "POST", "/api/bookings", booking(roomId, offset), user, UUID.randomUUID());
  }

  String dates(int offset) {
    return "startDate="
        + start.plusDays(offset)
        + "&endDate="
        + start.plusDays(offset + 3)
        + "&hotelId="
        + hotelId;
  }

  <T> List<T> parallel(int count, IntFunction<T> action) throws Exception {
    var gate = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(count)) {
      var futures = new ArrayList<Future<T>>();
      for (int i = 0; i < count; i++) {
        int index = i;
        futures.add(
            executor.submit(
                () -> {
                  gate.await();
                  return action.apply(index);
                }));
      }
      gate.countDown();
      var result = new ArrayList<T>();
      for (var future : futures) result.add(future.get(40, TimeUnit.SECONDS));
      return result;
    }
  }

  @Test
  void redeliveryIsIdempotentAndSameRoomEditDoesNotInflateHistory() {
    var body = booking(rooms.getFirst(), 0);
    UUID key = UUID.randomUUID();
    var original = api.expect(201, "POST", "/api/bookings", body, user, key);
    assertThat(api.expect(201, "POST", "/api/bookings", body, user, key)).isEqualTo(original);
    assertThat(original.path("status").asText()).isEqualTo("CONFIRMED");
    api.expect(409, "POST", "/api/bookings", booking(rooms.get(1), 0), user, key);
    api.expect(
        200, "PUT", "/api/bookings/" + original.path("id").asLong(), body, user, UUID.randomUUID());
    var catalog = api.get("/api/rooms/all?hotelId=" + hotelId, admin);
    assertThat(
            catalog
                .valueStream()
                .filter(r -> r.path("id").asLong() == rooms.getFirst())
                .findFirst()
                .orElseThrow()
                .path("timesBooked")
                .asLong())
        .isEqualTo(1);
    assertThat(api.get("/api/bookings", user).path("totalElements").asLong()).isEqualTo(1);
  }

  @Test
  void parallelOverlapsAllowExactlyOneBooking() throws Exception {
    var body = booking(rooms.getFirst(), 0);
    var responses =
        parallel(12, i -> api.exchange("POST", "/api/bookings", body, user, UUID.randomUUID()));
    assertThat(responses).filteredOn(r -> r.status() == 201).hasSize(1);
    assertThat(responses).filteredOn(r -> r.status() == 409).hasSize(11);
  }

  @Test
  void concurrentAutoSelectionUsesEveryRoomAndRecommendationsAreOrdered() throws Exception {
    var body =
        Map.of(
            "hotelId",
            hotelId,
            "autoSelect",
            true,
            "startDate",
            start.toString(),
            "endDate",
            start.plusDays(3).toString());
    var chosen =
        parallel(
            4,
            i ->
                api.expect(201, "POST", "/api/bookings", body, user, UUID.randomUUID())
                    .path("roomId")
                    .asLong());
    assertThat(chosen).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(rooms);
    var recommendations = api.get("/api/bookings/recommendations?" + dates(10), user);
    var actual = recommendations.valueStream().toList();
    var ordered =
        actual.stream()
            .sorted(
                Comparator.comparingLong((JsonNode r) -> r.path("timesBooked").asLong())
                    .thenComparingLong(r -> r.path("id").asLong()))
            .toList();
    assertThat(actual).containsExactlyElementsOf(ordered);
  }

  @Test
  void failedMovePreservesOriginalThenMoveAndCancellationSucceed() {
    var first = create(rooms.getFirst(), 0);
    long id = first.path("id").asLong();
    create(rooms.get(1), 0);
    api.expect(
        409, "PUT", "/api/bookings/" + id, booking(rooms.get(1), 0), user, UUID.randomUUID());
    assertThat(api.get("/api/bookings/" + id, user).path("roomId").asLong())
        .isEqualTo(rooms.getFirst());
    var moved =
        api.expect(
            200, "PUT", "/api/bookings/" + id, booking(rooms.get(2), 0), user, UUID.randomUUID());
    assertThat(moved.path("roomId").asLong()).isEqualTo(rooms.get(2));
    UUID cancel = UUID.randomUUID();
    var result = api.expect(200, "DELETE", "/api/bookings/" + id, null, user, cancel);
    assertThat(result.path("status").asText()).isEqualTo("CANCELLED");
    assertThat(api.expect(200, "DELETE", "/api/bookings/" + id, null, user, cancel))
        .isEqualTo(result);
    var available =
        api.get("/api/rooms?" + dates(0), user)
            .valueStream()
            .map(r -> r.path("id").asLong())
            .toList();
    assertThat(available).contains(rooms.getFirst(), rooms.get(2)).doesNotContain(rooms.get(1));
  }

  @Test
  void ownershipSecurityDateValidationAndInternalRoutesAreEnforced() {
    long id = create(rooms.getFirst(), 0).path("id").asLong();
    api.expect(401, "GET", "/api/bookings", null, null, null);
    api.expect(403, "GET", "/api/bookings", null, admin, null);
    api.expect(403, "GET", "/api/users", null, user, null);
    api.expect(404, "GET", "/api/bookings/" + id, null, api.login("bob", "User12345!"), null);
    var invalid = new HashMap<>(booking(rooms.getFirst(), 0));
    invalid.put("startDate", invalid.get("endDate"));
    api.expect(400, "POST", "/api/bookings", invalid, user, UUID.randomUUID());
    String path = "/api/rooms/" + rooms.getFirst() + "/release";
    var release = Map.of("requestId", UUID.randomUUID().toString(), "compensation", false);
    api.expect(403, "POST", path, release, user, null);
    try (var hotel =
        new ApiClient(System.getenv().getOrDefault("HOTEL_TEST_URL", "http://localhost:8082"))) {
      hotel.expect(403, "POST", path, release, user, null);
    }
  }

  @Test
  void paginationStatisticsAndCrudRespectHistoricalData() {
    for (int i = 0; i < 3; i++) create(rooms.get(i), 0);
    var history = api.get("/api/bookings?size=2", user);
    assertThat(history.path("content").size()).isEqualTo(2);
    assertThat(history.path("totalElements").asLong()).isEqualTo(3);
    assertThat(history.path("totalPages").asInt()).isEqualTo(2);
    var stats = api.get("/api/hotels/statistics?" + dates(0), admin);
    assertThat(stats.size()).isEqualTo(4);
    assertThat(stats.valueStream().mapToLong(r -> r.path("reservedNights").asLong()).sum())
        .isEqualTo(9);
    api.expect(409, "DELETE", "/api/rooms/" + rooms.getFirst(), null, admin, null);
    api.expect(409, "DELETE", "/api/hotels/" + hotelId, null, admin, null);
    long unused = rooms.getLast();
    api.expect(
        200,
        "PUT",
        "/api/rooms/" + unused,
        Map.of("hotelId", hotelId, "number", "renamed", "available", false),
        admin,
        null);
    assertThat(api.get("/api/rooms/" + unused, admin).path("available").asBoolean()).isFalse();
    api.expect(204, "DELETE", "/api/rooms/" + unused, null, admin, null);
    api.expect(404, "GET", "/api/rooms/" + unused, null, admin, null);
    api.expect(
        200,
        "PUT",
        "/api/hotels/" + hotelId,
        Map.of("name", "Updated " + username, "address", "Новый адрес"),
        admin,
        null);
    assertThat(api.get("/api/hotels/" + hotelId, user).path("name").asText()).startsWith("Updated");
    api.expect(200, "PATCH", "/api/users/" + userId, Map.of("enabled", false), admin, null);
    api.expect(
        401,
        "POST",
        "/api/auth/login",
        Map.of("username", username, "password", "Test12345!"),
        null,
        null);
  }
}
