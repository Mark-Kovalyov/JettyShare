package mayton.web;

import java.util.Optional;

public final class HttpRequestRange {

    public Optional<Long> from;
    public Optional<Long> to;

    public HttpRequestRange(Optional<Long> from, Optional<Long> to) {
        this.from = from;
        this.to = to;
    }

    public Optional<Long> getLength() {
        if (from.isPresent() && to.isPresent()) {
            return Optional.of(to.get() - from.get());
        } else {
            return Optional.empty();
        }
    }
}
