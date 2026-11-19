package ru.urfu.xbackend.messages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

record Message(long id, long authorId, String text, Instant createdAt, Instant updatedAt) {}

record MessageRequest(@NotBlank @Size(max = 10000) String text) {}
