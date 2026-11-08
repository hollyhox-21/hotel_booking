import { test } from "node:test";
import assert from "node:assert/strict";
import { request, mutationKey, ApiError } from "./api.js";
test("booking retry keeps key after lost response; changed input uses a new key", () => {
  const values = new Map();
  const storage = {
    getItem: (k) => values.get(k),
    setItem: (k, v) => values.set(k, v),
  };
  const a = mutationKey(storage, "dates-a");
  assert.equal(mutationKey(storage, "dates-a"), a);
  assert.notEqual(mutationKey(storage, "dates-b"), a);
  assert.equal(mutationKey(storage, "dates-a"), a);
});
test("request forwards JWT and idempotency key, preserves structured errors", async () => {
  const original = globalThis.fetch;
  try {
    let sent;
    globalThis.fetch = async (url, options) => {
      sent = { url, options };
      return new Response(
        JSON.stringify({ message: "Номер занят", traceId: "trace-1" }),
        { status: 409 },
      );
    };
    await assert.rejects(
      request("/bookings", {
        token: "jwt",
        method: "POST",
        key: "abc",
        body: { roomId: 1 },
      }),
      (e) =>
        e instanceof ApiError && e.status === 409 && e.traceId === "trace-1",
    );
    assert.equal(sent.url, "/api/bookings");
    assert.equal(sent.options.headers.Authorization, "Bearer jwt");
    assert.equal(sent.options.headers["Idempotency-Key"], "abc");
  } finally {
    globalThis.fetch = original;
  }
});
test("network error is distinguishable so caller can retain mutation key", async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async () => {
      throw new TypeError("fetch failed");
    };
    await assert.rejects(request("/bookings"), (e) => e.status === 0);
  } finally {
    globalThis.fetch = original;
  }
});
