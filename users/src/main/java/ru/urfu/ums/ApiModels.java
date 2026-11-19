package ru.urfu.ums;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record Role(int id, String name) {}

record UserSummary(long id, String firstName, String lastName, List<String> roles) {}

record Profile(long id, String firstName, String lastName, String email, String phoneNumber,
               List<String> roles, Instant createdAt, Instant updatedAt, boolean emailConfirmed) {}

record LoginResponse(String token, Profile user) {}

record RegistrationResponse(@JsonUnwrapped Profile profile, UUID confirmationId) {}

record EmailConfirmation(UUID confirmationId) {}

record ConfirmEmailRequest(@NotNull UUID confirmationId,
                           @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}

record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
                             @NotBlank @Size(min = 8, max = 72) String newPassword) {}

record RegisterRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "\\+?[0-9]{7,19}") String phoneNumber,
        @NotBlank @Size(min = 8, max = 72) String password) {}

record LoginRequest(@NotBlank @Email @Size(max = 255) String email,
                    @NotBlank @Size(max = 72) String password) {}

final class ProfilePatch {
    @Pattern(regexp = "(?s).*\\S.*")
    @Size(max = 255)
    private String firstName;

    @Pattern(regexp = "(?s).*\\S.*")
    @Size(max = 255)
    private String lastName;

    @Email
    @Size(min = 1, max = 255)
    private String email;

    @Pattern(regexp = "\\+?[0-9]{7,19}")
    private String phoneNumber;

    private boolean phonePresent;

    @JsonSetter(nulls = Nulls.FAIL)
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonSetter
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        phonePresent = true;
    }

    String firstName() { return firstName; }
    String lastName() { return lastName; }
    String email() { return email; }
    String phoneNumber() { return phoneNumber; }
    boolean phonePresent() { return phonePresent; }

    boolean isEmpty() {
        return firstName == null && lastName == null && email == null && !phonePresent;
    }
}
