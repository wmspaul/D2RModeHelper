package com.ransom.d2r.objects;

public interface ProcessRunner {
    void run(Process inProgress);
    void onFinish(int exitCode);
}
