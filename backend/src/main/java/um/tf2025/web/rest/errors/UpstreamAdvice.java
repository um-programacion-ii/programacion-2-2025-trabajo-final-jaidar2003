package um.tf2025.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@ControllerAdvice
public class UpstreamAdvice {

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ProblemDetail> handle(WebClientRequestException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Upstream unavailable");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }
}
