package com.grupoproyeccion.play.modules.flypass_account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class N8nIntegrationService {

    private final RestTemplate restTemplate;

    // Inyectamos la URL del webhook desde application.properties
    @Value("${n8n.flypass.webhook.url}")
    private String n8nWebhookUrl;

    public N8nIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envía el archivo PDF a n8n y devuelve la respuesta JSON como un String.
     * @param file El archivo PDF recibido del usuario.
     * @return Un String que contiene el JSON con las transacciones extraídas.
     * @throws IOException Si hay un error al leer el archivo.
     */
    public String processPdfViaN8n(MultipartFile file) throws IOException {
        // Preparamos el archivo para ser enviado como datos binarios
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        // Creamos un cuerpo de formulario multipart, que es el estándar para enviar archivos
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        // Configuramos las cabeceras para indicar que estamos enviando un formulario
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Creamos la entidad de la petición con el cuerpo y las cabeceras
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Hacemos la llamada POST al webhook de n8n y esperamos la respuesta
        ResponseEntity<String> response = restTemplate.postForEntity(n8nWebhookUrl, requestEntity, String.class);

        // Devolvemos el cuerpo de la respuesta, que debería ser el JSON de la IA
        return response.getBody();
    }
}