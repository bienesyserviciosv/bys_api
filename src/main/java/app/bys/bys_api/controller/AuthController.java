package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.AuthRequest;
import app.bys.bys_api.model.dto.AuthResponse;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.service.AuthService;
import app.bys.bys_api.validation.OnCreate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  @PostMapping("/final_user/register")
  public ResponseEntity<Map<String, Object>> registerFinalUser(@Validated({OnCreate.class}) @RequestBody FinalUserDto finalUserDto) {
      FinalUserDto userRegistered = authService.registerFinalUser(finalUserDto);
      Map<String, Object> response = new HashMap<>();
      response.put("message", "Final User register successfully");
      response.put("user", userRegistered);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
  @PostMapping("/service_provider/register")
  public ResponseEntity<Map<String, Object>> registerServiceProvider(@Validated({OnCreate.class}) @RequestBody ServiceProviderDto serviceProviderDto) {
      ServiceProviderDto providerRegistered = authService.registerServiceProvider(serviceProviderDto);
      Map<String, Object> response = new HashMap<>();
      response.put("message", "Service Provider register successfully");
      response.put("user", providerRegistered);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
      return authService.login(authRequest);
        }
    }


