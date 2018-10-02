package data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import api.Payment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Payment> loadPreviousPayments() {
        try {
            final String content = new String(
                    Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("previous_payments.json").toURI())));

            final List<Payment> payments = mapper.reader()
                    .forType(new TypeReference<List<Payment>>() {})
                    .readValue(content);

            return payments;

        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
