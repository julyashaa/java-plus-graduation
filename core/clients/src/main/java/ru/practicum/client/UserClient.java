package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.user.UserDto;
import ru.practicum.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {
    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping("/short")
    List<UserShortDto> getAllShortUsers();
}