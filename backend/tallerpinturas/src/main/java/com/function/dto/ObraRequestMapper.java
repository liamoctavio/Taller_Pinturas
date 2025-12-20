package com.function.dto;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public final class ObraRequestMapper {

    private ObraRequestMapper() {
    }

    public static ObraDTO from(Map<String, Object> in) {
        ObraDTO dto = new ObraDTO();

        dto.setIdTipo(asLong(in.get("id_tipo_obra")));
        dto.setTitulo(asString(in.get("titulo")));
        dto.setDescripcion(asString(in.get("descripcion")));
        dto.setImagen(asString(in.get("imagenBase64")));
        dto.setIdAzure(asUUID(in.get("id_azure")));

        return dto;
    }

    /* ==== helpers seguros ==== */

    private static String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Long asLong(Object o) {
        return o instanceof Number number ? number.longValue() : null;
    }

    private static UUID asUUID(Object o) {
        try {
            if (o instanceof String str && !str.isBlank()) {
                return UUID.fromString(str);
            }
        } catch (IllegalArgumentException e) {
            // UUID inválido → se ignora
        }
        return null;
    }
}
