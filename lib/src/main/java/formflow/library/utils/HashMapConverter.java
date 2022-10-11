package formflow.library.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        String stringInfo = null;

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            stringInfo = objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            System.out.println("Error serializing data (map -> JSON): " + e.getMessage());
        }
        return stringInfo;
    }

    @Override
    public HashMap<String, Object> convertToEntityAttribute(String dbData) {

        HashMap<String, Object> info = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            info = objectMapper.readValue(dbData, HashMap.class);
        } catch (IOException e) {
            System.out.println("Unable to deserialized data from db: " + e.getMessage());
        }

        return info;
    }
}
