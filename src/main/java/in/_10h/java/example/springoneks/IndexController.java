package in._10h.java.example.springoneks;

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

@RestController
@RequestMapping("/")
public class IndexController {

    private final AWSSecurityTokenServiceAsync sts;
    private final AmazonEC2Async ec2;
    private final AmazonS3 s3sync;

    @Autowired
    public IndexController() {
        this.sts = AWSSecurityTokenServiceAsyncClientBuilder.defaultClient();
        this.ec2 = AmazonEC2AsyncClientBuilder.defaultClient();
        this.s3sync = AmazonS3ClientBuilder.defaultClient();
    }

    @GetMapping("")
    public Mono<String> index() {
        return Mono.just("Hello World!");
    }

    @GetMapping("caller-identity")
    public Mono<GetCallerIdentityResult> getCallerIdentity() {
        final GetCallerIdentityRequest req = new GetCallerIdentityRequest();
        final Future<GetCallerIdentityResult> future = this.sts.getCallerIdentityAsync(req);
        return Mono.fromSupplier(() -> {
            try {
                return future.get();
            } catch (Throwable t) {
                throw new InternalError(t);
            }
        });
    }

    @GetMapping("instances")
    public Flux<String> listInstances() {
        final DescribeInstancesPager pager = new DescribeInstancesPager(this.ec2);
        return Flux.generate(pager)
            .flatMapIterable(DescribeInstancesResult::getReservations)
            .flatMapIterable(Reservation::getInstances)
            .map(Instance::getInstanceId);
    }

    class DescribeInstancesPager implements Consumer<SynchronousSink<DescribeInstancesResult>> {
        private Optional<DescribeInstancesResult> previousResult = Optional.empty();
        private AmazonEC2Async ec2;
        public DescribeInstancesPager(final AmazonEC2Async ec2) {
            this.ec2 = ec2;
        }
        @Override
        public void accept(final SynchronousSink<DescribeInstancesResult> sink) {
            if (this.previousResult.isEmpty()) {
                final DescribeInstancesResult result = this.ec2.describeInstances();
                this.previousResult = Optional.of(result);
                sink.next(result);
                if (result.getNextToken() == null) {
                    sink.complete();
                }
                return;
            }

            final DescribeInstancesResult previous = this.previousResult.get();
            final String nextToken = previous.getNextToken();
            if (nextToken == null) {
                sink.error(new InternalError("nextToken == null, but called next."));
                return;
            }

            final DescribeInstancesRequest req = new DescribeInstancesRequest();
            req.setNextToken(nextToken);

            final DescribeInstancesResult nextResult = this.ec2.describeInstances(req);
            this.previousResult = Optional.of(nextResult);
            sink.next(nextResult);
            if (nextResult.getNextToken() == null) {
                sink.complete();
            }
        }
    }

    @GetMapping("buckets")
    public Flux<String> listBuckets() {
        return Mono.fromSupplier(this.s3sync::listBuckets)
            .flux()
            .flatMapIterable(Function.identity())
            .map(Bucket::getName);
    }

}
