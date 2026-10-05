/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.transport;

// This file contains unique, previously uncovered mocking and stubbing cases
// identified by comparing it with the normal TransportHandshakerTests file.

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.network.CloseableChannel;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.crate.common.unit.TimeValue;
import io.crate.concurrent.FutureActionListener;
import io.netty.channel.embedded.EmbeddedChannel;

public class TransportHandshakerMockStubTests extends ESTestCase {

    private TransportHandshaker handshaker;
    private DiscoveryNode node;
    private CloseableChannel channel;
    private TestThreadPool threadPool;
    private TransportHandshaker.HandshakeRequestSender requestSender;

    // This setup matches the existing TransportHandshakerTests setup and is modeled after it.
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        String nodeId = "node-id";
        channel = new CloseableChannel(new EmbeddedChannel(), false);
        requestSender = mock(TransportHandshaker.HandshakeRequestSender.class);
        node = new DiscoveryNode(
            nodeId,
            nodeId,
            nodeId,
            "host",
            "host_address",
            buildNewFakeTransportAddress(),
            Collections.emptyMap(),
            Collections.emptySet(),
            Version.CURRENT
        );
        threadPool = new TestThreadPool("thread-pool");
        handshaker = new TransportHandshaker(Version.CURRENT, threadPool, requestSender);
    }

    // Matches TransportHandshakerTests teardown.
    @Override
    @After
    public void tearDown() throws Exception {
        threadPool.shutdown();
        super.tearDown();
    }

    // This test models a socket closing while a handshake is still pending.
    // The request sender is mocked so the test does not send data over a real
    // network connection. No explicit stubbing is needed here: Mockito's
    // default behavior for this void method is to do nothing, which represents
    // a successful request send. The test then closes the embedded channel and
    // verifies that the handshaker reports the connection-reset failure and
    // removes the pending handshake. The verify call confirms that the mocked
    // sender was used to start the handshake.
    @Test
    public void testHandshakeFailsWhenChannelCloses() throws Exception {
        FutureActionListener<Version> versionFuture = new FutureActionListener<>();
        long requestId = randomLongBetween(1, 10);

        handshaker.sendHandshake(
            requestId,
            node,
            channel,
            new TimeValue(30, TimeUnit.SECONDS),
            versionFuture
        );

        verify(requestSender).sendRequest(node, channel, requestId, Version.CURRENT);
        assertThat(handshaker.getNumPendingHandshakes()).isEqualTo(1);

        channel.close();

        assertThatThrownBy(versionFuture::get)
            .cause()
            .isExactlyInstanceOf(TransportException.class)
            .hasMessageContaining("handshake failed because connection reset");
        assertThat(handshaker.removeHandlerForHandshake(requestId)).isNull();
    }

    // Unlike testHandshakeFailsWhenChannelCloses, which closes the channel
    // after sendHandshake returns, this test stubs the sender to close it
    // during request dispatch. It covers the timing-specific case where the
    // connection disappears while the handshake request is being sent.
    @Test
    public void testHandshakeFailsIfChannelClosesWhileRequestIsSent() throws Exception {
        FutureActionListener<Version> versionFuture = new FutureActionListener<>();
        long requestId = randomLongBetween(1, 10);

        doAnswer(invocation -> {
            channel.close();
            return null;
        }).when(requestSender).sendRequest(node, channel, requestId, Version.CURRENT);

        handshaker.sendHandshake(
            requestId,
            node,
            channel,
            new TimeValue(30, TimeUnit.SECONDS),
            versionFuture
        );

        assertThatThrownBy(versionFuture::get)
            .cause()
            .isExactlyInstanceOf(TransportException.class)
            .hasMessageContaining("handshake failed because connection reset");
        assertThat(handshaker.removeHandlerForHandshake(requestId)).isNull();
    }
}
