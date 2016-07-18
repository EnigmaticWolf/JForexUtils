package com.jforex.programming.client.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jforex.programming.client.JFSystemListener;
import com.jforex.programming.client.StrategyRunData;
import com.jforex.programming.client.StrategyRunState;
import com.jforex.programming.connection.ConnectionState;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import rx.observers.TestSubscriber;

@RunWith(HierarchicalContextRunner.class)
public class JFSystemListenerTest {

    private JFSystemListener jfSystemListener;

    private final TestSubscriber<StrategyRunData> runDataSubscriber =
            new TestSubscriber<>();
    private final TestSubscriber<ConnectionState> connectionStateSubscriber =
            new TestSubscriber<>();
    private final long processID = 42;

    @Before
    public void setUp() {
        jfSystemListener = new JFSystemListener();

        jfSystemListener.strategyRunDataObservable().subscribe(runDataSubscriber);
        jfSystemListener.connectionStateObservable().subscribe(connectionStateSubscriber);
    }

    private void assertSubscriber(final TestSubscriber<?> subscriber,
                                  final int itemIndex) {
        subscriber.assertNoErrors();
        subscriber.assertNotCompleted();
        subscriber.assertValueCount(itemIndex + 1);
    }

    public class WhenOnStart {

        private void assertRunData(final StrategyRunState runState,
                                   final int itemIndex) {
            assertSubscriber(runDataSubscriber, itemIndex);

            final StrategyRunData runData = runDataSubscriber.getOnNextEvents().get(itemIndex);
            assertThat(runData.processID(), equalTo(processID));
            assertThat(runData.state(), equalTo(runState));
        }

        @Before
        public void setUp() {
            jfSystemListener.onStart(processID);
        }

        @Test
        public void onStartRunDataIsPublishedCorrect() {
            assertRunData(StrategyRunState.STARTED, 0);
        }

        public class WhenOnStop {

            @Before
            public void setUp() {
                jfSystemListener.onStop(processID);
            }

            @Test
            public void onStopRunDataIsPublishedCorrect() {
                assertRunData(StrategyRunState.STOPPED, 1);
            }
        }
    }

    public class WhenOnConnect {

        private void assertConnectionState(final ConnectionState connectionState,
                                           final int itemIndex) {
            assertSubscriber(connectionStateSubscriber, itemIndex);

            assertThat(connectionStateSubscriber.getOnNextEvents().get(itemIndex),
                       equalTo(connectionState));
        }

        @Before
        public void setUp() {
            jfSystemListener.onConnect();
        }

        @Test
        public void onConnectIsPublishedCorrect() {
            assertConnectionState(ConnectionState.CONNECTED, 0);
        }

        public class WhenOnDisconnect {

            @Before
            public void setUp() {
                jfSystemListener.onDisconnect();
            }

            @Test
            public void onDisConnectIsPublishedCorrect() {
                assertConnectionState(ConnectionState.DISCONNECTED, 1);
            }
        }
    }
}