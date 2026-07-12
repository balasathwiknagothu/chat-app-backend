package com.chatapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    private String name;

    @NotBlank(message = "Room type is required")
    @Pattern(regexp = "DIRECT|GROUP", message = "Type must be either DIRECT or GROUP")
    private String type;
}