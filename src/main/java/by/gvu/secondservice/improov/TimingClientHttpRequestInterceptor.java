package by.gvu.secondservice.improov;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final String HEADER_TIME_START = "time-start";
    private static final String HEADER_TIME_END = "time-end";

    // Используем ConcurrentHashMap для потокобезопасного хранения данных
    private final ConcurrentHashMap<String, Long> requestTimes = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String requestUri = request.getURI().toString();

        // Записываем время начала запроса
        long localTimeStart = System.nanoTime();
        requestTimes.put(requestUri + "-local-start", localTimeStart);

        ClientHttpResponse response = execution.execute(request, body);

        // Записываем время окончания запроса
        long localTimeEnd = System.nanoTime();
        requestTimes.put(requestUri + "-local-end", localTimeEnd);

        // Получаем значения времени из заголовков ответа
        long remoteTimeStart = getHeaderAsLong(response, HEADER_TIME_START);
        long remoteTimeEnd = getHeaderAsLong(response, HEADER_TIME_END);

        // Записываем время из заголовков в потокобезопасную карту
        requestTimes.put(requestUri + "-remote-start", remoteTimeStart);
        requestTimes.put(requestUri + "-remote-end", remoteTimeEnd);

        computeAndLogMetrics(requestUri);

        return response;
    }

    private long getHeaderAsLong(ClientHttpResponse response, String headerName) {
        if (response.getHeaders().containsKey(headerName)) {
            String headerValue = response.getHeaders().getFirst(headerName);
            return Long.parseLong(headerValue);
        }
        return 0;
    }

    private void computeAndLogMetrics(String requestUri) {
        Long localStart = requestTimes.get(requestUri + "-local-start");
        Long localEnd = requestTimes.get(requestUri + "-local-end");
        Long remoteStart = requestTimes.get(requestUri + "-remote-start");
        Long remoteEnd = requestTimes.get(requestUri + "-remote-end");

        if (localStart != null && localEnd != null && remoteStart != null && remoteEnd != null) {
            long localDuration = localEnd - localStart;
            long remoteDuration = remoteEnd - remoteStart;
            long totalLatency = localEnd - remoteStart; // или localEnd - localStart + remoteEnd - remoteStart в зависимости от вашей метрики

            // Логирование
            log.info("localDuration: {}", localDuration);
            log.info("remoteDuration: {}", remoteDuration);
            log.info("totalLatency: {}", totalLatency);

            // Сбор метрик
            Timer.builder("http.client.requests")
                    .tag("uri", requestUri)
                    .tag("status", String.valueOf(200)) // Статус можно сделать более универсальным
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(totalLatency / 1_000_000, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
