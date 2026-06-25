package com.nexcart.userservice.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User details returned by the API")
public class UserResponse {

    @Schema(
        description = "Unique user identifier",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "User's first name",
        example = "John"
    )
    private String firstName;

    @Schema(
        description = "User's last name",
        example = "Doe"
    )
    private String lastName;

    @Schema(
        description = "User's email address",
        example = "john.doe@example.com"
    )
    private String email;

    @Schema(
        description = "User's phone number",
        example = "9876543210"
    )
    private String phoneNumber;
}
