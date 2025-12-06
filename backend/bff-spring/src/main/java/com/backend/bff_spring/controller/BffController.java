package com.backend.bff_spring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.*; 
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff") // Esta será la puerta de entrada: http://localhost:8080/bff
@CrossOrigin(origins = "*") // Permitir llamadas desde cualquier origen (dev only)
public class BffController {

    private final WebClient webClient;
    private final String backendUrl;

    // Inyectamos el WebClient y la URL que configuramos en application.yml
    public BffController(WebClient webClient, 
                         @Value("${functions.tallerPinturasBaseUrl}") String backendUrl) {
        this.webClient = webClient;
        this.backendUrl = backendUrl;
    }

    // Endpoint para obtener usuarios
    // Ruta final: http://localhost:8080/bff/usuarios
    @GetMapping("/usuarios")
    public Flux<Object> getUsuarios() {
        System.out.println(" BFF recibiendo llamada... contactando a: " + backendUrl + "/usuarios");
        
        return webClient.get()
                .uri(backendUrl + "/usuarios") // Llama al puerto 7071
                .retrieve()
                .bodyToFlux(Object.class); // Devuelve lo que sea que responda el backend
    }
    
    // Endpoint para obtener obras
    // Ruta final: http://localhost:8080/bff/obras
    @GetMapping("/obras")
    public Flux<Object> getObras() {
        return webClient.get()
                .uri(backendUrl + "/obras")
                .retrieve()
                .bodyToFlux(Object.class);
    }

    // 2. VER UNA OBRA (Con la foto en Base64)
    @GetMapping("/obras/{id}")
    public Mono<Object> getObraDetalle(@PathVariable Long id) {
        // Le pasamos el parámetro includeImage=true a la Azure Function
        return webClient.get()
                .uri(backendUrl + "/obras/" + id + "?includeImage=true")
                .retrieve()
                .bodyToMono(Object.class);
    }
    // 3. SUBIR OBRA (Recibe el JSON con la imagen en base64 y lo manda al backend)
    @PostMapping("/obras")
    public Mono<Object> crearObra(@RequestBody Object obraJson) {
        return webClient.post()
                .uri(backendUrl + "/obras")
                .bodyValue(obraJson)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // 4. EDITAR OBRA (PUT)
    // Recibe ID en la URL y los datos nuevos en el cuerpo
    @PutMapping("/obras/{id}")
    public Mono<Object> editarObra(@PathVariable Long id, @RequestBody Object obraJson) {
        return webClient.put()
                .uri(backendUrl + "/obras/" + id)
                .bodyValue(obraJson)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // 5. ELIMINAR OBRA (DELETE)
    @DeleteMapping("/obras/{id}")
    public Mono<Void> eliminarObra(@PathVariable Long id) {
        return webClient.delete()
                .uri(backendUrl + "/obras/" + id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Endpoint para listar Eventos
    @GetMapping("/eventos")
    public Flux<Object> getEventos() {
        return webClient.get()
                .uri(backendUrl + "/eventos") // Llama a tu función robusta
                .retrieve()
                .bodyToFlux(Object.class);
    }


}