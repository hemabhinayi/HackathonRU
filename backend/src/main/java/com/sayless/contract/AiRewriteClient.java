package com.sayless.contract;

import java.util.List;

public interface AiRewriteClient {

    List<String> generate(String normalizedMessage, Tone tone);
}
