/*
 * Copyright (c) 2018, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.panteleyev.money.ssh;

import org.testng.annotations.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import static org.panteleyev.money.BaseTestUtils.randomPort;
import static org.panteleyev.money.BaseTestUtils.randomString;
import static org.testng.Assert.assertEquals;

public class TestSshManager {
    private static SshSession newSshSession() {
        return new SshSession(randomString(), randomString(), randomPort(),
                randomString(), randomString(), randomPort());
    }

    private static final List<SshSession> SESSIONS = List.of(
            newSshSession(), newSshSession(), newSshSession(), newSshSession()).stream()
            .sorted(Comparator.comparing(SshSession::getName)).collect(Collectors.toList());

    @Test
    public void testSaveLoad() throws Exception {
        SshManager.setSessions(SESSIONS);

        try (var out = new ByteArrayOutputStream()) {
            SshManager.saveSessions(out);

            try (var in = new ByteArrayInputStream(out.toByteArray())) {
                SshManager.setSessions(List.of());

                SshManager.loadSessions(in);

                assertEquals(SshManager.getSessions().stream().sorted(Comparator.comparing(SshSession::getName))
                        .collect(Collectors.toList()), SESSIONS);
            }
        }
    }
}