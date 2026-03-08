package de.kittelberger.bosch.adapter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DomainController {

  @Value("${domain.media.default}")
  private String mediaDomain;

  @GetMapping("/domain")
  public String getDomain() {
    return mediaDomain;
  }
}
