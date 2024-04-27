package io.esper.android.files.util;

import android.os.Bundle;

interface IRemoteCallback {
    void sendResult(in Bundle result);
}
