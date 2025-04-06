package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
@SpringBootApplication
public class Main {

    private static String auth = "ADM:O0Ju|yR?pG"; // Логин и пароль

    @Value("${login}")
    private String login;
    @Value("${pass}")
    private String pass;
    private String authInfo = login + ":" + pass;
    private static String encodedAuth;


    private static final String refKey = "170b86b0-ff3f-11ef-ab81-bb3bb1425b1c";
    private static final String propertyKey = "44a5416b-6f3a-11ee-ab59-fdf03c13e17b";
    private static final String newValue = "9999";

    @PostConstruct
    public void init() {

        this.authInfo = login + ":" + pass;
        this.encodedAuth = Base64.getEncoder().encodeToString(authInfo.getBytes(StandardCharsets.UTF_8));
    }


    public static void main(String[] args) throws IOException {


        String json = getRequest();
        patchRequest(json);



    }

    // Выполняем PATCH запрос к 1С
    public static void patchRequest(String json) throws IOException {
        String documentType = URLEncoder.encode("ЗаказПокупателя", StandardCharsets.UTF_8);
        String baseUrl = "https://1c.svs-tech.pro/UNF/odata/standard.odata/Document_" +
                documentType + "(guid'" + refKey + "')";
        baseUrl = baseUrl.replaceAll(" ", "%20").replaceAll("'", "%27");
        System.out.println("Patch Request URL: " + baseUrl);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(baseUrl);

            // Установка заголовков
            httpPatch.setHeader("Authorization", "Basic " + encodedAuth);
            httpPatch.setHeader("Content-Type", "application/json");
            httpPatch.setHeader("Accept", "application/json");

            // Полное тело запроса со всеми табличными частями
            String jsonInputString = "{\"ДополнительныеРеквизиты\": " + json + "}";

            httpPatch.setEntity(new StringEntity(jsonInputString, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Response Code: " + statusCode);
                //System.out.println("Response Body: " + responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    System.out.println("Update successful");
                    // Проверьте в 1С, что данные обновились
                } else {
                    System.out.println("Update failed");
                }
            }
        }
    }

    // Выполняем GET запрос к 1С
    public static String getRequest() throws IOException {
        String responseBody = "";
        String number = "НФНФ-003502";
        encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String documentType = URLEncoder.encode("ЗаказПокупателя", StandardCharsets.UTF_8);
        String refKey = "170b86b0-ff3f-11ef-ab81-bb3bb1425b1c";
        String baseUrl = "https://1c.svs-tech.pro/UNF/odata/standard.odata/Document_" +
                documentType + "?$filter=Number eq '" + number + "'&$format=json";
        baseUrl = baseUrl.replaceAll(" ", "%20").replaceAll("'", "%27");
        System.out.println("Get Request URL: " + baseUrl);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(baseUrl);

            // Установка заголовков
            httpGet.setHeader("Authorization", "Basic " + encodedAuth);
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setHeader("Accept", "application/json");


            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                responseBody = EntityUtils.toString(response.getEntity());

                //System.out.println("Response Code: " + statusCode);
                //System.out.println("Response Body: " + responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    System.out.println("Response successful");
                    // Проверьте в 1С, что данные обновились
                } else {
                    System.out.println("Response failed");
                }
            }
        }
        responseBody = parse(responseBody);

        return responseBody;
    }

    // Парсим запрос, получаем текущие значения массива ДополнительныеРеквизиты, заменяем значение поля "Значение" для propertyKey
    public static String parse(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootArray = objectMapper.readTree(responseBody);
        JsonNode valueArray = rootArray.get("value");
        JsonNode typeArray = null;
        for (JsonNode value : valueArray) {
            typeArray = value;
        }
        ArrayList<JsonNode> arrayList = new ArrayList<>();
        ArrayNode additionalProps = (ArrayNode) typeArray.get("ДополнительныеРеквизиты");

        for (JsonNode additionalProp : additionalProps) {
            ObjectNode propNode = additionalProp.deepCopy();
            if (additionalProp.path("Свойство_Key").asText().equals(propertyKey)) {
                propNode.put("Значение", newValue);
                arrayList.add(propNode);
            } else {
                arrayList.add(additionalProp);
            }
        }
        ArrayList<String> list = new ArrayList<>();

        for (JsonNode prop : arrayList) {
            list.add(prop.toString());

        }

        return list.toString();
    }
}