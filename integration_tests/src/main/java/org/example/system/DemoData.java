package org.example.system;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;

/** Repeatable demo setup using public API calls rather than writes across service databases. */
public final class DemoData {
  private DemoData() {}

  static UUID key(String name) {
    // RFC 9562 UUIDv5; preserve the keys used before the migration from the old scripts.
    UUID namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    try {
      var sha = MessageDigest.getInstance("SHA-1");
      sha.update(
          ByteBuffer.allocate(16)
              .putLong(namespace.getMostSignificantBits())
              .putLong(namespace.getLeastSignificantBits())
              .array());
      byte[] hash =
          sha.digest(
              ("hotel-demo-" + LocalDate.now() + "-" + name).getBytes(StandardCharsets.UTF_8));
      hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
      hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
      var buffer = ByteBuffer.wrap(hash);
      return new UUID(buffer.getLong(), buffer.getLong());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Map<String, Object> booking(long roomId) {
    var start = LocalDate.now().plusDays(1);
    return Map.of(
        "roomId",
        roomId,
        "autoSelect",
        false,
        "startDate",
        start.toString(),
        "endDate",
        start.plusDays(2).toString());
  }

  public static void main(String[] args) {
    try (var api = new ApiClient(ApiClient.gatewayUrl())) {
      String token = api.login("alice", "User12345!");
      api.expect(201, "POST", "/api/bookings", booking(1), token, key("confirmed"));
      var cancelled =
          api.expect(201, "POST", "/api/bookings", booking(2), token, key("cancel-create"));
      api.expect(
          200,
          "DELETE",
          "/api/bookings/" + cancelled.path("id").asLong(),
          null,
          token,
          key("cancel"));
      var moved = api.expect(201, "POST", "/api/bookings", booking(3), token, key("move-create"));
      api.expect(
          200, "PUT", "/api/bookings/" + moved.path("id").asLong(), booking(4), token, key("move"));
      api.expect(409, "POST", "/api/bookings", booking(8), token, key("unavailable"));
      System.out.println(
          "Demo ready: confirmed, cancelled, moved, compensated and unavailable-room scenarios");
    }
  }
}
