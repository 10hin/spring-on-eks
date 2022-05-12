package in._10h.java.example.springoneks;

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class IndexController {

    private final AWSSecurityTokenServiceAsync sts;

    @Autowired
    public IndexController() {
        this.sts = AWSSecurityTokenServiceAsyncClientBuilder.defaultClient();
    }

    @GetMapping("")
    public Mono<String> index() {
        return Mono.just("Hello World!");
    }

    @GetMapping("caller-identity")
    public Mono<GetCallerIdentityResult> getCallerIdentity() {
        final GetCallerIdentityRequest req = new GetCallerIdentityRequest();
        final GetCallerIdentityResult res = this.sts.getCallerIdentity(req);
        return Mono.just(res);
    }
}
