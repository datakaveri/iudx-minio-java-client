package com.iudx.app;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSession;

public class CustomEventListenerProviderFactory implements EventListenerProviderFactory {
  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new CustomEventListenerProvider(session);
  }

  @Override
  public void init(org.keycloak.Config.Scope config) {
    // Initialize any configuration settings if needed
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // Post-initialization steps, if needed
  }

  @Override
  public void close() {
    // Cleanup resources if needed
  }

  @Override
  public String getId() {
    return "custom-event-listener";
  }
}
