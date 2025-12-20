package com.function.events;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

public final class EventBusEG {


  private static EventGridPublisherClient<EventGridEvent> client() {
    String endpoint = System.getenv("EG_TOPIC_ENDPOINT");
    String key = System.getenv("EG_ACCESS_KEY");
    if (endpoint == null || key == null) {
      throw new IllegalStateException("Faltan EG_TOPIC_ENDPOINT / EG_ACCESS_KEY");
    }
    return new EventGridPublisherClientBuilder()
        .endpoint(endpoint)
        .credential(new AzureKeyCredential(key))
        .buildEventGridEventPublisherClient();
  }

  public static void publish(String type, String subject, Object data) {
    BinaryData bd = (data == null)
        ? BinaryData.fromObject(java.util.Collections.emptyMap())
        : BinaryData.fromObject(data);
    EventGridEvent ev = new EventGridEvent(subject, type, bd, "1.0");
    client().sendEvent(ev);
  }

  private EventBusEG() {}
}

