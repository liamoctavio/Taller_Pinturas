package com.function.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class EventoRequestMapper {
    private EventoRequestMapper() {}

  public static EventoDTO from(Map<String, Object> in) {
    EventoDTO dto = new EventoDTO();

    dto.setIdTipoEvento(asLong(in.get("id_tipo_evento")));
    dto.setIdRol(asLong(in.get("id_rol")));
    dto.setTitulo(asString(in.get("titulo")));
    dto.setDescripcion(asString(in.get("descripcion")));
    dto.setDireccion(asString(in.get("direccion")));
    dto.setPrecio(asBigDecimal(in.get("precio")));
    dto.setFechaInicio(asInstant(in.get("fechaInicio")));
    dto.setFechaTermino(asInstant(in.get("fechaTermino")));
    dto.setIdAzure(asUUID(in.get("id_azure")));

    return dto;
  }

  /* ==== helpers ==== */

  private static String asString(Object o) {
    return o != null ? o.toString() : null;
  }

  private static Long asLong(Object o) {
    return (o instanceof Number number) ? number.longValue() : null;
  }

  private static BigDecimal asBigDecimal(Object o) {
    return o instanceof Number ? new BigDecimal(o.toString()) : null;
  }

  private static Instant asInstant(Object o) {
    try {
      return o != null ? Instant.parse(o.toString()) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static UUID asUUID(Object o) {
    try {
      return o != null ? UUID.fromString(o.toString()) : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
