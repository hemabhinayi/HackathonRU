package com.sayless.contract;

import java.util.List;

public interface RewriteService {

    List<String> rewrite(String message, String tone);
}
